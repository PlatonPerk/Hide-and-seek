// Модуль «Прятки» — библиотека, которую встраивают в приложение.
// Собирается тем же тулчейном, что и :app (AGP 9 со встроенным Kotlin), поэтому
// отдельный плагин org.jetbrains.kotlin.android и блок kotlinOptions здесь не нужны.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.hideandseek"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Только Compose UI и Foundation: без библиотек для картинок, сети и JSON.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
}
