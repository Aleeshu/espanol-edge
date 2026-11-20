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

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Style
import androidx.compose.runtime.Composable
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskDataForBuiltinTask
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.litertlm.Contents
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/**
 * CustomTask registration for Spanish Flashcards & SM-2 Spaced Repetition. Opens directly without
 * requiring an LLM model download via [com.google.ai.edge.gallery.ui.navigation.GalleryNavGraph].
 */
class SpanishFlashcardsTask
@Inject
constructor(@param:ApplicationContext context: Context) : CustomTask {
  override val task: Task =
    Task(
      id = BuiltInTaskId.SPANISH_FLASHCARDS,
      label = context.getString(R.string.task_label_spanish_flashcards),
      category = Category.LLM,
      icon = Icons.Outlined.Style,
      description = context.getString(R.string.task_desc_spanish_flashcards),
      shortDescription = context.getString(R.string.task_short_desc_spanish_flashcards),
      models = mutableListOf(),
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    systemInstruction: Contents?,
    onDone: (String) -> Unit,
  ) {
    onDone("")
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    onDone()
  }

  @Composable
  override fun MainScreen(data: Any) {
    val navUp: () -> Unit =
      when (data) {
        is CustomTaskDataForBuiltinTask -> data.onNavUp
        else -> ({})
      }
    SpanishFlashcardsScreen(navigateUp = navUp)
  }
}

@Module
@InstallIn(SingletonComponent::class)
internal object SpanishFlashcardsTaskModule {
  @Provides
  @IntoSet
  fun provideSpanishFlashcardsTask(@ApplicationContext context: Context): CustomTask {
    return SpanishFlashcardsTask(context)
  }
}
