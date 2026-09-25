import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin (no Android), so the phone app, widgets and Wear OS can all share it,
// and its tests run fast on the computer.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
}
