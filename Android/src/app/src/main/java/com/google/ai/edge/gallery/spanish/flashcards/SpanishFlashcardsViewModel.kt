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

package com.google.ai.edge.gallery.spanish.flashcards

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.DataStoreRepository
import com.google.ai.edge.gallery.proto.DictionaryEntry
import com.google.ai.edge.gallery.spanish.ReviewRating
import com.google.ai.edge.gallery.spanish.SpanishLearningSettings
import com.google.ai.edge.gallery.spanish.SpanishSrsScheduler
import com.google.ai.edge.gallery.spanish.SpanishTtsHelper
import com.google.ai.edge.gallery.spanish.SpanishVocabRepository
import com.google.ai.edge.gallery.spanish.localeTag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FlashcardDeckFilter(val label: String) {
  ALL("All"),
  DUE("Due"),
  CAMERA("Camera"),
  CHAT("Chat"),
  STARTER("Starter"),
}

data class SpanishFlashcardsUiState(
  val allCards: List<DictionaryEntry> = emptyList(),
  val dueCards: List<DictionaryEntry> = emptyList(),
  val filteredDeckCards: List<DictionaryEntry> = emptyList(),
  val masteredCount: Int = 0,
  val currentStudyCard: DictionaryEntry? = null,
  val isCardFlipped: Boolean = false,
  val practiceAllMode: Boolean = false,
  val searchQuery: String = "",
  val selectedFilter: FlashcardDeckFilter = FlashcardDeckFilter.ALL,
  val settings: SpanishLearningSettings = SpanishLearningSettings(),
)

/** ViewModel driving Spanish Flashcards study sessions, SM-2 scheduling, TTS, and deck CRUD. */
@HiltViewModel
class SpanishFlashcardsViewModel
@Inject
constructor(
  private val vocabRepository: SpanishVocabRepository,
  dataStoreRepository: DataStoreRepository,
  private val ttsHelper: SpanishTtsHelper,
) : ViewModel() {

  private val searchQuery = MutableStateFlow("")
  private val selectedFilter = MutableStateFlow(FlashcardDeckFilter.ALL)
  private val isCardFlipped = MutableStateFlow(false)
  private val practiceAllMode = MutableStateFlow(false)
  private val practiceAllIndex = MutableStateFlow(0)

  private data class DeckControlsState(
    val query: String,
    val filter: FlashcardDeckFilter,
    val flipped: Boolean,
    val practiceAll: Boolean,
    val practiceIndex: Int,
  )

  private val controlsFlow =
    combine(
      searchQuery,
      selectedFilter,
      isCardFlipped,
      practiceAllMode,
      practiceAllIndex,
    ) { query, filter, flipped, practiceAll, index ->
      DeckControlsState(
        query = query,
        filter = filter,
        flipped = flipped,
        practiceAll = practiceAll,
        practiceIndex = index,
      )
    }

  val uiState: StateFlow<SpanishFlashcardsUiState> =
    combine(
        vocabRepository.spanishEntriesFlow,
        dataStoreRepository.spanishLearningSettingsFlow,
        controlsFlow,
      ) { allCards, settings, controls ->
        val nowMs = System.currentTimeMillis()
        val dueCards = SpanishSrsScheduler.sortDueCards(allCards, nowMs)
        val masteredCount = allCards.count { SpanishSrsScheduler.isMastered(it) }

        val effectivePracticeAll = controls.practiceAll && allCards.isNotEmpty()
        val currentStudyCard =
          when {
            effectivePracticeAll -> allCards[controls.practiceIndex.mod(allCards.size)]
            dueCards.isNotEmpty() -> dueCards.first()
            else -> null
          }

        val normalizedQuery = controls.query.trim()
        val filteredDeckCards =
          allCards.filter { entry ->
            val matchesFilter =
              when (controls.filter) {
                FlashcardDeckFilter.ALL -> true
                FlashcardDeckFilter.DUE -> SpanishSrsScheduler.isDue(entry, nowMs)
                FlashcardDeckFilter.CAMERA ->
                  entry.sourceModule.equals("camera", ignoreCase = true) ||
                    entry.sourceModule.isBlank()
                FlashcardDeckFilter.CHAT ->
                  entry.sourceModule.equals("chat", ignoreCase = true) ||
                    entry.sourceModule.equals("pronunciation", ignoreCase = true)
                FlashcardDeckFilter.STARTER ->
                  entry.sourceModule.equals("starter", ignoreCase = true)
              }
            val matchesQuery =
              normalizedQuery.isEmpty() ||
                entry.word.contains(normalizedQuery, ignoreCase = true) ||
                entry.translation.contains(normalizedQuery, ignoreCase = true) ||
                entry.targetSentence.contains(normalizedQuery, ignoreCase = true)
            matchesFilter && matchesQuery
          }

        SpanishFlashcardsUiState(
          allCards = allCards,
          dueCards = dueCards,
          filteredDeckCards = filteredDeckCards,
          masteredCount = masteredCount,
          currentStudyCard = currentStudyCard,
          isCardFlipped = controls.flipped,
          practiceAllMode = effectivePracticeAll,
          searchQuery = controls.query,
          selectedFilter = controls.filter,
          settings = settings,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SpanishFlashcardsUiState(),
      )

  init {
    viewModelScope.launch { vocabRepository.ensureStarterDeckSeeded() }
  }

  fun flipCard() {
    isCardFlipped.value = !isCardFlipped.value
  }

  fun rateCurrentCard(rating: ReviewRating) {
    val card = uiState.value.currentStudyCard ?: return
    ttsHelper.stop()
    isCardFlipped.value = false
    if (practiceAllMode.value) {
      practiceAllIndex.value = practiceAllIndex.value + 1
    }
    viewModelScope.launch { vocabRepository.recordReview(card.word, rating) }
  }

  fun startPracticeAll() {
    isCardFlipped.value = false
    practiceAllIndex.value = 0
    practiceAllMode.value = true
  }

  fun exitPracticeAll() {
    isCardFlipped.value = false
    practiceAllMode.value = false
  }

  fun speak(text: String, slow: Boolean = false) {
    ttsHelper.speak(
      text = text,
      slow = slow,
      localeTag = uiState.value.settings.dialect.localeTag,
    )
  }

  fun updateSearchQuery(query: String) {
    searchQuery.value = query
  }

  fun selectFilter(filter: FlashcardDeckFilter) {
    selectedFilter.value = filter
  }

  fun addCard(
    word: String,
    translation: String,
    targetSentence: String = "",
    nativeSentence: String = "",
    notes: String = "",
  ) {
    viewModelScope.launch {
      vocabRepository.saveWord(
        word = word,
        translation = translation,
        targetSentence = targetSentence,
        nativeSentence = nativeSentence,
        notes = notes,
        sourceModule = "manual",
      )
    }
  }

  fun deleteCard(word: String) {
    viewModelScope.launch { vocabRepository.deleteWord(word) }
  }

  fun resetProgress() {
    isCardFlipped.value = false
    practiceAllMode.value = false
    practiceAllIndex.value = 0
    viewModelScope.launch { vocabRepository.resetProgress() }
  }

  fun restoreStarterDeck() {
    viewModelScope.launch { vocabRepository.restoreStarterDeck() }
  }

  suspend fun loadThumbnail(entry: DictionaryEntry): ImageBitmap? =
    vocabRepository.loadVocabThumbnail(entry)

  override fun onCleared() {
    ttsHelper.stop()
    super.onCleared()
  }
}
