package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import com.example.data.Match
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CalendarHelper {

    fun addToCalendar(context: Context, match: Match) {
        val title = "⚽ FIFA 2026: ${match.team1} vs ${match.team2}"
        val description = if (match.group.length == 1) {
            "FIFA World Cup 2026 Group ${match.group} match. Venue: ${match.venue}"
        } else {
            "FIFA World Cup 2026 ${match.stage ?: "Knockout"} match. Venue: ${match.venue}"
        }

        val beginTime = Calendar.getInstance(java.util.TimeZone.getTimeZone("America/New_York"))
        try {
            // E.g. date: "Thu, Jun 11" and timeET: "15:00"
            // We append 2026 as the tournament year.
            val cleanDate = match.date.replace(Regex("""\s+cont\..*"""), "").trim()
            val dateStr = "$cleanDate, 2026 ${match.timeET}" 
            val format = SimpleDateFormat("EEE, MMM d, yyyy HH:mm", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("America/New_York")
            }
            val dateObj = format.parse(dateStr)
            if (dateObj != null) {
                // Calendar ET time parsed successfully (subtraction happens for CST if needed)
                beginTime.time = dateObj
            }
        } catch (e: Exception) {
            // Fallback default setting to June 11, 2026 15:00 ET
            beginTime.set(2026, Calendar.JUNE, 11, 15, 0, 0)
        }

        val endTime = (beginTime.clone() as Calendar).apply {
            add(Calendar.MINUTE, 105) // ~1 hour 45 minutes for a full fixture
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.timeInMillis)
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.Events.EVENT_LOCATION, match.venue)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No calendar application found", Toast.LENGTH_SHORT).show()
        }
    }
}
