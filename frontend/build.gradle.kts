import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.metro)
}

val jvmToolchainVersion = providers.gradleProperty("jvmToolchainVersion").map(String::toInt).get()
val lwjglVersion = libs.versions.lwjgl.get()

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
            runtimeOnly(dependencies.variantOf(libs.lwjgl) { classifier("natives-macos") })
            runtimeOnly(dependencies.variantOf(libs.lwjgl) { classifier("natives-macos-arm64") })
            runtimeOnly(dependencies.variantOf(libs.lwjgl) { classifier("natives-linux") })
            runtimeOnly(dependencies.variantOf(libs.lwjgl) { classifier("natives-linux-arm64") })
            runtimeOnly(dependencies.variantOf(libs.lwjgl) { classifier("natives-windows") })

            implementation(libs.lwjglOpenal)
            runtimeOnly(dependencies.variantOf(libs.lwjglOpenal) { classifier("natives-macos") })
            runtimeOnly(dependencies.variantOf(libs.lwjglOpenal) { classifier("natives-macos-arm64") })
            runtimeOnly(dependencies.variantOf(libs.lwjglOpenal) { classifier("natives-linux") })
            runtimeOnly(dependencies.variantOf(libs.lwjglOpenal) { classifier("natives-linux-arm64") })
            runtimeOnly(dependencies.variantOf(libs.lwjglOpenal) { classifier("natives-windows") })
        }
    }
}

compose.desktop {
    application {
        mainClass = "app.MainKt"
        if (System.getProperty("os.name").lowercase().contains("mac")) {
            jvmArgs += "-Xdock:name=Kassette"
        }
    }
}
