@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    android {
        namespace = "org.mass"
        compileSdk = 37
        minSdk = 24

        withHostTest {}

        androidResources {
            enable = true
        }

        packaging {
            jniLibs {
                useLegacyPackaging = false
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xbinary=bundleId=org.u_judge_client.ComposeApp"
        )
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.annotation)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.android.pdfview)

            implementation(libs.compose.ui.tooling)
        }

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.preview)
            implementation(libs.ui.backhandler)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.uiToolingPreview)

            implementation(libs.navigation.compose)

            implementation(libs.dns.sd.kt)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
