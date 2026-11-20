package com.example.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.DinoyoConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

object DinoyoNotificationHelper {
  private const val TAG = "DinoyoFCM"
  private const val SUBSCRIBE_ENDPOINT = "https://dinoyo.vercel.app/api/push/subscribe"
  const val EXTRA_TARGET_URL = "extra_target_url"

  private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channelId = context.getString(R.string.notification_channel_id)
      val channelName = context.getString(R.string.notification_channel_name)
      val channelDesc = context.getString(R.string.notification_channel_desc)
      val channel = NotificationChannel(
        channelId,
        channelName,
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = channelDesc
        enableVibration(true)
      }
      val manager = context.getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }

  fun syncFcmToken(context: Context) {
    val appContext = context.applicationContext
    if (FirebaseApp.getApps(appContext).isEmpty()) {
      Log.w(
        TAG,
        "FirebaseApp belum terinisialisasi. Tambahkan google-services.json dari project Firebase tejiwemo ke folder app/."
      )
      return
    }
    FirebaseMessaging.getInstance().token
      .addOnSuccessListener { token ->
        if (!token.isNullOrBlank()) {
          registerTokenWithServer(token)
        }
      }
      .addOnFailureListener { error ->
        Log.e(TAG, "Gagal mengambil token FCM Android", error)
      }
  }

  fun registerTokenWithServer(token: String) {
    ioScope.launch {
      // Coba daftarkan dengan platform "android", lalu fallback ke "web" jika API server hanya menerima "web"
      val status = postSubscribePayload(token = token, platform = "android")
      if (status in 400..499) {
        postSubscribePayload(token = token, platform = "web")
      }
    }
  }

  private fun postSubscribePayload(token: String, platform: String): Int {
    return runCatching {
      val url = URL(SUBSCRIBE_ENDPOINT)
      val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 10_000
        readTimeout = 10_000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
      }
      val body = JSONObject().apply {
        put("token", token)
        put("platform", platform)
      }.toString()

      OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
        writer.write(body)
        writer.flush()
      }
      val code = conn.responseCode
      conn.disconnect()
      code
    }.getOrElse { error ->
      Log.e(TAG, "Gagal mengirim token FCM ke server ($platform)", error)
      -1
    }
  }

  fun showNotification(
    context: Context,
    title: String,
    body: String,
    targetPathOrUrl: String?,
  ) {
    createNotificationChannel(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        Log.w(TAG, "Izin POST_NOTIFICATIONS belum diberikan oleh pengguna")
        return
      }
    }

    val resolvedUrl = resolveTargetUrl(targetPathOrUrl)
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_TARGET_URL, resolvedUrl)
    }

    val notificationId = (System.currentTimeMillis() and 0xFFFFFFF).toInt()
    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val channelId = context.getString(R.string.notification_channel_id)
    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_stat_dinoyo)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
  }

  fun resolveTargetUrl(rawUrl: String?): String {
    if (rawUrl.isNullOrBlank()) return DinoyoConfig.HOME_URL
    if (rawUrl.startsWith("https://") || rawUrl.startsWith("http://")) {
      return rawUrl
    }
    val cleanPath = if (rawUrl.startsWith("/")) rawUrl else "/$rawUrl"
    return DinoyoConfig.HOME_URL.trimEnd('/') + cleanPath
  }
}
