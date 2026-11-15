plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.paparazzi)
}


val appVersionName = providers.gradleProperty("versionName").orElse("0.0.0-dev")
val appVersionCode = providers.gradleProperty("versionCode").map { it.toInt() }.orElse(
    providers.provider {
        val name = appVersionName.get()
        val base = name.substringBefore('-').substringBefore('+')
        val parts = base.split('.')
        if (parts.size >= 3) {
            val major = parts[0].toIntOrNull() ?: 0
            val minor = parts[1].toIntOrNull() ?: 0
            val patch = parts[2].toIntOrNull() ?: 0
            maxOf(1, major * 10000 + minor * 100 + patch)
        } else {
            1
        }
    }
)

android {
    namespace = "com.omniflow.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.omniflow.android"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode.get()
        versionName = appVersionName.get()
    }

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
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.fragment)
    implementation(libs.okhttp)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.okhttp.mockwebserver)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
