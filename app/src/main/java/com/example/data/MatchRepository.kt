package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.example.ui.NotificationHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    // True once a valid API key has triggered a successful real-data fetch;
    // keeps the local simulation from overwriting live API scores.
    @Volatile private var useRealApi = false
    private var apiPollingJob: Job? = null

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

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    // Refresh matches from live Football API if key is supplied.
    // Retries up to 3 times with exponential backoff on network errors.
    suspend fun refreshLiveScores(apiKey: String) {
        val service = apiService ?: return
        if (apiKey.isEmpty() || apiKey == "MY_FOOTBALL_API_KEY") {
            Log.d("MatchRepository", "No valid football API key. Skipping network refresh.")
            return
        }
        if (!isNetworkAvailable()) {
            Log.d("MatchRepository", "No network available, skipping API refresh.")
            return
        }
        var delayMs = 2000L
        for (attempt in 1..3) {
            try {
                // Fetch live matches first; merge with full fixture list for status sync
                val liveResponse = service.getLiveFixtures(apiKey = apiKey)
                val fullResponse = service.getFixtures(apiKey = apiKey)
                val apiFixtures = (liveResponse.response + fullResponse.response)
                    .distinctBy { it.fixture.id }

                if (apiFixtures.isNotEmpty()) {
                    useRealApi = true
                    val localMatches = matchDao.getAllMatches()
                    val updatedList = localMatches.map { local ->
                        // Require both home AND away team names to match to avoid false positives
                        val apiMatch = apiFixtures.find { api ->
                            (api.teams.home.name.contains(local.team1, ignoreCase = true) ||
                             local.team1.contains(api.teams.home.name, ignoreCase = true)) &&
                            (api.teams.away.name.contains(local.team2, ignoreCase = true) ||
                             local.team2.contains(api.teams.away.name, ignoreCase = true))
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
                return // success
            } catch (e: Exception) {
                Log.w("MatchRepository", "API refresh attempt $attempt/3 failed: ${e.message}")
                if (attempt < 3) delay(delayMs)
                delayMs *= 2
            }
        }
        Log.e("MatchRepository", "All API refresh attempts exhausted")
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

    // Starts a continuous background loop that polls the real API every 60 seconds.
    // Cancels any previous polling loop. Simulation is suppressed while this is active.
    fun startContinuousPolling(apiKey: String) {
        if (apiKey.isBlank() || apiKey == "MY_FOOTBALL_API_KEY") return
        apiPollingJob?.cancel()
        apiPollingJob = scope.launch {
            while (true) {
                refreshLiveScores(apiKey)
                delay(60_000)
            }
        }
    }

    // Simulate match timeline events such as Kickoffs, Goals, Cards, Fulltime results.
    // Skips all simulation work when a real API key is active to avoid overwriting live data.
    private fun startLiveScoreSimulation() {
        scope.launch {
            delay(5000) // wait for database to fully load/seed
            while (true) {
                if (useRealApi) {
                    delay(15000)
                    continue
                }
                try {
                    val list = matchDao.getAllMatches()
                    if (list.isNotEmpty()) {
                        // Find matches that are currently LIVE
                        var liveMatches = list.filter { it.status == "LIVE" }
                        
                        // If no matches are live, randomly start 2-3 group matches
                        if (liveMatches.isEmpty()) {
                            val groupMatches = list.filter { it.status == "UPCOMING" && it.group.length == 1 }
                            if (groupMatches.isNotEmpty()) {
                                val startedMatches = groupMatches.shuffled().take(3).map { match ->
                                    match.copy(status = "LIVE", team1Score = 0, team2Score = 0, minute = "1")
                                }
                                matchDao.updateMatches(startedMatches)
                                startedMatches.filter { it.isFavorite }.forEach { m ->
                                    notificationHelper.showNotification(
                                        title = "⚽ Match KICKOFF!",
                                        message = "${m.team1} vs ${m.team2} has kicked off in CST!",
                                        matchId = m.id
                                    )
                                }
                                liveMatches = startedMatches
                            }
                        }

                        // Increment score or progress time — accumulate all writes then flush in one transaction
                        val matchUpdates = mutableListOf<Match>()
                        val pendingNotifications = mutableListOf<Triple<String, String, Int>>()

                        liveMatches.forEach { match ->
                            val currentMin = match.minute?.toIntOrNull() ?: 0
                            if (currentMin >= 90) {
                                val finishedMatch = match.copy(status = "FINISHED", minute = "FT")
                                matchUpdates.add(finishedMatch)
                                if (finishedMatch.isFavorite) {
                                    pendingNotifications.add(Triple(
                                        "🏁 Full Time Result",
                                        "FT: ${finishedMatch.team1} ${finishedMatch.team1Score} - ${finishedMatch.team2Score} ${finishedMatch.team2}",
                                        finishedMatch.id
                                    ))
                                }
                            } else {
                                val nextMin = currentMin + Random.nextInt(5, 12)
                                val updatedMin = if (nextMin > 90) 90 else nextMin

                                var t1Score = match.team1Score ?: 0
                                var t2Score = match.team2Score ?: 0
                                var eventTriggered = false
                                var eventTitle = ""
                                var eventMessage = ""

                                // ~12% home goal, ~12% away goal, ~2% red card per poll
                                val eventChance = Random.nextInt(100)
                                when {
                                    eventChance < 12 -> {
                                        t1Score++
                                        eventTriggered = true
                                        eventTitle = "⚽ GOAL! - ${match.team1}"
                                        eventMessage = "${match.team1} scores! ${match.team1} $t1Score - $t2Score ${match.team2} (${updatedMin}')"
                                    }
                                    eventChance in 12..23 -> {
                                        t2Score++
                                        eventTriggered = true
                                        eventTitle = "⚽ GOAL! - ${match.team2}"
                                        eventMessage = "${match.team2} scores! ${match.team1} $t1Score - $t2Score ${match.team2} (${updatedMin}')"
                                    }
                                    eventChance in 24..25 -> {
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
                                matchUpdates.add(updatedMatch)

                                if (eventTriggered && updatedMatch.isFavorite) {
                                    pendingNotifications.add(Triple(eventTitle, eventMessage, updatedMatch.id))
                                }
                            }
                        }

                        if (matchUpdates.isNotEmpty()) matchDao.updateMatches(matchUpdates)
                        pendingNotifications.forEach { (title, msg, id) ->
                            notificationHelper.showNotification(title, msg, id)
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
