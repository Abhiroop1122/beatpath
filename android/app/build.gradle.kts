plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.beatpath.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.beatpath.app"
        minSdk = 27
        targetSdk = 36
        versionCode = 9
        versionName = "1.2.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Sideload builds for GitHub Releases are signed with the local debug key so they install anywhere.
            // Before the Play Store, replace this with a real release keystore (see BUILD-PLAN.md §5).
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
