package io.mehow.laboratory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface FeatureStorage {
  fun <T : Feature<*>> observeFeatureName(featureClass: Class<T>): Flow<String?>
  suspend fun <T : Feature<*>> getFeatureName(featureClass: Class<T>): String?
  @JvmDefault suspend fun <T : Feature<*>> setFeature(feature: T): Boolean = setFeatures(feature)
  suspend fun <T : Feature<*>> setFeatures(vararg features: T): Boolean

  companion object {
    @OptIn(ExperimentalCoroutinesApi::class) fun inMemory() = object : FeatureStorage {
      private var features = emptyMap<Class<*>, String>()
      private val featureFlow = MutableStateFlow(features)

      override fun <T : Feature<*>> observeFeatureName(featureClass: Class<T>) = featureFlow
        .map { it[featureClass] }
        .distinctUntilChanged()

      override suspend fun <T : Feature<*>> getFeatureName(featureClass: Class<T>) = features[featureClass]

      override suspend fun <T : Feature<*>> setFeatures(vararg features: T): Boolean {
        for (feature in features) {
          this.features += feature.javaClass to feature.name
        }
        featureFlow.value = this.features
        return true
      }
    }

    fun sourced(
      localSource: FeatureStorage,
      remoteSources: Map<String, FeatureStorage>,
    ): FeatureStorage = SourcedFeatureStorage(localSource, remoteSources)
  }
}
