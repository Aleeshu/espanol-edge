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
import com.google.ai.edge.gallery.spanish.SpanishSrsScheduler
import com.google.ai.edge.gallery.spanish.SpanishVocabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Lightweight home-screen ViewModel exposing live flashcard due counts and starter seeding. */
@HiltViewModel
class SpanishHomeViewModel
@Inject
constructor(private val spanishVocabRepository: SpanishVocabRepository) : ViewModel() {
  init {
    viewModelScope.launch { spanishVocabRepository.ensureStarterDeckSeeded() }
  }

  val dueFlashcardsCount: StateFlow<Int> =
    spanishVocabRepository.spanishEntriesFlow
      .map { entries ->
        val nowMs = System.currentTimeMillis()
        entries.count { SpanishSrsScheduler.isDue(it, nowMs) }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0,
      )

  val totalFlashcardsCount: StateFlow<Int> =
    spanishVocabRepository.spanishEntriesFlow
      .map { it.size }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0,
      )
}
