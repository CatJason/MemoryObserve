plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "cat.jason.performance"
    compileSdk = 33

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    afterEvaluate {
        publishing {
            val versionName = "1.1.0" // 当前版本依赖库版本号，这个 jitpack 不会使用到，只是我们开发者自己查看
            publications {
                create<MavenPublication>("release") {
                    // Applies the component for the release build variant.
                    from(components["release"]) // 表示发布 release（jitpack 都不会使用到）

                    // You can then customize attributes of the publication as shown below.
                    groupId = "com.catjason.performance" // 这个是依赖库的组 id
                    artifactId = "performance" // 依赖库的名称（jitpack 都不会使用到）
                    version = versionName
                }
            }
            repositories {
                // 下面这部分，不是很清楚加不加，但是最后加上
                maven {
                    // change URLs to point to your repos, e.g. http://my.org/repo
                    val baseUrl = buildDir.parent
                    val releasesRepoUrl = "$baseUrl/repos/releases"
                    val snapshotsRepoUrl = "$baseUrl/repos/snapshots"
                    url = uri(if (versionName.endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                }
            }
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.21")
    implementation("com.kuaishou.koom:koom-java-leak-static:2.2.1")
}