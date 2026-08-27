package com.lputouch.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimetableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimetableWidget()

    companion object {
        /** Application-scoped coroutine for widget updates. Survives activity changes. */
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun update(context: Context) {
            scope.launch {
                try {
                    TimetableWidget().updateAll(context)
                } catch (e: Exception) {
                    android.util.Log.e("TimetableWidget", "Widget update failed", e)
                }
            }
        }
    }
}
