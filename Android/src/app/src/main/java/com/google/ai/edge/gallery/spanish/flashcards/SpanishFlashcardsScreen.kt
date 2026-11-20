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

package com.google.ai.edge.gallery.spanish.flashcards

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.proto.DictionaryEntry
import com.google.ai.edge.gallery.spanish.ReviewRating
import com.google.ai.edge.gallery.spanish.SpanishSrsScheduler
import com.google.ai.edge.gallery.spanish.shortLabel

/** Full-screen Spanish Flashcards & SM-2 Spaced Repetition deck screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpanishFlashcardsScreen(
  navigateUp: () -> Unit,
  viewModel: SpanishFlashcardsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var selectedTab by remember { mutableIntStateOf(0) }
  var showOverflowMenu by remember { mutableStateOf(false) }
  var showAddDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.task_label_spanish_flashcards)) },
        navigationIcon = {
          IconButton(onClick = navigateUp) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          Box {
            IconButton(onClick = { showOverflowMenu = true }) {
              Icon(Icons.Rounded.MoreVert, contentDescription = "Deck options")
            }
            DropdownMenu(
              expanded = showOverflowMenu,
              onDismissRequest = { showOverflowMenu = false },
            ) {
              DropdownMenuItem(
                text = { Text("Restore Starter Deck") },
                onClick = {
                  showOverflowMenu = false
                  viewModel.restoreStarterDeck()
                },
              )
              DropdownMenuItem(
                text = { Text("Reset Study Progress") },
                onClick = {
                  showOverflowMenu = false
                  viewModel.resetProgress()
                },
              )
            }
          }
        },
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = { showAddDialog = true }) {
        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.flashcards_add_card))
      }
    },
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      PrimaryTabRow(selectedTabIndex = selectedTab) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Study (${uiState.dueCards.size} due)") },
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Deck (${uiState.allCards.size})") },
        )
      }

      if (selectedTab == 0) {
        StudyTabContent(uiState = uiState, viewModel = viewModel)
      } else {
        DeckTabContent(uiState = uiState, viewModel = viewModel)
      }
    }
  }

  if (showAddDialog) {
    AddFlashcardDialog(
      onDismiss = { showAddDialog = false },
      onAdd = { word, translation, targetSentence, nativeSentence, notes ->
        viewModel.addCard(
          word = word,
          translation = translation,
          targetSentence = targetSentence,
          nativeSentence = nativeSentence,
          notes = notes,
        )
        showAddDialog = false
      },
    )
  }
}

@Composable
private fun StudyTabContent(
  uiState: SpanishFlashcardsUiState,
  viewModel: SpanishFlashcardsViewModel,
) {
  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Summary stats bar.
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      StatPill(
        label = "Due",
        value = uiState.dueCards.size.toString(),
        modifier = Modifier.weight(1f),
      )
      StatPill(
        label = "Mastered",
        value = uiState.masteredCount.toString(),
        modifier = Modifier.weight(1f),
      )
      StatPill(
        label = "Accent",
        value = uiState.settings.dialect.shortLabel,
        modifier = Modifier.weight(1f),
      )
    }

    if (uiState.practiceAllMode && uiState.dueCards.isEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Free review mode (all cards)",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        TextButton(onClick = viewModel::exitPracticeAll) { Text("Done") }
      }
    }

    val card = uiState.currentStudyCard
    if (card == null) {
      CaughtUpCard(
        hasAnyCards = uiState.allCards.isNotEmpty(),
        onPracticeAll = viewModel::startPracticeAll,
        onResetProgress = viewModel::resetProgress,
      )
    } else {
      val thumbnail by
        produceState<ImageBitmap?>(initialValue = null, key1 = card.word, key2 = card.learnLanguage) {
          value = viewModel.loadThumbnail(card)
        }

      Card(
        modifier = Modifier.fillMaxWidth().clickable { viewModel.flipCard() },
        shape = RoundedCornerShape(20.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          ),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            SourceBadge(source = sourceLabelFor(card))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Rounded.Flip,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.size(4.dp))
              Text(
                text = if (uiState.isCardFlipped) "Back" else "Tap to flip",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          if (thumbnail != null) {
            Image(
              bitmap = thumbnail!!,
              contentDescription = card.word,
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(112.dp).clip(RoundedCornerShape(14.dp)),
            )
          }

          Text(
            text = card.word,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
              onClick = { viewModel.speak(card.word, slow = false) },
              leadingIcon = {
                Icon(
                  Icons.AutoMirrored.Rounded.VolumeUp,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text("1.0x") },
            )
            AssistChip(
              onClick = { viewModel.speak(card.word, slow = true) },
              leadingIcon = {
                Icon(
                  Icons.Rounded.SlowMotionVideo,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text("0.7x Slow") },
            )
          }

          if (!uiState.isCardFlipped) {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
              onClick = viewModel::flipCard,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("Show Answer")
            }
          } else {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
              text = card.translation,
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold,
              textAlign = TextAlign.Center,
            )
            if (card.targetSentence.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Text(
                      text = card.targetSentence,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Medium,
                    )
                    if (card.nativeSentence.isNotBlank()) {
                      Text(
                        text = card.nativeSentence,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  IconButton(onClick = { viewModel.speak(card.targetSentence, slow = false) }) {
                    Icon(
                      Icons.AutoMirrored.Rounded.VolumeUp,
                      contentDescription = "Play sentence",
                    )
                  }
                }
              }
            }
            if (card.definition.isNotBlank()) {
              Text(
                text = card.definition,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }
            if (card.notes.isNotBlank()) {
              Text(
                text = card.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      }

      if (uiState.isCardFlipped) {
        Text(
          text = "How well did you recall it?",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          for (rating in ReviewRating.entries) {
            val intervalLabel = SpanishSrsScheduler.previewIntervalLabel(card, rating)
            Button(
              onClick = { viewModel.rateCurrentCard(rating) },
              modifier = Modifier.weight(1f),
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
              colors =
                when (rating) {
                  ReviewRating.AGAIN ->
                    ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.errorContainer,
                      contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                  ReviewRating.HARD ->
                    ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.secondaryContainer,
                      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                  ReviewRating.GOOD -> ButtonDefaults.buttonColors()
                  ReviewRating.EASY ->
                    ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                      contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                },
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(rating.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(intervalLabel, style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DeckTabContent(
  uiState: SpanishFlashcardsUiState,
  viewModel: SpanishFlashcardsViewModel,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    OutlinedTextField(
      value = uiState.searchQuery,
      onValueChange = viewModel::updateSearchQuery,
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
      placeholder = { Text("Search Spanish or English cards") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )

    Row(
      modifier =
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      for (filter in FlashcardDeckFilter.entries) {
        FilterChip(
          selected = uiState.selectedFilter == filter,
          onClick = { viewModel.selectFilter(filter) },
          label = { Text(filter.label) },
        )
      }
    }

    if (uiState.filteredDeckCards.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
          text = "No matching Spanish flashcards yet. Save words from Tutor Chat, Camera Tutor, or tap + to add your own.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(items = uiState.filteredDeckCards, key = { it.word.lowercase() }) { entry ->
          DeckCardItem(
            entry = entry,
            onSpeak = { viewModel.speak(entry.word, slow = false) },
            onDelete = { viewModel.deleteCard(entry.word) },
          )
        }
      }
    }
  }
}

@Composable
private fun DeckCardItem(
  entry: DictionaryEntry,
  onSpeak: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = entry.word,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
          SourceBadge(source = sourceLabelFor(entry))
          if (SpanishSrsScheduler.isMastered(entry)) {
            Icon(
              Icons.Rounded.CheckCircle,
              contentDescription = "Mastered",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
          }
        }
        Text(
          text = entry.translation,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (entry.targetSentence.isNotBlank()) {
          Text(
            text = entry.targetSentence,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      IconButton(onClick = onSpeak) {
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Speak")
      }
      IconButton(onClick = onDelete) {
        Icon(
          Icons.Rounded.DeleteOutline,
          contentDescription = "Delete",
          tint = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

@Composable
private fun CaughtUpCard(
  hasAnyCards: Boolean,
  onPracticeAll: () -> Unit,
  onResetProgress: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        Icons.Rounded.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(48.dp),
      )
      Text(
        text = stringResource(R.string.flashcards_all_caught_up),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
      )
      Text(
        text =
          if (hasAnyCards) {
            "No cards are due right now. You can review the whole deck anytime or capture new words with Camera & Tutor Chat."
          } else {
            "Your Spanish deck is empty. Tap + to add a word or restore the starter deck from the menu."
          },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (hasAnyCards) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(onClick = onPracticeAll) { Text("Practice All Cards") }
          OutlinedButton(onClick = onResetProgress) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Reset Due")
          }
        }
      }
    }
  }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun SourceBadge(source: String) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
  ) {
    Text(
      text = source,
      style = MaterialTheme.typography.labelSmall,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}

private fun sourceLabelFor(entry: DictionaryEntry): String =
  when (entry.sourceModule.lowercase()) {
    "camera",
    "" -> "Camera"
    "chat" -> "Chat"
    "pronunciation" -> "Audio Lab"
    "starter" -> "Starter"
    "manual" -> "Manual"
    else -> entry.sourceModule.replaceFirstChar { it.uppercase() }
  }

@Composable
private fun AddFlashcardDialog(
  onDismiss: () -> Unit,
  onAdd: (word: String, translation: String, targetSentence: String, nativeSentence: String, notes: String) -> Unit,
) {
  var word by remember { mutableStateOf("") }
  var translation by remember { mutableStateOf("") }
  var targetSentence by remember { mutableStateOf("") }
  var nativeSentence by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.flashcards_add_card)) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
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
          value = targetSentence,
          onValueChange = { targetSentence = it },
          label = { Text("Spanish example sentence (optional)") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = nativeSentence,
          onValueChange = { nativeSentence = it },
          label = { Text("English example sentence (optional)") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Grammar or dialect notes (optional)") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onAdd(word, translation, targetSentence, nativeSentence, notes) },
        enabled = word.isNotBlank() && translation.isNotBlank(),
      ) {
        Text("Add")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
