package com.example.capacitorforegroundwebsocket

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class ForegroundWebSocketService : Service() {

    companion object {
        private var webSocket: WebSocket? = null

        fun sendMessage(message: String) {
            webSocket?.send(message)
        }
    }

    private lateinit var client: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ip = intent?.getStringExtra("ip") ?: return START_NOT_STICKY
        val port = intent.getIntExtra("port", 0)
        val isWss = intent.getBooleanExtra("isWss", false)
        val title = intent.getStringExtra("title") ?: "Foreground WebSocket Service"
        val description = intent.getStringExtra("description") ?: "Running..."

        // Create notification channel
        val channelId = "ForegroundWebSocketChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Foreground WebSocket Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(R.drawable.ic_notification) // Uygulamanıza uygun ikon
            .build()

        startForeground(1, notification)

        // WebSocket URL oluşturuluyor
        val protocol = if (isWss) "wss" else "ws"
        val url = "$protocol://$ip:$port"

        val request = Request.Builder().url(url).build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Bağlantı açıldığında yapılacak işlemler
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Gelen mesajı al, MainActivity'yi uyandır
                wakeUpMainActivity(text)
                // İsteğe bağlı: JS tarafına bildirim göndermek için yerel broadcast kullanılabilir.
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Binary mesajlar işlenebilir.
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                // Kapatıldığında yapılacak işlemler
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Hata durumunda yapılacak işlemler
            }
        }

        webSocket = client.newWebSocket(request, listener)

        return START_STICKY
    }

    // Ana uygulamayı uyandırmak için wake lock ve intent kullanımı
    private fun wakeUpMainActivity(message: String) {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "capacitor-foreground-websocket:WAKE_LOCK"
        )
        // Kısa süreli (örneğin 3 saniye) uyandırma
        wakeLock.acquire(3000)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("websocket_message", message)
        }
        startActivity(intent)
        wakeLock.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Service destroyed")
        webSocket = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}