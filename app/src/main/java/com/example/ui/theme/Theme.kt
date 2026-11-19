package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
  primary = TealPrimaryDark,
  onPrimary = OnTealPrimaryDark,
  primaryContainer = TealContainerDark,
  onPrimaryContainer = OnTealContainerDark,
  secondary = SageSecondaryDark,
  onSecondary = OnSageSecondaryDark,
  secondaryContainer = SageContainerDark,
  onSecondaryContainer = OnSageContainerDark,
  tertiary = TerracottaTertiaryDark,
  onTertiary = OnTerracottaTertiaryDark,
  tertiaryContainer = TerracottaContainerDark,
  onTertiaryContainer = OnTerracottaContainerDark,
  background = BackgroundDark,
  onBackground = OnBackgroundDark,
  surface = SurfaceDark,
  onSurface = OnSurfaceDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = OnSurfaceVariantDark,
  surfaceContainer = SurfaceContainerDark,
  surfaceContainerHigh = SurfaceContainerHighDark,
  outline = OutlineDark,
  outlineVariant = OutlineVariantDark,
  error = ErrorDark,
  onError = OnErrorDark,
  errorContainer = ErrorContainerDark,
  onErrorContainer = OnErrorContainerDark,
)

private val LightColorScheme = lightColorScheme(
  primary = TealPrimaryLight,
  onPrimary = OnTealPrimaryLight,
  primaryContainer = TealContainerLight,
  onPrimaryContainer = OnTealContainerLight,
  secondary = SageSecondaryLight,
  onSecondary = OnSageSecondaryLight,
  secondaryContainer = SageContainerLight,
  onSecondaryContainer = OnSageContainerLight,
  tertiary = TerracottaTertiaryLight,
  onTertiary = OnTerracottaTertiaryLight,
  tertiaryContainer = TerracottaContainerLight,
  onTertiaryContainer = OnTerracottaContainerLight,
  background = BackgroundLight,
  onBackground = OnBackgroundLight,
  surface = SurfaceLight,
  onSurface = OnSurfaceLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  surfaceContainer = SurfaceContainerLight,
  surfaceContainerHigh = SurfaceContainerHighLight,
  outline = OutlineLight,
  outlineVariant = OutlineVariantLight,
  error = ErrorLight,
  onError = OnErrorLight,
  errorContainer = ErrorContainerLight,
  onErrorContainer = OnErrorContainerLight,
)

val DinoyoShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun DinoyoTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = DinoyoTypography,
    shapes = DinoyoShapes,
    content = content,
  )
}
