plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.emotion.detection"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.emotion.detection"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = false
    }

    // ✅ Modern replacement for deprecated aaptOptions
    androidResources {
        noCompress += "tflite"
    }

    // ✅ 16 KB alignment–compliant packaging setup
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // Modern setting: use proper alignment for 16 KB page size
            useLegacyPackaging = false
            keepDebugSymbols += listOf("**/*.so")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // TensorFlow Lite runtime
    implementation("org.tensorflow:tensorflow-lite:2.17.0")

    // ✅ Add this line for GPU acceleration
    implementation("org.tensorflow:tensorflow-lite-gpu:2.17.0")
    // TensorFlow Lite GPU runtime and API
    implementation("org.tensorflow:tensorflow-lite-gpu:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.17.0")


    // TensorFlow Lite metadata
    implementation(libs.tensorflow.lite.metadata)

    // UI
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ML Kit Face Detection
    implementation("com.google.mlkit:face-detection:16.1.6")

    // CameraX
    val cameraVersion = "1.2.3"
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing (optional)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
