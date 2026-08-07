import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.metro)
}

fun semVerToInt(version: String): String {
    val parts = version.split(".")

    require(parts.size == 3) { "Version must be in major.minor.patch format" }

    val major = parts[0].toInt()
    val minor = parts[1].toInt()
    val patch = parts[2].toInt()

    require(major in 0..99)
    require(minor in 0..99)
    require(patch in 0..99)

    return "%02d%02d%02d"
        .format(major, minor, patch)
        .toInt()
        .toString()
}

val version = "0.1.0"
val build = "indev"

project.version = listOfNotNull(version, build).joinToString(separator = "-")

val jvmToolchainVersion = providers.gradleProperty("jvmToolchainVersion").map(String::toInt).get()

val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()

val isArm64 = osArch == "aarch64" || osArch == "arm64"

val desktopPlatform = when {
    osName.contains("win") && isArm64 -> "windows-arm64"
    osName.contains("win") -> "windows-x64"

    osName.contains("mac") && isArm64 -> "macos-arm64"
    osName.contains("mac") -> "macos-x64"

    osName.contains("linux") && isArm64 -> "linux-arm64"
    osName.contains("linux") -> "linux-x64"

    else -> error("Unsupported desktop platform: os.name=$osName, os.arch=$osArch")
}

val lwjglNatives = when (desktopPlatform) {
    "windows-x64" -> "natives-windows"
    "windows-arm64" -> "natives-windows-arm64"
    "macos-x64" -> "natives-macos"
    "macos-arm64" -> "natives-macos-arm64"
    "linux-x64" -> "natives-linux"
    "linux-arm64" -> "natives-linux-arm64"
    else -> error("Unsupported desktop platform: $desktopPlatform")
}

kotlin {
    jvm()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "kassette.js"
            }
        }
        binaries.executable()
    }
    jvmToolchain(jvmToolchainVersion)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":nes"))
            implementation(libs.compose.foundation)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.kermit)
            implementation(libs.kotlinxCoroutinesCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlinTest)
            implementation(libs.kotlinxCoroutinesTest)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinxBrowser)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.clikt)
            implementation(libs.kotlinxCoroutinesSwing)
            implementation(libs.jinput)
            runtimeOnly(dependencies.variantOf(libs.jinput) { classifier("natives-all") })

            implementation(libs.lwjgl)
            runtimeOnly(
                dependencies.variantOf(libs.lwjgl) { classifier(lwjglNatives) }
            )

            implementation(libs.lwjglOpenal)
            runtimeOnly(
                dependencies.variantOf(libs.lwjglOpenal) { classifier(lwjglNatives) }
            )
        }
    }
}

compose.desktop {
    application {
        mainClass = "app.MainKt"
        nativeDistributions {
            val projectVersion = project.version as String
            packageName = "Kassette"
            modules("java.instrument", "java.management", "jdk.unsupported")

            macOS {
                iconFile.set(project.file("icons/kassette.icns"))
                val macVersion = semVerToInt(version)
                packageVersion = macVersion
                packageBuildVersion = macVersion
            }

            windows {
                packageVersion = projectVersion
                iconFile.set(project.file("icons/kassette.ico"))
            }

            linux {
                packageVersion = projectVersion
                iconFile.set(project.file("icons/kassette.png"))
            }

        }

        if (osName.contains("mac")) {
            jvmArgs += "-Xdock:name=Kassette"
        }
    }
}

tasks.register<Zip>("zipDesktopDistribution") {
    group = "distribution"
    description = "Creates a ZIP containing the desktop application image."
    dependsOn("createDistributable")

    from(layout.buildDirectory.dir("compose/binaries/main/app"))

    archiveBaseName.set("kassette")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set(desktopPlatform)

    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    includeEmptyDirs = false

    filesMatching("**/*.app/Contents/MacOS/*") {
        permissions {
            unix("rwxr-xr-x")
        }
    }

    filesMatching("**/bin/*") {
        permissions {
            unix("rwxr-xr-x")
        }
    }
}
