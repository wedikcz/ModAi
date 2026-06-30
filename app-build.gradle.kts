plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.automod.ai"
    compileSdk = 35
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.automod.ai"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-O3 -march=armv8.5-a+fp16+rcpc+dotprod+flagm -flto=thin -fvisibility=hidden -fomit-frame-pointer"
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
        aidl = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-Xcontext-receivers",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all"
        )
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.0"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,proguard/*.pro}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation-graphics")
    implementation("androidx.compose.runtime:runtime-livedata")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Koin DI
    implementation("io.insert-koin:koin-android:4.0.0")
    implementation("io.insert-koin:koin-androidx-compose:4.0.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Local modules
    implementation(project(":r2frida_bridge"))
    implementation(project(":r2ghidra_bridge"))
    implementation(project(":metacognitive_core"))
    implementation(project(":dynamic_analyzer"))
    implementation(project(":mod_builder"))
    implementation(project(":hack_menu_engine"))

    // Frida gadget injection
    implementation("com.github.frida:gadget:16.5.9")

    // R2 core
    implementation("io.radare:radare2:5.9.4")

    // APKTool wrapper
    implementation("com.android.tools.build:apksig:8.7.0")
    implementation("org.smali:smali:3.0.3")
    implementation("org.smali:baksmali:3.0.3")

    // Dex lib
    implementation("org.smali:dexlib2:3.0.3")

    // Signing
    implementation("org.bouncycastle:bcpkix-jdk18on:1.79")
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")

    // UI helpers
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
