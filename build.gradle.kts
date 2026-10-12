// Root build: declare plugin versions once; modules apply what they need.
// Declaring the Kotlin plugins here puts KGP ${libs.versions.kotlin} on the build classpath,
// which is the Kotlin version AGP 9 built-in Kotlin compiles with.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}
