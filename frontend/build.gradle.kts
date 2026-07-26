plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
}

val jvmToolchainVersion = providers.gradleProperty("jvmToolchainVersion").map(String::toInt).get()
val lwjglVersion = libs.versions.lwjgl.get()

kotlin {
    jvm()
    jvmToolchain(jvmToolchainVersion)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":nes"))
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kotlinTest)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.clikt)
            implementation(libs.kotlinInjectRuntime)
            implementation(libs.jinput)
            implementation(libs.lwjgl)
            implementation(libs.lwjglJawt)
            implementation(libs.lwjglOpenGl)
            implementation(libs.lwjglOpenal)
            implementation("org.lwjglx:lwjgl3-awt:${libs.versions.lwjgl3Awt.get()}") {
                exclude(group = "org.lwjgl")
            }
            runtimeOnly("net.java.jinput:jinput:${libs.versions.jinput.get()}:natives-all")
            runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-macos")
            runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-macos")
            runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:natives-macos")
            runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-macos-arm64")
            runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-macos-arm64")
            runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:natives-macos-arm64")
            runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-linux")
            runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-linux")
            runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:natives-linux")
            runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-linux-arm64")
            runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-linux-arm64")
            runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:natives-linux-arm64")
            runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-windows")
            runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-windows")
            runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:natives-windows")
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
            jvmArgs += "-Xdock:name=CartridgeVM NES"
        }
    }
}
