package com.lputouch.app.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object NotificationPermissionHelper {
    /**
     * Returns true if POST_NOTIFICATIONS permission is already granted,
     * or if the device is below Android 13 (TIRAMISU) where the permission isn't needed.
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request POST_NOTIFICATIONS permission. Only works from an Activity context.
     * Returns false if not applicable (pre-Android 13) or context is not an Activity.
     */
    fun requestNotificationPermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) return false // already granted

        activity.requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1001
        )
        return true
    }
}
