import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

fun runCommand(command: String): String {
    return try {
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine = command.split(" ")
            standardOutput = byteOut
        }
        byteOut.toString().trim()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

val gitCommitCount = runCommand("git rev-list HEAD --count").toIntOrNull() ?: 1
val gitCommitHash = runCommand("git rev-parse --verify --short HEAD")

android {
    namespace = "com.dashboard.kotlin"
    compileSdk = 36
    ndkVersion = "22.1.7171670"

    defaultConfig {
        applicationId = "com.dashboard.kotlin"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = gitCommitCount
        versionName = "5.5"
        versionNameSuffix = ".r${gitCommitCount}.${gitCommitHash}"
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
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "${rootProject.name}-v${versionName}-${buildType.name}.apk"
        }
    }

    buildTypes {
        release {
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
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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

    implementation(libs.libsu.core)
    implementation(libs.libsu.busybox)
}
