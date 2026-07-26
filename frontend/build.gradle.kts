import org.gradle.api.artifacts.MinimalExternalModuleDependency

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
}

fun swtDependency(): Provider<MinimalExternalModuleDependency> {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    return when {
        os.contains("mac") && arch == "aarch64" -> libs.swtMacosAarch64
        os.contains("mac") -> libs.swtMacosX64
        os.contains("linux") && arch == "aarch64" -> libs.swtLinuxAarch64
        os.contains("linux") -> libs.swtLinuxX64
        os.contains("win") && arch == "aarch64" -> libs.swtWindowsAarch64
        os.contains("win") -> libs.swtWindowsX64
        else -> error("Unsupported SWT platform: $os / $arch")
    }
}

val jvmToolchainVersion = providers.gradleProperty("jvmToolchainVersion").map(String::toInt).get()
val lwjglVersion = libs.versions.lwjgl.get()

kotlin {
    jvm()
    jvmToolchain(jvmToolchainVersion)

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":nes"))
            implementation(compose.desktop.currentOs)
            implementation(libs.clikt)
            implementation(libs.kermit)
            implementation(libs.kotlinInjectRuntime)
            implementation(swtDependency())
            implementation(libs.jna)
            implementation(libs.lwjgl)
            implementation(libs.lwjglGlfw)
            implementation(libs.lwjglOpenal)
            runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-macos")
            runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-macos")
            runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:natives-macos")
            runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-macos-arm64")
            runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-macos-arm64")
            runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:natives-macos-arm64")
            runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-linux")
            runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-linux")
            runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:natives-linux")
            runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-linux-arm64")
            runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-linux-arm64")
            runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:natives-linux-arm64")
            runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-windows")
            runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-windows")
            runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:natives-windows")
        }
        jvmTest.dependencies {
            implementation(libs.kotlinTest)
        }
    }
}

dependencies {
    add("kspJvm", libs.kotlinInjectCompiler)
}

compose.desktop {
    application {
        mainClass = "app.MainKt"
        if (System.getProperty("os.name").lowercase().contains("mac")) {
            jvmArgs += listOf("-XstartOnFirstThread", "-Xdock:name=CartridgeVM NES")
        }
    }
}
