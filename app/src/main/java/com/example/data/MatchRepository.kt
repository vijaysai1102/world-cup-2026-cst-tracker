package com.example.data

import android.content.Context
import android.util.Log
import com.example.ui.NotificationHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MatchRepository(
    private val context: Context,
    private val matchDao: MatchDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Flowing lists from Room
    val allMatches: Flow<List<Match>> = matchDao.getAllMatchesFlow()
    val favoriteMatches: Flow<List<Match>> = matchDao.getFavoriteMatchesFlow()

    private var apiService: FootballApiService? = null
    private val notificationHelper = NotificationHelper(context)

    init {
        setupRetrofit()
        scope.launch {
            // Seed DB from HTML file if empty
            val count = matchDao.getCount()
            if (count == 0) {
                val matches = HtmlScheduleParser.parse(context)
                if (matches.isNotEmpty()) {
                    matchDao.insertAll(matches)
                }
            }
            
            // Start our Live Score Simulation Background Thread
            startLiveScoreSimulation()
        }
    }

    private fun setupRetrofit() {
        try {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://v3.football.api-sports.io/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            apiService = retrofit.create(FootballApiService::class.java)
        } catch (e: Exception) {
            Log.e("MatchRepository", "Failed to setup Retrofit client", e)
        }
    }

    // Refresh matches from live Football API if key is supplied
    suspend fun refreshLiveScores(apiKey: String) {
        val service = apiService ?: return
        if (apiKey.isEmpty() || apiKey == "MY_FOOTBALL_API_KEY") {
            Log.d("MatchRepository", "No valid football API key. Skipping network refresh.")
            return
        }
        try {
            // Query live matches or overall fixtures
            val response = service.getFixtures(apiKey = apiKey)
            if (response.response.isNotEmpty()) {
                val localMatches = matchDao.getAllMatches()
                val updatedList = localMatches.map { local ->
                    // Match API entry with local fixture using team names or date
                    val apiMatch = response.response.find { api ->
                        api.teams.home.name.contains(local.team1, ignoreCase = true) ||
                        local.team1.contains(api.teams.home.name, ignoreCase = true)
                    }
                    if (apiMatch != null) {
                        local.copy(
                            team1Score = apiMatch.goals.home,
                            team2Score = apiMatch.goals.away,
                            status = when (apiMatch.fixture.status.short) {
                                "NS" -> "UPCOMING"
                                "1H", "2H", "HT" -> "LIVE"
                                "FT", "AET", "PEN" -> "FINISHED"
                                else -> local.status
                            },
                            minute = apiMatch.fixture.status.elapsed?.toString()
                        )
                    } else {
                        local
                    }
                }
                matchDao.insertAll(updatedList)
            }
        } catch (e: Exception) {
            Log.e("MatchRepository", "Error refreshing live scores from internet", e)
        }
    }

    // Dynamic Standings calculation based on played matches
    // This allows accurate automatic standings even when completely offline!
    fun calculateStandings(matches: List<Match>): Map<String, List<StandingTeam>> {
        val standings = mutableMapOf<String, MutableMap<String, StandingTeam>>()

        // Initialize groups A to L
        val groupNames = ('A'..'L').map { it.toString() }
        groupNames.forEach { group ->
            standings[group] = mutableMapOf()
        }

        matches.forEach { match ->
            val group = match.group
            if (group.length == 1 && group[0] in 'A'..'L') {
                val teamMap = standings[group] ?: return@forEach
                
                // Initialize teams in map if not present
                val t1 = match.team1
                val t2 = match.team2
                if (!teamMap.containsKey(t1)) teamMap[t1] = StandingTeam(t1, group)
                if (!teamMap.containsKey(t2)) teamMap[t2] = StandingTeam(t2, group)

                val st1 = teamMap[t1]!!
                val st2 = teamMap[t2]!!

                if (match.status == "FINISHED" || match.status == "LIVE" && match.team1Score != null && match.team2Score != null) {
                    val s1 = match.team1Score!!
                    val s2 = match.team2Score!!

                    val playedDelta = 1
                    val w1 = if (s1 > s2) 1 else 0
                    val d1 = if (s1 == s2) 1 else 0
                    val l1 = if (s1 < s2) 1 else 0

                    val w2 = if (s2 > s1) 1 else 0
                    val d2 = if (s2 == s1) 1 else 0
                    val l2 = if (s2 < s1) 1 else 0

                    val p1 = w1 * 3 + d1
                    val p2 = w2 * 3 + d2

                    teamMap[t1] = st1.copy(
                        played = st1.played + playedDelta,
                        wins = st1.wins + w1,
                        draws = st1.draws + d1,
                        losses = st1.losses + l1,
                        goalsFor = st1.goalsFor + s1,
                        goalsAgainst = st1.goalsAgainst + s2,
                        points = st1.points + p1
                    )

                    teamMap[t2] = st2.copy(
                        played = st2.played + playedDelta,
                        wins = st2.wins + w2,
                        draws = st2.draws + d2,
                        losses = st2.losses + l2,
                        goalsFor = st2.goalsFor + s2,
                        goalsAgainst = st2.goalsAgainst + s1,
                        points = st2.points + p2
                    )
                }
            }
        }

        return standings.mapValues { (_, teamMap) ->
            teamMap.values.sortedWith(
                compareByDescending<StandingTeam> { it.points }
                    .thenByDescending { it.goalsDiff }
                    .thenByDescending { it.goalsFor }
                    .thenBy { it.teamName }
            )
        }
    }

    suspend fun toggleFavorite(matchId: Int, isFav: Boolean) {
        matchDao.updateFavorite(matchId, isFav)
    }

    // Simulate match timeline events such as Kickoffs, Goals, Cards, Fulltime results
    private fun startLiveScoreSimulation() {
        scope.launch {
            delay(5000) // wait for database to fully load/seed
            while (true) {
                try {
                    val list = matchDao.getAllMatches()
                    if (list.isNotEmpty()) {
                        // Find matches that are currently LIVE
                        var liveMatches = list.filter { it.status == "LIVE" }
                        
                        // If no matches are live, let's randomly start 2-3 group matches!
                        if (liveMatches.isEmpty()) {
                            val groupMatches = list.filter { it.status == "UPCOMING" && it.group.length == 1 }
                            if (groupMatches.isNotEmpty()) {
                                val toLive = groupMatches.shuffled().take(3)
                                toLive.forEach { match ->
                                    val startMatch = match.copy(
                                        status = "LIVE",
                                        team1Score = 0,
                                        team2Score = 0,
                                        minute = "1"
                                    )
                                    matchDao.updateMatch(startMatch)
                                    
                                    // Kickoff alert for favorite team
                                    if (startMatch.isFavorite) {
                                        notificationHelper.showNotification(
                                            title = "⚽ Match KICKOFF!",
                                            message = "${startMatch.team1} vs ${startMatch.team2} has kicked off in CST!",
                                            matchId = startMatch.id
                                        )
                                    }
                                }
                                liveMatches = toLive
                            }
                        }

                        // Increment score or progress time of currently live games
                        liveMatches.forEach { match ->
                            val currentMin = match.minute?.toIntOrNull() ?: 0
                            if (currentMin >= 90) {
                                // Finish the game
                                val finishedMatch = match.copy(
                                    status = "FINISHED",
                                    minute = "FT"
                                )
                                matchDao.updateMatch(finishedMatch)
                                
                                // Full time alert for favorite team
                                if (finishedMatch.isFavorite) {
                                    notificationHelper.showNotification(
                                        title = "🏁 Full Time Result",
                                        message = "FT: ${finishedMatch.team1} ${finishedMatch.team1Score} - ${finishedMatch.team2Score} ${finishedMatch.team2}",
                                        matchId = finishedMatch.id
                                    )
                                }
                            } else {
                                // Progress minute
                                val nextMin = currentMin + Random.nextInt(5, 12)
                                val updatedMin = if (nextMin > 90) 90 else nextMin
                                
                                // Check for goals or cards
                                var t1Score = match.team1Score ?: 0
                                var t2Score = match.team2Score ?: 0
                                var eventTriggered = false
                                var eventTitle = ""
                                var eventMessage = ""

                                val eventChance = Random.nextInt(100)
                                when {
                                    eventChance < 15 -> { // Goal Home
                                        t1Score++
                                        eventTriggered = true
                                        eventTitle = "⚽ GOAL! - ${match.team1}"
                                        eventMessage = "${match.team1} scores! New score: ${match.team1} $t1Score - $t2Score ${match.team2} (${updatedMin}')"
                                    }
                                    eventChance in 15..29 -> { // Goal Away
                                        t2Score++
                                        eventTriggered = true
                                        eventTitle = "⚽ GOAL! - ${match.team2}"
                                        eventMessage = "${match.team2} scores! New score: ${match.team1} $t1Score - $t2Score ${match.team2} (${updatedMin}')"
                                    }
                                    eventChance in 30..35 -> { // Red Card
                                        eventTriggered = true
                                        val team = if (Random.nextBoolean()) match.team1 else match.team2
                                        eventTitle = "🔴 RED CARD!"
                                        eventMessage = "Red card issued to $team player at ${updatedMin}'!"
                                    }
                                }

                                val updatedMatch = match.copy(
                                    team1Score = t1Score,
                                    team2Score = t2Score,
                                    minute = updatedMin.toString()
                                )
                                matchDao.updateMatch(updatedMatch)

                                if (eventTriggered && updatedMatch.isFavorite) {
                                    notificationHelper.showNotification(
                                        title = eventTitle,
                                        message = eventMessage,
                                        matchId = updatedMatch.id
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MatchRepository", "Simulation loop interval failure", e)
                }
                delay(15000) // simulation advances every 15 seconds
            }
        }
    }
}

// Model representing Team Standings entries
data class StandingTeam(
    val teamName: String,
    val groupName: String,
    val played: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val points: Int = 0
) {
    val goalsDiff: Int get() = goalsFor - goalsAgainst
}
