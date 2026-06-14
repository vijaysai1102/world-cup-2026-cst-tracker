package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Match
import com.example.data.MatchRepository
import com.example.data.StandingTeam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorldCupViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = MatchRepository(application, db.matchDao())

    val allMatchesFlow: Flow<List<Match>> = repository.allMatches
    val favoriteMatchesFlow: Flow<List<Match>> = repository.favoriteMatches

    // State indicators
    val searchQuery = MutableStateFlow("")
    val activeStage = MutableStateFlow("all") // "all", "group", "r32", "r16", "qf", "sf", "fin"
    val showOnlyFavorites = MutableStateFlow(false)

    // Current inspected team (for Team Details Sub-screen)
    val selectedTeam = MutableStateFlow<String?>(null)

    // Match Group Map representing "group fixtures by date"
    val filteredMatchesGrouped: StateFlow<Map<String, List<Match>>> = combine(
        allMatchesFlow,
        searchQuery,
        activeStage,
        showOnlyFavorites,
        selectedTeam
    ) { matches, query, stage, onlyFavs, teamName ->
        var filtered = matches

        // 1. Team Detail check
        if (!teamName.isNullOrEmpty()) {
            filtered = filtered.filter { 
                it.team1.contains(teamName, ignoreCase = true) || 
                it.team2.contains(teamName, ignoreCase = true) 
            }
        }

        // 2. Only favorites check
        if (onlyFavs) {
            filtered = filtered.filter { it.isFavorite }
        }

        // 3. Stage filters check
        if (stage != "all") {
            filtered = filtered.filter { match ->
                when (stage) {
                    "group" -> match.group.length == 1 && match.group[0] in 'A'..'L'
                    "r32" -> match.group == "R32"
                    "r16" -> match.group == "R16"
                    "qf" -> match.group == "QF"
                    "sf" -> match.group == "SF"
                    "fin" -> match.group == "3P" || match.group == "FIN"
                    else -> true
                }
            }
        }

        // 4. Search query check
        if (query.isNotEmpty()) {
            filtered = filtered.filter { match ->
                match.team1.contains(query, ignoreCase = true) ||
                match.team2.contains(query, ignoreCase = true) ||
                match.venue.contains(query, ignoreCase = true) ||
                match.group.contains(query, ignoreCase = true) ||
                (match.stage != null && match.stage.contains(query, ignoreCase = true))
            }
        }

        // Group by Date
        filtered.groupBy { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // Dynamic Standings calculations
    val groupStandings: StateFlow<Map<String, List<StandingTeam>>> = allMatchesFlow.map { matches ->
        repository.calculateStandings(matches)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun toggleFavorite(matchId: Int, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(matchId, isFav)
        }
    }

    // --- Gemini AI Tactical Analysis Hooks ---
    val geminiAnalysis = MutableStateFlow<String?>(null)
    val isGeminiLoading = MutableStateFlow(false)
    val customGeminiKey = MutableStateFlow("")
    val customFootballKey = MutableStateFlow("")

    init {
        val prefs = application.getSharedPreferences("api_settings", android.content.Context.MODE_PRIVATE)
        val geminiKey = prefs.getString("custom_gemini_key", "") ?: ""
        val footballKey = prefs.getString("custom_football_key", "") ?: ""
        // Treat placeholder values as absent so they never reach API calls
        if (geminiKey.isNotBlank() && geminiKey != "MY_GEMINI_API_KEY") {
            customGeminiKey.value = geminiKey
        }
        if (footballKey.isNotBlank() && footballKey != "MY_FOOTBALL_API_KEY") {
            customFootballKey.value = footballKey
        }
    }

    fun saveCustomGeminiKey(key: String) {
        customGeminiKey.value = key
        val prefs = getApplication<Application>().getSharedPreferences("api_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("custom_gemini_key", key).apply()
    }

    fun saveCustomFootballKey(key: String) {
        val trimmedKey = key.trim()
        customFootballKey.value = trimmedKey
        val prefs = getApplication<Application>().getSharedPreferences("api_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("custom_football_key", trimmedKey).apply()
        
        // Trigger an immediate network scores refresh with the new key
        if (trimmedKey.isNotBlank()) {
            triggerApiRefresh(trimmedKey)
        }
    }

    fun fetchMatchPrediction(match: Match) {
        viewModelScope.launch {
            isGeminiLoading.value = true
            geminiAnalysis.value = null
            val scoreStr = if (match.status != "UPCOMING") "${match.team1Score ?: 0} - ${match.team2Score ?: 0}" else "N/A"
            val response = com.example.data.GeminiService.getMatchAnalysis(
                team1 = match.team1,
                team2 = match.team2,
                group = match.group,
                venue = match.venue,
                status = match.status,
                score = scoreStr,
                customApiKey = customGeminiKey.value
            )
            geminiAnalysis.value = response
            isGeminiLoading.value = false
        }
    }

    fun clearGeminiAnalysis() {
        geminiAnalysis.value = null
        isGeminiLoading.value = false
    }

    fun triggerApiRefresh(apiKey: String) {
        repository.startContinuousPolling(apiKey)
    }
}
