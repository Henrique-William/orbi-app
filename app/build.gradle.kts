import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY")
    ?: localProperties.getProperty("GOOGLE_MAPS_API_KEY")
    ?: System.getenv("MAPS_API_KEY")
    ?: System.getenv("GOOGLE_MAPS_API_KEY")
    ?: ""

android {
    namespace = "com.orbi.orbi_app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.orbi.orbi_app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "google_maps_key", mapsApiKey)
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildFeatures {
        resValues = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.jvmArgs(
                    "-XX:+EnableDynamicAgentLoading",
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED"
                )
            }
        }
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.swiperefreshlayout)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.osmdroid.android)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.mockito.core)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}