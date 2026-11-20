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

import com.google.ai.edge.gallery.proto.DictionaryEntry
import kotlin.math.max
import kotlin.math.roundToInt

/** Spaced-repetition (SM-2) review ratings. */
enum class ReviewRating(val quality: Int, val label: String) {
  AGAIN(quality = 1, label = "Again"),
  HARD(quality = 3, label = "Hard"),
  GOOD(quality = 4, label = "Good"),
  EASY(quality = 5, label = "Easy"),
}

data class SrsUpdate(
  val nextReviewAtMs: Long,
  val intervalDays: Int,
  val repetitions: Int,
  val easeFactor: Float,
  val lastReviewedAtMs: Long,
)

/** Pure SM-2 spaced repetition scheduler for Spanish vocabulary cards. */
object SpanishSrsScheduler {
  const val DEFAULT_EASE_FACTOR = 2.5f
  const val MIN_EASE_FACTOR = 1.3f
  const val DAY_MS = 24L * 60L * 60L * 1000L
  const val AGAIN_DELAY_MS = 10L * 60L * 1000L

  fun scheduleNext(
    entry: DictionaryEntry,
    rating: ReviewRating,
    nowMs: Long = System.currentTimeMillis(),
  ): SrsUpdate {
    val currentEase = if (entry.easeFactor > 0f) entry.easeFactor else DEFAULT_EASE_FACTOR
    val currentReps = max(0, entry.repetitions)
    val currentInterval = max(0, entry.intervalDays)

    val q = rating.quality
    val updatedEase =
      (currentEase + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f)))
        .coerceAtLeast(MIN_EASE_FACTOR)

    if (rating == ReviewRating.AGAIN) {
      return SrsUpdate(
        nextReviewAtMs = nowMs + AGAIN_DELAY_MS,
        intervalDays = 0,
        repetitions = 0,
        easeFactor = updatedEase,
        lastReviewedAtMs = nowMs,
      )
    }

    val nextReps = currentReps + 1
    val nextIntervalDays =
      when (rating) {
        ReviewRating.HARD -> {
          when {
            currentReps == 0 -> 1
            currentReps == 1 -> 3
            else -> max(currentInterval + 1, (currentInterval * 1.2f).roundToInt())
          }
        }
        ReviewRating.GOOD -> {
          when (currentReps) {
            0 -> 1
            1 -> 6
            else -> max(currentInterval + 1, (currentInterval * updatedEase).roundToInt())
          }
        }
        ReviewRating.EASY -> {
          when (currentReps) {
            0 -> 4
            1 -> 10
            else -> max(currentInterval + 2, (currentInterval * updatedEase * 1.3f).roundToInt())
          }
        }
        ReviewRating.AGAIN -> 0
      }

    return SrsUpdate(
      nextReviewAtMs = nowMs + nextIntervalDays * DAY_MS,
      intervalDays = nextIntervalDays,
      repetitions = nextReps,
      easeFactor = updatedEase,
      lastReviewedAtMs = nowMs,
    )
  }

  fun previewIntervalLabel(entry: DictionaryEntry, rating: ReviewRating): String {
    if (rating == ReviewRating.AGAIN) return "<10m"
    val preview = scheduleNext(entry, rating, nowMs = 0L)
    return if (preview.intervalDays <= 0) "<10m" else "${preview.intervalDays}d"
  }

  fun isDue(entry: DictionaryEntry, nowMs: Long = System.currentTimeMillis()): Boolean {
    return entry.nextReviewAtMs <= 0L || entry.nextReviewAtMs <= nowMs
  }

  fun isMastered(entry: DictionaryEntry): Boolean {
    return entry.repetitions >= 3 && entry.intervalDays >= 10
  }

  /**
   * Orders due cards with previously reviewed overdue cards first (oldest due first), followed by
   * brand-new cards (`nextReviewAtMs == 0`).
   */
  fun sortDueCards(
    entries: List<DictionaryEntry>,
    nowMs: Long = System.currentTimeMillis(),
  ): List<DictionaryEntry> {
    return entries
      .filter { isDue(it, nowMs) }
      .sortedWith(
        compareBy<DictionaryEntry> { it.nextReviewAtMs <= 0L }
          .thenBy { it.nextReviewAtMs }
          .thenBy { it.word.lowercase() }
      )
  }
}
