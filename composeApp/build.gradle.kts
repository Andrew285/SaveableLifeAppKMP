import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation("androidx.work:work-runtime-ktx:2.9.1")

            implementation("com.google.android.gms:play-services-auth:21.2.0")
            implementation("com.google.api-client:google-api-client-android:2.2.0")
            implementation("com.google.apis:google-api-services-drive:v3-rev20240521-2.0.0")

            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.koin.android)
        }
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:3.0.3")
            implementation("io.ktor:ktor-client-auth:3.0.3")

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(projects.shared)

            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation("com.google.api-client:google-api-client:2.2.0")
            implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
            implementation("com.google.apis:google-api-services-drive:v3-rev20240521-2.0.0")

            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.java)
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

sqldelight {
    databases {
        create("SaveableDatabase") {
            packageName.set("org.simpleapps.saveablekmp.data.db")
        }
    }
}

android {
    namespace = "org.simpleapps.saveablekmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.simpleapps.saveablekmp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "org.simpleapps.saveablekmp.MainKt"

        jvmArgs += listOf("-Xmx512m")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SaveableApp"
            packageVersion = "1.0.0"

            // ← Bundlює JVM разом з додатком (~80-100MB але працює без Java на системі)
            includeAllModules = true

            windows {
                menuGroup = "SaveableApp"
                upgradeUuid = "12345678-1234-1234-1234-123456789012" // будь-який UUID
                dirChooser = true
                perUserInstall = true
                iconFile.set(project.file("src/jvmMain/resources/ic_saveable.ico"))
            }
        }
    }
}