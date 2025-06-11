// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

dependencies {
    implementation('io.reactivex.rxjava3.kotlin:3.0.1')
    implementation('io.reactivex.rxjava3:rxandroid:3.0.2')
    implementation('io.reactivex.rxjava3:rxjava:3.1.5')
}