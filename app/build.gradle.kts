import java.util.Properties
import java.io.FileInputStream
import java.io.FileOutputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// ---------------------------------------------------------
// Dynamic Version Auto-Incrementer for Release Builds
// ---------------------------------------------------------
val versionPropsFile = file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    val fis = FileInputStream(versionPropsFile)
    versionProps.load(fis)
    fis.close()
} else {
    versionProps["versionCode"] = "1"
    versionProps["versionName"] = "1.0.0"
    val fos = FileOutputStream(versionPropsFile)
    versionProps.store(fos, "Initial version configuration")
    fos.close()
}

var vCode = versionProps["versionCode"].toString().toIntOrNull() ?: 1
var vName = versionProps["versionName"].toString()

// Check if the current gradle task contains "Release" to trigger version increment
val isReleaseBuild = project.gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
if (isReleaseBuild) {
    vCode += 1
    val parts = vName.split(".")
    vName = if (parts.size == 3) {
        val patch = (parts[2].toIntOrNull() ?: 0) + 1
        "${parts[0]}.${parts[1]}.$patch"
    } else {
        "1.0.$vCode"
    }
    // Store incremented version properties back to the properties file
    versionProps["versionCode"] = vCode.toString()
    versionProps["versionName"] = vName
    val fos = FileOutputStream(versionPropsFile)
    versionProps.store(fos, "Auto-incremented on release build")
    fos.close()
}

android {
    namespace = "com.suprogramuotavisata.markit"
    compileSdk = 36
    
    defaultConfig {
        applicationId = "com.suprogramuotavisata.com.suprogramuotavisata.markit" // Changed package/application identifier
        minSdk = 24
        targetSdk = 36
        versionCode = vCode
        versionName = vName
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

// Modern AGP 8.x API to customize the output APK filename using VariantOutputImpl
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val versionStr = output.versionName.get() ?: "1.0.0"
            val outputImpl = output as? com.android.build.api.variant.impl.VariantOutputImpl
            outputImpl?.outputFileName = "com.suprogramuotavisata.markit_$versionStr.apk"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // CameraX
  implementation("androidx.camera:camera-core:1.4.1")
  implementation("androidx.camera:camera-camera2:1.4.1")
  implementation("androidx.camera:camera-lifecycle:1.4.1")
  implementation("androidx.camera:camera-view:1.4.1")

  // Google Auth & Scanning
  implementation("com.google.android.gms:play-services-auth:21.2.0")
  implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
  implementation("com.google.mlkit:barcode-scanning:17.3.0")
  implementation("com.google.mlkit:text-recognition:16.0.1")

  // Android Printing
  implementation("androidx.print:print:1.0.0")

  // ZXing (Barcode Generation)
  implementation("com.google.zxing:core:3.5.3")

  // Coil (Image Loading)
  implementation("io.coil-kt:coil-compose:2.6.0")

  // Extended Material Icons
  implementation("androidx.compose.material:material-icons-extended")
  
  // Compose LiveData Integration
  implementation("androidx.compose.runtime:runtime-livedata")
}
