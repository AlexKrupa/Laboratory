package io.mehow.laboratory

/**
 * A representation of a feature flag.
 */
interface Feature<T> : Comparable<T> where T : Enum<T>, T : Feature<T> {
  /**
   * A name of a feature that should uniquely identify it within this feature flag.
   */
  val name: String

  /**
   * Determines whether a feature is the default value of the feature flag. If more than one feature has this value
   * set to `true` the first one among them is used. If no feature has this value set to `true`
   * the first one value is used.
   */
  val isDefaultValue: Boolean

  /**
   * Source of feature flag values. When `null` it is assumed that the feature flag has only local source of values.
   * Source is also a feature flag, which means it can be controlled by [Laboratory] as well. By convention,
   * if this property is not `null`, one of the source values should be `Local`. For example, the following code
   * will result in a flag that can be controlled by local, Firebase or Aws source.
   *
   * ```
   * enum class SomeFeature(
   *   override val isDefaultValue: Boolean = false
   * ) : Feature<SomeFeature> {
   *   FirstValue(isDefaultValue = true),
   *   SecondValue,
   *   ;
   *
   *   @Suppress("UNCHECKED_CAST")
   *   override val sourcedWith = Source::class.java as Class<Feature<*>>
   *
   *   enum class Source(
   *     override val isDefaultValue: Boolean = false
   *   ) : Feature<Source> {
   *     Local(isDefaultValue = true),
   *     Firebase,
   *     Aws,
   *     ;
   *   }
   * }
   * ```
   *
   * It should have the same value for all feature values. If sources differ between values the first one is used.
   */
  @JvmDefault val sourcedWith: Class<Feature<*>>? get() = null

  /**
   * Description of the feature flag that can be used for more contextual information. It should have the same value
   * for all feature values. If descriptions differ between values the first one is used.
   */
  @JvmDefault val description: String get() = ""
}
