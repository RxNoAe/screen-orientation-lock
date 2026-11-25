import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

/**
 * 正式签名配置从仓库外的 keystore.properties 读取。
 *
 * 该文件不入库，所以别人 clone 下来不会自动拿到你的签名密钥。
 * 没有这个文件时 [releaseSigningConfig] 为 null，release 包会退化成未签名，
 * 而不是让构建失败——开源项目应当「clone 即可构建」。
 */
val releaseSigningConfig: Pair<File, Properties>? = run {
    val propsFile = rootProject.file("keystore.properties")
    if (!propsFile.exists()) return@run null
    val props = Properties().apply { propsFile.inputStream().use { load(it) } }
    rootProject.file(props.getProperty("storeFile")) to props
}

android {
    namespace = "com.orientlock"

    // Android 17(API 37)的 SDK 只以 37.0 / 37.1 / 37.2 这种「次版本」包发布,
    // 没有 AGP 8.9.3 能识别的纯 platforms;android-37,所以这里仍按 36 编译。
    // 这不影响在 Android 17 上运行:compileSdk 只决定能用哪些编译期 API,
    // 运行行为由 targetSdk 决定(见下面 targetSdk 的说明)。
    compileSdk = 36

    defaultConfig {
        applicationId = "com.orientlock"
        minSdk = 26

        // 刻意停在 35,不要跟着 compileSdk 升到 37。
        // Android 17(API 37)起,targetSdk >= 37 的应用在宽度 ≥ 600dp 的大屏
        // (平板、折叠屏)上会被系统忽略方向限制,且不再提供 Android 16 的退出开关——
        // 那会直接废掉本软件的核心功能。
        // 依据:docs/superpowers/specs/2026-09-25-orientation-lock-design.md §2.6。
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"
    }

    signingConfigs {
        create("release") {
            releaseSigningConfig?.let { (store, props) ->
                storeFile = store
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
            // 显式打开全部签名方案，不能依赖 AGP 的默认值。
            //
            // AGP 默认只在 minSdk >= 24 时签 v2、不签 v1，这在小新 Pad Plus 2023 上
            // 直接装不上（提示「软件包似乎无效」）——某些国产 ROM 的安装器仍会走
            // v1 校验路径。三个方案全开、让系统自己挑能认的那个，多几 KB 体积换兼容。
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            // 只有拿到密钥时才套用；没有就保持未签名，构建不失败
            if (releaseSigningConfig != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
