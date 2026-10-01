import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release signing: env vars on CI, or an untracked keystore.properties locally.
// Without either, release builds just come out unsigned.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(env: String, prop: String): String? =
    providers.environmentVariable(env).orNull ?: keystoreProps.getProperty(prop)

val releaseStoreFile = signingValue("REX_KEYSTORE", "storeFile")
val releaseStorePassword = signingValue("REX_KEYSTORE_PASSWORD", "storePassword")

android {
    namespace = "io.github.awakelol.rex"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.awakelol.rex"
        minSdk = 26
        targetSdk = 37
        versionCode = providers.gradleProperty("rexVersionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("rexVersionName").orNull ?: "0.1"
    }

    signingConfigs {
        if (releaseStoreFile != null && releaseStorePassword != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = "rex"
                keyPassword = releaseStorePassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
