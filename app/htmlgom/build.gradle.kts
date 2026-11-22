import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.absolutemango.htmlgom"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.absolutemango.htmlgom"
        minSdk = 33
        targetSdk = 37
        versionCode = 4
        versionName = "v2.0R - EOL"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.clear()
            abiFilters += listOf("arm64-v8a")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    flavorDimensions += listOf("edition")

    productFlavors {
        create("standard") {
            dimension = "edition"
        }
        create("lite") {
            dimension = "edition"
            versionNameSuffix = "-lite"
            applicationIdSuffix = ".lite"
        }
    }

    val localProperties = Properties().apply {
        val propFile = rootProject.file("local.properties")
        if (propFile.exists()) {
            propFile.inputStream().use { stream ->
                load(stream)
            }
        }
    }

    signingConfigs {
        create("release") {
            val storePath = localProperties.getProperty("RELEASE_STORE_FILE")
                ?: System.getenv("RELEASE_STORE_FILE")
            val storePass = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                ?: System.getenv("RELEASE_STORE_PASSWORD")
            val alias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                ?: System.getenv("RELEASE_KEY_ALIAS")
            val keyPass = localProperties.getProperty("RELEASE_KEY_PASSWORD")
                ?: System.getenv("RELEASE_KEY_PASSWORD")

            if (storePath != null && file(storePath).exists()) {
                storeFile = file(storePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile?.exists() == true) {
                releaseSigning
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xlambdas=class",
            "-Xcontext-parameters"
        )
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kyant.shapes)
    implementation(libs.haze)
    implementation(libs.haze.blur)
    implementation(libs.haze.glass)
    implementation(libs.compose.cloudy)
    implementation(project(":backdrop"))
    implementation(project(":editor"))
    implementation(project(":expressive-ui"))
    "standardImplementation"(libs.geckoview)
    implementation(libs.sora.editor.textmate)
    implementation(libs.sora.editor.monarch)
    implementation(libs.google.generativeai)
    implementation(libs.jlatexmath)
    implementation(libs.jlatexmath.font.greek)
    implementation(libs.jlatexmath.font.cyrillic)
    implementation(libs.markdown.parser)
    implementation(libs.play.services.wearable)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

val skipRustCoreProp = providers.gradleProperty("skipRustCore").orNull == "true"
val ndkHomeProp = providers.environmentVariable("ANDROID_NDK_HOME").orNull
    ?: providers.gradleProperty("android.ndkHome").orNull
    ?: "D:/sdk/ndk/26.1.10909125"

tasks.register<Exec>("buildRustCore") {
    workingDir = file("rust-core")
    inputs.dir(file("rust-core/src"))
    inputs.file(file("rust-core/Cargo.toml"))
    outputs.dir(file("src/main/jniLibs"))

    enabled = !skipRustCoreProp

    val cargoPath = File(System.getProperty("user.home"), ".cargo/bin/cargo.exe")
    val cargoExecutable = if (cargoPath.exists()) cargoPath.absolutePath else "cargo"

    environment("ANDROID_NDK_HOME", ndkHomeProp)

    val mingwBin = file("C:/Users/nahin/AppData/Local/Microsoft/WinGet/Packages/MartinStorsjo.LLVM-MinGW.UCRT_Microsoft.Winget.Source_8wekyb3d8bbwe/llvm-mingw-20261020-ucrt-x86_64/bin")
    val userCargoBin = File(System.getProperty("user.home"), ".cargo/bin")
    val currentPath = System.getenv("PATH") ?: ""
    val updatedPath = buildString {
        if (mingwBin.exists()) append("${mingwBin.absolutePath};")
        if (userCargoBin.exists()) append("${userCargoBin.absolutePath};")
        append(currentPath)
    }
    environment("PATH", updatedPath)

    commandLine(cargoExecutable, "ndk", "-t", "arm64-v8a", "-t", "x86_64", "-o", "../src/main/jniLibs", "build", "--release")
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }.configureEach {
    dependsOn("buildRustCore")
}
