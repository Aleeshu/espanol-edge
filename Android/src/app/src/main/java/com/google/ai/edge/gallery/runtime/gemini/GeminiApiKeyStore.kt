/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.runtime.gemini

import com.google.ai.edge.gallery.data.DataStoreRepository

/**
 * Process-wide facade for reading and updating the user-configured Gemini API key backed by
 * [DataStoreRepository]'s encrypted secrets store.
 */
object GeminiApiKeyStore {
  @Volatile private var repository: DataStoreRepository? = null
  @Volatile private var cachedKey: String? = null

  fun initialize(dataStoreRepository: DataStoreRepository) {
    repository = dataStoreRepository
    cachedKey = (dataStoreRepository.readGeminiApiKey() ?: "").trim()
  }

  fun getApiKey(): String {
    cachedKey?.let { return it }
    val loaded = repository?.readGeminiApiKey()?.trim().orEmpty()
    cachedKey = loaded
    return loaded
  }

  fun hasApiKey(): Boolean = getApiKey().isNotEmpty()

  fun saveApiKey(apiKey: String) {
    val trimmed = apiKey.trim()
    cachedKey = trimmed
    repository?.saveGeminiApiKey(trimmed)
  }

  fun clearApiKey() {
    cachedKey = ""
    repository?.clearGeminiApiKey()
  }
}
