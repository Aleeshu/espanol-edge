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

/** Builds tailored Spanish-learning system prompts from user dialect, CEFR level, and mode. */
object SpanishPromptBuilder {

  fun buildTutorSystemPrompt(
    settings: SpanishLearningSettings,
    scenario: SpanishRoleplayScenario,
  ): String {
    val level = settings.cefrLevel.normalized()
    val correctionRule =
      if (settings.inlineCorrections) {
        "- Grammar & phrasing feedback: If the learner makes a noticeable grammar, gender, conjugation, or vocabulary mistake in Spanish, start with one concise line `**Correction:** <natural Spanish> — *<brief friendly reason>*` before continuing the conversation naturally. If their Spanish is already natural (or they wrote in English), skip the correction block."
      } else {
        "- Do not interrupt the flow with explicit grammar corrections unless the learner explicitly asks you to check their Spanish."
      }

    val englishHintsRule =
      if (settings.showEnglishHints) {
        when (level) {
          CefrLevel.CEFR_A1,
          CefrLevel.CEFR_A2 ->
            "- English hints: Include short parenthetical English glosses for tricky new Spanish words, and add a brief `*(Hint: ...)*` translation line when helpful for comprehension."
          else ->
            "- English hints: Converse primarily in Spanish, adding a short parenthetical English gloss only for idiomatic or uncommon expressions."
        }
      } else {
        "- Immersion mode: Respond strictly in Spanish without English translations unless the learner asks for a translation."
      }

    return buildString {
      appendLine("You are an encouraging, patient, native Spanish tutor in Español Edge.")
      appendLine("Active mode: ${scenario.title} (${scenario.spanishTitle}).")
      appendLine("Dialect preference (${settings.dialect.displayName}): ${settings.dialect.promptGuidance}")
      appendLine("Target proficiency (${level.code}): ${level.promptGuidance}")
      appendLine("Scenario instructions: ${scenario.roleplayDirective}")
      appendLine(correctionRule)
      appendLine(englishHintsRule)
      appendLine("- Keep responses focused and conversational (typically 2-4 sentences) so the learner gets plenty of turns to speak or reply.")
      append("- When introducing a handy new Spanish word or expression, highlight it in bold (e.g. **la cuenta**) so the learner can easily save it to their flashcard deck.")
    }
  }

  fun buildCameraDialectAddon(settings: SpanishLearningSettings): String {
    val level = settings.cefrLevel.normalized()
    return buildString {
      appendLine()
      appendLine("Spanish Learner Profile:")
      appendLine("- Preferred dialect (${settings.dialect.displayName}): ${settings.dialect.promptGuidance}")
      appendLine("- Target proficiency (${level.code}): ${level.promptGuidance}")
      append("- Keep the focal vocabulary word in its standard singular form with definite article (e.g. el/la) when teaching a noun.")
    }
  }

  fun buildPronunciationSystemPrompt(
    settings: SpanishLearningSettings,
    mode: PronunciationPracticeMode,
    targetPhrase: SpanishPracticePhrase?,
  ): String {
    val level = settings.cefrLevel.normalized()
    val targetSection =
      if (targetPhrase != null && mode != PronunciationPracticeMode.FREE_SPEAKING) {
        """
        Practice mode: ${mode.label}
        Target Spanish phrase the learner is attempting: "${targetPhrase.spanish}"
        English meaning: "${targetPhrase.english}"
        Phonetic focus: ${targetPhrase.phoneticTip}
        Compare what you hear in the audio clip directly against this target phrase.
        """.trimIndent()
      } else {
        """
        Practice mode: ${mode.label}
        The learner is speaking freely in Spanish. Listen carefully to both what they say and how they pronounce it.
        """.trimIndent()
      }

    return buildString {
      appendLine("You are a warm, observant Spanish pronunciation and listening coach in Español Edge.")
      appendLine("Dialect target (${settings.dialect.displayName}): ${settings.dialect.promptGuidance}")
      appendLine("Learner proficiency (${level.code}): ${level.promptGuidance}")
      appendLine(targetSection)
      appendLine("When you receive an audio recording (or text), structure your feedback concisely using these headings:")
      appendLine("1. **What I heard**: Accurate Spanish transcript of what the learner actually said.")
      appendLine("2. **Pronunciation & Rhythm**: 2-3 specific, actionable tips on vowels, single r vs rolled rr, syllable stress, consonant softness (b/v, d, j/g, ñ, ll/y), and ${settings.dialect.shortLabel} cadence.")
      appendLine("3. **Natural Spanish**: A clean, idiomatic ${settings.dialect.shortLabel} version of what they wanted to say.")
      append("4. **Next Drill**: One short follow-up phrase at ${level.code} level for them to try next.")
    }
  }
}
