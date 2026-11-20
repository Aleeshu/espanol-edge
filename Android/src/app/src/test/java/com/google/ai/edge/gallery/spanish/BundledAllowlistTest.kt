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

package com.google.ai.edge.gallery.spanish

import com.google.ai.edge.gallery.data.ModelAllowlist
import com.google.ai.edge.gallery.data.ModelType
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import java.io.File
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledAllowlistTest {

  private fun findBundledAssetFile(): File {
    var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
    while (dir != null) {
      val direct = File(dir, "src/main/assets/model_allowlist.json")
      if (direct.isFile) return direct
      val fromRoot = File(dir, "Android/src/app/src/main/assets/model_allowlist.json")
      if (fromRoot.isFile) return fromRoot
      val fromAndroidSrc = File(dir, "app/src/main/assets/model_allowlist.json")
      if (fromAndroidSrc.isFile) return fromAndroidSrc
      dir = dir.parentFile
    }
    error("Could not locate src/main/assets/model_allowlist.json from ${System.getProperty("user.dir")}")
  }

  private fun findRepoHostedAllowlistFile(): File {
    var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
    while (dir != null) {
      val candidate = File(dir, "model_allowlists/1_0_24.json")
      if (candidate.isFile) return candidate
      dir = dir.parentFile
    }
    error("Could not locate model_allowlists/1_0_24.json from ${System.getProperty("user.dir")}")
  }

  private fun parseStrict(jsonText: String): ModelAllowlist {
    val reader = JsonReader(StringReader(jsonText)).apply { isLenient = false }
    return Gson().fromJson(reader, ModelAllowlist::class.java)
  }

  @Test
  fun bundledAssetAndRepoAllowlistMatchAndParseStrictly() {
    val assetText = findBundledAssetFile().readText(Charsets.UTF_8)
    val repoText = findRepoHostedAllowlistFile().readText(Charsets.UTF_8)
    assertEquals(assetText, repoText)

    val parsed = parseStrict(assetText)
    assertNotNull(parsed)
    assertTrue(parsed.models.isNotEmpty())
  }

  @Test
  fun bundledAllowlistContainsFreePhoneLlmsAsrAndTtsModels() {
    val allowlist = parseStrict(findBundledAssetFile().readText(Charsets.UTF_8))
    val byName = allowlist.models.associateBy { it.name }

    val expectedPhoneLlms =
      listOf(
        "Gemma-4-E2B-it",
        "Gemma-4-E4B-it",
        "Qwen2.5-1.5B-Instruct",
        "DeepSeek-R1-Distill-Qwen-1.5B",
        "Gemma3-1B-IT",
        "Gemma-3n-E2B-it",
        "Gemma-3n-E4B-it",
      )
    for (name in expectedPhoneLlms) {
      assertTrue("Missing phone LLM '$name'", byName.containsKey(name))
    }

    val expectedAsrModels = listOf("Parakeet TDT 0.6B ASR", "ML Kit ASR (Basic)", "ML Kit ASR (Advanced)")
    for (name in expectedAsrModels) {
      val entry = byName[name]
      assertNotNull("Missing ASR model '$name'", entry)
      assertEquals(ModelType.ASR, entry!!.modelType)
      assertTrue(
        "ASR model '$name' should support Spanish (es-ES)",
        entry.asrConfig?.languages?.contains("es-ES") == true,
      )
    }

    val expectedTtsModels =
      listOf("Kokoro 82M (TTS)", "PocketTTS 100M (TTS)", "Phonon 31M (TTS)", "ML Kit TTS")
    for (name in expectedTtsModels) {
      val entry = byName[name]
      assertNotNull("Missing TTS model '$name'", entry)
      assertEquals(ModelType.TTS, entry!!.modelType)
    }
    for (spanishTts in listOf("Kokoro 82M (TTS)", "PocketTTS 100M (TTS)", "ML Kit TTS")) {
      val entry = byName.getValue(spanishTts)
      assertTrue(
        "TTS model '$spanishTts' should support es-ES",
        entry.ttsConfig?.languages?.contains("es-ES") == true,
      )
    }
  }
}
