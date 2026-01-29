plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.u_judge_client.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.u_judge_client"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

configurations.all {
    resolutionStrategy {
        force("com.google.protobuf:protobuf-kotlin:4.33.4")
        force("com.google.protobuf:protobuf-java:4.33.4")
        force("com.google.protobuf:protobuf-java-util:4.33.4")
    }
}

dependencies {
    implementation(project(":composeApp"))
}