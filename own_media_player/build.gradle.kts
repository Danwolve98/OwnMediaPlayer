plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id ("maven-publish")
}

android {
    namespace = "com.danwolve.own_media_player"

    publishing {
        singleVariant("release") {
           
        }
    }

    defaultConfig {
        compileSdk = 36
        minSdk = 24
        version = "1.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions{
        targetSdk = 36
    }

    lint {
        targetSdk = 36
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
    }
    buildFeatures{
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation ("androidx.appcompat:appcompat:1.7.1")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("androidx.media3:media3-session:1.7.1")
    implementation ("androidx.media3:media3-exoplayer:1.7.1")
    implementation ("androidx.media3:media3-ui:1.7.1")
}


publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.danwolve"
            artifactId = "ownmediaplayer"
            version = "1.8.0"

            // 'from' debe ir fuera del afterEvaluate, pero puede necesitarlo si el componente aún no existe
            afterEvaluate {
                from(components.findByName("release") ?: return@afterEvaluate)
            }
        }
    }
}