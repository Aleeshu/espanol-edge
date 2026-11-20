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

import com.google.ai.edge.gallery.proto.CefrLevel
import com.google.ai.edge.gallery.proto.SpanishDialect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpanishPromptBuilderTest {

  @Test
  fun buildTutorSystemPrompt_includesDialectGuidanceForAllDialects() {
    val expectationByDialect =
      mapOf(
        SpanishDialect.SPANISH_DIALECT_LATIN_AMERICAN to "Latin American",
        SpanishDialect.SPANISH_DIALECT_MEXICAN to "Mexican",
        SpanishDialect.SPANISH_DIALECT_SPAIN to "vosotros",
        SpanishDialect.SPANISH_DIALECT_RIOPLATENSE to "voseo",
        SpanishDialect.SPANISH_DIALECT_COLOMBIAN to "Colombian",
      )

    for ((dialect, expectedToken) in expectationByDialect) {
      val prompt =
        SpanishPromptBuilder.buildTutorSystemPrompt(
          settings = SpanishLearningSettings(dialect = dialect, cefrLevel = CefrLevel.CEFR_B1),
          scenario = SpanishRoleplayScenario.CAFE,
        )
      assertTrue("Missing $expectedToken for $dialect", prompt.contains(expectedToken))
      assertTrue(prompt.contains("B1"))
      assertTrue(prompt.contains("Café"))
    }
  }

  @Test
  fun buildTutorSystemPrompt_respectsInlineCorrectionsAndEnglishHintsToggles() {
    val withFeedback =
      SpanishPromptBuilder.buildTutorSystemPrompt(
        settings =
          SpanishLearningSettings(
            cefrLevel = CefrLevel.CEFR_A1,
            inlineCorrections = true,
            showEnglishHints = true,
          ),
        scenario = SpanishRoleplayScenario.TRAVEL,
      )
    assertTrue(withFeedback.contains("**Correction:**"))
    assertTrue(withFeedback.contains("English hints"))

    val immersionOnly =
      SpanishPromptBuilder.buildTutorSystemPrompt(
        settings =
          SpanishLearningSettings(
            cefrLevel = CefrLevel.CEFR_C1,
            inlineCorrections = false,
            showEnglishHints = false,
          ),
        scenario = SpanishRoleplayScenario.TRAVEL,
      )
    assertFalse(immersionOnly.contains("**Correction:**"))
    assertTrue(immersionOnly.contains("Immersion mode"))
    assertTrue(immersionOnly.contains("C1"))
  }

  @Test
  fun buildCameraDialectAddon_includesDialectCefrAndArticleGuidance() {
    val addon =
      SpanishPromptBuilder.buildCameraDialectAddon(
        SpanishLearningSettings(
          dialect = SpanishDialect.SPANISH_DIALECT_SPAIN,
          cefrLevel = CefrLevel.CEFR_B2,
        )
      )
    assertTrue(addon.contains("Spain (Castilian)"))
    assertTrue(addon.contains("B2"))
    assertTrue(addon.contains("el/la"))
  }

  @Test
  fun buildPronunciationSystemPrompt_handlesShadowingAndFreeSpeakingModes() {
    val phrase = SpanishPracticePhrases.ALL.first()
    val settings =
      SpanishLearningSettings(
        dialect = SpanishDialect.SPANISH_DIALECT_MEXICAN,
        cefrLevel = CefrLevel.CEFR_A2,
      )

    val shadowingPrompt =
      SpanishPromptBuilder.buildPronunciationSystemPrompt(
        settings = settings,
        mode = PronunciationPracticeMode.SHADOWING,
        targetPhrase = phrase,
      )
    assertTrue(shadowingPrompt.contains(phrase.spanish))
    assertTrue(shadowingPrompt.contains("**What I heard**"))
    assertTrue(shadowingPrompt.contains("**Pronunciation & Rhythm**"))

    val freeSpeakingPrompt =
      SpanishPromptBuilder.buildPronunciationSystemPrompt(
        settings = settings,
        mode = PronunciationPracticeMode.FREE_SPEAKING,
        targetPhrase = null,
      )
    assertTrue(freeSpeakingPrompt.contains("Free Speaking"))
    assertTrue(freeSpeakingPrompt.contains("speaking freely"))
  }
}
