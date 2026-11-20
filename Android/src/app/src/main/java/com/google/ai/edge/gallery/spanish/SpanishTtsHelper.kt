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

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "AGSpanishTtsHelper"
private const val NORMAL_SPEECH_RATE = 1.0f
private const val SLOW_SPEECH_RATE = 0.7f

private data class PendingSpeechRequest(
  val text: String,
  val slow: Boolean,
  val localeTag: String,
)

/**
 * Lightweight Android [TextToSpeech] helper tailored for Spanish regional accents and slow 0.7x
 * learner playback.
 */
@Singleton
class SpanishTtsHelper @Inject constructor(@param:ApplicationContext private val context: Context) {
  private var tts: TextToSpeech? = null
  private val _isReady = MutableStateFlow(false)
  val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

  private var pendingRequest: PendingSpeechRequest? = null

  private fun ensureInitialized() {
    if (tts != null) return
    tts =
      TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
          _isReady.value = true
          pendingRequest?.let { req ->
            pendingRequest = null
            speakInternal(req.text, req.slow, req.localeTag)
          }
        } else {
          Log.w(TAG, "Android TextToSpeech initialization failed with status=$status")
          _isReady.value = false
          pendingRequest = null
        }
      }
  }

  /** Speaks [text] in Spanish using [localeTag] at 1.0x or 0.7x speed when [slow] is true. */
  fun speak(
    text: String,
    slow: Boolean = false,
    localeTag: String = "es-US",
  ) {
    val clean = text.trim()
    if (clean.isEmpty()) return

    if (tts == null || !_isReady.value) {
      pendingRequest = PendingSpeechRequest(clean, slow, localeTag)
      ensureInitialized()
      return
    }
    speakInternal(clean, slow, localeTag)
  }

  private fun speakInternal(text: String, slow: Boolean, localeTag: String) {
    val engine = tts ?: return
    val preferredLocale = Locale.forLanguageTag(localeTag)
    val preferredResult = engine.setLanguage(preferredLocale)
    if (
      preferredResult == TextToSpeech.LANG_MISSING_DATA ||
        preferredResult == TextToSpeech.LANG_NOT_SUPPORTED
    ) {
      val fallbackResult = engine.setLanguage(Locale("es", "ES"))
      if (
        fallbackResult == TextToSpeech.LANG_MISSING_DATA ||
          fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED
      ) {
        engine.setLanguage(Locale("es"))
      }
    }
    engine.setSpeechRate(if (slow) SLOW_SPEECH_RATE else NORMAL_SPEECH_RATE)
    engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "spanish_tts_${System.nanoTime()}")
  }

  fun stop() {
    pendingRequest = null
    tts?.stop()
  }

  fun shutdown() {
    pendingRequest = null
    _isReady.value = false
    tts?.stop()
    tts?.shutdown()
    tts = null
  }
}
