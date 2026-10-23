import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val appConfig = JsonSlurper().parse(rootProject.file("app-config.json")) as Map<*, *>
val versionConfig = JsonSlurper().parse(rootProject.file("android-version.json")) as Map<*, *>
val sdkConfig = Properties().apply {
    rootProject.file("android-sdk.properties").inputStream().use { load(it) }
}

android {
    namespace = "com.actionanand.carechime.app"
    compileSdk = sdkConfig.getProperty("COMPILE_SDK_VERSION").toInt()
    buildToolsVersion = sdkConfig.getProperty("BUILD_TOOLS_VERSION")

    defaultConfig {
        applicationId = appConfig["applicationId"] as String
        minSdk = sdkConfig.getProperty("MIN_SDK_VERSION").toInt()
        targetSdk = sdkConfig.getProperty("TARGET_SDK_VERSION").toInt()
        versionCode = (versionConfig["versionCode"] as Number).toInt()
        versionName = versionConfig["versionName"] as String
        resValue("string", "app_name", appConfig["appName"] as String)
        resValue("string", "app_tagline", appConfig["tagline"] as String)
    }

    buildFeatures {
        compose = true
        resValues = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Robolectric-backed Room tests run as local JVM unit tests in CI.
            isIncludeAndroidResources = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false // Enable after real app code and keep rules exist.
            isShrinkResources = false
        }
    }
}

// Exported Room schemas are committed under app/schemas and are the migration test baseline.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
