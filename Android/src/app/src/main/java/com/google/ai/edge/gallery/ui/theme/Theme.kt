/*
 * Copyright 2025 Google LLC
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

package com.google.ai.edge.gallery.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.ai.edge.gallery.proto.Theme

private val lightScheme =
  lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
  )

private val darkScheme =
  darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
  )

@Immutable
data class CustomColors(
  val appTitleGradientColors: List<Color> = listOf(),
  val tabHeaderBgColor: Color = Color.Transparent,
  val taskCardBgColor: Color = Color.Transparent,
  val taskBgColors: List<Color> = listOf(),
  val taskBgGradientColors: List<List<Color>> = listOf(),
  val taskIconColors: List<Color> = listOf(),
  val taskIconShapeBgColor: Color = Color.Transparent,
  val homeBottomGradient: List<Color> = listOf(),
  val userBubbleBgColor: Color = Color.Transparent,
  val agentBubbleBgColor: Color = Color.Transparent,
  val linkColor: Color = Color.Transparent,
  val successColor: Color = Color.Transparent,
  val positiveStrokeColor: Color = Color.Transparent,
  val negativeStrokeColor: Color = Color.Transparent,
  val lassoStrokeColor: Color = Color.Transparent,
  val recordButtonBgColor: Color = Color.Transparent,
  val waveFormBgColor: Color = Color.Transparent,
  val modelInfoIconColor: Color = Color.Transparent,
  val warningContainerColor: Color = Color.Transparent,
  val warningTextColor: Color = Color.Transparent,
  val errorContainerColor: Color = Color.Transparent,
  val errorTextColor: Color = Color.Transparent,
  val newFeatureContainerColor: Color = Color.Transparent,
  val newFeatureTextColor: Color = Color.Transparent,
  val bgStarColor: Color = Color.Transparent,
  val promoBannerBgBrush: Brush = Brush.verticalGradient(listOf(Color.Transparent)),
  val promoBannerIconBgBrush: Brush = Brush.verticalGradient(listOf(Color.Transparent)),
  /**
   * Glow drawn over the live camera feed (for example the AR Language Teacher's thinking state). It
   * sits on camera pixels rather than a themed surface, so it uses the same colors in light and
   * dark themes.
   */
  val cameraHudGlowColor: Color = Color.Transparent,
  /** Text and icon color drawn on [cameraHudGlowColor]. */
  val onCameraHudGlowColor: Color = Color.Transparent,
  /** The colors of the XR Language Teacher `Vocab Popper` and `Buddy Runner` game screens. */
  val vocabGamePalette: VocabGamePalette = lightVocabGamePalette,
)

val LocalCustomColors: ProvidableCompositionLocal<CustomColors> = staticCompositionLocalOf {
  CustomColors()
}

val lightCustomColors =
  CustomColors(
    appTitleGradientColors = listOf(Color(0xFFD94A26), Color(0xFFE58E17)),
    tabHeaderBgColor = Color(0xFFB83B1F),
    taskCardBgColor = surfaceContainerLowestLight,
    taskBgColors =
      listOf(
        // terracotta cream
        Color(0xFFFFF4F0),
        // mediterranean seafoam
        Color(0xFFF1FAF9),
        // warm saffron
        Color(0xFFFFF9EC),
        // azulejo mist
        Color(0xFFF3F6FF),
      ),
    taskBgGradientColors =
      listOf(
        // Terracotta Coral
        listOf(Color(0xFFE85D3F), Color(0xFFB83B1F)),
        // Mediterranean Teal
        listOf(Color(0xFF269994), Color(0xFF0E6B6B)),
        // Saffron Gold
        listOf(Color(0xFFF2A93B), Color(0xFFC97308)),
        // Azulejo Indigo
        listOf(Color(0xFF5B7CFA), Color(0xFF2F4CC9)),
      ),
    taskIconColors =
      listOf(
        // Terracotta
        Color(0xFFB83B1F),
        // Teal
        Color(0xFF0E6B6B),
        // Saffron
        Color(0xFFC97308),
        // Indigo
        Color(0xFF2F4CC9),
      ),
    taskIconShapeBgColor = Color.White,
    homeBottomGradient = listOf(Color(0x00FFFBF8), Color(0xFFFFE5C7)),
    agentBubbleBgColor = Color(0xFFF3E7DF),
    userBubbleBgColor = Color(0xFFB83B1F),
    linkColor = Color(0xFF0E6B6B),
    successColor = Color(0xFF1F7A4C),
    positiveStrokeColor = Color(0xFF1F7A4C),
    negativeStrokeColor = Color(0xFFD93025),
    lassoStrokeColor = Color(0xFF0E6B6B),
    recordButtonBgColor = Color(0xFFDC4D24),
    waveFormBgColor = Color(0xFFB8A39B),
    modelInfoIconColor = Color(0xFFCBB6AE),
    warningContainerColor = Color(0xFFFFF3DC),
    warningTextColor = Color(0xFFB55D00),
    errorContainerColor = Color(0xFFFCE8E6),
    errorTextColor = Color(0xFFD93025),
    newFeatureContainerColor = Color(0xFFFFE5B4),
    newFeatureTextColor = Color(0xFF6B3D00),
    bgStarColor = Color(0x30E8793F),
    promoBannerBgBrush =
      Brush.linearGradient(
        colorStops =
          arrayOf(
            0.0f to Color(0x42FF9E7D),
            0.6154f to Color(0x42F28B50),
            1.0f to Color(0x42F7C45F),
          ),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
      ),
    promoBannerIconBgBrush =
      Brush.linearGradient(
        colorStops =
          arrayOf(
            0.2442f to Color(0x3BB83B1F),
            0.4296f to Color(0x3BE85D3F),
            0.6651f to Color(0x3BF49B2A),
          ),
        start = Offset(0f, 1f),
        end = Offset(1f, 0f),
      ),
    cameraHudGlowColor = primaryLight,
    onCameraHudGlowColor = onPrimaryLight,
    vocabGamePalette = lightVocabGamePalette,
  )

val darkCustomColors =
  CustomColors(
    appTitleGradientColors = listOf(Color(0xFFFFB59E), Color(0xFFF7C45F)),
    tabHeaderBgColor = Color(0xFF9C3017),
    taskCardBgColor = surfaceContainerHighDark,
    taskBgColors =
      listOf(
        // warm terracotta charcoal
        Color(0xFF211714),
        // deep mediterranean slate
        Color(0xFF141E1E),
        // roasted amber
        Color(0xFF211B13),
        // night azulejo
        Color(0xFF171A24),
      ),
    taskBgGradientColors =
      listOf(
        // Terracotta Coral
        listOf(Color(0xFFE85D3F), Color(0xFFB83B1F)),
        // Mediterranean Teal
        listOf(Color(0xFF269994), Color(0xFF0E6B6B)),
        // Saffron Gold
        listOf(Color(0xFFF2A93B), Color(0xFFC97308)),
        // Azulejo Indigo
        listOf(Color(0xFF5B7CFA), Color(0xFF2F4CC9)),
      ),
    taskIconColors =
      listOf(
        // Terracotta
        Color(0xFFE87358),
        // Teal
        Color(0xFF56BDB8),
        // Saffron
        Color(0xFFF7C45F),
        // Indigo
        Color(0xFF8BA2FF),
      ),
    taskIconShapeBgColor = Color(0xFF241E1B),
    homeBottomGradient = listOf(Color(0x00151210), Color(0x22E58E17)),
    agentBubbleBgColor = Color(0xFF26201D),
    userBubbleBgColor = Color(0xFF8F2912),
    linkColor = Color(0xFF79D7D4),
    successColor = Color(0xFF8AD4A6),
    positiveStrokeColor = Color(0xFF4CAF7D),
    negativeStrokeColor = Color(0xFFEA6D65),
    lassoStrokeColor = Color(0xFF79D7D4),
    recordButtonBgColor = Color(0xFFE85D3F),
    waveFormBgColor = Color(0xFF9E8D86),
    modelInfoIconColor = Color(0xFFB5A39C),
    warningContainerColor = Color(0xFF4F3818),
    warningTextColor = Color(0xFFF7C45F),
    errorContainerColor = Color(0xFF523A3B),
    errorTextColor = Color(0xFFEE675C),
    newFeatureContainerColor = Color(0xFF5A3400),
    newFeatureTextColor = Color(0xFFFFE0A3),
    bgStarColor = Color(0x1EE86B38),
    promoBannerBgBrush =
      Brush.linearGradient(
        colorStops = arrayOf(0.0f to Color(0x82632314), 0.8077f to Color(0x822E170F)),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
      ),
    promoBannerIconBgBrush =
      Brush.linearGradient(
        colorStops =
          arrayOf(
            0.2442f to Color(0x6FB83B1F),
            0.4296f to Color(0x6FE85D3F),
            0.6651f to Color(0x6FF49B2A),
          ),
        start = Offset(0f, 1f),
        end = Offset(1f, 0f),
      ),
    cameraHudGlowColor = primaryLight,
    onCameraHudGlowColor = onPrimaryLight,
    vocabGamePalette = darkVocabGamePalette,
  )

val MaterialTheme.customColors: CustomColors
  @Composable @ReadOnlyComposable get() = LocalCustomColors.current

/**
 * Controls the color of the phone's status bar icons based on whether the app is using a dark
 * theme.
 */
@Composable
fun StatusBarColorController(useDarkTheme: Boolean) {
  val view = LocalView.current
  val currentWindow = (view.context as? Activity)?.window

  if (currentWindow != null) {
    SideEffect {
      WindowCompat.setDecorFitsSystemWindows(currentWindow, false)
      val controller = WindowCompat.getInsetsController(currentWindow, view)
      controller.isAppearanceLightStatusBars = !useDarkTheme // Set to true for light icons
    }
  }
}

@Composable
fun GalleryTheme(content: @Composable () -> Unit) {
  val themeOverride = ThemeSettings.themeOverride
  val darkTheme: Boolean =
    (isSystemInDarkTheme() || themeOverride.value == Theme.THEME_DARK) &&
      themeOverride.value != Theme.THEME_LIGHT
  val view = LocalView.current

  StatusBarColorController(useDarkTheme = darkTheme)

  val colorScheme =
    when {
      darkTheme -> darkScheme
      else -> lightScheme
    }

  val customColorsPalette = if (darkTheme) darkCustomColors else lightCustomColors

  CompositionLocalProvider(LocalCustomColors provides customColorsPalette) {
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
  }

  // Make sure the navigation bar stays transparent on manual theme changes.
  LaunchedEffect(darkTheme) {
    val window = (view.context as Activity).window
    window.isNavigationBarContrastEnforced = false
  }
}
