import com.android.build.gradle.AppExtension
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

/** True when this invocation is packaging an APK/AAB (new version stamp applied). */
val taskNamesLower = gradle.startParameter.taskNames.map { it.lowercase(Locale.ROOT) }
val isPackagingBuild = taskNamesLower.any { name ->
    name.contains("assemble") || name.contains("bundle")
}

val utcFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)

val packagingUtcStamp: String = utcFormatter.format(Instant.now())
/** Monotonic-ish int for Play-style versionCode on each packaging build. */
val packagingVersionCode: Int = Instant.now().epochSecond.toInt().coerceAtLeast(1)
val packagingVersionName: String = "1.0.$packagingUtcStamp"

android {
    namespace = "com.zavscom.adonidirectory"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.zavscom.adonidirectory"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = if (isPackagingBuild) packagingVersionCode else 1
        versionName = if (isPackagingBuild) packagingVersionName else "1.0-dev"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // AndroidX graphics-path .so often cannot be llvm-stripped on Windows; keep symbols so AGP skips strip.
        jniLibs {
            keepDebugSymbols += "**/libandroidx.graphics.path.so"
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}

// --- APK archiving + persistent build counter (sibling to :app → android/archive-counter.txt) ---
val archiveCounterFile: File = rootProject.layout.projectDirectory.file("archive-counter.txt").asFile

fun readAndIncrementArchiveCounter(): Int {
    archiveCounterFile.parentFile?.mkdirs()
    val previous = if (archiveCounterFile.exists()) {
        archiveCounterFile.readText().trim().toIntOrNull() ?: 0
    } else {
        0
    }
    val next = previous + 1
    archiveCounterFile.writeText("$next\n")
    return next
}

afterEvaluate {
    val androidApp = extensions.getByType(AppExtension::class.java)
    androidApp.applicationVariants.configureEach {
        val variant = this
        val variantName = variant.name.replaceFirstChar { it.uppercaseChar() }
        val archiveTask = tasks.register("archive${variantName}Apk") {
            group = "archiving"
            description =
                "Copy ${variant.name} APK(s) to ${rootProject.projectDir.name}/apk-archive with UTC date-time"
            doLast {
                val buildNum = readAndIncrementArchiveCounter()
                val apkRoot =
                    layout.buildDirectory.dir("outputs/apk/${variant.name}").get().asFile
                val apks = apkRoot.walkTopDown()
                    .filter { it.isFile && it.extension.equals("apk", ignoreCase = true) }
                    .toList()
                if (apks.isEmpty()) {
                    logger.warn("No APK found under ${apkRoot.absolutePath}; skipping archive")
                    return@doLast
                }
                val archiveDir =
                    rootProject.layout.projectDirectory.dir("apk-archive").asFile.apply { mkdirs() }
                val fileStamp = utcFormatter.format(Instant.now())
                val safeVersion =
                    variant.versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                apks.forEach { apk ->
                    val base =
                        apk.nameWithoutExtension.replace(Regex("[^A-Za-z0-9._-]"), "_")
                    val outName =
                        "AdoniDirectory-b${buildNum}-v${safeVersion}-${variant.name}-$base-$fileStamp.apk"
                    val dest = File(archiveDir, outName)
                    apk.copyTo(dest, overwrite = true)
                    logger.lifecycle("Archived APK -> ${dest.absolutePath}")
                }
            }
        }

        assembleProvider.configure {
            finalizedBy(archiveTask)
        }
    }
}
