import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.sqldelight)
}

android {
    namespace = "com.omniflow.core"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.datetime)
    implementation(libs.sqldelight.runtime)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.serialization.json)
    implementation(libs.sqldelight.android)
    implementation(libs.poi.ooxml)
    testImplementation(libs.sqldelight.jvm)
    testImplementation(libs.kotlin.test)
}

sqldelight {
    databases {
        create("OmniFlowDatabase") {
            packageName.set("com.omniflow.core.db")
        }
    }
}
