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

import com.google.ai.edge.gallery.data.Accelerator
import com.google.ai.edge.gallery.data.BackendSpec
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.LlmProfile
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.RuntimeType
import com.google.ai.edge.gallery.data.createLlmChatConfigs

/** Built-in hybrid Cloud Gemini API model definitions for Español Edge. */
object GeminiCloudModels {
  const val GEMINI_2_5_FLASH_NAME = "Gemini 2.5 Flash (Cloud)"
  const val GEMINI_2_5_FLASH_ID = "gemini-2.5-flash"

  const val GEMINI_2_5_FLASH_LITE_NAME = "Gemini 2.5 Flash-Lite (Cloud)"
  const val GEMINI_2_5_FLASH_LITE_ID = "gemini-2.5-flash-lite"

  val targetTaskIds: Set<String> =
    setOf(
      BuiltInTaskId.LLM_CHAT,
      BuiltInTaskId.LLM_XR_LANGUAGE_TEACHER,
      BuiltInTaskId.LLM_ASK_AUDIO,
    )

  fun apiModelIdFor(modelName: String): String =
    when (modelName) {
      GEMINI_2_5_FLASH_LITE_NAME -> GEMINI_2_5_FLASH_LITE_ID
      else -> GEMINI_2_5_FLASH_ID
    }

  fun allModels(): List<Model> =
    listOf(
      buildCloudModel(
        name = GEMINI_2_5_FLASH_NAME,
        modelId = GEMINI_2_5_FLASH_ID,
        info =
          "Hybrid Cloud AI via Google Gemini API (requires API key in Settings; instant multimodal Spanish tutoring, vision & audio).",
        defaultMaxToken = 4096,
      ),
      buildCloudModel(
        name = GEMINI_2_5_FLASH_LITE_NAME,
        modelId = GEMINI_2_5_FLASH_LITE_ID,
        info =
          "Fast, lightweight Cloud AI via Google Gemini API (requires API key in Settings; quick Spanish tutor replies, camera lessons & pronunciation feedback).",
        defaultMaxToken = 2048,
      ),
    )

  private fun buildCloudModel(
    name: String,
    modelId: String,
    info: String,
    defaultMaxToken: Int,
  ): Model {
    val model =
      Model(
        name = name,
        displayName = name,
        modelId = modelId,
        info = info,
        configs =
          createLlmChatConfigs(
            defaultMaxToken = defaultMaxToken,
            defaultTemperature = 0.7f,
            accelerators = listOf(Accelerator.CPU),
          ),
        learnMoreUrl = "https://ai.google.dev/gemini-api/docs/models",
        showRunAgainButton = false,
        llmProfile = LlmProfile(maxTokens = 8192),
        supportImage = true,
        supportAudio = true,
        backendSpec = BackendSpec(runtimeType = RuntimeType.GEMINI_CLOUD),
      )
    model.preProcess()
    return model
  }
}
