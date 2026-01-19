@file:Suppress("UnstableApiUsage")

// Copyright 2024 anyone-Hub
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.chaquo.python")
    id("kotlinx-serialization")
    id("kotlin-parcelize")
}

android {
    namespace = "com.anyonehub.barpos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.anyonehub.barpos"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        vectorDrawables {
            useSupportLibrary = true
        }
    }
    
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "lib/**/libc++_shared.so"
        }
    }
    
    ndkVersion = "29.0.14206865"

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
            freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        }
    }
}

chaquopy {
    defaultConfig {
        version = "3.14"
        pip {
            install("simplejson")
            install("py-moneyed")
            install("pendulum")
            install("pydantic<2")
            install("babel")
            install("typing")
            install("googletrans==4.0.2")
        }
    }
}

dependencies {
    // --- CORE ANDROID COMPOSE ROOT ---
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.ui:ui:1.10.1")
    implementation("androidx.compose.ui:ui-graphics:1.10.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.10.1")
    implementation("androidx.compose.foundation:foundation:1.10.1")
    // Added for specific M3 icon sets and advanced adaptive layouts
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    // --- COMPOSE FOUNDATION & UI ---
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0")
    implementation("androidx.compose.material3.adaptive:adaptive:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
    // --- NAVIGATION (Pure Compose) ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation3:navigation3-runtime:1.0.0")
    implementation("androidx.navigation3:navigation3-ui:1.0.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    implementation("androidx.compose.material3.adaptive:adaptive:1.3.0-alpha06")
    implementation("androidx.compose.material3.adaptive:adaptive-layout:1.3.0-alpha06")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.5.0-alpha12")
    implementation("androidx.compose.animation:animation:1.10.1")
    implementation("androidx.compose.animation:animation-graphics:1.10.1")
    implementation("androidx.compose.foundation:foundation-layout:1.10.1")
    implementation("androidx.compose.foundation:foundation:1.10.1")
    // --- DATABASE & HILT ---
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    //     Hilt-Dagger
    implementation("com.google.dagger:hilt-android:2.58")
    ksp("com.google.dagger:hilt-android-compiler:2.58")
    // Removed implementation("androidx.hilt:hilt-compiler") as it causes R8 issues
    ksp("androidx.hilt:hilt-compiler:1.3.0")
    // --- WORKMANAGER ---
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    implementation("androidx.hilt:hilt-work:1.3.0")
    // --- SUPABASE & KTOR (Online-First Stack) ---
    implementation("io.github.jan-tennert.supabase:auth-kt:3.2.6")
    implementation("io.github.jan-tennert.supabase:realtime-kt:3.2.6")
    implementation("io.github.jan-tennert.supabase:functions-kt:3.2.6")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.2.6")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.2.6")
    implementation("io.github.jan-tennert.supabase:compose-auth:3.2.6")
    implementation("io.github.jan-tennert.supabase:compose-auth-ui:3.2.6")
    implementation("io.github.jan-tennert.supabase:apollo-graphql:3.2.6")
    //     Ktor
    implementation("io.ktor:ktor-client-core:3.3.3")
    implementation("io.ktor:ktor-client-okhttp:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
    implementation("io.ktor:ktor-client-logging:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
    // --- UTILS & TIME ---
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

hilt {
    enableAggregatingTask = true
}
