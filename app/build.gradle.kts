plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val appVersionName = providers.gradleProperty("versionName").orElse("0.0.0-dev")

android {
    namespace = "com.omniflow.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.omniflow.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = appVersionName.get()
    }

    sourceSets["main"].assets.srcDir(rootProject.file("assets"))

    if (providers.gradleProperty("androidArm64Only").isPresent) {
        splits {
            abi {
                isEnable = true
                reset()
                include("arm64-v8a")
                isUniversalApk = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.datetime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.biometric)
    // biometric 1.1.0 会把 fragment 拉回 1.2.5，其 FragmentActivity 仍校验 requestCode 只能用低 16 位，
    // 与 activity 1.12 的 ActivityResultRegistry（requestCode ≥ 0x10000）冲突，导致文件选择等启动即崩溃。
    implementation(libs.androidx.fragment)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
