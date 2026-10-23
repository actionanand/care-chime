import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
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

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation("junit:junit:4.13.2")
}
