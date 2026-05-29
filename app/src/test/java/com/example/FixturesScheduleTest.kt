package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.HtmlScheduleParser
import com.example.data.Match
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FixturesScheduleTest {

    @Test
    fun verifyTotalMatchesCount() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val matches = HtmlScheduleParser.parse(context)
        assertEquals("Total matches should be 104", 104, matches.size)
    }

    @Test
    fun verifyGroupStageDailyMatchesCount() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val matches = HtmlScheduleParser.parse(context)
        
        // Group matches by date
        val matchesByDate = matches.groupBy { it.date }
        
        // Opening day (June 11) - 2 matches
        assertEquals(2, matchesByDate["Thu, Jun 11"]?.size ?: 0)
        
        // Day 2 (June 12) - 2 matches
        assertEquals(2, matchesByDate["Fri, Jun 12"]?.size ?: 0)
        
        // Group stage days June 13 to June 23 should have exactly 4 matches per day
        val standardGroupStageDates = listOf(
            "Sat, Jun 13", "Sun, Jun 14", "Mon, Jun 15", "Tue, Jun 16", "Wed, Jun 17",
            "Thu, Jun 18", "Fri, Jun 19", "Sat, Jun 20", "Sun, Jun 21", "Mon, Jun 22", "Tue, Jun 23"
        )
        for (date in standardGroupStageDates) {
            val count = matchesByDate[date]?.size ?: 0
            assertEquals("Date $date should have exactly 4 matches", 4, count)
        }
        
        // Final group stage days June 24 to June 27 should have exactly 6 matches per day
        val finalGroupStageDates = listOf(
            "Wed, Jun 24", "Thu, Jun 25", "Fri, Jun 26", "Sat, Jun 27"
        )
        for (date in finalGroupStageDates) {
            val count = matchesByDate[date]?.size ?: 0
            assertEquals("Date $date should have exactly 6 matches", 6, count)
        }
    }

    @Test
    fun verifyMidnightMatchCstConversion() {
        // Australia vs Turkey is stored with date "Sat, Jun 13" and timeET "00:00"
        // Let's verify that toCstTime() correctly returns 11:00 PM
        val match = Match(
            group = "D",
            team1 = "Australia",
            team2 = "Türkiye",
            date = "Sat, Jun 13",
            timeET = "00:00",
            venue = "BC Place, Vancouver",
            stage = null
        )
        
        val cstTime = match.toCstTime()
        assertEquals("11:00", cstTime.display)
        assertEquals("PM", cstTime.ampm)
    }

    @Test
    fun verifyCalendarDateOffsetForMidnightMatches() {
        // Simulate CalendarHelper parsing for a normal match vs a midnight match
        val normalMatch = Match(
            group = "A",
            team1 = "Mexico",
            team2 = "South Africa",
            date = "Thu, Jun 11",
            timeET = "15:00",
            venue = "Estadio Azteca",
            stage = null
        )
        
        val midnightMatch = Match(
            group = "D",
            team1 = "Australia",
            team2 = "Türkiye",
            date = "Sat, Jun 13",
            timeET = "00:00",
            venue = "BC Place, Vancouver",
            stage = null
        )

        // Helper to parse like CalendarHelper
        fun parseMatchDate(match: Match): Calendar {
            val beginTime = Calendar.getInstance(java.util.TimeZone.getTimeZone("America/New_York"))
            val cleanDate = match.date.replace(Regex("""\s+cont\..*"""), "").trim()
            val dateStr = "$cleanDate, 2026 ${match.timeET}" 
            val format = SimpleDateFormat("EEE, MMM d, yyyy HH:mm", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("America/New_York")
            }
            var dateObj = format.parse(dateStr)
            if (dateObj != null) {
                if (match.timeET == "00:00") {
                    val cal = Calendar.getInstance().apply { time = dateObj }
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    dateObj = cal.time
                }
                beginTime.time = dateObj
            }
            return beginTime
        }

        val normalTime = parseMatchDate(normalMatch)
        assertEquals(2026, normalTime.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, normalTime.get(Calendar.MONTH))
        assertEquals(11, normalTime.get(Calendar.DAY_OF_MONTH))
        assertEquals(15, normalTime.get(Calendar.HOUR_OF_DAY))

        val midnightTime = parseMatchDate(midnightMatch)
        assertEquals(2026, midnightTime.get(Calendar.YEAR))
        // Midnight match: Sat, Jun 13 with timeET 00:00 should be parsed as Sun, Jun 14 00:00 ET
        assertEquals(Calendar.JUNE, midnightTime.get(Calendar.MONTH))
        assertEquals(14, midnightTime.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, midnightTime.get(Calendar.HOUR_OF_DAY))
    }
}
