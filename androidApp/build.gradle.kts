plugins {
    id("com.android.application")
}

android {
    namespace = "org.u_judge_client.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.u_judge_client"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
    signingConfigs {
        // The pilot release key never lives in the repository: it comes from the environment of the build (I7).
        val keystore = providers.environmentVariable("UJUDGE_RELEASE_KEYSTORE").orNull
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = providers.environmentVariable("UJUDGE_RELEASE_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("UJUDGE_RELEASE_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("UJUDGE_RELEASE_KEY_PASSWORD").get()
            }
        }
    }
    buildTypes {
        getByName("release") {
            // Without the release key the minified APK is signed with the build machine's debug key, so it installs
            // for a smoke test; each CI runner has its own debug key, so such an APK cannot update another one.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":composeApp"))
}
