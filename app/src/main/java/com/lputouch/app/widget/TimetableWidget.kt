package com.lputouch.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

import com.lputouch.app.data.db.AppDatabase
import com.lputouch.app.data.db.CachedTimetableEntity
import java.util.Calendar

class TimetableWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Run database query on background thread implicitly as provideGlance is a suspend fun
        val db = AppDatabase.get(context)
        val allItems = db.timetableDao().getAll()
        
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Convert Java Calendar (1=Sunday) to API format (1=Monday...7=Sunday)
        val appDay = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
        
        val todayClasses = allItems
            .filter { it.day == appDay }
            .sortedBy { item ->
                val timeStr = item.attendanceTime
                try {
                    val startPart = timeStr.substringBefore("-").trim()
                    val amPm = timeStr.substringAfterLast(" ").trim().uppercase()
                    val parts = startPart.split(":")
                    var hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val minute = parts.getOrNull(1)?.substringBefore(" ")?.toIntOrNull() ?: 0
                    if (amPm.contains("PM") && hour < 12) hour += 12
                    if (amPm.contains("AM") && hour == 12) hour = 0
                    hour * 60 + minute
                } catch (e: Exception) {
                    0
                }
            }

        provideContent {
            GlanceTheme {
                WidgetContent(todayClasses)
            }
        }
    }

    @Composable
    private fun WidgetContent(classes: List<CachedTimetableEntity>) {
        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(16.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Text(
                    text = "Today's Schedule",
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(12.dp))
                
                if (classes.isEmpty()) {
                    Text(
                        text = "No classes scheduled for today. Enjoy!",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    )
                } else {
                    Column(
                        modifier = GlanceModifier.fillMaxSize()
                    ) {
                        classes.forEach { t ->
                            ClassItem(t)
                            Spacer(modifier = GlanceModifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ClassItem(t: CachedTimetableEntity) {
        val desc = t.description.replace("\r", "").replace("\n", "").trim()
        val isAvailableShortly = desc.contains("available Shortly", ignoreCase = true)
        
        val courseCodeFix = Regex("C:\\s*:?\\s*([^ /]+)").find(desc)?.groupValues?.get(1)
        val roomFix = Regex("R:\\s*:?\\s*([^ /]+)").find(desc)?.groupValues?.get(1)
        
        val parts = desc.split("/")
        val type = parts.firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: "Course"

        val displayTitle = t.courseName.takeIf { it.isNotBlank() } 
            ?: t.courseCode.takeIf { it.isNotBlank() } 
            ?: courseCodeFix?.takeIf { it.isNotBlank() } 
            ?: if (isAvailableShortly) "Notice" else type
            
        val displayRoom = t.roomNo.takeIf { it.isNotBlank() } ?: roomFix
        
        // Glance does not support Cards out of the box with elevation, so we build it manually
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(
                    if (isAvailableShortly) GlanceTheme.colors.primaryContainer 
                    else GlanceTheme.colors.surfaceVariant
                )
                .padding(12.dp)
        ) {
            Column {
                if (!isAvailableShortly && t.attendanceTime.isNotBlank()) {
                    Text(
                        text = t.attendanceTime,
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
                
                Text(
                    text = displayTitle,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                displayRoom?.takeIf { it.isNotBlank() }?.let { room ->
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = "Room: $room",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
