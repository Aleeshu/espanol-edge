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

package com.google.ai.edge.gallery.runtime.gemini

import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatus
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.ModelDownloader
import com.google.ai.edge.gallery.data.Task

/**
 * Zero-download [ModelDownloader] for [com.google.ai.edge.gallery.data.RuntimeType.GEMINI_CLOUD]
 * models. Cloud Gemini models require no local weights on disk and are immediately ready.
 */
class GeminiCloudModelDownloader : ModelDownloader {
  override suspend fun queryStatus(model: Model): ModelDownloadStatus =
    ModelDownloadStatus(status = ModelDownloadStatusType.SUCCEEDED)

  override fun download(
    task: Task?,
    model: Model,
    includeExtraDataFiles: Boolean,
    onStatusUpdated: (Model, ModelDownloadStatus) -> Unit,
  ) {
    onStatusUpdated(model, ModelDownloadStatus(status = ModelDownloadStatusType.SUCCEEDED))
  }

  override val supportsCancel: Boolean = false

  override fun cancel(model: Model) {}

  override suspend fun delete(model: Model) {}
}
