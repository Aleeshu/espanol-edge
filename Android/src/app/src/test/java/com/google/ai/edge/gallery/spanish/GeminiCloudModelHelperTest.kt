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

import com.google.ai.edge.gallery.customtasks.xrlanguageteacher.models.TeacherResponseSchema
import com.google.ai.edge.gallery.runtime.gemini.GeminiCloudModelHelper
import com.google.ai.edge.gallery.runtime.gemini.GeminiTurn
import com.google.gson.JsonParser
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiCloudModelHelperTest {

  @Test
  fun ensureWavFormat_wrapsRawPcmWithValid44ByteWavHeader() {
    val rawPcm = byteArrayOf(1, 2, 3, 4, 5, 6)
    val wav = GeminiCloudModelHelper.ensureWavFormat(rawPcm, sampleRate = 16000)

    assertEquals(44 + rawPcm.size, wav.size)
    assertEquals("RIFF", String(wav, 0, 4, StandardCharsets.US_ASCII))
    assertEquals("WAVE", String(wav, 8, 4, StandardCharsets.US_ASCII))
    assertEquals("fmt ", String(wav, 12, 4, StandardCharsets.US_ASCII))
    assertEquals("data", String(wav, 36, 4, StandardCharsets.US_ASCII))

    val header = ByteBuffer.wrap(wav, 0, 44).order(ByteOrder.LITTLE_ENDIAN)
    assertEquals(36 + rawPcm.size, header.getInt(4))
    assertEquals(16000, header.getInt(24))
    assertEquals(rawPcm.size, header.getInt(40))
    assertArrayEquals(rawPcm, wav.copyOfRange(44, wav.size))
  }

  @Test
  fun ensureWavFormat_preservesExistingWavHeader() {
    val pcm = byteArrayOf(10, 20, 30, 40)
    val firstPass = GeminiCloudModelHelper.ensureWavFormat(pcm, sampleRate = 16000)
    val secondPass = GeminiCloudModelHelper.ensureWavFormat(firstPass, sampleRate = 16000)

    assertArrayEquals(firstPass, secondPass)
  }

  @Test
  fun sanitizeSchemaForGemini_stripsUnsupportedDirectivesRecursively() {
    val sanitized =
      GeminiCloudModelHelper.sanitizeSchemaForGemini(TeacherResponseSchema.pass2CardJsonSchema())

    assertFalse(sanitized.has("x-guidance"))
    assertEquals("object", sanitized.get("type").asString)
    assertTrue(sanitized.has("required"))
    val props = sanitized.getAsJsonObject("properties")
    assertNotNull(props)
    val wordProp = props.getAsJsonObject("word")
    assertNotNull(wordProp)
    assertFalse(wordProp.has("pattern"))
    assertEquals("string", wordProp.get("type").asString)
  }

  @Test
  fun buildGenerateContentRequestJson_includesHistorySystemInstructionAndMedia() {
    val schema =
      GeminiCloudModelHelper.sanitizeSchemaForGemini(TeacherResponseSchema.pass2CardJsonSchema())
    val json =
      GeminiCloudModelHelper.buildGenerateContentRequestJson(
        systemInstructionText = "Eres un profesor de español.",
        history =
          listOf(
            GeminiTurn(role = "user", text = "Hola"),
            GeminiTurn(role = "model", text = "¡Hola! ¿Cómo estás?"),
          ),
        currentTurnText = "Explícame esta foto",
        base64Images = listOf("aW1hZ2U="),
        base64AudioClips = listOf("YXVkaW8="),
        temperature = 0.7f,
        topP = 0.9f,
        topK = 40,
        maxOutputTokens = 512,
        sanitizedResponseSchema = schema,
      )

    val parsed = JsonParser.parseString(json).asJsonObject
    assertEquals(
      "Eres un profesor de español.",
      parsed.getAsJsonObject("systemInstruction").getAsJsonArray("parts")[0].asJsonObject.get("text").asString,
    )
    val contents = parsed.getAsJsonArray("contents")
    assertEquals(3, contents.size())
    val lastParts = contents[2].asJsonObject.getAsJsonArray("parts")
    assertEquals(3, lastParts.size())
    assertEquals("image/jpeg", lastParts[0].asJsonObject.getAsJsonObject("inlineData").get("mimeType").asString)
    assertEquals("audio/wav", lastParts[1].asJsonObject.getAsJsonObject("inlineData").get("mimeType").asString)
    assertEquals("Explícame esta foto", lastParts[2].asJsonObject.get("text").asString)

    val genConfig = parsed.getAsJsonObject("generationConfig")
    assertEquals("application/json", genConfig.get("responseMimeType").asString)
    assertTrue(genConfig.has("responseJsonSchema"))
  }

  @Test
  fun extractTextFromSseLine_parsesDeltaSkipsThoughtsAndHandlesDone() {
    val sseDelta =
      """data: {"candidates":[{"content":{"parts":[{"thought":true,"text":"thinking"},{"text":"¡Buenos días!"}]}}]}"""
    assertEquals("¡Buenos días!", GeminiCloudModelHelper.extractTextFromSseLine(sseDelta))

    assertNull(GeminiCloudModelHelper.extractTextFromSseLine("data: [DONE]"))
    assertNull(GeminiCloudModelHelper.extractTextFromSseLine("event: ping"))
    assertNull(GeminiCloudModelHelper.extractTextFromSseLine(""))
  }
}
