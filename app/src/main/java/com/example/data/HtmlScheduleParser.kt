package com.example.data

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

object HtmlScheduleParser {

    private fun extractValue(line: String, key: String): String {
        // Matches key:'value' or key:"value"
        val regex = Regex("""$key\s*:\s*['"]([^'"]*)['"]""")
        return regex.find(line)?.groupValues?.get(1) ?: ""
    }

    fun parse(context: Context): List<Match> {
        val matchesList = mutableListOf<Match>()
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("schedule.html")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.contains("group:") && currentLine.contains("t1:")) {
                    val group = extractValue(currentLine, "group")
                    val t1 = extractValue(currentLine, "t1")
                    val t2 = extractValue(currentLine, "t2")
                    val date = extractValue(currentLine, "date")
                    val timeET = extractValue(currentLine, "timeET")
                    var venue = extractValue(currentLine, "venue")
                    
                    if (venue.isEmpty()) {
                        // Fallback in case of special escaping (e.g. Levi's Stadium)
                        val vRegex = Regex("""venue\s*:\s*([^,}]*)""")
                        val rawV = vRegex.find(currentLine)?.groupValues?.get(1)?.trim() ?: ""
                        venue = rawV.trim('\'').trim('"')
                    }
                    
                    var stage: String? = extractValue(currentLine, "stage")
                    if (stage.isNullOrEmpty()) {
                        stage = null
                    }
                    
                    if (group.isNotEmpty() && t1.isNotEmpty() && t2.isNotEmpty()) {
                        matchesList.add(
                            Match(
                                group = group,
                                team1 = t1,
                                team2 = t2,
                                date = date,
                                timeET = timeET,
                                venue = venue,
                                stage = stage
                            )
                        )
                    }
                }
            }
            reader.close()
            Log.d("HtmlScheduleParser", "Successfully parsed ${matchesList.size} matches from HTML schedule.")
        } catch (e: Exception) {
            Log.e("HtmlScheduleParser", "Failed to parse HTML schedule asset", e)
        }
        return matchesList
    }
}
