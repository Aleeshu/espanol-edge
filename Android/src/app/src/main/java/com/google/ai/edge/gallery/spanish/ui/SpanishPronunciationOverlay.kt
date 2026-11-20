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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.spanish.PronunciationPracticeMode
import com.google.ai.edge.gallery.spanish.SpanishLearningSettings
import com.google.ai.edge.gallery.spanish.SpanishPracticePhrase
import com.google.ai.edge.gallery.spanish.code
import com.google.ai.edge.gallery.spanish.shortLabel
import com.google.ai.edge.gallery.ui.theme.emptyStateContent
import com.google.ai.edge.gallery.ui.theme.emptyStateTitle
import kotlinx.coroutines.delay

/** Compact Pronunciation & Listening Lab drill dock rendered above the audio composer. */
@Composable
fun SpanishPronunciationDock(
  settings: SpanishLearningSettings,
  mode: PronunciationPracticeMode,
  currentPhrase: SpanishPracticePhrase?,
  saveFeedback: String?,
  onSelectMode: (PronunciationPracticeMode) -> Unit,
  onListenNormal: () -> Unit,
  onListenSlow: () -> Unit,
  onNextPhrase: () -> Unit,
  onSavePhrase: () -> Unit,
  onDismissFeedback: () -> Unit,
) {
  LaunchedEffect(saveFeedback) {
    if (saveFeedback != null) {
      delay(2_200)
      onDismissFeedback()
    }
  }

  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      for (item in PronunciationPracticeMode.entries) {
        FilterChip(
          selected = item == mode,
          onClick = { onSelectMode(item) },
          label = { Text(item.label) },
        )
      }
      Text(
        text = "${settings.dialect.shortLabel} · ${settings.cefrLevel.code}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp),
      )
    }

    if (currentPhrase != null) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = currentPhrase.spanish,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = currentPhrase.english,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "Focus: ${currentPhrase.phoneticTip}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            AssistChip(
              onClick = onListenNormal,
              leadingIcon = {
                Icon(
                  Icons.AutoMirrored.Rounded.VolumeUp,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text("1.0x Listen") },
            )
            AssistChip(
              onClick = onListenSlow,
              leadingIcon = {
                Icon(
                  Icons.Rounded.SlowMotionVideo,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text("0.7x Slow") },
            )
            AssistChip(
              onClick = onNextPhrase,
              leadingIcon = {
                Icon(
                  Icons.AutoMirrored.Rounded.NavigateNext,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text("Next") },
            )
            OutlinedButton(onClick = onSavePhrase) {
              Icon(
                Icons.Rounded.BookmarkAdd,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Text(" Save", style = MaterialTheme.typography.labelMedium)
            }
          }
        }
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
}

/** Empty state for Pronunciation & Listening Lab. */
@Composable
fun SpanishPronunciationEmptyState(
  settings: SpanishLearningSettings,
  mode: PronunciationPracticeMode,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier.align(Alignment.Center)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 36.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = stringResource(R.string.askaudio_emptystate_title),
        style = emptyStateTitle,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
      )
      Text(
        text =
          "${mode.description}\nTarget accent: ${settings.dialect.shortLabel} · Level ${settings.cefrLevel.code}",
        style = emptyStateContent,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Text(
        text = stringResource(R.string.askaudio_emptystate_content),
        style = emptyStateContent,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}
