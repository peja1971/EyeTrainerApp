import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  // --- local.properties (release signing) ---
  val localProps = Properties()
  val localPropsFile = rootProject.file("local.properties")
  if (localPropsFile.exists()) {
    localPropsFile.inputStream().run {
      localProps.load(this)
      close()
    }
  }

  val keystoreFilePath = localProps.getProperty("KEYSTORE_FILE")
  val keystorePass     = localProps.getProperty("KEYSTORE_PASSWORD")
  val keyAliasProp     = localProps.getProperty("KEY_ALIAS")
  val keyPass          = localProps.getProperty("KEY_PASSWORD")

  namespace = "com.peja.eyetrainer"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.peja.eyetrainer"
    minSdk = 24
    targetSdk = 35
    versionCode = 5
    versionName = "2.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      if (keystoreFilePath.isNullOrBlank())
        throw GradleException("Missing KEYSTORE_FILE in local.properties")
      if (keystorePass.isNullOrBlank())
        throw GradleException("Missing KEYSTORE_PASSWORD in local.properties")
      if (keyAliasProp.isNullOrBlank())
        throw GradleException("Missing KEY_ALIAS in local.properties")
      if (keyPass.isNullOrBlank())
        throw GradleException("Missing KEY_PASSWORD in local.properties")

      storeFile = rootProject.file(keystoreFilePath)
      storePassword = keystorePass
      keyAlias = keyAliasProp
      keyPassword = keyPass
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("release")

      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlinOptions {
    jvmTarget = "11"
  }

  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)

  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  implementation("com.google.android.material:material:1.12.0")
}
