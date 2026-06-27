plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.ticktrack"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ticktrack"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    // MySQL JDBC Connector for direct database connection
    implementation("mysql:mysql-connector-java:5.1.49")
    // jBcrypt for verifying CI4 password hashes
    implementation("org.mindrot:jbcrypt:0.4")

    // Glide for images
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // Lottie for animations
    implementation("com.airbnb.android:lottie:6.1.0")

    // MPAndroidChart for dashboard
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Facebook Shimmer
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    
    // Swipe Refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // uCrop for image cropping
    implementation("com.github.yalantis:ucrop:2.2.8")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
