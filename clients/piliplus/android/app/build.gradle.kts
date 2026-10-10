import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

val agpMajorVersion = com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION
    .substringBefore('.')
    .toInt()
val builtInKotlinProperty = providers.gradleProperty("android.builtInKotlin").orNull
val isBuiltInKotlinEnabled = agpMajorVersion >= 9 &&
        (builtInKotlinProperty == null || builtInKotlinProperty.toBoolean())
if (!isBuiltInKotlinEnabled) {
    apply(plugin = "org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.piliplus"
    compileSdk = 37
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    val panelAcceptance = providers.gradleProperty("panelAcceptance").orNull == "true"
    testBuildType = if (panelAcceptance) "release" else "debug"

    defaultConfig {
        applicationId = "io.github.vrbilibili.quest"
        testInstrumentationRunner = if (panelAcceptance) "com.example.piliplus.PanelAcceptanceProbe"
            else "com.example.piliplus.PanelRecoveryRegression"
        minSdk = 34
        targetSdk = 37
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    // Retained historical notices are not part of this Meta-free panel APK.
    androidResources { ignoreAssetsPattern = "meta-spatial-sdk-0.14.0" }
    packagingOptions.jniLibs.useLegacyPackaging = true
    // Flutter/plugin JNI folders may bypass NDK ABI selection; strip unused ABIs from Quest releases.
    if (gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }) {
        packagingOptions.jniLibs.excludes += setOf("**/x86/**", "**/x86_64/**", "**/armeabi-v7a/**")
    }

    val keyProperties = Properties().also {
        val properties = rootProject.file("key.properties")
        if (properties.exists())
            it.load(properties.inputStream())
    }

    val config = keyProperties.getProperty("storeFile")?.let {
        signingConfigs.create("release") {
            storeFile = file(it)
            storePassword = keyProperties.getProperty("storePassword")
            keyAlias = keyProperties.getProperty("keyAlias")
            keyPassword = keyProperties.getProperty("keyPassword")
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildFeatures {
        if (project.hasProperty("dev")) {
            resValues = true
        }
    }

    buildTypes {
        release {
            signingConfig = config
            ndk { abiFilters += "arm64-v8a" }
            if (project.hasProperty("dev")) {
                applicationIdSuffix = ".dev"
                resValue(
                    type = "string",
                    name = "app_name",
                    value = "PiliPlus dev",
                )
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs["debug"]
            applicationIdSuffix = ".debug"
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.forEach { output ->
            (output as ApkVariantOutputImpl).versionCodeOverride = flutter.versionCode
        }
    }
}

// Never publish a release silently signed with the shared Android debug key.
gradle.taskGraph.whenReady {
    if (allTasks.any { it.project == project && it.name.contains("Release") } &&
        !rootProject.file("key.properties").exists()) {
        throw GradleException("Release requires android/key.properties; see docs/RELEASE.md")
    }
}

kotlin {
    sourceSets.getByName("main").kotlin.exclude("**/Cinema*.kt")
    sourceSets.getByName("androidTest").kotlin.exclude("**/Cinema*.kt", "**/QuestHardwareRecoveryRegression.kt")
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}


dependencies {
}

// Retain the historical XR driver as source only; it is not a panel test.
tasks.withType<JavaCompile>().configureEach { exclude("**/QuestUiDriver.java") }
