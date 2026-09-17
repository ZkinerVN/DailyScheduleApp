plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.dailyscheduleapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.dailyscheduleapp"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
        // UI & Material Components
        implementation("com.google.android.material:material:1.12.0")
        implementation("androidx.appcompat:appcompat:1.7.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")

        // Architecture Components: ViewModel & LiveData
        implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.4")
        implementation("androidx.lifecycle:lifecycle-livedata:2.8.4")

        // Room Database
        val roomVersion = "2.6.1"
        implementation("androidx.room:room-runtime:$roomVersion")
        annotationProcessor("androidx.room:room-compiler:$roomVersion")

        // Thư viện vẽ biểu đồ
        implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    }
