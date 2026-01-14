@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val gitCommitCountProvider: Provider<Int> by extra(
    providers.exec {
        commandLine("git", "rev-list", "HEAD", "--count")
    }.standardOutput.asText.map { it.trim().toIntOrNull() ?: 0 }
)

val gitHashProvider: Provider<String> by extra(
    providers.exec {
        commandLine("git", "rev-parse", "--verify", "--short", "HEAD")
    }.standardOutput.asText.map { it.trim() }
)

android {
    namespace = "com.dashboard.kotlin"
    compileSdk = 36
    ndkVersion = "22.1.7171670"

    defaultConfig {
        applicationId = "com.dashboard.kotlin"
        minSdk = 30
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = gitCommitCountProvider.get()
        versionName = "5.5"
        versionNameSuffix = ".r${gitCommitCountProvider.get()}.${gitHashProvider.get()}"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags("-std=c++20")
                abiFilters("arm64-v8a")
            }
        }
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    applicationVariants.all {
        outputs.all {
            (this as BaseVariantOutputImpl).outputFileName =
                "${rootProject.name}-v${versionName}-${buildType.name}.apk"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfig ?: signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
            // proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin.compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
    externalNativeBuild {
        cmake {
            version = "3.18.1"
            path = file("./src/cpp/CMakeLists.txt")
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    // mmkv
    implementation(libs.mmkv.static)

    implementation(libs.okhttp)
    implementation(libs.gson)

    implementation(libs.bundles.libsu)
}
