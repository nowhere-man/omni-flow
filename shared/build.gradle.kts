import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    jvm()
    val appleTargets = listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        macosX64(),
        macosArm64(),
    )
    appleTargets.forEach { target ->
        target.binaries.framework {
            baseName = "OmniFlowShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.coroutines.core)
            implementation(libs.datetime)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.serialization.json)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.jvm)
            implementation(libs.poi.ooxml)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android)
            implementation(libs.poi.ooxml)
        }
        appleMain.dependencies {
            implementation(libs.sqldelight.native)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.omniflow.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
}

sqldelight {
    databases {
        create("OmniFlowDatabase") {
            packageName.set("com.omniflow.shared.db")
        }
    }
}
