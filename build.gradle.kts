plugins {
    id("com.android.application") version "9.3.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    // KSP 2.3.x is versioned independently of Kotlin and supports AGP 9 built-in Kotlin (Room code generation).
    id("com.google.devtools.ksp") version "2.3.12" apply false
}

// Follow the tested AGP/Kotlin release line used by LocalTell.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}
