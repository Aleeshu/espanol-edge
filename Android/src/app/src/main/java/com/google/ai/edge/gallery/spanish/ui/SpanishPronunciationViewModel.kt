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

package com.google.ai.edge.gallery.spanish.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.DataStoreRepository
import com.google.ai.edge.gallery.spanish.PronunciationPracticeMode
import com.google.ai.edge.gallery.spanish.SpanishLearningSettings
import com.google.ai.edge.gallery.spanish.SpanishPracticePhrase
import com.google.ai.edge.gallery.spanish.SpanishPracticePhrases
import com.google.ai.edge.gallery.spanish.SpanishPromptBuilder
import com.google.ai.edge.gallery.spanish.SpanishTtsHelper
import com.google.ai.edge.gallery.spanish.SpanishVocabRepository
import com.google.ai.edge.gallery.spanish.code
import com.google.ai.edge.gallery.spanish.localeTag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Coordinates target phrase drills, 1.0x/0.7x TTS playback, and coaching prompts for Audio Lab. */
@HiltViewModel
class SpanishPronunciationViewModel
@Inject
constructor(
  private val dataStoreRepository: DataStoreRepository,
  private val spanishVocabRepository: SpanishVocabRepository,
  private val spanishTtsHelper: SpanishTtsHelper,
) : ViewModel() {

  val settings: StateFlow<SpanishLearningSettings> =
    dataStoreRepository.spanishLearningSettingsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = dataStoreRepository.readSpanishLearningSettings(),
    )

  private val _mode = MutableStateFlow(PronunciationPracticeMode.SHADOWING)
  val mode: StateFlow<PronunciationPracticeMode> = _mode.asStateFlow()

  private val _phraseIndex = MutableStateFlow(0)
  val phraseIndex: StateFlow<Int> = _phraseIndex.asStateFlow()

  private val _saveFeedback = MutableStateFlow<String?>(null)
  val saveFeedback: StateFlow<String?> = _saveFeedback.asStateFlow()

  val currentPhrase: StateFlow<SpanishPracticePhrase?> =
    combine(settings, _mode, _phraseIndex) { curSettings, curMode, index ->
        if (curMode == PronunciationPracticeMode.FREE_SPEAKING) {
          null
        } else {
          val phrases = SpanishPracticePhrases.forModeAndLevel(curMode, curSettings.cefrLevel)
          phrases.getOrNull(index.mod(phrases.size))
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue =
          SpanishPracticePhrases.forModeAndLevel(
              PronunciationPracticeMode.SHADOWING,
              dataStoreRepository.readSpanishLearningSettings().cefrLevel,
            )
            .firstOrNull(),
      )

  fun selectMode(newMode: PronunciationPracticeMode) {
    _mode.value = newMode
    _phraseIndex.value = 0
  }

  fun nextPhrase() {
    val phrases = SpanishPracticePhrases.forModeAndLevel(_mode.value, settings.value.cefrLevel)
    if (phrases.isNotEmpty()) {
      _phraseIndex.value = (_phraseIndex.value + 1) % phrases.size
    }
  }

  fun previousPhrase() {
    val phrases = SpanishPracticePhrases.forModeAndLevel(_mode.value, settings.value.cefrLevel)
    if (phrases.isNotEmpty()) {
      _phraseIndex.value = (_phraseIndex.value - 1 + phrases.size) % phrases.size
    }
  }

  fun playCurrentPhrase(slow: Boolean) {
    val phrase = currentPhrase.value ?: return
    spanishTtsHelper.speak(
      text = phrase.spanish,
      slow = slow,
      localeTag = settings.value.dialect.localeTag,
    )
  }

  fun saveCurrentPhraseToFlashcards() {
    val phrase = currentPhrase.value ?: return
    val word = phrase.targetWord.ifBlank { phrase.spanish }
    viewModelScope.launch {
      spanishVocabRepository.ensureStarterDeckSeeded()
      spanishVocabRepository.saveWord(
        word = word,
        translation = phrase.english,
        targetSentence = phrase.spanish,
        nativeSentence = phrase.english,
        definition = "${phrase.minLevel.code} · Pronunciation drill",
        notes = phrase.phoneticTip,
        sourceModule = "pronunciation",
      )
      _saveFeedback.value = "Saved \"$word\" to flashcards"
    }
  }

  fun clearSaveFeedback() {
    _saveFeedback.value = null
  }

  fun buildSystemPrompt(
    curSettings: SpanishLearningSettings = settings.value,
    curMode: PronunciationPracticeMode = _mode.value,
    targetPhrase: SpanishPracticePhrase? = currentPhrase.value,
  ): String =
    SpanishPromptBuilder.buildPronunciationSystemPrompt(
      settings = curSettings,
      mode = curMode,
      targetPhrase = targetPhrase,
    )

  override fun onCleared() {
    spanishTtsHelper.stop()
    super.onCleared()
  }
}
