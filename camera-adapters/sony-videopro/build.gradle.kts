plugins {
    id("com.android.library")
}

android {
    namespace = "com.androiddjibridge.sony"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":bridge-core"))
}
