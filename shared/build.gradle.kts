import java.net.URI
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// All clients in this framework share one explicit build target. Defaults preserve normal builds.
val sequoOverrides = listOf("sequoApiBaseUrl", "sequoReleaseId", "sequoEnvironment")
    .associateWith { providers.gradleProperty(it).orNull }
require(sequoOverrides.values.all { it == null } || sequoOverrides.values.all { !it.isNullOrBlank() }) {
    "Supply sequoApiBaseUrl, sequoReleaseId and sequoEnvironment together."
}
val sequoApiBaseUrl = sequoOverrides["sequoApiBaseUrl"]?.trimEnd('/') ?: "https://api.sequoservice.com"
val sequoReleaseId = sequoOverrides["sequoReleaseId"] ?: "development"
val sequoEnvironment = sequoOverrides["sequoEnvironment"] ?: "PRODUCTION"
require(sequoEnvironment in setOf("PRODUCTION", "STAGING", "LOCAL_FIXTURE")) { "Unsupported sequoEnvironment." }
require(sequoReleaseId.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,79}"))) { "Invalid sequoReleaseId." }
val sequoTarget = runCatching { URI(sequoApiBaseUrl) }.getOrElse { error("Invalid sequoApiBaseUrl.") }
require(sequoTarget.host != null && sequoTarget.rawUserInfo == null && sequoTarget.rawQuery == null &&
    sequoTarget.rawFragment == null && sequoTarget.rawPath.orEmpty().isEmpty() &&
    (sequoTarget.port == -1 || sequoTarget.port in 1..65535)) { "API target must be an origin without credentials, path, query or fragment." }
val sequoLocalHost = sequoTarget.host.lowercase() in setOf("localhost", "127.0.0.1", "10.0.2.2", "[::1]", "::1")
require(if (sequoEnvironment == "LOCAL_FIXTURE") sequoLocalHost && sequoTarget.scheme in setOf("http", "https")
    else sequoTarget.scheme == "https" && !sequoLocalHost) { "LOCAL_FIXTURE requires a loopback/emulator target; other environments require nonlocal HTTPS." }
val generateSequoBuildConfig = tasks.register("generateSequoBuildConfig") {
    val apiBaseUrl = sequoApiBaseUrl
    val releaseId = sequoReleaseId
    val environment = sequoEnvironment
    val output = layout.buildDirectory.dir("generated/sequoBuildConfig/commonMain")
    inputs.property("apiBaseUrl", sequoApiBaseUrl)
    inputs.property("releaseId", sequoReleaseId)
    inputs.property("environment", sequoEnvironment)
    outputs.dir(output)
    doLast {
        val target = output.get().file("dev/orestegabo/sequorider/core/network/GeneratedSequoBuildConfig.kt").asFile
        target.parentFile.mkdirs()
        target.writeText("""
            package dev.orestegabo.sequorider.core.network

            internal object GeneratedSequoBuildConfig {
                const val ApiBaseUrl = "$apiBaseUrl"
                const val ReleaseId = "$releaseId"
                const val Environment = "$environment"
            }
        """.trimIndent() + "\n")
    }
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    android {
       namespace = "dev.orestegabo.sequorider.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        getByName("androidHostTest").dependencies {
            implementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.security.crypto)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// A task-backed source directory keeps Android and iOS on the same target.
kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(generateSequoBuildConfig)
