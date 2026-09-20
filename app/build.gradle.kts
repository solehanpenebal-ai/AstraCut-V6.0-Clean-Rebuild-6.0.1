plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.astracut.v60"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    defaultConfig {
        applicationId = "com.astracut.v60"
        minSdk = 26
        targetSdk = 36
        versionCode = 60100
        versionName = "6.0.1"
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.media3:media3-common:1.11.1")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-transformer:1.11.1")
    testImplementation("junit:junit:4.13.2")
}


kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
