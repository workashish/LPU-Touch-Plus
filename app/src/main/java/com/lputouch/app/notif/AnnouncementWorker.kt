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

        val lastNotified = app.sessionStore.lastNotifiedAnnouncementId()

        // Find all announcements that are newer than the last notified one.
        // We notify about each new one (up to a reasonable limit to avoid notification spam).
        val newAnnouncements = announcements.filter { ann ->
            val id = ann.announcementId ?: return@filter false
            id.isNotBlank() && id != lastNotified
        }.take(5) // Cap at 5 notifications per polling cycle

        if (newAnnouncements.isEmpty()) return Result.success()

        // Post a notification for each new announcement
        newAnnouncements.forEachIndexed { index, ann ->
            postNotification(
                title = ann.subject ?: "New announcement",
                text = ann.uploadedBy ?: "",
                notificationId = 1000 + index // Unique ID per notification
            )
        }

        // Update the last notified ID to the most recent one (first in list)
        val mostRecentId = newAnnouncements.first().announcementId
        if (!mostRecentId.isNullOrBlank()) {
            app.sessionStore.saveLastNotifiedAnnouncementId(mostRecentId)
        }

        return Result.success()
    }

    private fun postNotification(title: String, text: String, notificationId: Int = 1) {
        val context = applicationContext
        val channelId = "announcements"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Announcements", NotificationManager.IMPORTANCE_HIGH)
        )

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use system icon as fallback
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
