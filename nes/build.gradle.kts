import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
}

val jvmToolchainVersion = providers.gradleProperty("jvmToolchainVersion").map(String::toInt).get()

kotlin {
    jvm()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    jvmToolchain(jvmToolchainVersion)
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
            implementation(libs.kotlinInjectRuntime)
            implementation(libs.kotlinxCoroutinesCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlinTest)
        }
    }
}

dependencies {
    add("kspJvm", libs.kotlinInjectCompiler)
    add("kspWasmJs", libs.kotlinInjectCompiler)
}
