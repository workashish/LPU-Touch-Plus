package com.lputouch.app.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lputouch.app.LPUTouchApp
import com.lputouch.app.MainActivity
import com.lputouch.app.R

class AnnouncementWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as LPUTouchApp
        val announcements = try {
            app.studentRepository.getAnnouncements(forceRefresh = true)
        } catch (e: Exception) {
            return Result.retry()
        }
        if (announcements.isEmpty()) return Result.success()

        // Only notify about announcements we haven't already told the user about.
        val latest = announcements.first()
        val latestId = latest.announcementId ?: ""
        val lastNotified = app.sessionStore.lastNotifiedAnnouncementId()
        if (latestId.isNotBlank() && latestId == lastNotified) return Result.success()

        postNotification(latest.subject ?: "New announcement", latest.uploadedBy ?: "")
        if (latestId.isNotBlank()) app.sessionStore.saveLastNotifiedAnnouncementId(latestId)
        return Result.success()
    }

    private fun postNotification(title: String, text: String) {
        val context = applicationContext
        val channelId = "announcements"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Announcements", NotificationManager.IMPORTANCE_HIGH)
        )

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1, notification)
    }
}
