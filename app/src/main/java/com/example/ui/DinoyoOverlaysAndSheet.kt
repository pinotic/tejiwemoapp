package com.example.ui

import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun DinoyoInitialLoadingOverlay(progress: Int) {
  val infiniteTransition = rememberInfiniteTransition(label = "brand_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "pulse_scale",
  )

  val backgroundBrush = Brush.verticalGradient(
    colors = listOf(
      MaterialTheme.colorScheme.background,
      MaterialTheme.colorScheme.surfaceContainer,
    )
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(backgroundBrush)
      .padding(32.dp)
      .testTag("initial_loading_overlay"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.widthIn(max = 360.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(104.dp)
          .scale(pulseScale)
          .border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            shape = RoundedCornerShape(30.dp),
          )
          .padding(8.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_dinoyo_icon_foreground),
          contentDescription = stringResource(R.string.cd_brand_logo),
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp)),
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = stringResource(R.string.loading_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(24.dp))

      if (progress in 1..99) {
        LinearProgressIndicator(
          progress = { progress / 100f },
          modifier = Modifier
            .width(180.dp)
            .height(5.dp)
            .clip(CircleShape),
          color = MaterialTheme.colorScheme.tertiary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
      } else {
        LinearProgressIndicator(
          modifier = Modifier
            .width(180.dp)
            .height(5.dp)
            .clip(CircleShape),
          color = MaterialTheme.colorScheme.tertiary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
      }
    }
  }
}

@Composable
fun DinoyoErrorScreen(
  isOffline: Boolean,
  onRetry: () -> Unit,
  onOpenInBrowser: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .padding(24.dp)
      .testTag("error_state_screen"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.widthIn(max = 440.dp),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_dinoyo_offline),
        contentDescription = stringResource(R.string.cd_offline_illustration),
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxWidth()
          .height(220.dp)
          .clip(RoundedCornerShape(24.dp)),
      )

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = stringResource(
          if (isOffline) R.string.error_offline_title else R.string.error_load_title
        ),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = stringResource(
          if (isOffline) R.string.error_offline_description else R.string.error_load_description
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(28.dp))

      Button(
        onClick = onRetry,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("retry_load_button"),
        shape = RoundedCornerShape(16.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Refresh,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.action_retry),
          style = MaterialTheme.typography.labelLarge,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedButton(
        onClick = onOpenInBrowser,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("error_open_browser_button"),
        shape = RoundedCornerShape(16.dp),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.action_open_browser),
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

@Composable
fun DinoyoMoreSheetContent(
  pageTitle: String,
  currentUrl: String,
  isDesktopMode: Boolean,
  isImmersiveMode: Boolean,
  onToggleDesktopMode: () -> Unit,
  onToggleImmersiveMode: () -> Unit,
  onOpenInBrowser: () -> Unit,
  onCopyLink: () -> Unit,
  onClearCache: () -> Unit,
) {
  val displayHost = remember(currentUrl) {
    runCatching { Uri.parse(currentUrl).host }.getOrNull() ?: DinoyoConfig.HOME_HOST
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 20.dp)
      .padding(bottom = 24.dp)
      .testTag("more_options_sheet"),
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_dinoyo_icon_foreground),
          contentDescription = stringResource(R.string.cd_brand_logo),
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = pageTitle.ifBlank { stringResource(R.string.app_name) },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Lock,
              contentDescription = stringResource(R.string.cd_secure_connection),
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = displayHost,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = stringResource(R.string.sheet_title),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )

    SheetSwitchItem(
      icon = Icons.Outlined.DesktopWindows,
      title = stringResource(R.string.option_desktop_mode),
      subtitle = stringResource(R.string.option_desktop_mode_desc),
      checked = isDesktopMode,
      onToggle = onToggleDesktopMode,
      testTag = "switch_desktop_mode",
    )

    SheetSwitchItem(
      icon = Icons.Outlined.Fullscreen,
      title = stringResource(R.string.option_immersive_mode),
      subtitle = stringResource(R.string.option_immersive_mode_desc),
      checked = isImmersiveMode,
      onToggle = onToggleImmersiveMode,
      testTag = "switch_immersive_mode",
    )

    HorizontalDivider(
      modifier = Modifier.padding(vertical = 8.dp),
      color = MaterialTheme.colorScheme.outlineVariant,
    )

    SheetActionItem(
      icon = Icons.AutoMirrored.Outlined.OpenInNew,
      title = stringResource(R.string.option_open_in_browser),
      subtitle = stringResource(R.string.option_open_in_browser_desc),
      onClick = onOpenInBrowser,
      testTag = "action_open_browser",
    )

    SheetActionItem(
      icon = Icons.Outlined.ContentCopy,
      title = stringResource(R.string.option_copy_link),
      subtitle = stringResource(R.string.option_copy_link_desc),
      onClick = onCopyLink,
      testTag = "action_copy_link",
    )

    SheetActionItem(
      icon = Icons.Outlined.DeleteSweep,
      title = stringResource(R.string.option_clear_cache),
      subtitle = stringResource(R.string.option_clear_cache_desc),
      onClick = onClearCache,
      testTag = "action_clear_cache",
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = stringResource(R.string.version_label),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun SheetSwitchItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onToggle: () -> Unit,
  testTag: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onToggle)
      .minimumInteractiveComponentSize()
      .padding(horizontal = 8.dp, vertical = 10.dp)
      .testTag(testTag),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(24.dp),
    )
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Switch(
      checked = checked,
      onCheckedChange = { onToggle() },
    )
  }
}

@Composable
private fun SheetActionItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  testTag: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .minimumInteractiveComponentSize()
      .padding(horizontal = 8.dp, vertical = 12.dp)
      .testTag(testTag),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(24.dp),
    )
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
