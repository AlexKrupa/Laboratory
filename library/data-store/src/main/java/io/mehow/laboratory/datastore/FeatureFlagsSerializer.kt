package io.mehow.laboratory.datastore

import androidx.datastore.Serializer
import java.io.InputStream
import java.io.OutputStream

object FeatureFlagsSerializer : Serializer<FeatureFlags> {
  override fun readFrom(input: InputStream): FeatureFlags {
    return FeatureFlags.ADAPTER.decode(input)
  }

  override fun writeTo(t: FeatureFlags, output: OutputStream) {
    FeatureFlags.ADAPTER.encode(output, t)
  }
}