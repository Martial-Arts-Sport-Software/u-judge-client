@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)

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
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
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
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
