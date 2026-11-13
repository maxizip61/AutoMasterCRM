plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "ru.automaster.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.automaster.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        // Настройка для Room (сохранение схем)
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas") }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.lifecycle.viewmodel.compose)
        implementation(libs.androidx.activity.compose)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.ui)
        implementation(libs.androidx.ui.graphics)
        implementation(libs.androidx.ui.tooling.preview)
        implementation(libs.androidx.material3)
        implementation(libs.androidx.material.icons.extended)
        implementation(libs.androidx.navigation.compose)

        // Room
        implementation(libs.androidx.room.runtime)
        implementation(libs.androidx.room.ktx)
        ksp(libs.androidx.room.compiler)

        // Hilt
        implementation(libs.hilt.android)
        ksp(libs.hilt.compiler)
        implementation(libs.androidx.hilt.navigation.compose)

        // WorkManager & Hilt Work
        implementation(libs.androidx.work.runtime)
        implementation(libs.androidx.hilt.work)
        ksp(libs.androidx.hilt.compiler)

        // Security & Biometric
        implementation(libs.androidx.biometric)
        implementation(libs.androidx.security.crypto)

        // DataStore & Lifecycle
        implementation(libs.androidx.datastore.preferences)
        implementation(libs.androidx.lifecycle.process)
        implementation(libs.androidx.lifecycle.runtime.compose)

        // Coil (Фото)
        implementation(libs.coil.compose)
        implementation(libs.kotlinx.coroutines.android)

        // Тесты
        testImplementation(libs.junit)
        testImplementation(libs.kotlinx.coroutines.test)
        testImplementation(libs.turbine)
        androidTestImplementation(libs.androidx.room.testing)
        androidTestImplementation(libs.androidx.test.core)
        testImplementation(libs.hilt.android.testing)
        kspTest(libs.hilt.compiler)
        androidTestImplementation("androidx.test.ext:junit:1.1.5")
        androidTestImplementation("androidx.test:runner:1.5.2")
        androidTestImplementation("androidx.test:rules:1.5.0")
        debugImplementation(libs.androidx.ui.tooling)
    }
    // WorkManager
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Security & Biometric
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Lifecycle
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Testing
    androidTestImplementation(libs.androidx.room.testing)

}