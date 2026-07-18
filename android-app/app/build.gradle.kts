plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
}

android {
    namespace = "com.dailywork.attedance"
    compileSdk = 34

    val admobAppId = project.findProperty("ADMOB_APP_ID")?.toString() ?: System.getenv("ADMOB_APP_ID") ?: ""
    val admobAppOpenAdUnitId = project.findProperty("ADMOB_APP_OPEN_AD_UNIT_ID")?.toString() ?: System.getenv("ADMOB_APP_OPEN_AD_UNIT_ID") ?: ""
    val admobAdaptiveBannerAdUnitId = project.findProperty("ADMOB_ADAPTIVE_BANNER_AD_UNIT_ID")?.toString() ?: System.getenv("ADMOB_ADAPTIVE_BANNER_AD_UNIT_ID") ?: ""
    val admobFixedBannerAdUnitId = project.findProperty("ADMOB_FIXED_BANNER_AD_UNIT_ID")?.toString() ?: System.getenv("ADMOB_FIXED_BANNER_AD_UNIT_ID") ?: ""
    val admobInterstitialAdUnitId = project.findProperty("ADMOB_INTERSTITIAL_AD_UNIT_ID")?.toString() ?: System.getenv("ADMOB_INTERSTITIAL_AD_UNIT_ID") ?: ""
    val admobRewardedAdUnitId = project.findProperty("ADMOB_REWARDED_AD_UNIT_ID")?.toString() ?: System.getenv("ADMOB_REWARDED_AD_UNIT_ID") ?: ""
    val admobRewardedInterstitialAdUnitId = project.findProperty("ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID")?.toString() ?: System.getenv("ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID") ?: ""
    val admobNativeAdUnitId = project.findProperty("ADMOB_NATIVE_AD_UNIT_ID")?.toString() ?: System.getenv("ADMOB_NATIVE_AD_UNIT_ID") ?: ""
    val admobNativeVideoAdUnitId = project.findProperty("ADMOB_NATIVE_VIDEO_AD_UNIT_ID")?.toString() ?: System.getenv("ADMOB_NATIVE_VIDEO_AD_UNIT_ID") ?: ""
    val admobTestDeviceIds = project.findProperty("ADMOB_TEST_DEVICE_IDS")?.toString() ?: System.getenv("ADMOB_TEST_DEVICE_IDS") ?: ""

    tasks.register("verifyAdmobConfig") {
        doFirst {
            if (admobAppId.isBlank()) {
                throw GradleException("Production ADMOB_APP_ID is missing! Please configure ADMOB_APP_ID in gradle.properties or environment variables for release builds.")
            }
            val units = listOf(
                "ADMOB_APP_OPEN_AD_UNIT_ID" to admobAppOpenAdUnitId,
                "ADMOB_ADAPTIVE_BANNER_AD_UNIT_ID" to admobAdaptiveBannerAdUnitId,
                "ADMOB_FIXED_BANNER_AD_UNIT_ID" to admobFixedBannerAdUnitId,
                "ADMOB_INTERSTITIAL_AD_UNIT_ID" to admobInterstitialAdUnitId,
                "ADMOB_REWARDED_AD_UNIT_ID" to admobRewardedAdUnitId,
                "ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID" to admobRewardedInterstitialAdUnitId,
                "ADMOB_NATIVE_AD_UNIT_ID" to admobNativeAdUnitId,
                "ADMOB_NATIVE_VIDEO_AD_UNIT_ID" to admobNativeVideoAdUnitId
            )
            for ((name, value) in units) {
                if (value.isBlank()) {
                    throw GradleException("Production $name is missing! Please configure it in gradle.properties or environment variables for release builds.")
                }
            }
        }
    }

    val androidComponents = extensions.findByType(com.android.build.api.variant.ApplicationAndroidComponentsExtension::class.java)
    androidComponents?.onVariants { variant ->
        if (variant.name == "release") {
            tasks.matching {
                it.name.startsWith("assembleRelease") ||
                it.name.startsWith("bundleRelease") ||
                it.name.startsWith("compileRelease") ||
                it.name.startsWith("packageRelease")
            }.configureEach {
                dependsOn("verifyAdmobConfig")
            }
        }
    }

    defaultConfig {
        applicationId = "com.dailywork.attedance"
        minSdk = 24
        targetSdk = 34
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toInt() ?: 1
        versionName = "1.0.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "ADMOB_TEST_DEVICE_IDS", "\"$admobTestDeviceIds\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "../keystore/default-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: (project.findProperty("KEYSTORE_PASSWORD")?.toString() ?: "android")
            keyAlias = System.getenv("KEY_ALIAS") ?: (project.findProperty("KEY_ALIAS")?.toString() ?: "androiddebugkey")
            keyPassword = System.getenv("KEY_PASSWORD") ?: (project.findProperty("KEY_PASSWORD")?.toString() ?: "android")
        }
        getByName("debug") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "../keystore/default-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: (project.findProperty("KEYSTORE_PASSWORD")?.toString() ?: "android")
            keyAlias = System.getenv("KEY_ALIAS") ?: (project.findProperty("KEY_ALIAS")?.toString() ?: "androiddebugkey")
            keyPassword = System.getenv("KEY_PASSWORD") ?: (project.findProperty("KEY_PASSWORD")?.toString() ?: "android")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")

            manifestPlaceholders["ADMOB_APP_ID"] = admobAppId
            buildConfigField("Boolean", "ADS_TEST_MODE", "false")
            buildConfigField("String", "ADMOB_APP_OPEN_AD_UNIT_ID", "\"$admobAppOpenAdUnitId\"")
            buildConfigField("String", "ADMOB_ADAPTIVE_BANNER_AD_UNIT_ID", "\"$admobAdaptiveBannerAdUnitId\"")
            buildConfigField("String", "ADMOB_FIXED_BANNER_AD_UNIT_ID", "\"$admobFixedBannerAdUnitId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"$admobInterstitialAdUnitId\"")
            buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"$admobRewardedAdUnitId\"")
            buildConfigField("String", "ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID", "\"$admobRewardedInterstitialAdUnitId\"")
            buildConfigField("String", "ADMOB_NATIVE_AD_UNIT_ID", "\"$admobNativeAdUnitId\"")
            buildConfigField("String", "ADMOB_NATIVE_VIDEO_AD_UNIT_ID", "\"$admobNativeVideoAdUnitId\"")
        }
        create("internal") {
            initWith(getByName("release"))
            isDebuggable = true
            matchingFallbacks += listOf("release", "debug")

            buildConfigField("Boolean", "ADS_TEST_MODE", "true")
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
            versionNameSuffix = "-internal"

            buildConfigField("String", "ADMOB_APP_OPEN_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "ADMOB_ADAPTIVE_BANNER_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "ADMOB_FIXED_BANNER_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "ADMOB_NATIVE_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "ADMOB_NATIVE_VIDEO_AD_UNIT_ID", "\"\"")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("Boolean", "ADS_TEST_MODE", "true")
            buildConfigField("String", "ADMOB_APP_OPEN_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "ADMOB_ADAPTIVE_BANNER_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "ADMOB_FIXED_BANNER_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "ADMOB_NATIVE_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "ADMOB_NATIVE_VIDEO_AD_UNIT_ID", "\"\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    ksp {
        arg("room.generateKotlin", "true")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Fix kotlin stdlib duplicates
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.0") {
            because("kotlin-stdlib-jdk7 is now a part of kotlin-stdlib")
        }
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0") {
            because("kotlin-stdlib-jdk8 is now a part of kotlin-stdlib")
        }
    }

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.6.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Credential Manager for Modern Google Sign-In
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Icons
    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    // Google Mobile Ads (AdMob) & User Messaging Platform (UMP)
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    implementation("com.google.android.ump:user-messaging-platform:2.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Coil and Firebase Storage
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.google.firebase:firebase-storage")

    // ZXing for QR Code Scanning (Aadhaar)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

dependencies {
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    val work_version = "2.9.0"
    implementation("androidx.work:work-runtime-ktx:$work_version")
}
