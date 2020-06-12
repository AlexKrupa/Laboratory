plugins {
  id("com.android.library")
  kotlin("android")
  kotlin("kapt")
}

android {
  resourcePrefix("io_mehow_laboratory_")
}

dependencies {
  api(project(":library:inspector"))
  api(Libs.HyperionPlugin)
  implementation(Libs.Kotlin.StdLibJdk7)
  implementation(Libs.AndroidX.AppCompat)
  kapt(Libs.AutoService)
}

apply(from = rootProject.file("gradle/gradle-mvn-push.gradle"))