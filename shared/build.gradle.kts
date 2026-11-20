import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    android {
        namespace = "com.horsesgo.shared"
        compileSdk = 36
        minSdk = 23
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        androidResources.enable = true
        withHostTest {}
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "HorsesGoShared"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.resources)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.coroutines.core)
            implementation(libs.serialization.json)
            implementation(libs.mmkv)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }
    }
}

compose.resources {
    packageOfResClass = "com.horsesgo.resources"
}
