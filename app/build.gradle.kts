plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.nihongoquest"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.nihongoquest"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}


val generateLauncherIcon by tasks.registering {
    val encodedLogo = layout.projectDirectory.file("src/main/assets/nihongo_quest_logo.png.b64")
    val generatedRes = layout.buildDirectory.dir("generated/res/launcher")
    inputs.file(encodedLogo)
    outputs.dir(generatedRes)
    doLast {
        val iconFile = generatedRes.get().file("mipmap-xxxhdpi/ic_launcher.png").asFile
        iconFile.parentFile.mkdirs()
        iconFile.writeBytes(java.util.Base64.getDecoder().decode(encodedLogo.asFile.readText().trim()))
    }
}

android.sourceSets.getByName("main").res.srcDir(layout.buildDirectory.dir("generated/res/launcher"))
tasks.named("preBuild").configure { dependsOn(generateLauncherIcon) }
