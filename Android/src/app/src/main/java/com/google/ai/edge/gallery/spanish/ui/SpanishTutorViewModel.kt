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
import com.google.ai.edge.gallery.spanish.SpanishLearningSettings
import com.google.ai.edge.gallery.spanish.SpanishPromptBuilder
import com.google.ai.edge.gallery.spanish.SpanishRoleplayScenario
import com.google.ai.edge.gallery.spanish.SpanishTtsHelper
import com.google.ai.edge.gallery.spanish.SpanishVocabRepository
import com.google.ai.edge.gallery.spanish.localeTag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Coordinates scenario roleplay state, Spanish settings sync, TTS, and flashcard saving for Chat. */
@HiltViewModel
class SpanishTutorViewModel
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

  private val _selectedScenario = MutableStateFlow(SpanishRoleplayScenario.FREE_CHAT)
  val selectedScenario: StateFlow<SpanishRoleplayScenario> = _selectedScenario.asStateFlow()

  private val _saveFeedback = MutableStateFlow<String?>(null)
  val saveFeedback: StateFlow<String?> = _saveFeedback.asStateFlow()

  fun selectScenario(scenario: SpanishRoleplayScenario) {
    _selectedScenario.value = scenario
  }

  fun buildSystemPrompt(
    curSettings: SpanishLearningSettings = settings.value,
    scenario: SpanishRoleplayScenario = _selectedScenario.value,
  ): String = SpanishPromptBuilder.buildTutorSystemPrompt(curSettings, scenario)

  fun saveVocabWord(
    word: String,
    translation: String,
    exampleSentence: String = "",
    notes: String = "",
  ) {
    val cleanWord = word.trim()
    val cleanTranslation = translation.trim()
    if (cleanWord.isEmpty() || cleanTranslation.isEmpty()) return
    viewModelScope.launch {
      spanishVocabRepository.ensureStarterDeckSeeded()
      spanishVocabRepository.saveWord(
        word = cleanWord,
        translation = cleanTranslation,
        targetSentence = exampleSentence.trim(),
        definition = "${_selectedScenario.value.title} (${settings.value.dialect.name.lowercase()})",
        notes = notes.trim(),
        sourceModule = "chat",
      )
      _saveFeedback.value = "Saved \"$cleanWord\" to flashcards"
    }
  }

  fun clearSaveFeedback() {
    _saveFeedback.value = null
  }

  fun speakSpanish(text: String, slow: Boolean = false) {
    spanishTtsHelper.speak(text = text, slow = slow, localeTag = settings.value.dialect.localeTag)
  }

  override fun onCleared() {
    spanishTtsHelper.stop()
    super.onCleared()
  }
}
