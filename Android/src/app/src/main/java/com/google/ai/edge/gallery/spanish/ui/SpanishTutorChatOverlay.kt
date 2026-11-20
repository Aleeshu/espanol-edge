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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.spanish.SpanishLearningSettings
import com.google.ai.edge.gallery.spanish.SpanishRoleplayScenario
import com.google.ai.edge.gallery.spanish.code
import com.google.ai.edge.gallery.spanish.shortLabel
import com.google.ai.edge.gallery.ui.theme.emptyStateContent
import com.google.ai.edge.gallery.ui.theme.emptyStateTitle
import kotlinx.coroutines.delay

/** Compact scenario selector, dialect/CEFR pill, and flashcard capture bar for Spanish Tutor Chat. */
@Composable
fun SpanishTutorChatBar(
  settings: SpanishLearningSettings,
  selectedScenario: SpanishRoleplayScenario,
  saveFeedback: String?,
  onSelectScenario: (SpanishRoleplayScenario) -> Unit,
  onSaveWord: (word: String, translation: String, example: String) -> Unit,
  onDismissFeedback: () -> Unit,
) {
  var showSaveDialog by remember { mutableStateOf(false) }

  LaunchedEffect(saveFeedback) {
    if (saveFeedback != null) {
      delay(2_200)
      onDismissFeedback()
    }
  }

  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AssistChip(
        onClick = { showSaveDialog = true },
        leadingIcon = {
          Icon(
            Icons.Rounded.BookmarkAdd,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        },
        label = { Text(stringResource(R.string.save_word_to_flashcards)) },
      )
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      ) {
        Text(
          text =
            "${settings.dialect.shortLabel} · ${settings.cefrLevel.code}" +
              if (settings.inlineCorrections) " · Corrections ON" else "",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
      }
      for (scenario in SpanishRoleplayScenario.entries) {
        FilterChip(
          selected = scenario == selectedScenario,
          onClick = { onSelectScenario(scenario) },
          label = { Text(scenario.title) },
        )
      }
    }

    if (saveFeedback != null) {
      Text(
        text = saveFeedback,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp),
      )
    }
  }

  if (showSaveDialog) {
    SaveSpanishWordDialog(
      onDismiss = { showSaveDialog = false },
      onSave = { word, translation, example ->
        onSaveWord(word, translation, example)
        showSaveDialog = false
      },
    )
  }
}

/** Empty state for Spanish Tutor Chat with clickable scenario starter prompts. */
@Composable
fun SpanishTutorEmptyState(
  settings: SpanishLearningSettings,
  scenario: SpanishRoleplayScenario,
  onStarterPromptClicked: (String) -> Unit,
  onSpeakPrompt: (String) -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier.align(Alignment.Center)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ) {
        Text(
          text = "¡A conversar!",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
      }
      Text(
        text = "${scenario.spanishTitle} · ${scenario.title}",
        style = emptyStateTitle,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
      )
      Text(
        text =
          "${scenario.description}\n${settings.dialect.shortLabel} Spanish · CEFR ${settings.cefrLevel.code}",
        style = emptyStateContent,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      for (starter in scenario.starterPrompts) {
        Card(
          onClick = { onStarterPromptClicked(starter.spanishText) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = starter.spanishText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = starter.englishSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(onClick = { onSpeakPrompt(starter.spanishText) }) {
              Icon(
                Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "Listen in Spanish",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SaveSpanishWordDialog(
  onDismiss: () -> Unit,
  onSave: (word: String, translation: String, example: String) -> Unit,
) {
  var word by remember { mutableStateOf("") }
  var translation by remember { mutableStateOf("") }
  var example by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Save to Flashcards") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = word,
          onValueChange = { word = it },
          label = { Text("Spanish word or phrase") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = translation,
          onValueChange = { translation = it },
          label = { Text("English meaning") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = example,
          onValueChange = { example = it },
          label = { Text("Example sentence (optional)") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(word, translation, example) },
        enabled = word.isNotBlank() && translation.isNotBlank(),
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
