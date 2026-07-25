plugins {
    alias(libs.plugins.ksp)
    id("buildsrc.convention.kotlin-jvm")
    application
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

    implementation(platform(libs.lwjglBom))
    implementation(libs.bundles.lwjgl)

    // MacOsX
    runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-macos") })
    runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-macos") })
    runtimeOnly(variantOf(libs.lwjglOpenal) { classifier("natives-macos") })
    runtimeOnly(variantOf(libs.lwjglOpengl) { classifier("natives-macos") })
    runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-macos-arm64") })
    runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-macos-arm64") })
    runtimeOnly(variantOf(libs.lwjglOpenal) { classifier("natives-macos-arm64") })
    runtimeOnly(variantOf(libs.lwjglOpengl) { classifier("natives-macos-arm64") })

    // Linux
    runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-linux") })
    runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-linux") })
    runtimeOnly(variantOf(libs.lwjglOpenal) { classifier("natives-linux") })
    runtimeOnly(variantOf(libs.lwjglOpengl) { classifier("natives-linux") })
    runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-linux-arm64") })
    runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-linux-arm64") })
    runtimeOnly(variantOf(libs.lwjglOpenal) { classifier("natives-linux-arm64") })
    runtimeOnly(variantOf(libs.lwjglOpengl) { classifier("natives-linux-arm64") })

    // Windows
    runtimeOnly(variantOf(libs.lwjgl) { classifier("natives-windows") })
    runtimeOnly(variantOf(libs.lwjglGlfw) { classifier("natives-windows") })
    runtimeOnly(variantOf(libs.lwjglOpenal) { classifier("natives-windows") })
    runtimeOnly(variantOf(libs.lwjglOpengl) { classifier("natives-windows") })

    testImplementation(libs.kotlinTest)
}

application {
    mainClass = "app.MainKt"
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
    }
}

tasks.named<JavaExec>("run") {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        jvmArgs("-XstartOnFirstThread")
    }
}
