import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.playServicesAuth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

val sequoEnvironment = providers.gradleProperty("sequoEnvironment").orElse("PRODUCTION").get()
val sequoReleaseId = providers.gradleProperty("sequoReleaseId").orNull
val sequoAcceptance = sequoEnvironment != "PRODUCTION"

android {
    namespace = "dev.orestegabo.sequorider"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.orestegabo.sequorider"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["sequoAppLabel"] = if (sequoAcceptance) "SequoRider [$sequoEnvironment]" else "SequoRider"
        manifestPlaceholders["sequoCleartextAllowed"] = "false"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            if (sequoAcceptance) {
                applicationIdSuffix = if (sequoEnvironment == "LOCAL_FIXTURE") ".fixture" else ".staging"
                versionNameSuffix = "-${sequoReleaseId ?: "acceptance"}"
            }
            manifestPlaceholders["sequoCleartextAllowed"] = (sequoEnvironment == "LOCAL_FIXTURE").toString()
        }
        release {
            isMinifyEnabled = false
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
    buildFeatures {
        compose = true
    }
}
