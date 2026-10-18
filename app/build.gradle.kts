import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

//
// Release signing. A real key is used when one is set up in keystore.properties at the
// repo root (storeFile / storePassword / keyAlias / keyPassword), which is what a build
// meant for other people should use. Without it the debug key signs the release too — that
// keeps the build installable over an existing debug install, session and all, instead of
// failing on a signature mismatch.
//
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.delidehset.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.delidehset.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.2.1"
        resourceConfigurations += listOf("tr", "en")
    }

    testOptions {
        // The JVM tests drive the repository and parser code against the live site, and
        // that code logs through android.util.Log. Without this, every logged line throws
        // "Method d in android.util.Log not mocked" instead of returning a default.
        unitTests.isReturnDefaultValues = true
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // Compose's runtime checks and the unshrunk dex are a large part of why a
            // debug build of this app stutters while scrolling and animating; so the
            // release build is the one that has to be fast: shrunk, optimised, signed.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = if (keystoreProperties.isNotEmpty()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    debugImplementation(libs.androidx.ui.tooling)

    // JVM tests so the request/parse pipeline can be checked against the live site.
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
