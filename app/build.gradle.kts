plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("app.cash.sqldelight") version "2.0.2"
    kotlin("plugin.serialization") version "2.0.20"
}

android {
    namespace = "com.example.spoilalert"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.spoilalert"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.exifinterface)
    val kotlin_version = "1.7.0"
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)

    //Camera
    implementation("androidx.camera:camera-camera2:1.1.0-alpha04")
    implementation("androidx.camera:camera-lifecycle:1.1.0-alpha04")
    implementation("androidx.camera:camera-view:1.0.0-alpha21")

    //BitmapDL
    implementation("com.github.bumptech.glide:glide:4.4.0")

    // QR
    implementation("com.google.android.gms:play-services-vision:20.1.3")

    // SQL
    implementation("app.cash.sqldelight:android-driver:2.0.2")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.klaxon)

    //openfoods
    val ktor_version = "2.1.0"
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-android:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
    implementation("io.ktor:ktor-http:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.example.spoilalert")
        }
    }
}