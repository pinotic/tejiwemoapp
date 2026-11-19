package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R

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
        Text(
          text = stringResource(R.string.action_open_browser),
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}
