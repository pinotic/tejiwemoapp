package com.example.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DinoyoFirebaseMessagingService : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    DinoyoNotificationHelper.registerTokenWithServer(token)
  }

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)

    val title = message.notification?.title
      ?: message.data["title"]
      ?: "Portal Warga RT 01 Dinoyo"
    val body = message.notification?.body
      ?: message.data["body"]
      ?: "Ada pembaruan baru di Portal Warga."
    val targetUrl = message.data["url"]

    DinoyoNotificationHelper.showNotification(
      context = applicationContext,
      title = title,
      body = body,
      targetPathOrUrl = targetUrl,
    )
  }
}
