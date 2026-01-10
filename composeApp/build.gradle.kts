import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val apiKey = localProperties.getProperty("API_KEY") ?: ""

// Task to copy local.properties to JVM resources
tasks.register("copyLocalProperties") {
    doLast {
        val resourcesDir = file("src/jvmMain/resources")
        resourcesDir.mkdirs()
        val targetFile = file("src/jvmMain/resources/local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.copyTo(targetFile, overwrite = true)
        }
    }
}

// Make sure local.properties is copied before JVM compilation
tasks.matching { it.name.contains("compileKotlinJvm") || it.name.contains("jvmProcessResources") }.configureEach {
    dependsOn("copyLocalProperties")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation("ai.koog:koog-agents:0.6.0")
//            implementation("ai.koog:prompt-executor-google-client-iossimulatorarm64:0.6.0")
//            implementation("ai.koog:koog-executor-google:0.5.4")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
//            // Koog AI
//            implementation(libs.koog.agents)
//            implementation(libs.koog.core)
//            implementation(libs.koog.google)
            
            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Voyager
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenModel)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)
            implementation(compose.materialIconsExtended)
            implementation("cafe.adriel.voyager:voyager-tab-navigator:1.0.0")

            // DateTime (required by Koog - using compat version for 0.6.x API)
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat")

            // Serialization
            implementation(libs.kotlinx.serialization.json)
            
            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("ai.koog:prompt-executor-google-client-jvm:0.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat")
            implementation(libs.sqldelight.sqlite.driver)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat")
            implementation(libs.sqldelight.android.driver)
            implementation("io.insert-koin:koin-android:4.0.0")
        }
    }
}

android {
    namespace = "com.example.the_jury"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.the_jury"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        
        // Add API key to BuildConfig
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/*.kotlin_module",
                "META-INF/DEPENDENCIES"
            )
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

sqldelight {
    databases {
        create("JuryDatabase") {
            packageName.set("com.example.the_jury.database")
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.example.the_jury.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.example.the_jury"
            packageVersion = "1.0.0"
        }
    }
}
