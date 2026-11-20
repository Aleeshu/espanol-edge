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

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.gallery.data.ConfigKeys
import com.google.ai.edge.gallery.data.DEFAULT_TEMPERATURE
import com.google.ai.edge.gallery.data.DEFAULT_TOPK
import com.google.ai.edge.gallery.data.DEFAULT_TOPP
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.SAMPLE_RATE
import com.google.ai.edge.gallery.data.markInitializationFailed
import com.google.ai.edge.gallery.data.markInitialized
import com.google.ai.edge.gallery.data.resetInitialization
import com.google.ai.edge.gallery.runtime.CleanUpListener
import com.google.ai.edge.gallery.runtime.EXTRA_CONTEXT_MAX_OUTPUT_TOKENS
import com.google.ai.edge.gallery.runtime.EXTRA_CONTEXT_MULTI_TURN_VISION_SESSION
import com.google.ai.edge.gallery.runtime.LlmModelHelper
import com.google.ai.edge.gallery.runtime.ResultListener
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.Role
import com.google.ai.edge.litertlm.ToolProvider
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

private const val TAG = "AGGeminiCloudHelper"
private const val GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
private const val EXTRA_CONTEXT_EPHEMERAL_INFERENCE = "aicore_ephemeral_inference"
private const val DEFAULT_CLOUD_MAX_TOKENS = 2048

data class GeminiTurn(val role: String, val text: String)

data class GeminiCloudModelInstance(
  val apiModelId: String,
  val chatHistory: MutableList<GeminiTurn> = CopyOnWriteArrayList(),
  @Volatile var systemInstructionText: String? = null,
  @Volatile var activeSessionImages: List<Bitmap> = emptyList(),
  @Volatile var multiTurnVisionSession: Boolean = false,
  @Volatile var inferenceJob: Job? = null,
  val fallbackScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
)

/**
 * Cloud [LlmModelHelper] implementation calling the Google Gemini REST API (`generateContent` /
 * `streamGenerateContent?alt=sse`) using the user-configured API key from [GeminiApiKeyStore].
 */
object GeminiCloudModelHelper : LlmModelHelper {
  private val gson = Gson()
  private val cleanUpListeners = ConcurrentHashMap<String, CleanUpListener>()
  internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

  override fun initialize(
    context: Context,
    model: Model,
    taskId: String,
    supportImage: Boolean,
    supportAudio: Boolean,
    onDone: (String) -> Unit,
    systemInstruction: Contents?,
    tools: List<ToolProvider>,
    enableConversationConstrainedDecoding: Boolean,
    initialMessages: List<Message>,
    coroutineScope: CoroutineScope?,
    enableResponseFormat: Boolean,
  ) {
    if (!GeminiApiKeyStore.hasApiKey()) {
      val msg = missingApiKeyMessage()
      model.markInitializationFailed(IllegalStateException(msg))
      onDone(msg)
      return
    }
    val instance =
      GeminiCloudModelInstance(
        apiModelId = resolveApiModelId(model),
        chatHistory = CopyOnWriteArrayList(toGeminiTurns(initialMessages)),
        systemInstructionText = systemInstruction?.toString()?.takeIf { it.isNotBlank() },
      )
    model.markInitialized(instance)
    onDone("")
  }

  override suspend fun loadModel(
    context: Context,
    model: Model,
    taskId: String,
    supportImage: Boolean,
    supportAudio: Boolean,
    onProgress: (String) -> Unit,
  ): String? {
    if (!GeminiApiKeyStore.hasApiKey()) {
      val msg = missingApiKeyMessage()
      model.markInitializationFailed(IllegalStateException(msg))
      return msg
    }
    if (model.instance is GeminiCloudModelInstance) {
      return null
    }
    model.markInitialized(GeminiCloudModelInstance(apiModelId = resolveApiModelId(model)))
    return null
  }

  override fun resetConversation(
    model: Model,
    supportImage: Boolean,
    supportAudio: Boolean,
    systemInstruction: Contents?,
    tools: List<ToolProvider>,
    enableConversationConstrainedDecoding: Boolean,
    initialMessages: List<Message>,
    enableResponseFormat: Boolean,
  ) {
    if (model.instance !is GeminiCloudModelInstance) return
    try {
      resetConversationOrThrow(
        model = model,
        supportImage = supportImage,
        supportAudio = supportAudio,
        systemInstruction = systemInstruction,
        tools = tools,
        enableConversationConstrainedDecoding = enableConversationConstrainedDecoding,
        initialMessages = initialMessages,
        enableResponseFormat = enableResponseFormat,
      )
    } catch (e: Exception) {
      Log.e(TAG, "Failed to reset Gemini Cloud conversation", e)
    }
  }

  override fun resetConversationOrThrow(
    model: Model,
    supportImage: Boolean,
    supportAudio: Boolean,
    systemInstruction: Contents?,
    tools: List<ToolProvider>,
    enableConversationConstrainedDecoding: Boolean,
    initialMessages: List<Message>,
    enableResponseFormat: Boolean,
  ) {
    val instance =
      model.instance as? GeminiCloudModelInstance
        ?: throw IllegalStateException("Gemini Cloud model '${model.name}' is not loaded.")
    instance.inferenceJob?.cancel()
    instance.chatHistory.clear()
    instance.chatHistory.addAll(toGeminiTurns(initialMessages))
    instance.systemInstructionText = systemInstruction?.toString()?.takeIf { it.isNotBlank() }
    instance.multiTurnVisionSession = false
    recycleSessionImages(instance.activeSessionImages)
    instance.activeSessionImages = emptyList()
  }

  override fun cleanUp(model: Model, onDone: () -> Unit) {
    cleanUpListeners.remove(model.name)
    val instance = model.instance as? GeminiCloudModelInstance
    if (instance != null) {
      instance.inferenceJob?.cancel()
      recycleSessionImages(instance.activeSessionImages)
      instance.activeSessionImages = emptyList()
      instance.chatHistory.clear()
      instance.fallbackScope.cancel()
    }
    model.resetInitialization()
    onDone()
  }

  override fun runInference(
    model: Model,
    input: String,
    resultListener: ResultListener,
    cleanUpListener: CleanUpListener,
    onError: (message: String) -> Unit,
    images: List<Bitmap>,
    audioClips: List<ByteArray>,
    coroutineScope: CoroutineScope?,
    extraContext: Map<String, String>,
    responseJsonSchema: String?,
  ) {
    val apiKey = GeminiApiKeyStore.getApiKey()
    if (apiKey.isEmpty()) {
      onError(missingApiKeyMessage())
      return
    }
    val instance =
      (model.instance as? GeminiCloudModelInstance)
        ?: GeminiCloudModelInstance(apiModelId = resolveApiModelId(model)).also {
          model.markInitialized(it)
        }
    cleanUpListeners[model.name] = cleanUpListener

    val isEphemeral =
      extraContext[EXTRA_CONTEXT_EPHEMERAL_INFERENCE] == "true" ||
        !responseJsonSchema.isNullOrBlank()
    val useMultiTurnVision =
      !isEphemeral && extraContext[EXTRA_CONTEXT_MULTI_TURN_VISION_SESSION] == "true"
    instance.multiTurnVisionSession = useMultiTurnVision

    val requestImages =
      if (isEphemeral || !useMultiTurnVision) {
        images.filter { !it.isRecycled }
      } else {
        resolveSessionImages(instance, images)
      }

    val temperature =
      model.getFloatConfigValue(key = ConfigKeys.TEMPERATURE, defaultValue = DEFAULT_TEMPERATURE)
    val topP = model.getFloatConfigValue(key = ConfigKeys.TOPP, defaultValue = DEFAULT_TOPP)
    val topK = model.getIntConfigValue(key = ConfigKeys.TOPK, defaultValue = DEFAULT_TOPK)
    val maxOutputTokens =
      extraContext[EXTRA_CONTEXT_MAX_OUTPUT_TOKENS]?.toIntOrNull()?.takeIf { it > 0 }
        ?: model
          .getIntConfigValue(key = ConfigKeys.MAX_OUTPUT_TOKENS, defaultValue = -1)
          .takeIf { it > 0 }
        ?: model.getIntConfigValue(
          key = ConfigKeys.MAX_TOKENS,
          defaultValue = DEFAULT_CLOUD_MAX_TOKENS,
        )

    val sanitizedSchema =
      responseJsonSchema
        ?.takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { sanitizeSchemaForGemini(raw) }.getOrNull() }

    val historySnapshot = if (isEphemeral) emptyList() else instance.chatHistory.toList()
    val systemTextSnapshot = if (isEphemeral) null else instance.systemInstructionText

    val scope = coroutineScope ?: instance.fallbackScope
    instance.inferenceJob?.cancel()
    instance.inferenceJob =
      scope.launch(ioDispatcher) {
        var deliveredDone = false
        try {
          val encodedImages = requestImages.mapNotNull(::encodeBitmapJpegBase64)
          val encodedAudio =
            audioClips.map { clip -> Base64.getEncoder().encodeToString(ensureWavFormat(clip)) }

          val requestBody =
            buildGenerateContentRequestJson(
              systemInstructionText = systemTextSnapshot,
              history = historySnapshot,
              currentTurnText = input,
              base64Images = encodedImages,
              base64AudioClips = encodedAudio,
              temperature = temperature,
              topP = topP,
              topK = topK,
              maxOutputTokens = maxOutputTokens,
              sanitizedResponseSchema = sanitizedSchema,
            )

          val fullResponse =
            if (sanitizedSchema != null) {
              executeUnaryRequest(
                apiModelId = instance.apiModelId,
                apiKey = apiKey,
                requestJson = requestBody,
              )
            } else {
              executeStreamingSseRequest(
                apiModelId = instance.apiModelId,
                apiKey = apiKey,
                requestJson = requestBody,
                onDelta = { delta -> resultListener(delta, false, null) },
              )
            }

          currentCoroutineContext().ensureActive()
          if (!isEphemeral) {
            val recordedUserText =
              input.ifBlank {
                if (encodedAudio.isNotEmpty()) "[Audio]" else "[Image]"
              }
            instance.chatHistory.add(GeminiTurn(role = "user", text = recordedUserText))
            instance.chatHistory.add(GeminiTurn(role = "model", text = fullResponse))
          }
          deliveredDone = true
          if (sanitizedSchema != null) {
            resultListener(fullResponse, true, null)
          } else {
            resultListener("", true, null)
          }
        } catch (e: CancellationException) {
          Log.i(TAG, "Gemini Cloud inference cancelled for '${model.name}'.")
          if (!deliveredDone) {
            cleanUpListener()
          }
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Gemini Cloud inference failed for '${model.name}'", e)
          onError(e.message ?: "Gemini Cloud request failed.")
        }
      }
  }

  override fun stopResponse(model: Model) {
    (model.instance as? GeminiCloudModelInstance)?.inferenceJob?.cancel()
  }

  private fun resolveApiModelId(model: Model): String =
    model.modelId.ifBlank { GeminiCloudModels.apiModelIdFor(model.name) }

  private fun missingApiKeyMessage(): String =
    "Add your Gemini API key in Settings (⚙) to use Cloud Gemini models, or choose an On-Device Gemma model."

  private fun toGeminiTurns(messages: List<Message>): List<GeminiTurn> =
    messages.mapNotNull { msg ->
      val text = msg.contents.toString().trim()
      if (text.isEmpty()) {
        null
      } else {
        GeminiTurn(role = if (msg.role == Role.USER) "user" else "model", text = text)
      }
    }

  private fun resolveSessionImages(
    instance: GeminiCloudModelInstance,
    newImages: List<Bitmap>,
  ): List<Bitmap> {
    val validNew = newImages.filter { !it.isRecycled }
    if (validNew.isEmpty()) {
      return instance.activeSessionImages.filter { !it.isRecycled }
    }
    recycleSessionImages(instance.activeSessionImages)
    val copies = validNew.mapNotNull { it.copy(Bitmap.Config.ARGB_8888, false) }
    instance.activeSessionImages = copies
    return copies
  }

  private fun recycleSessionImages(bitmaps: List<Bitmap>) {
    bitmaps.forEach { bitmap ->
      if (!bitmap.isRecycled) {
        bitmap.recycle()
      }
    }
  }

  private fun encodeBitmapJpegBase64(bitmap: Bitmap): String? =
    try {
      if (bitmap.isRecycled) {
        null
      } else {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        Base64.getEncoder().encodeToString(stream.toByteArray())
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to encode bitmap for Gemini Cloud request", e)
      null
    }

  private fun executeUnaryRequest(
    apiModelId: String,
    apiKey: String,
    requestJson: String,
  ): String {
    val endpoint = "$GEMINI_API_BASE/$apiModelId:generateContent"
    val conn = openPostConnection(endpoint, apiKey, accept = "application/json")
    return try {
      conn.outputStream.use { it.write(requestJson.toByteArray(StandardCharsets.UTF_8)) }
      val code = conn.responseCode
      if (code !in 200..299) {
        throw IllegalStateException(formatHttpError(code, readErrorBody(conn)))
      }
      val body = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
      extractTextFromResponseJson(body).orEmpty()
    } finally {
      conn.disconnect()
    }
  }

  private suspend fun executeStreamingSseRequest(
    apiModelId: String,
    apiKey: String,
    requestJson: String,
    onDelta: (String) -> Unit,
  ): String {
    val endpoint = "$GEMINI_API_BASE/$apiModelId:streamGenerateContent?alt=sse"
    val conn = openPostConnection(endpoint, apiKey, accept = "text/event-stream")
    return try {
      conn.outputStream.use { it.write(requestJson.toByteArray(StandardCharsets.UTF_8)) }
      val code = conn.responseCode
      if (code !in 200..299) {
        throw IllegalStateException(formatHttpError(code, readErrorBody(conn)))
      }
      val fullText = StringBuilder()
      BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8)).use { reader ->
        while (true) {
          currentCoroutineContext().ensureActive()
          val line = reader.readLine() ?: break
          val delta = extractTextFromSseLine(line)
          if (!delta.isNullOrEmpty()) {
            fullText.append(delta)
            onDelta(delta)
          }
        }
      }
      fullText.toString()
    } finally {
      conn.disconnect()
    }
  }

  private fun openPostConnection(endpoint: String, apiKey: String, accept: String): HttpURLConnection {
    val url = URL(endpoint)
    return (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 20_000
      readTimeout = 90_000
      doOutput = true
      setRequestProperty("Content-Type", "application/json; charset=UTF-8")
      setRequestProperty("Accept", accept)
      setRequestProperty("x-goog-api-key", apiKey)
    }
  }

  private fun readErrorBody(conn: HttpURLConnection): String {
    val stream = conn.errorStream ?: return ""
    return runCatching {
        stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
      }
      .getOrDefault("")
  }

  private fun formatHttpError(code: Int, rawBody: String): String {
    val apiMessage =
      runCatching {
          JsonParser.parseString(rawBody)
            .asJsonObject
            .getAsJsonObject("error")
            ?.get("message")
            ?.asString
        }
        .getOrNull()
    return when (code) {
      401,
      403 ->
        "Gemini API key rejected (HTTP $code). Check your API key in Settings (⚙) or switch to an On-Device Gemma model."
      429 ->
        "Gemini API rate limit or quota exceeded (HTTP 429). Try again shortly or use an On-Device Gemma model."
      else ->
        if (!apiMessage.isNullOrBlank()) {
          "Gemini API error ($code): $apiMessage"
        } else {
          "Gemini API request failed (HTTP $code)."
        }
    }
  }

  /**
   * Ensures [rawBytes] carries a 44-byte RIFF/WAVE header so raw 16 kHz mono 16-bit PCM clips and
   * already-wrapped WAV clips can both be sent safely as `audio/wav`.
   */
  internal fun ensureWavFormat(rawBytes: ByteArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
    if (
      rawBytes.size >= 12 &&
        rawBytes[0] == 'R'.code.toByte() &&
        rawBytes[1] == 'I'.code.toByte() &&
        rawBytes[2] == 'F'.code.toByte() &&
        rawBytes[3] == 'F'.code.toByte() &&
        rawBytes[8] == 'W'.code.toByte() &&
        rawBytes[9] == 'A'.code.toByte() &&
        rawBytes[10] == 'V'.code.toByte() &&
        rawBytes[11] == 'E'.code.toByte()
    ) {
      return rawBytes
    }
    val dataSize = rawBytes.size
    val totalDataLen = dataSize + 36
    val channels = 1
    val bitsPerSample = 16
    val byteRate = sampleRate * channels * (bitsPerSample / 8)
    val blockAlign = channels * (bitsPerSample / 8)

    val header =
      ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray(StandardCharsets.US_ASCII))
        putInt(totalDataLen)
        put("WAVE".toByteArray(StandardCharsets.US_ASCII))
        put("fmt ".toByteArray(StandardCharsets.US_ASCII))
        putInt(16) // PCM subchunk size
        putShort(1.toShort()) // AudioFormat = 1 (PCM)
        putShort(channels.toShort())
        putInt(sampleRate)
        putInt(byteRate)
        putShort(blockAlign.toShort())
        putShort(bitsPerSample.toShort())
        put("data".toByteArray(StandardCharsets.US_ASCII))
        putInt(dataSize)
      }
    return ByteArray(44 + dataSize).also { out ->
      System.arraycopy(header.array(), 0, out, 0, 44)
      System.arraycopy(rawBytes, 0, out, 44, dataSize)
    }
  }

  /**
   * Sanitizes a LiteRT-LM constrained JSON schema for Gemini REST `responseJsonSchema` by stripping
   * unsupported `x-guidance` and `pattern` directives recursively while preserving `type`,
   * `properties`, `description`, `required`, `items`, and `enum`.
   */
  internal fun sanitizeSchemaForGemini(rawSchema: String): JsonObject {
    val parsed = JsonParser.parseString(rawSchema).asJsonObject
    return stripUnsupportedSchemaKeys(parsed)
  }

  private fun stripUnsupportedSchemaKeys(source: JsonObject): JsonObject {
    val result = JsonObject()
    for ((key, value) in source.entrySet()) {
      if (key == "x-guidance" || key == "pattern") {
        continue
      }
      result.add(key, sanitizeSchemaElement(value))
    }
    return result
  }

  private fun sanitizeSchemaElement(element: com.google.gson.JsonElement): com.google.gson.JsonElement {
    return when {
      element.isJsonObject -> stripUnsupportedSchemaKeys(element.asJsonObject)
      element.isJsonArray ->
        JsonArray().apply {
          element.asJsonArray.forEach { child -> add(sanitizeSchemaElement(child)) }
        }
      else -> element.deepCopy()
    }
  }

  internal fun buildGenerateContentRequestJson(
    systemInstructionText: String?,
    history: List<GeminiTurn>,
    currentTurnText: String,
    base64Images: List<String> = emptyList(),
    base64AudioClips: List<String> = emptyList(),
    temperature: Float = DEFAULT_TEMPERATURE,
    topP: Float = DEFAULT_TOPP,
    topK: Int = DEFAULT_TOPK,
    maxOutputTokens: Int = DEFAULT_CLOUD_MAX_TOKENS,
    sanitizedResponseSchema: JsonObject? = null,
  ): String {
    val root = JsonObject()

    if (!systemInstructionText.isNullOrBlank()) {
      val sysParts = JsonArray().apply { add(JsonObject().apply { addProperty("text", systemInstructionText) }) }
      root.add("systemInstruction", JsonObject().apply { add("parts", sysParts) })
    }

    val contents = JsonArray()
    for (turn in history) {
      val parts = JsonArray().apply { add(JsonObject().apply { addProperty("text", turn.text) }) }
      contents.add(
        JsonObject().apply {
          addProperty("role", turn.role)
          add("parts", parts)
        }
      )
    }

    val userParts = JsonArray()
    for (imgB64 in base64Images) {
      userParts.add(inlineDataPart("image/jpeg", imgB64))
    }
    for (audioB64 in base64AudioClips) {
      userParts.add(inlineDataPart("audio/wav", audioB64))
    }
    if (currentTurnText.isNotEmpty() || (base64Images.isEmpty() && base64AudioClips.isEmpty())) {
      userParts.add(JsonObject().apply { addProperty("text", currentTurnText) })
    }
    contents.add(
      JsonObject().apply {
        addProperty("role", "user")
        add("parts", userParts)
      }
    )
    root.add("contents", contents)

    val generationConfig =
      JsonObject().apply {
        addProperty("temperature", temperature)
        addProperty("topP", topP)
        addProperty("topK", topK)
        addProperty("maxOutputTokens", maxOutputTokens)
        if (sanitizedResponseSchema != null) {
          addProperty("responseMimeType", "application/json")
          add("responseJsonSchema", sanitizedResponseSchema)
        }
      }
    root.add("generationConfig", generationConfig)

    return gson.toJson(root)
  }

  private fun inlineDataPart(mimeType: String, base64Data: String): JsonObject =
    JsonObject().apply {
      add(
        "inlineData",
        JsonObject().apply {
          addProperty("mimeType", mimeType)
          addProperty("data", base64Data)
        },
      )
    }

  internal fun extractTextFromSseLine(line: String): String? {
    val trimmed = line.trim()
    if (!trimmed.startsWith("data:")) return null
    val payload = trimmed.removePrefix("data:").trim()
    if (payload.isEmpty() || payload == "[DONE]") return null
    return extractTextFromResponseJson(payload)
  }

  internal fun extractTextFromResponseJson(rawJson: String): String? {
    val obj = runCatching { JsonParser.parseString(rawJson).asJsonObject }.getOrNull() ?: return null
    val candidates = obj.getAsJsonArray("candidates") ?: return null
    if (candidates.size() == 0) return null
    val first = candidates[0]?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
    val content = first.getAsJsonObject("content") ?: return null
    val parts = content.getAsJsonArray("parts") ?: return null
    val sb = StringBuilder()
    for (partEl in parts) {
      val part = partEl.takeIf { it.isJsonObject }?.asJsonObject ?: continue
      val isThought = part.get("thought")?.takeIf { it.isJsonPrimitive }?.asBoolean == true
      if (isThought) continue
      val text = part.get("text")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
      sb.append(text)
    }
    return sb.toString().takeIf { it.isNotEmpty() }
  }
}
