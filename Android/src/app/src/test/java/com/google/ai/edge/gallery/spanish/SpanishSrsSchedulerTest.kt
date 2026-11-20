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

import com.google.ai.edge.gallery.proto.dictionaryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpanishSrsSchedulerTest {

  private fun newCard(
    word: String = "el café",
    repetitions: Int = 0,
    intervalDays: Int = 0,
    easeFactor: Float = SpanishSrsScheduler.DEFAULT_EASE_FACTOR,
    nextReviewAtMs: Long = 0L,
  ) = dictionaryEntry {
    this.word = word
    this.translation = "coffee"
    this.repetitions = repetitions
    this.intervalDays = intervalDays
    this.easeFactor = easeFactor
    this.nextReviewAtMs = nextReviewAtMs
  }

  @Test
  fun scheduleNext_again_resetsRepetitionsAndSchedulesTenMinuteDelay() {
    val reviewedCard = newCard(repetitions = 4, intervalDays = 14, easeFactor = 2.5f)
    val now = 1_000_000L

    val update = SpanishSrsScheduler.scheduleNext(reviewedCard, ReviewRating.AGAIN, nowMs = now)

    assertEquals(0, update.repetitions)
    assertEquals(0, update.intervalDays)
    assertEquals(now + SpanishSrsScheduler.AGAIN_DELAY_MS, update.nextReviewAtMs)
    assertEquals(now, update.lastReviewedAtMs)
    assertTrue(update.easeFactor < 2.5f)
  }

  @Test
  fun scheduleNext_easeFactorNeverDropsBelowMinimum() {
    val hardCard = newCard(repetitions = 1, intervalDays = 1, easeFactor = 1.32f)

    val update = SpanishSrsScheduler.scheduleNext(hardCard, ReviewRating.AGAIN, nowMs = 5_000L)

    assertEquals(SpanishSrsScheduler.MIN_EASE_FACTOR, update.easeFactor, 0.0001f)
  }

  @Test
  fun scheduleNext_good_followsSm2IntervalProgression() {
    val now = 10_000L
    val first = SpanishSrsScheduler.scheduleNext(newCard(), ReviewRating.GOOD, nowMs = now)
    assertEquals(1, first.repetitions)
    assertEquals(1, first.intervalDays)
    assertEquals(now + 1 * SpanishSrsScheduler.DAY_MS, first.nextReviewAtMs)

    val afterFirst =
      newCard(repetitions = first.repetitions, intervalDays = first.intervalDays, easeFactor = first.easeFactor)
    val second = SpanishSrsScheduler.scheduleNext(afterFirst, ReviewRating.GOOD, nowMs = now)
    assertEquals(2, second.repetitions)
    assertEquals(6, second.intervalDays)

    val afterSecond =
      newCard(repetitions = second.repetitions, intervalDays = second.intervalDays, easeFactor = second.easeFactor)
    val third = SpanishSrsScheduler.scheduleNext(afterSecond, ReviewRating.GOOD, nowMs = now)
    assertEquals(3, third.repetitions)
    assertTrue(third.intervalDays >= 14)
  }

  @Test
  fun scheduleNext_hardAndEasy_produceOrderedIntervalsOnFirstReview() {
    val fresh = newCard()
    val hard = SpanishSrsScheduler.scheduleNext(fresh, ReviewRating.HARD, nowMs = 0L)
    val good = SpanishSrsScheduler.scheduleNext(fresh, ReviewRating.GOOD, nowMs = 0L)
    val easy = SpanishSrsScheduler.scheduleNext(fresh, ReviewRating.EASY, nowMs = 0L)

    assertEquals(1, hard.intervalDays)
    assertEquals(1, good.intervalDays)
    assertEquals(4, easy.intervalDays)
    assertTrue(easy.easeFactor > good.easeFactor)
    assertTrue(good.easeFactor > hard.easeFactor)
  }

  @Test
  fun previewIntervalLabel_formatsRatingsAccurately() {
    val fresh = newCard()
    assertEquals("<10m", SpanishSrsScheduler.previewIntervalLabel(fresh, ReviewRating.AGAIN))
    assertEquals("1d", SpanishSrsScheduler.previewIntervalLabel(fresh, ReviewRating.GOOD))
    assertEquals("4d", SpanishSrsScheduler.previewIntervalLabel(fresh, ReviewRating.EASY))
  }

  @Test
  fun isDue_and_isMastered_and_sortDueCards_workTogether() {
    val now = 500_000L
    val brandNew = newCard(word = "nuevo", nextReviewAtMs = 0L)
    val olderOverdue = newCard(word = "viejo", repetitions = 1, intervalDays = 1, nextReviewAtMs = 100_000L)
    val newerOverdue = newCard(word = "reciente", repetitions = 2, intervalDays = 6, nextReviewAtMs = 400_000L)
    val futureCard = newCard(word = "futuro", repetitions = 3, intervalDays = 14, nextReviewAtMs = 900_000L)

    assertTrue(SpanishSrsScheduler.isDue(brandNew, now))
    assertTrue(SpanishSrsScheduler.isDue(olderOverdue, now))
    assertFalse(SpanishSrsScheduler.isDue(futureCard, now))
    assertTrue(SpanishSrsScheduler.isMastered(futureCard))
    assertFalse(SpanishSrsScheduler.isMastered(newerOverdue))

    val sorted = SpanishSrsScheduler.sortDueCards(listOf(futureCard, brandNew, newerOverdue, olderOverdue), now)
    assertEquals(listOf("viejo", "reciente", "nuevo"), sorted.map { it.word })
  }
}
