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
import com.google.ai.edge.gallery.proto.Language
import com.google.ai.edge.gallery.proto.dictionaryEntry

/** Curated starter deck of 16 high-utility A1–B1 Spanish words and expressions. */
object SpanishStarterDeck {
  val entries: List<DictionaryEntry> by lazy {
    listOf(
      starterCard(
        word = "el café con leche",
        translation = "coffee with milk",
        targetSentence = "Buenos días, ¿me da un café con leche por favor?",
        nativeSentence = "Good morning, can I have a coffee with milk please?",
        definition = "A1 · Café & ordering",
        notes = "Ask with '¿me pones...?' in Spain or '¿me regala...?' in Colombia.",
      ),
      starterCard(
        word = "la cuenta",
        translation = "the bill / check",
        targetSentence = "Disculpe, ¿nos puede traer la cuenta cuando pueda?",
        nativeSentence = "Excuse me, could you bring us the bill when you can?",
        definition = "A1 · Restaurant essential",
        notes = "Common across all Spanish-speaking countries.",
      ),
      starterCard(
        word = "ahorita",
        translation = "right now / in a little while",
        targetSentence = "Ahorita salgo de la casa para encontrarnos en el parque.",
        nativeSentence = "I'm leaving the house shortly to meet you at the park.",
        definition = "A2 · Latin American nuance",
        notes = "Can mean 'right this second' or 'later' depending on context and country.",
      ),
      starterCard(
        word = "¿dónde queda...?",
        translation = "where is ... located?",
        targetSentence = "Perdón, ¿dónde queda la estación de metro más cercana?",
        nativeSentence = "Excuse me, where is the nearest metro station?",
        definition = "A1 · Directions",
        notes = "Very natural in Latin America; in Spain people often use '¿dónde está...?'.",
      ),
      starterCard(
        word = "tener ganas de",
        translation = "to feel like (doing something)",
        targetSentence = "Hoy tengo muchas ganas de caminar por el centro histórico.",
        nativeSentence = "Today I really feel like walking through the historic center.",
        definition = "A2 · Everyday idiom",
        notes = "Followed by an infinitive verb or a noun.",
      ),
      starterCard(
        word = "madrugar",
        translation = "to wake up early",
        targetSentence = "Mañana tengo que madrugar porque mi vuelo sale a las seis.",
        nativeSentence = "Tomorrow I have to get up early because my flight leaves at six.",
        definition = "B1 · Daily routine verb",
        notes = "No single-word English equivalent; pairs nicely with 'trasnochar' (stay up late).",
      ),
      starterCard(
        word = "el boleto",
        translation = "the ticket",
        targetSentence = "Compré el boleto de autobús con anticipación para viajar cómodo.",
        nativeSentence = "I bought the bus ticket in advance to travel comfortably.",
        definition = "A1 · Travel",
        notes = "Called 'el billete' in Spain and 'el pasaje' in Colombia/Peru/Chile.",
      ),
      starterCard(
        word = "echar de menos",
        translation = "to miss (someone or something)",
        targetSentence = "Echo mucho de menos la comida casera de mi abuela.",
        nativeSentence = "I really miss my grandmother's homemade cooking.",
        definition = "B1 · Emotions & expression",
        notes = "Common in Spain and formal LatAm; 'extrañar' is the everyday LatAm synonym.",
      ),
      starterCard(
        word = "aprovechar",
        translation = "to make the most of / take advantage of",
        targetSentence = "Vamos a aprovechar el buen tiempo para almorzar en la terraza.",
        nativeSentence = "Let's make the most of the good weather to have lunch on the patio.",
        definition = "B1 · Positive action verb",
        notes = "Unlike 'take advantage of', 'aprovechar' is usually positive.",
      ),
      starterCard(
        word = "vale / está bien",
        translation = "okay / sounds good",
        targetSentence = "Vale, nos vemos mañana a las cinco frente al museo.",
        nativeSentence = "Okay, see you tomorrow at five in front of the museum.",
        definition = "A1 · Agreement",
        notes = "'Vale' is iconic in Spain; in Mexico say 'sale' or 'órale', in LatAm 'está bien' / 'dale'.",
      ),
      starterCard(
        word = "llevarse bien",
        translation = "to get along well",
        targetSentence = "Mis compañeros de trabajo y yo nos llevamos muy bien.",
        nativeSentence = "My coworkers and I get along really well.",
        definition = "A2 · Reflexive relationships",
        notes = "Opposite: 'llevarse mal'.",
      ),
      starterCard(
        word = "dar un paseo",
        translation = "to go for a walk / stroll",
        targetSentence = "Después de cenar solemos dar un paseo por la plaza.",
        nativeSentence = "After dinner we usually go for a stroll around the square.",
        definition = "A2 · Leisure collocation",
        notes = "Uses 'dar' (to give), not 'tomar' or 'ir'.",
      ),
      starterCard(
        word = "el recado",
        translation = "errand / short message",
        targetSentence = "Tengo que hacer varios recados antes de que cierre el banco.",
        nativeSentence = "I have to run several errands before the bank closes.",
        definition = "B1 · Daily life noun",
        notes = "In Mexico, 'hacer un mandado' is also common.",
      ),
      starterCard(
        word = "quedarse",
        translation = "to stay / remain",
        targetSentence = "Nos vamos a quedar en este barrio hasta el domingo.",
        nativeSentence = "We are going to stay in this neighborhood until Sunday.",
        definition = "A2 · Essential reflexive verb",
        notes = "Compare with non-reflexive 'quedar' (to arrange to meet / to be located).",
      ),
      starterCard(
        word = "sin embargo",
        translation = "however / nevertheless",
        targetSentence = "El museo es pequeño; sin embargo, tiene obras fascinantes.",
        nativeSentence = "The museum is small; however, it has fascinating artworks.",
        definition = "B1 · Discourse connector",
        notes = "Polished connector for B1–B2 speaking and writing.",
      ),
      starterCard(
        word = "disculpe / con permiso",
        translation = "excuse me (polite / passing by)",
        targetSentence = "Con permiso, ¿podría pasar hacia el asiento de la ventana?",
        nativeSentence = "Excuse me, could I get through to the window seat?",
        definition = "A1 · Politeness distinction",
        notes = "Use 'disculpe/perdón' to get attention or apologize; 'con permiso' when squeezing past.",
      ),
    )
  }

  private fun starterCard(
    word: String,
    translation: String,
    targetSentence: String,
    nativeSentence: String,
    definition: String,
    notes: String,
  ): DictionaryEntry = dictionaryEntry {
    this.word = word
    this.translation = translation
    this.mainLanguage = Language.LANGUAGE_ENGLISH
    this.learnLanguage = Language.LANGUAGE_SPANISH
    this.targetSentence = targetSentence
    this.nativeSentence = nativeSentence
    this.definition = definition
    this.sourceModule = "starter"
    this.easeFactor = SpanishSrsScheduler.DEFAULT_EASE_FACTOR
    this.notes = notes
  }
}
