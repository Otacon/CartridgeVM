plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
}

val jvmToolchainVersion = providers.gradleProperty("jvmToolchainVersion").map(String::toInt).get()

kotlin {
    jvm()
    jvmToolchain(jvmToolchainVersion)

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
            implementation(libs.kotlinInjectRuntime)
        }
        commonTest.dependencies {
            implementation(libs.kotlinTest)
        }
    }
}

dependencies {
    add("kspJvm", libs.kotlinInjectCompiler)
}
