plugins {
    id("com.android.test")
}

android {
    namespace = "com.keeftalk.chat.macrobenchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        targetSdk = 37

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.runner)
    implementation(libs.benchmark.macro)
}
