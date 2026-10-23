import java.util.Properties
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        FileInputStream(versionPropsFile).use { load(it) }
    } else {
        setProperty("VERSION_CODE", "30")
        setProperty("VERSION_NAME", "1.0.30")
    }
}
val currentVersionCode = (versionProps.getProperty("VERSION_CODE") ?: "30").toInt()
val currentVersionName = versionProps.getProperty("VERSION_NAME") ?: "1.0.30"

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.foodtrack.mxpt"
    minSdk = 26
    targetSdk = 36
    versionCode = currentVersionCode
    versionName = currentVersionName

    ndk {
      abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
    }

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.play.services.code.scanner)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.text.google.fonts)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.androidx.health.connect)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("bumpVersion") {
    description = "Increments VERSION_CODE and updates VERSION_NAME in version.properties and app-update.json"
    group = "versioning"
    notCompatibleWithConfigurationCache("Modifies project version files dynamically")
    doLast {
        val propsFile = rootProject.file("version.properties")
        val props = Properties()
        if (propsFile.exists()) {
            FileInputStream(propsFile).use { props.load(it) }
        }
        val oldCode = (props.getProperty("VERSION_CODE") ?: "30").toInt()
        val newCode = oldCode + 1
        val oldName = props.getProperty("VERSION_NAME") ?: "1.0.0"
        val parts = oldName.split(".")
        val major = parts.getOrNull(0) ?: "1"
        val minor = parts.getOrNull(1) ?: "0"
        val newName = "$major.$minor.$newCode"

        props.setProperty("VERSION_CODE", newCode.toString())
        props.setProperty("VERSION_NAME", newName)
        FileOutputStream(propsFile).use { props.store(it, "FoodLab Version Configuration") }
        println("SUCCESS: Bumped version to versionCode=$newCode, versionName=$newName")

        val updateJsonFile = rootProject.file("app-update.json")
        if (updateJsonFile.exists()) {
            val content = updateJsonFile.readText()
            val today = LocalDate.now().toString()
            val updated = content
                .replace(Regex("""\"versionCode\":\\s*\\d+"""), "\"versionCode\": $newCode")
                .replace(Regex("""\"versionName\":\\s*\"[^\"]+\""""), "\"versionName\": \"$newName\"")
                .replace(Regex("""\"releaseDate\":\\s*\"[^\"]+\""""), "\"releaseDate\": \"$today\"")
            updateJsonFile.writeText(updated)
            println("SUCCESS: Updated app-update.json with new version details.")
        }
    }
}
