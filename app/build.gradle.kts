import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
}

val gitCommitId = rootProject.extra["gitCommitId"] as String
val gitCommitCount = rootProject.extra["gitCommitCount"] as Int

android {
    namespace = "io.github.sheikhmehraann.onepeek"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.sheikhmehraann.onepeek"
        minSdk = 26
        targetSdk = 36
        versionCode = gitCommitCount
        versionName = "1.$gitCommitCount"

        buildConfigField("String", "COMMIT_HASH", "\"$gitCommitId\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        val envKeystore = System.getenv("ONEPEEK_KEYSTORE_PATH")
            ?.takeIf { it.isNotBlank() }
            ?.let(::file)
            ?.takeIf { it.isFile }
        val defaultKeystore = file(System.getProperty("user.home") + "/.android/onepeek.keystore")
            .takeIf { it.isFile }
        val keystoreFile = envKeystore ?: defaultKeystore

        if (keystoreFile != null) {
            register("release") {
                storeFile = keystoreFile
                storePassword = System.getenv("ONEPEEK_STORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: "onepeek"
                keyAlias = System.getenv("ONEPEEK_KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "onepeek"
                keyPassword = System.getenv("ONEPEEK_KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: "onepeek"
            }
        }
    }

    buildTypes {
        getByName("release") {
            // Keep ProxyBillingActivity and BillingClient class names intact for host manifest resolution.
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "**"
            merges += "META-INF/xposed/*"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
    // Billing 5.2.1 uses API v17 PRODUCT_DETAILS and ProxyBillingActivity directly without v6/v7 proxy changes.
    implementation(libs.play.billing)
    testImplementation(libs.junit)
}
