plugins {
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "1.9.21" apply false
}

// Koin version management
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("io.insert-koin:koin-gradle-plugin:3.5.6")
    }
}
