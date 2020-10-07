package io.mehow.laboratory.gradle

import arrow.core.NonEmptyList
import arrow.core.nel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.file.shouldNotExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mehow.laboratory.generator.FeatureValuesCollision
import io.mehow.laboratory.generator.FeaturesCollision
import io.mehow.laboratory.generator.InvalidFeatureName
import io.mehow.laboratory.generator.InvalidFeatureValues
import io.mehow.laboratory.generator.InvalidPackageName
import io.mehow.laboratory.generator.NoFeatureValues
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import java.io.File

class GenerateFeatureSourceFactoryTaskSpec : StringSpec({
  lateinit var gradleRunner: GradleRunner

  beforeTest {
    gradleRunner = GradleRunner.create()
      .withPluginClasspath()
      .withArguments("generateFeatureSourceFactory", "--stacktrace")
  }

  afterTest {
    File("src/test/projects").getOutputDirs().forEach(File::cleanUpDir)
  }

  "generates factory without any features" {
    val fixture = "source-factory-generate-empty".toFixture()

    val result = gradleRunner.withProjectDir(fixture).build()

    result.task(":generateFeatureSourceFactory")!!.outcome shouldBe SUCCESS

    val factory = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    factory.shouldExist()

    factory.readText() shouldContain """
      |fun FeatureFactory.Companion.featureSourceGenerated(): FeatureFactory =
      |    GeneratedFeatureSourceFactory
      |
      |private object GeneratedFeatureSourceFactory : FeatureFactory {
      |  override fun create() = emptySet<Class<Feature<*>>>()
      |}
    """.trimMargin("|")
  }

  "generates factory with features" {
    val fixture = "source-factory-generate-features".toFixture()

    val result = gradleRunner.withProjectDir(fixture).build()

    result.task(":generateFeatureSourceFactory")!!.outcome shouldBe SUCCESS

    val factory = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    factory.shouldExist()

    factory.readText() shouldContain """
      |fun FeatureFactory.Companion.featureSourceGenerated(): FeatureFactory =
      |    GeneratedFeatureSourceFactory
      |
      |private object GeneratedFeatureSourceFactory : FeatureFactory {
      |  @Suppress("UNCHECKED_CAST")
      |  override fun create() = setOf(
      |    Class.forName("io.mehow.first.FeatureA${"\${'$'}"}Source"),
      |    Class.forName("io.mehow.second.FeatureB${"\${'$'}"}Source")
      |  ) as Set<Class<Feature<*>>>
      |}
    """.trimMargin("|")
  }

  "uses implicit package name" {
    val fixture = "source-factory-package-name-implicit".toFixture()

    gradleRunner.withProjectDir(fixture).build()

    val factory = fixture.featureSourceStorageFile("io.mehow.implicit.GeneratedFeatureSourceFactory")
    factory.shouldExist()

    factory.readText() shouldContain "package io.mehow.implicit"
  }

  "uses explicit package name" {
    val fixture = "source-factory-package-name-explicit".toFixture()

    gradleRunner.withProjectDir(fixture).build()

    val factory = fixture.featureSourceStorageFile("io.mehow.explicit.GeneratedFeatureSourceFactory")
    factory.shouldExist()

    factory.readText() shouldContain "package io.mehow.explicit"
  }

  "overrides implicit package name" {
    val fixture = "source-factory-package-name-explicit-override".toFixture()

    gradleRunner.withProjectDir(fixture).build()

    val factory = fixture.featureSourceStorageFile("io.mehow.explicit.GeneratedFeatureSourceFactory")
    factory.shouldExist()

    factory.readText() shouldContain "package io.mehow.explicit"
  }

  "generates internal factory" {
    val fixture = "source-factory-generate-internal".toFixture()

    gradleRunner.withProjectDir(fixture).build()

    val factory = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    factory.shouldExist()

    factory.readText() shouldContain "internal fun FeatureFactory.Companion.featureSourceGenerated()"
  }

  "generates public factory" {
    val fixture = "source-factory-generate-public".toFixture()

    gradleRunner.withProjectDir(fixture).build()

    val factory = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    factory.shouldExist()

    // Ensure public by checking a new line before enum declaration.
    // Change after https://github.com/square/kotlinpoet/pull/933
    factory.readText() shouldContain """
      |
      |fun FeatureFactory.Companion.featureSourceGenerated()
    """.trimMargin("|")
  }

  "fails for corrupted factory package name" {
    val fixture = "source-factory-package-name-corrupted".toFixture()

    val result = gradleRunner.withProjectDir(fixture).buildAndFail()

    result.task(":generateFeatureSourceFactory")!!.outcome shouldBe FAILED
    result.output shouldContain InvalidPackageName("!!!.GeneratedFeatureSourceFactory").message

    val feature = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    feature.shouldNotExist()
  }

  "fails for features with no values" {
    val fixture = "source-factory-feature-values-missing".toFixture()

    val result = gradleRunner.withProjectDir(fixture).buildAndFail()

    result.task(":generateFeatureSourceFactory")!!.outcome shouldBe FAILED
    result.output shouldContain NoFeatureValues("Feature").message

    val feature = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    feature.shouldNotExist()
  }

  "fails for features with colliding values" {
    val fixture = "source-factory-feature-values-colliding".toFixture()

    val result = gradleRunner.withProjectDir(fixture).buildAndFail()

    result.task(":generateFeatureSourceFactory")!!.outcome shouldBe FAILED
    result.output shouldContain FeatureValuesCollision("First".nel(), "Feature").message

    val feature = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    feature.shouldNotExist()
  }

  "fails for features with corrupted values" {
    val fixture = "source-factory-feature-values-corrupted".toFixture()

    val result = gradleRunner.withProjectDir(fixture).buildAndFail()

    result.task(":generateFeatureSourceFactory")!!.outcome shouldBe FAILED
    result.output shouldContain InvalidFeatureValues(NonEmptyList("!!!, ???"), "Feature").message

    val feature = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    feature.shouldNotExist()
  }

  "fails for features with corrupted names" {
    val fixture = "source-factory-feature-name-corrupted".toFixture()

    val result = gradleRunner.withProjectDir(fixture).buildAndFail()

    result.task(":generateFeatureSourceFactory")!!.outcome shouldBe FAILED
    result.output shouldContain InvalidFeatureName("!!!", "!!!").message

    val feature = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    feature.shouldNotExist()
  }

  "fails for features with corrupted package names" {
    val fixture = "source-factory-feature-package-name-corrupted".toFixture()

    val result = gradleRunner.withProjectDir(fixture).buildAndFail()

    result.task(":generateFeatureSourceFactory")!!.outcome shouldBe FAILED
    result.output shouldContain InvalidPackageName("!!!.Feature").message

    val feature = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    feature.shouldNotExist()
  }

  "fails for features with colliding namespaces" {
    val fixture = "source-factory-feature-namespace-colliding".toFixture()

    val result = gradleRunner.withProjectDir(fixture).buildAndFail()

    result.task(":generateFeatureSourceFactory")!!.outcome shouldBe FAILED
    result.output shouldContain FeaturesCollision("io.mehow.Feature".nel()).message

    val feature = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    feature.shouldNotExist()
  }

  "generates factory with features from all modules" {
    val fixture = "source-factory-multimodule-generate-all".toFixture()

    val result = gradleRunner.withProjectDir(fixture).build()

    result.task(":generateFeatureSourceFactory")!!.outcome shouldBe SUCCESS

    val factory = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    factory.shouldExist()

    factory.readText() shouldContain """
      |fun FeatureFactory.Companion.featureSourceGenerated(): FeatureFactory =
      |    GeneratedFeatureSourceFactory
      |
      |private object GeneratedFeatureSourceFactory : FeatureFactory {
      |  @Suppress("UNCHECKED_CAST")
      |  override fun create() = setOf(
      |    Class.forName("FeatureA${"\${'$'}"}Source"),
      |    Class.forName("FeatureB${"\${'$'}"}Source"),
      |    Class.forName("RootFeature${"\${'$'}"}Source")
      |  ) as Set<Class<Feature<*>>>
      |}
    """.trimMargin("|")
  }

  "generates factory with features from not excluded modules" {
    val fixture = "source-factory-multimodule-generate-filtered".toFixture()

    val result = gradleRunner.withProjectDir(fixture).build()

    result.task(":generateFeatureSourceFactory")!!.outcome shouldBe SUCCESS

    val factory = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    factory.shouldExist()

    factory.readText() shouldContain """
      |fun FeatureFactory.Companion.featureSourceGenerated(): FeatureFactory =
      |    GeneratedFeatureSourceFactory
      |
      |private object GeneratedFeatureSourceFactory : FeatureFactory {
      |  @Suppress("UNCHECKED_CAST")
      |  override fun create() = setOf(
      |    Class.forName("FeatureB${"\${'$'}"}Source"),
      |    Class.forName("RootFeature${"\${'$'}"}Source")
      |  ) as Set<Class<Feature<*>>>
      |}
    """.trimMargin("|")
  }

  "fails for features with colliding namespaces between modules" {
    val fixture = "source-factory-multimodule-namespace-colliding".toFixture()

    val result = gradleRunner.withProjectDir(fixture).buildAndFail()

    result.task(":generateFeatureSourceFactory")!!.outcome shouldBe FAILED
    result.output shouldContain FeaturesCollision("Feature".nel()).message

    val feature = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    feature.shouldNotExist()
  }

  "generates factory for Android project" {
    val fixture = "source-factory-android-smoke".toFixture()

    val result = gradleRunner.withProjectDir(fixture).build()

    result.task(":generateFeatureSourceFactory")!!.outcome shouldBe SUCCESS

    val factory = fixture.featureSourceStorageFile("GeneratedFeatureSourceFactory")
    factory.shouldExist()

    factory.readText() shouldContain """
      |fun FeatureFactory.Companion.featureSourceGenerated(): FeatureFactory =
      |    GeneratedFeatureSourceFactory
      |
      |private object GeneratedFeatureSourceFactory : FeatureFactory {
      |  override fun create() = emptySet<Class<Feature<*>>>()
      |}
    """.trimMargin("|")
  }
})
