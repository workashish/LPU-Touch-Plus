package com.lputouch.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.lputouch.app.MainActivity
import com.lputouch.app.data.db.AppDatabase
import com.lputouch.app.data.db.CachedTimetableEntity
import java.util.Calendar
import androidx.glance.LocalContext
import androidx.glance.action.clickable

class TimetableWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.get(context)
        val allItems = db.timetableDao().getAll()
        val allAttendance = db.attendanceDao().getAll()
        val attendanceMap = allAttendance.associate {
            it.courseCode.trim().uppercase() to it.faculty
        }

        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val appDay = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
        val dayName = when (appDay) {
            1 -> "Monday"; 2 -> "Tuesday"; 3 -> "Wednesday"
            4 -> "Thursday"; 5 -> "Friday"; 6 -> "Saturday"
            else -> "Sunday"
        }
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val todayClasses = allItems
            .filter { it.day == appDay }
            .map {
                val cCode = (it.courseCode.takeIf { c -> c.isNotBlank() }
                    ?: Regex("(?:C:|Course:)\\s*([^ /]+)").find(it.description)?.groupValues?.get(1)
                    ?: "").trim().uppercase()
                val stitchedFaculty = it.facultyName.takeIf { f -> f.isNotBlank() }
                    ?: attendanceMap[cCode]?.takeIf { f -> f.isNotBlank() }
                    ?: ""
                it.copy(facultyName = stitchedFaculty)
            }
            .sortedBy { item -> parseTimeToMinutes(item.attendanceTime) }

        // Classify each class as past / ongoing / upcoming
        val classified = todayClasses.map { item ->
            val startMin = parseTimeToMinutes(item.attendanceTime)
            val endMin = parseEndTimeToMinutes(item.attendanceTime)
            when {
                startMin <= currentMinutes && currentMinutes < endMin -> ClassStatus.ONGOING
                currentMinutes < startMin -> ClassStatus.UPCOMING
                else -> ClassStatus.PAST
            }
        }

        val nextClass = todayClasses.zip(classified).firstOrNull { it.second == ClassStatus.UPCOMING }
        val ongoingClass = todayClasses.zip(classified).firstOrNull { it.second == ClassStatus.ONGOING }
        val upcomingCount = classified.count { it == ClassStatus.UPCOMING }
        val completedCount = classified.count { it == ClassStatus.PAST } +
                classified.count { it == ClassStatus.ONGOING }

        provideContent {
            GlanceTheme {
                WidgetMain(
                    classes = todayClasses,
                    classified = classified,
                    dayName = dayName,
                    totalClasses = todayClasses.size,
                    completedCount = completedCount,
                    upcomingCount = upcomingCount,
                    nextClassTime = nextClass?.first?.attendanceTime?.split("-")?.firstOrNull()?.trim(),
                    ongoingClass = ongoingClass?.first,
                )
            }
        }
    }

    // ── Main Widget Layout ───────────────────────────────────────────────────

    @Composable
    private fun WidgetMain(
        classes: List<CachedTimetableEntity>,
        classified: List<ClassStatus>,
        dayName: String,
        totalClasses: Int,
        completedCount: Int,
        upcomingCount: Int,
        nextClassTime: String?,
        ongoingClass: CachedTimetableEntity?,
    ) {
        val context = LocalContext.current
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(GlanceTheme.colors.background)
                .padding(14.dp)
                .clickable(actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                ))
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {

                // ── Header ───────────────────────────────────────────────
                WidgetHeader(dayName, totalClasses, completedCount, upcomingCount)

                Spacer(modifier = GlanceModifier.height(10.dp))

                if (classes.isEmpty()) {
                    // ── Empty State ──────────────────────────────────────
                    WidgetEmptyState()
                } else {
                    // ── Ongoing class banner (if any) ────────────────────
                    ongoingClass?.let { ongoing ->
                        WidgetOngoingBanner(ongoing)
                        Spacer(modifier = GlanceModifier.height(8.dp))
                    }

                    // ── Class List ───────────────────────────────────────
                    androidx.glance.appwidget.lazy.LazyColumn(
                        modifier = GlanceModifier.fillMaxSize()
                    ) {
                        classes.forEachIndexed { index, t ->
                            item {
                                val status = classified[index]
                                val isNext = status == ClassStatus.UPCOMING &&
                                        classes.take(index).all { c ->
                                            val i = classes.indexOf(c)
                                            classified[i] != ClassStatus.UPCOMING
                                        }
                                WidgetClassItem(t, status, isNext)
                                Spacer(modifier = GlanceModifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Header ───────────────────────────────────────────────────────────────

    @Composable
    private fun WidgetHeader(
        dayName: String,
        total: Int,
        completed: Int,
        upcoming: Int,
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "📅  $dayName",
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
                // Progress: completed / total
                if (total > 0) {
                    Text(
                        text = "$completed/$total",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 13.sp,
                        ),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(2.dp))

            // Summary line
            val summary = buildString {
                if (upcoming > 0) append("$upcoming upcoming")
                if (upcoming > 0 && completed > 0) append("  •  ")
                if (completed > 0) append("$completed done")
            }
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }

    // ── Ongoing Class Banner ─────────────────────────────────────────────────

    @Composable
    private fun WidgetOngoingBanner(t: CachedTimetableEntity) {
        val room = t.roomNo.takeIf { it.isNotBlank() }
        val faculty = t.facultyName.takeIf { it.isNotBlank() }

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "● NOW",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = t.courseName.takeIf { it.isNotBlank() } ?: t.courseCode,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(
                        text = t.attendanceTime.split("-").firstOrNull()?.trim() ?: "",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Spacer(modifier = GlanceModifier.height(4.dp))
                val details = listOfNotNull(
                    room?.let { "📍 $it" },
                    faculty?.let { "👤 $it" },
                ).joinToString("  •  ")
                if (details.isNotBlank()) {
                    Text(
                        text = details,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 12.sp,
                        ),
                    )
                }
            }
        }
    }

    // ── Class Item ───────────────────────────────────────────────────────────

    @Composable
    private fun WidgetClassItem(t: CachedTimetableEntity, status: ClassStatus, isNext: Boolean) {
        val desc = t.description.replace("\r", "").replace("\n", "").trim()
        val isNotice = desc.contains("available Shortly", ignoreCase = true)

        val courseCodeFix = Regex("(?:C:|Course:)\\s*([^ /]+)").find(desc)?.groupValues?.get(1)
        val roomFix = Regex("R:\\s*:?\\s*([^ /]+)").find(desc)?.groupValues?.get(1)

        val displayTitle = t.courseName.takeIf { it.isNotBlank() }
            ?: t.courseCode.takeIf { it.isNotBlank() }
            ?: courseCodeFix?.takeIf { it.isNotBlank() }
            ?: if (isNotice) "Notice" else "Course"
        val displayRoom = t.roomNo.takeIf { it.isNotBlank() } ?: roomFix

        // Styling based on status
        val bgColor = when {
            isNotice -> GlanceTheme.colors.primaryContainer
            isNext -> GlanceTheme.colors.tertiaryContainer
            status == ClassStatus.ONGOING -> GlanceTheme.colors.primaryContainer
            status == ClassStatus.PAST -> GlanceTheme.colors.surfaceVariant
            else -> GlanceTheme.colors.surfaceVariant
        }

        val isFaded = status == ClassStatus.PAST

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(12.dp)
                .background(bgColor)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: time + status indicator
                Column(
                    modifier = GlanceModifier.width(58.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    if (t.attendanceTime.isNotBlank() && !isNotice) {
                        val timeOnly = t.attendanceTime.split("-").firstOrNull()?.trim() ?: t.attendanceTime
                        Text(
                            text = timeOnly,
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    // Status badge
                    when {
                        isNext -> Text(
                            text = "NEXT",
                            style = TextStyle(
                                color = GlanceTheme.colors.tertiary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        status == ClassStatus.ONGOING -> Text(
                            text = "NOW",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        status == ClassStatus.PAST -> Text(
                            text = "✓",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp,
                            ),
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(10.dp))

                // Vertical divider
                Box(
                    modifier = GlanceModifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(
                            if (isNext) GlanceTheme.colors.tertiary
                            else GlanceTheme.colors.primary
                        )
                ) {}

                Spacer(modifier = GlanceModifier.width(10.dp))

                // Right: course details
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = displayTitle,
                        style = TextStyle(
                            color = if (isFaded) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    // Room + Faculty on same line
                    val meta = listOfNotNull(
                        displayRoom?.let { "📍 $it" },
                        t.facultyName.takeIf { it.isNotBlank() }?.let { "👤 $it" },
                    ).joinToString("  •  ")
                    if (meta.isNotBlank()) {
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = meta,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }
            }
        }
    }

    // ── Empty State ──────────────────────────────────────────────────────────

    @Composable
    private fun WidgetEmptyState() {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.surfaceVariant)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎉",
                    style = TextStyle(fontSize = 28.sp),
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "No classes today!",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "Enjoy your free day",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun parseTimeToMinutes(timeStr: String): Int {
        if (timeStr.isBlank()) return 0
        return try {
            val startPart = timeStr.substringBefore("-").trim()
            val amPm = timeStr.substringAfterLast(" ").trim().uppercase()
            val parts = startPart.split(":")
            var hour = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            if (amPm.contains("PM") && hour < 12) hour += 12
            if (amPm.contains("AM") && hour == 12) hour = 0
            hour * 60 + minute
        } catch (_: Exception) {
            0
        }
    }

    private fun parseEndTimeToMinutes(timeStr: String): Int {
        if (timeStr.isBlank()) return 0
        return try {
            val endPart = timeStr.substringAfter("-").trim()
            val amPm = endPart.substringAfterLast(" ").trim().uppercase()
            val timeOnly = endPart.substringBeforeLast(" ").trim()
            val parts = timeOnly.split(":")
            var hour = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            if (amPm.contains("PM") && hour < 12) hour += 12
            if (amPm.contains("AM") && hour == 12) hour = 0
            hour * 60 + minute
        } catch (_: Exception) {
            parseTimeToMinutes(timeStr) + 50 // fallback: assume 50 min class
        }
    }

    private enum class ClassStatus { PAST, ONGOING, UPCOMING }
}
