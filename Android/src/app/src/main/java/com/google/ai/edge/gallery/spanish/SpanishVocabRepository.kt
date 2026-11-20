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
import android.content.SharedPreferences
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import com.google.ai.edge.gallery.customtasks.xrlanguageteacher.models.XrLanguageTeacherDialogueFormatter
import com.google.ai.edge.gallery.customtasks.xrlanguageteacher.models.XrLanguageTeacherDictionary
import com.google.ai.edge.gallery.customtasks.xrlanguageteacher.models.XrLanguageTeacherPreferencesStore
import com.google.ai.edge.gallery.di.IoDispatcher
import com.google.ai.edge.gallery.proto.Dictionary
import com.google.ai.edge.gallery.proto.DictionaryEntry
import com.google.ai.edge.gallery.proto.Language
import com.google.ai.edge.gallery.proto.dictionaryEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val SPANISH_VOCAB_PREFS = "espanol_edge_vocab_prefs"
private const val KEY_STARTER_DECK_SEEDED = "starter_deck_seeded_v1"

/**
 * Shared repository for Spanish vocabulary flashcards backed by `@XrLanguageTeacherDictionary`
 * [DataStore], allowing Camera Spanish Tutor, Spanish Tutor Chat, Pronunciation Lab, and
 * Flashcards & SRS to share one live deck.
 */
@Singleton
class SpanishVocabRepository
@Inject
constructor(
  @param:ApplicationContext private val context: Context,
  @param:XrLanguageTeacherDictionary private val dictionaryDataStore: DataStore<Dictionary>,
  private val xrStorageRepository: XrLanguageTeacherPreferencesStore,
  @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
  private val prefs: SharedPreferences by lazy {
    context.getSharedPreferences(SPANISH_VOCAB_PREFS, Context.MODE_PRIVATE)
  }
  private val seedMutex = Mutex()
  private val seededInMemory = AtomicBoolean(false)

  /** Live stream of all saved Spanish entries in the shared dictionary. */
  val spanishEntriesFlow: Flow<List<DictionaryEntry>> =
    dictionaryDataStore.data
      .catch { emit(Dictionary.getDefaultInstance()) }
      .map { dict ->
        dict.entriesList.filter {
          it.learnLanguage == Language.LANGUAGE_SPANISH ||
            it.learnLanguage == Language.LANGUAGE_UNSPECIFIED
        }
      }
      .distinctUntilChanged()

  /** Seeds the starter deck once on first launch when no Spanish starter pass has run yet. */
  suspend fun ensureStarterDeckSeeded() {
    if (seededInMemory.get() || prefs.getBoolean(KEY_STARTER_DECK_SEEDED, false)) {
      seededInMemory.set(true)
      return
    }
    seedMutex.withLock {
      if (seededInMemory.get() || prefs.getBoolean(KEY_STARTER_DECK_SEEDED, false)) {
        seededInMemory.set(true)
        return
      }
      withContext(ioDispatcher) {
        dictionaryDataStore.updateData { current ->
          val hasAnySpanish =
            current.entriesList.any { it.learnLanguage == Language.LANGUAGE_SPANISH }
          if (hasAnySpanish) {
            return@updateData current
          }
          val builder = current.toBuilder()
          SpanishStarterDeck.entries.forEach { starter ->
            if (builder.entriesList.none { matchesWord(it, starter.word) }) {
              builder.addEntries(starter)
            }
          }
          builder.build()
        }
        prefs.edit { putBoolean(KEY_STARTER_DECK_SEEDED, true) }
        seededInMemory.set(true)
      }
    }
  }

  /** Saves or updates a Spanish vocabulary word from any module into the shared deck. */
  suspend fun saveWord(
    word: String,
    translation: String,
    targetSentence: String = "",
    nativeSentence: String = "",
    definition: String = "",
    notes: String = "",
    sourceModule: String = "manual",
  ) {
    val cleanWord = word.trim()
    val cleanTranslation = translation.trim()
    if (cleanWord.isEmpty() || cleanTranslation.isEmpty()) return

    withContext(ioDispatcher) {
      dictionaryDataStore.updateData { current ->
        val existingIndex = current.entriesList.indexOfFirst { matchesWord(it, cleanWord) }
        if (existingIndex >= 0) {
          val existing = current.getEntries(existingIndex)
          val updated =
            existing
              .toBuilder()
              .setWord(cleanWord)
              .setTranslation(cleanTranslation)
              .setLearnLanguage(Language.LANGUAGE_SPANISH)
              .setMainLanguage(Language.LANGUAGE_ENGLISH)
              .apply {
                if (targetSentence.isNotBlank()) setTargetSentence(targetSentence.trim())
                if (nativeSentence.isNotBlank()) setNativeSentence(nativeSentence.trim())
                if (definition.isNotBlank()) setDefinition(definition.trim())
                if (notes.isNotBlank()) setNotes(notes.trim())
                if (sourceModule.isNotBlank()) setSourceModule(sourceModule)
                if (easeFactor <= 0f) setEaseFactor(SpanishSrsScheduler.DEFAULT_EASE_FACTOR)
              }
              .build()
          current.toBuilder().setEntries(existingIndex, updated).build()
        } else {
          val created = dictionaryEntry {
            this.word = cleanWord
            this.translation = cleanTranslation
            this.mainLanguage = Language.LANGUAGE_ENGLISH
            this.learnLanguage = Language.LANGUAGE_SPANISH
            this.targetSentence = targetSentence.trim()
            this.nativeSentence = nativeSentence.trim()
            this.definition = definition.trim()
            this.notes = notes.trim()
            this.sourceModule = sourceModule.ifBlank { "manual" }
            this.easeFactor = SpanishSrsScheduler.DEFAULT_EASE_FACTOR
          }
          current.toBuilder().addEntries(created).build()
        }
      }
    }
  }

  /** Applies an SM-2 [rating] to [word] and persists the updated schedule. */
  suspend fun recordReview(
    word: String,
    rating: ReviewRating,
    nowMs: Long = System.currentTimeMillis(),
  ) {
    withContext(ioDispatcher) {
      dictionaryDataStore.updateData { current ->
        val index = current.entriesList.indexOfFirst { matchesWord(it, word) }
        if (index < 0) return@updateData current
        val existing = current.getEntries(index)
        val schedule = SpanishSrsScheduler.scheduleNext(existing, rating, nowMs)
        val updated =
          existing
            .toBuilder()
            .setNextReviewAtMs(schedule.nextReviewAtMs)
            .setIntervalDays(schedule.intervalDays)
            .setRepetitions(schedule.repetitions)
            .setEaseFactor(schedule.easeFactor)
            .setLastReviewedAtMs(schedule.lastReviewedAtMs)
            .build()
        current.toBuilder().setEntries(index, updated).build()
      }
    }
  }

  /** Deletes a card matching [word] from the deck along with any camera photo/sticker files. */
  suspend fun deleteWord(word: String) {
    withContext(ioDispatcher) {
      var deletedTarget: DictionaryEntry? = null
      var otherLangHasSameWord = false
      dictionaryDataStore.updateData { current ->
        val index = current.entriesList.indexOfFirst { matchesWord(it, word) }
        if (index < 0) return@updateData current
        val target = current.getEntries(index)
        deletedTarget = target
        otherLangHasSameWord =
          current.entriesList.any {
            it.learnLanguage != target.learnLanguage &&
              it.word.equals(target.word, ignoreCase = true)
          }
        current.toBuilder().removeEntries(index).build()
      }
      deletedTarget?.let { target ->
        val scopedName =
          XrLanguageTeacherDialogueFormatter.sanitizeWordForFilename(
            target.word,
            target.learnLanguage,
          )
        xrStorageRepository.deleteVocabImageFiles(
          scopedFileName = scopedName,
          unscopedSafeFileName =
            if (!otherLangHasSameWord) {
              XrLanguageTeacherDialogueFormatter.sanitizeWordForFilename(target.word)
            } else {
              null
            },
          legacyWord = if (!otherLangHasSameWord) target.word else null,
        )
      }
    }
  }

  /** Resets SM-2 review progress on all Spanish cards (making all cards due again). */
  suspend fun resetProgress() {
    withContext(ioDispatcher) {
      dictionaryDataStore.updateData { current ->
        val builder = current.toBuilder()
        for (i in 0 until builder.entriesCount) {
          val entry = builder.getEntries(i)
          if (
            entry.learnLanguage == Language.LANGUAGE_SPANISH ||
              entry.learnLanguage == Language.LANGUAGE_UNSPECIFIED
          ) {
            builder.setEntries(
              i,
              entry
                .toBuilder()
                .clearNextReviewAtMs()
                .clearIntervalDays()
                .clearRepetitions()
                .setEaseFactor(SpanishSrsScheduler.DEFAULT_EASE_FACTOR)
                .clearLastReviewedAtMs()
                .build(),
            )
          }
        }
        builder.build()
      }
    }
  }

  /** Restores any missing cards from the 16-card curated starter deck. */
  suspend fun restoreStarterDeck() {
    withContext(ioDispatcher) {
      dictionaryDataStore.updateData { current ->
        val builder = current.toBuilder()
        SpanishStarterDeck.entries.forEach { starter ->
          if (builder.entriesList.none { matchesWord(it, starter.word) }) {
            builder.addEntries(starter)
          }
        }
        builder.build()
      }
      prefs.edit { putBoolean(KEY_STARTER_DECK_SEEDED, true) }
      seededInMemory.set(true)
    }
  }

  /** Loads the saved camera thumbnail or sticker image for [entry], if available. */
  suspend fun loadVocabThumbnail(entry: DictionaryEntry): ImageBitmap? =
    withContext(ioDispatcher) {
      val learnLang =
        if (entry.learnLanguage == Language.LANGUAGE_UNSPECIFIED) {
          Language.LANGUAGE_SPANISH
        } else {
          entry.learnLanguage
        }
      xrStorageRepository.decodeSavedVocabImageBitmap(
        scopedFileName =
          XrLanguageTeacherDialogueFormatter.sanitizeWordForFilename(entry.word, learnLang),
        safeFileName = XrLanguageTeacherDialogueFormatter.sanitizeWordForFilename(entry.word),
        legacyWord = entry.word,
        maxSidePx = 512,
      )
    }

  /** Returns the current count of due Spanish cards for home screen summaries. */
  suspend fun getDueCount(nowMs: Long = System.currentTimeMillis()): Int =
    withContext(ioDispatcher) {
      spanishEntriesFlow.first().count { SpanishSrsScheduler.isDue(it, nowMs) }
    }

  private fun matchesWord(entry: DictionaryEntry, word: String): Boolean {
    val isSpanish =
      entry.learnLanguage == Language.LANGUAGE_SPANISH ||
        entry.learnLanguage == Language.LANGUAGE_UNSPECIFIED
    if (!isSpanish) return false
    return entry.word.equals(word, ignoreCase = true) ||
      XrLanguageTeacherDialogueFormatter.isSameVocabularyWord(entry.word, word)
  }
}
