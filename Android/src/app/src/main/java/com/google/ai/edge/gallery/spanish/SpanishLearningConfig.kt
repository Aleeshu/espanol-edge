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
import com.google.ai.edge.gallery.proto.Settings
import com.google.ai.edge.gallery.proto.SpanishDialect

/** User-configurable preferences for Spanish tutoring, pronunciation coaching, and SRS. */
data class SpanishLearningSettings(
  val dialect: SpanishDialect = SpanishDialect.SPANISH_DIALECT_LATIN_AMERICAN,
  val cefrLevel: CefrLevel = CefrLevel.CEFR_A2,
  val inlineCorrections: Boolean = true,
  val showEnglishHints: Boolean = true,
  val autoReadAloud: Boolean = false,
) {
  companion object {
    fun fromProto(settings: Settings): SpanishLearningSettings {
      val resolvedDialect =
        when (settings.spanishDialect) {
          SpanishDialect.SPANISH_DIALECT_UNSPECIFIED,
          SpanishDialect.UNRECOGNIZED -> SpanishDialect.SPANISH_DIALECT_LATIN_AMERICAN
          else -> settings.spanishDialect
        }
      val resolvedLevel =
        when (settings.spanishCefrLevel) {
          CefrLevel.CEFR_LEVEL_UNSPECIFIED,
          CefrLevel.UNRECOGNIZED -> CefrLevel.CEFR_A2
          else -> settings.spanishCefrLevel
        }
      return SpanishLearningSettings(
        dialect = resolvedDialect,
        cefrLevel = resolvedLevel,
        inlineCorrections =
          if (settings.hasInlineCorrectionsEnabled()) settings.inlineCorrectionsEnabled else true,
        showEnglishHints =
          if (settings.hasShowEnglishHints()) settings.showEnglishHints else true,
        autoReadAloud =
          if (settings.hasAutoReadSpanishAloud()) settings.autoReadSpanishAloud else false,
      )
    }
  }
}

val SELECTABLE_SPANISH_DIALECTS: List<SpanishDialect> =
  listOf(
    SpanishDialect.SPANISH_DIALECT_LATIN_AMERICAN,
    SpanishDialect.SPANISH_DIALECT_MEXICAN,
    SpanishDialect.SPANISH_DIALECT_SPAIN,
    SpanishDialect.SPANISH_DIALECT_RIOPLATENSE,
    SpanishDialect.SPANISH_DIALECT_COLOMBIAN,
  )

val SELECTABLE_CEFR_LEVELS: List<CefrLevel> =
  listOf(
    CefrLevel.CEFR_A1,
    CefrLevel.CEFR_A2,
    CefrLevel.CEFR_B1,
    CefrLevel.CEFR_B2,
    CefrLevel.CEFR_C1,
    CefrLevel.CEFR_C2,
  )

fun SpanishDialect.normalized(): SpanishDialect =
  when (this) {
    SpanishDialect.SPANISH_DIALECT_UNSPECIFIED,
    SpanishDialect.UNRECOGNIZED -> SpanishDialect.SPANISH_DIALECT_LATIN_AMERICAN
    else -> this
  }

val SpanishDialect.displayName: String
  get() =
    when (normalized()) {
      SpanishDialect.SPANISH_DIALECT_MEXICAN -> "Mexican"
      SpanishDialect.SPANISH_DIALECT_SPAIN -> "Spain (Castilian)"
      SpanishDialect.SPANISH_DIALECT_RIOPLATENSE -> "Rioplatense"
      SpanishDialect.SPANISH_DIALECT_COLOMBIAN -> "Colombian"
      else -> "Latin American"
    }

val SpanishDialect.shortLabel: String
  get() =
    when (normalized()) {
      SpanishDialect.SPANISH_DIALECT_MEXICAN -> "Mexico"
      SpanishDialect.SPANISH_DIALECT_SPAIN -> "Spain"
      SpanishDialect.SPANISH_DIALECT_RIOPLATENSE -> "Argentina"
      SpanishDialect.SPANISH_DIALECT_COLOMBIAN -> "Colombia"
      else -> "LatAm"
    }

val SpanishDialect.localeTag: String
  get() =
    when (normalized()) {
      SpanishDialect.SPANISH_DIALECT_MEXICAN -> "es-MX"
      SpanishDialect.SPANISH_DIALECT_SPAIN -> "es-ES"
      SpanishDialect.SPANISH_DIALECT_RIOPLATENSE -> "es-AR"
      SpanishDialect.SPANISH_DIALECT_COLOMBIAN -> "es-CO"
      else -> "es-US"
    }

val SpanishDialect.promptGuidance: String
  get() =
    when (normalized()) {
      SpanishDialect.SPANISH_DIALECT_MEXICAN ->
        "Use natural Mexican Spanish (ustedes for plural 'you', seseo pronunciation, and Mexican everyday terms such as mesero, camión, ahorita, qué padre when fitting)."
      SpanishDialect.SPANISH_DIALECT_SPAIN ->
        "Use natural Castilian Spanish from Spain (vosotros/os for informal plural 'you', distinción between c/z and s in pronunciation notes, and Peninsular terms such as vale, camarero, coche, ordenador)."
      SpanishDialect.SPANISH_DIALECT_RIOPLATENSE ->
        "Use natural Rioplatense Spanish from Argentina/Uruguay (voseo such as vos tenés / vos podés, ustedes for plural, sheismo/zheismo pronunciation notes for ll/y, and expressions like che, re, colectivo)."
      SpanishDialect.SPANISH_DIALECT_COLOMBIAN ->
        "Use natural Colombian Spanish (ustedes for plural, polite usted/tú balance, seseo pronunciation, and warm Colombian vocabulary such as chévere, tinto, ¿qué más?)."
      else ->
        "Use neutral Latin American Spanish (ustedes for plural 'you', seseo pronunciation, widely understood vocabulary across Latin America)."
    }

fun CefrLevel.normalized(): CefrLevel =
  when (this) {
    CefrLevel.CEFR_LEVEL_UNSPECIFIED,
    CefrLevel.UNRECOGNIZED -> CefrLevel.CEFR_A2
    else -> this
  }

fun CefrLevel.rank(): Int =
  when (normalized()) {
    CefrLevel.CEFR_A1 -> 1
    CefrLevel.CEFR_A2 -> 2
    CefrLevel.CEFR_B1 -> 3
    CefrLevel.CEFR_B2 -> 4
    CefrLevel.CEFR_C1 -> 5
    CefrLevel.CEFR_C2 -> 6
    else -> 2
  }

val CefrLevel.code: String
  get() =
    when (normalized()) {
      CefrLevel.CEFR_A1 -> "A1"
      CefrLevel.CEFR_A2 -> "A2"
      CefrLevel.CEFR_B1 -> "B1"
      CefrLevel.CEFR_B2 -> "B2"
      CefrLevel.CEFR_C1 -> "C1"
      CefrLevel.CEFR_C2 -> "C2"
      else -> "A2"
    }

val CefrLevel.displayName: String
  get() =
    when (normalized()) {
      CefrLevel.CEFR_A1 -> "A1 Beginner"
      CefrLevel.CEFR_A2 -> "A2 Elementary"
      CefrLevel.CEFR_B1 -> "B1 Intermediate"
      CefrLevel.CEFR_B2 -> "B2 Upper-Int"
      CefrLevel.CEFR_C1 -> "C1 Advanced"
      CefrLevel.CEFR_C2 -> "C2 Mastery"
      else -> "A2 Elementary"
    }

val CefrLevel.promptGuidance: String
  get() =
    when (normalized()) {
      CefrLevel.CEFR_A1 ->
        "CEFR A1 (Beginner): Keep Spanish sentences very short (5-10 words), use high-frequency concrete vocabulary and present tense, and speak warmly and slowly."
      CefrLevel.CEFR_A2 ->
        "CEFR A2 (Elementary): Use simple everyday Spanish in present, near future (ir a), and basic preterite/imperfect. Keep explanations concise and clear."
      CefrLevel.CEFR_B1 ->
        "CEFR B1 (Intermediate): Use natural conversational Spanish across past, future, conditional, and basic subjunctive. Write explanations mostly in Spanish with brief English clarifications only if needed."
      CefrLevel.CEFR_B2 ->
        "CEFR B2 (Upper-Intermediate): Use rich idiomatic Spanish, full subjunctive/compound tenses, and natural conversational nuance. Keep replies entirely in Spanish unless asked."
      CefrLevel.CEFR_C1 ->
        "CEFR C1 (Advanced): Converse fluently in sophisticated, idiomatic Spanish with nuanced register, connectors, and cultural idioms. Respond entirely in Spanish."
      CefrLevel.CEFR_C2 ->
        "CEFR C2 (Mastery): Converse as a peer native speaker using subtle humor, regional idioms, literary or professional register, and precise stylistic feedback in Spanish."
      else ->
        "CEFR A2 (Elementary): Use simple everyday Spanish in present, near future (ir a), and basic preterite."
    }

/** A starter prompt shown on the empty state of Spanish Tutor Chat. */
data class SpanishStarterPrompt(
  val spanishText: String,
  val englishSubtitle: String,
)

/** Roleplay and conversation modes for Spanish Tutor Chat. */
enum class SpanishRoleplayScenario(
  val id: String,
  val title: String,
  val spanishTitle: String,
  val description: String,
  val roleplayDirective: String,
  val starterPrompts: List<SpanishStarterPrompt>,
) {
  FREE_CHAT(
    id = "free_chat",
    title = "Free Chat",
    spanishTitle = "Charla Libre",
    description = "Open conversation on any topic with gentle tutor feedback.",
    roleplayDirective =
      "Engage in an open, friendly conversation in Spanish on whatever topic the learner brings up, asking one engaging follow-up question each turn.",
    starterPrompts =
      listOf(
        SpanishStarterPrompt(
          spanishText = "¡Hola! Hoy quiero practicar cómo hablar de mi rutina diaria.",
          englishSubtitle = "Practice talking about my daily routine",
        ),
        SpanishStarterPrompt(
          spanishText = "¿Me puedes enseñar 3 expresiones útiles que usen los hablantes nativos?",
          englishSubtitle = "Teach me 3 useful native expressions",
        ),
        SpanishStarterPrompt(
          spanishText = "Explícame la diferencia entre 'ser' y 'estar' con ejemplos.",
          englishSubtitle = "Explain ser vs estar with examples",
        ),
      ),
  ),
  CAFE(
    id = "cafe",
    title = "Café",
    spanishTitle = "En la Cafetería",
    description = "Order coffee, pastries, milk options, and pay the bill.",
    roleplayDirective =
      "Roleplay as a warm barista at a neighborhood café. Greet the learner, take their drink and food order, ask natural follow-up questions (size, milk, to stay or to go), and keep the scene immersive.",
    starterPrompts =
      listOf(
        SpanishStarterPrompt(
          spanishText = "Buenos días, ¿qué cafés tienen hoy?",
          englishSubtitle = "Good morning, what coffees do you have today?",
        ),
        SpanishStarterPrompt(
          spanishText = "Quisiera un café con leche de avena y un croissant, por favor.",
          englishSubtitle = "Order an oat milk latte and a croissant",
        ),
        SpanishStarterPrompt(
          spanishText = "¿Cuánto es en total? ¿Puedo pagar con tarjeta?",
          englishSubtitle = "Ask for the total and pay by card",
        ),
      ),
  ),
  TRAVEL(
    id = "travel",
    title = "Travel",
    spanishTitle = "De Viaje",
    description = "Navigate airports, hotels, transit, and ask for directions.",
    roleplayDirective =
      "Roleplay as a helpful local transit agent, hotel receptionist, or friendly passerby helping the learner navigate their trip in Spanish.",
    starterPrompts =
      listOf(
        SpanishStarterPrompt(
          spanishText = "Disculpa, ¿cómo llego a la estación central desde aquí?",
          englishSubtitle = "Ask how to reach the central station",
        ),
        SpanishStarterPrompt(
          spanishText = "Tengo una reserva a mi nombre para tres noches.",
          englishSubtitle = "Check in to a hotel reservation",
        ),
        SpanishStarterPrompt(
          spanishText = "¿A qué hora sale el próximo autobús al centro?",
          englishSubtitle = "Ask when the next bus leaves",
        ),
      ),
  ),
  RESTAURANT(
    id = "restaurant",
    title = "Restaurant",
    spanishTitle = "En el Restaurante",
    description = "Book a table, ask about ingredients, and order dinner.",
    roleplayDirective =
      "Roleplay as an attentive waiter/waitress at a lively local restaurant. Recommend dishes, answer ingredient or allergy questions, and take the learner's order.",
    starterPrompts =
      listOf(
        SpanishStarterPrompt(
          spanishText = "Buenas noches, ¿tienen una mesa para dos personas?",
          englishSubtitle = "Ask for a table for two",
        ),
        SpanishStarterPrompt(
          spanishText = "¿Qué plato de la casa me recomienda hoy?",
          englishSubtitle = "Ask for the house special recommendation",
        ),
        SpanishStarterPrompt(
          spanishText = "Soy alérgico a los mariscos; ¿este plato lleva camarones?",
          englishSubtitle = "Check ingredients for allergies",
        ),
      ),
  ),
  SHOPPING(
    id = "shopping",
    title = "Shopping",
    spanishTitle = "De Compras",
    description = "Ask for sizes, colors, prices, and try items at a market or shop.",
    roleplayDirective =
      "Roleplay as a friendly shopkeeper or market vendor helping the learner find the right size, color, price, or gift.",
    starterPrompts =
      listOf(
        SpanishStarterPrompt(
          spanishText = "Hola, ¿tienen esta camisa en talla mediana?",
          englishSubtitle = "Ask for a shirt in medium size",
        ),
        SpanishStarterPrompt(
          spanishText = "¿Dónde están los probadores, por favor?",
          englishSubtitle = "Ask where the fitting rooms are",
        ),
        SpanishStarterPrompt(
          spanishText = "¿Aceptan devoluciones si no me queda bien?",
          englishSubtitle = "Ask about the return policy",
        ),
      ),
  ),
  JOB_INTERVIEW(
    id = "job_interview",
    title = "Job Interview",
    spanishTitle = "Entrevista Laboral",
    description = "Practice formal Spanish for experience, strengths, and career goals.",
    roleplayDirective =
      "Roleplay as a polite hiring manager conducting a professional Spanish job interview. Ask realistic questions about background, teamwork, and strengths.",
    starterPrompts =
      listOf(
        SpanishStarterPrompt(
          spanishText = "Mucho gusto. Les cuento un poco sobre mi experiencia profesional.",
          englishSubtitle = "Introduce your professional background",
        ),
        SpanishStarterPrompt(
          spanishText = "Mi mayor fortaleza es resolver problemas bajo presión.",
          englishSubtitle = "Describe your biggest strength",
        ),
        SpanishStarterPrompt(
          spanishText = "¿Cómo es un día típico en este equipo?",
          englishSubtitle = "Ask what a typical day on the team looks like",
        ),
      ),
  ),
  MAKING_FRIENDS(
    id = "making_friends",
    title = "Making Friends",
    spanishTitle = "Hacer Amigos",
    description = "Small talk about hobbies, music, food, and weekend plans.",
    roleplayDirective =
      "Roleplay as a warm local peer meeting the learner at a community meetup or café. Keep the tone casual, friendly, and naturally conversational.",
    starterPrompts =
      listOf(
        SpanishStarterPrompt(
          spanishText = "¡Hola! Me encanta la música de este lugar. ¿Vienes seguido?",
          englishSubtitle = "Break the ice about music and hangout spots",
        ),
        SpanishStarterPrompt(
          spanishText = "Los fines de semana me gusta cocinar y caminar por el parque.",
          englishSubtitle = "Share weekend hobbies",
        ),
        SpanishStarterPrompt(
          spanishText = "¿Qué serie o película en español me recomiendas ver?",
          englishSubtitle = "Ask for Spanish show or movie recommendations",
        ),
      ),
  ),
  DOCTOR_PHARMACY(
    id = "doctor_pharmacy",
    title = "Doctor & Pharmacy",
    spanishTitle = "Médico y Farmacia",
    description = "Describe symptoms, ask about medicine, and understand instructions.",
    roleplayDirective =
      "Roleplay as a calm, helpful pharmacist or clinic doctor helping the learner describe symptoms and understand dosage instructions clearly.",
    starterPrompts =
      listOf(
        SpanishStarterPrompt(
          spanishText = "Hola, me duele la garganta desde ayer por la noche.",
          englishSubtitle = "Describe a sore throat since last night",
        ),
        SpanishStarterPrompt(
          spanishText = "¿Cada cuántas horas debo tomar estas pastillas?",
          englishSubtitle = "Ask how often to take medicine",
        ),
        SpanishStarterPrompt(
          spanishText = "¿Necesito receta médica para este jarabe?",
          englishSubtitle = "Ask if a prescription is needed",
        ),
      ),
  );

  companion object {
    fun fromId(id: String?): SpanishRoleplayScenario =
      entries.firstOrNull { it.id == id } ?: FREE_CHAT
  }
}

/** Practice modes inside Pronunciation & Listening Lab. */
enum class PronunciationPracticeMode(
  val id: String,
  val label: String,
  val description: String,
) {
  SHADOWING(
    id = "shadowing",
    label = "Shadowing",
    description = "Listen to a native-paced phrase, record yourself, and get accent feedback.",
  ),
  TRICKY_SOUNDS(
    id = "tricky_sounds",
    label = "Tricky Sounds",
    description = "Drill r/rr, ñ, ll/y, j/g, b/v, and vowel clarity.",
  ),
  FREE_SPEAKING(
    id = "free_speaking",
    label = "Free Speaking",
    description = "Speak freely in Spanish about anything and get grammar + accent coaching.",
  ),
}

/** A curated Spanish phrase for Pronunciation & Listening Lab drills. */
data class SpanishPracticePhrase(
  val id: String,
  val spanish: String,
  val english: String,
  val phoneticTip: String,
  val minLevel: CefrLevel,
  val mode: PronunciationPracticeMode,
  val targetWord: String = "",
)

object SpanishPracticePhrases {
  val ALL: List<SpanishPracticePhrase> =
    listOf(
      // Shadowing drills across CEFR levels.
      SpanishPracticePhrase(
        id = "sh_a1_1",
        spanish = "Buenos días, quisiera un café con leche, por favor.",
        english = "Good morning, I would like a coffee with milk, please.",
        phoneticTip = "Keep vowels crisp and short; link 'con_leche' smoothly.",
        minLevel = CefrLevel.CEFR_A1,
        mode = PronunciationPracticeMode.SHADOWING,
        targetWord = "el café con leche",
      ),
      SpanishPracticePhrase(
        id = "sh_a1_2",
        spanish = "Disculpa, ¿dónde está la estación de metro más cercana?",
        english = "Excuse me, where is the nearest subway station?",
        phoneticTip = "Stress the accented syllables: es-ta-CIÓN and cer-CA-na.",
        minLevel = CefrLevel.CEFR_A1,
        mode = PronunciationPracticeMode.SHADOWING,
        targetWord = "la estación",
      ),
      SpanishPracticePhrase(
        id = "sh_a2_1",
        spanish = "Ayer caminé por el mercado y compré fruta fresca.",
        english = "Yesterday I walked through the market and bought fresh fruit.",
        phoneticTip = "Clearly stress the final syllable in preterite 'ca-mi-NÉ' and 'com-PRÉ'.",
        minLevel = CefrLevel.CEFR_A2,
        mode = PronunciationPracticeMode.SHADOWING,
        targetWord = "el mercado",
      ),
      SpanishPracticePhrase(
        id = "sh_a2_2",
        spanish = "Cuando tengo tiempo libre, prefiero cocinar en casa.",
        english = "When I have free time, I prefer to cook at home.",
        phoneticTip = "Tap the single 'r' lightly in 'libre', 'prefiero', and 'cocinar'.",
        minLevel = CefrLevel.CEFR_A2,
        mode = PronunciationPracticeMode.SHADOWING,
        targetWord = "el tiempo libre",
      ),
      SpanishPracticePhrase(
        id = "sh_b1_1",
        spanish = "Aunque llueva esta tarde, vamos a salir a cenar juntos.",
        english = "Even if it rains this afternoon, we are going out to dinner together.",
        phoneticTip = "Notice the soft breathy 'j' in 'juntos' and smooth linking across 'vamos_a'.",
        minLevel = CefrLevel.CEFR_B1,
        mode = PronunciationPracticeMode.SHADOWING,
        targetWord = "aunque llueva",
      ),
      SpanishPracticePhrase(
        id = "sh_b2_1",
        spanish = "Ojalá hubiéramos llegado antes para aprovechar el atardecer.",
        english = "I wish we had arrived earlier to enjoy the sunset.",
        phoneticTip = "Keep the five pure vowels steady even in longer subjunctive phrases.",
        minLevel = CefrLevel.CEFR_B2,
        mode = PronunciationPracticeMode.SHADOWING,
        targetWord = "el atardecer",
      ),
      SpanishPracticePhrase(
        id = "sh_c1_1",
        spanish = "A fin de cuentas, vale la pena detenerse a escuchar con atención.",
        english = "At the end of the day, it is worth pausing to listen carefully.",
        phoneticTip = "Practice natural phrase grouping: 'A fin de cuentas' / 'vale la pena'.",
        minLevel = CefrLevel.CEFR_C1,
        mode = PronunciationPracticeMode.SHADOWING,
        targetWord = "a fin de cuentas",
      ),
      // Tricky Spanish sounds drills.
      SpanishPracticePhrase(
        id = "ts_1",
        spanish = "El perro de Pedro corre pero no rompe la rama.",
        english = "Pedro's dog runs but doesn't break the branch.",
        phoneticTip = "Contrast tapped single 'r' (Pedro, pero) vs trilled 'rr/r-' (perro, corre, rama).",
        minLevel = CefrLevel.CEFR_A1,
        mode = PronunciationPracticeMode.TRICKY_SOUNDS,
        targetWord = "el perro / pero",
      ),
      SpanishPracticePhrase(
        id = "ts_2",
        spanish = "Este carro rojo es demasiado caro para mí.",
        english = "This red car is too expensive for me.",
        phoneticTip = "Hold the trill in 'carro' and 'rojo', then use a quick single tap in 'caro' and 'para'.",
        minLevel = CefrLevel.CEFR_A1,
        mode = PronunciationPracticeMode.TRICKY_SOUNDS,
        targetWord = "el carro / caro",
      ),
      SpanishPracticePhrase(
        id = "ts_3",
        spanish = "Cada mañana extraño el otoño de mi niñez.",
        english = "Every morning I miss the autumn of my childhood.",
        phoneticTip = "Produce a true palatal nasal 'ñ' (like 'ny' in canyon) without splitting it into two syllables.",
        minLevel = CefrLevel.CEFR_A2,
        mode = PronunciationPracticeMode.TRICKY_SOUNDS,
        targetWord = "la mañana",
      ),
      SpanishPracticePhrase(
        id = "ts_4",
        spanish = "Lleva las llaves amarillas a la calle de la lluvia.",
        english = "Take the yellow keys to the street of the rain.",
        phoneticTip = "Keep 'll' and 'y' consistent with your chosen dialect (soft y-sound or Rioplatense sh-sound).",
        minLevel = CefrLevel.CEFR_A2,
        mode = PronunciationPracticeMode.TRICKY_SOUNDS,
        targetWord = "las llaves",
      ),
      SpanishPracticePhrase(
        id = "ts_5",
        spanish = "Jorge viajó en julio con gente muy generosa.",
        english = "Jorge traveled in July with very generous people.",
        phoneticTip = "Both 'j' and 'g' before e/i share the same throat fricative sound in Spanish.",
        minLevel = CefrLevel.CEFR_A2,
        mode = PronunciationPracticeMode.TRICKY_SOUNDS,
        targetWord = "la gente",
      ),
      SpanishPracticePhrase(
        id = "ts_6",
        spanish = "Vamos a beber vino blanco en el barco nuevo.",
        english = "We are going to drink white wine on the new boat.",
        phoneticTip = "In Spanish 'b' and 'v' share the exact same phoneme—firm after a pause, soft between vowels.",
        minLevel = CefrLevel.CEFR_A2,
        mode = PronunciationPracticeMode.TRICKY_SOUNDS,
        targetWord = "el barco",
      ),
    )

  fun forModeAndLevel(
    mode: PronunciationPracticeMode,
    cefrLevel: CefrLevel,
  ): List<SpanishPracticePhrase> {
    val normalized = cefrLevel.normalized()
    val byMode = ALL.filter { it.mode == mode }
    if (byMode.isEmpty()) return ALL
    val levelMatches = byMode.filter { it.minLevel.rank() <= normalized.rank() }
    return levelMatches.ifEmpty { byMode }
  }
}
