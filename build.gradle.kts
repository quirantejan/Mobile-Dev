buildscript {
    // Remove the classpath entry as it's not needed here anymore.
}

plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
