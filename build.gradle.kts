plugins {
    alias(libs.plugins.ksp)
    id("buildsrc.convention.kotlin-jvm")
    application
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
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

sourceSets {
    main {
        kotlin.srcDir("app/src/main/kotlin")
        resources.srcDir("app/src/main/resources")
    }
    test {
        kotlin.srcDir("app/src/test/kotlin")
        resources.srcDir("app/src/test/resources")
    }
}

dependencies {
    implementation(libs.clikt)

    implementation(libs.slf4jApi)
    runtimeOnly(libs.logbackClassic)
    ksp(libs.kotlinInjectCompiler)
    implementation(libs.kotlinInjectRuntime)

    implementation(swtDependency())
    implementation(libs.jna)

    implementation(platform(libs.lwjglBom))
    implementation(libs.lwjgl)
    implementation(libs.lwjglGlfw)
    implementation(libs.lwjglOpenal)

    // MacOsX
    runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-macos") })
    runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-macos") })
    runtimeOnly(variantOf(libs.lwjglOpenal) { classifier("natives-macos") })
    runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-macos-arm64") })
    runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-macos-arm64") })
    runtimeOnly(variantOf(libs.lwjglOpenal) { classifier("natives-macos-arm64") })

    // Linux
    runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-linux") })
    runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-linux") })
    runtimeOnly(variantOf(libs.lwjglOpenal) { classifier("natives-linux") })
    runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-linux-arm64") })
    runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-linux-arm64") })
    runtimeOnly(variantOf(libs.lwjglOpenal) { classifier("natives-linux-arm64") })

    // Windows
    runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-windows") })
    runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-windows") })
    runtimeOnly(variantOf(libs.lwjglOpenal) { classifier("natives-windows") })

    testImplementation(libs.kotlinTest)
}

application {
    mainClass = "app.MainKt"
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread", "-Xdock:name=CartridgeVM NES")
    }
}

tasks.named<JavaExec>("run") {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        jvmArgs("-XstartOnFirstThread")
        jvmArgs("-Xdock:name=CartridgeVM NES")
    }
}
