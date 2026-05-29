package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiFootballResponse(
    val response: List<ApiFixtureItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiFixtureItem(
    val fixture: ApiFixtureInfo,
    val teams: ApiTeamsInfo,
    val goals: ApiGoalsInfo,
    val score: ApiScoreInfo? = null
)

@JsonClass(generateAdapter = true)
data class ApiFixtureInfo(
    val id: Int,
    val date: String,
    val timezone: String?,
    val status: ApiStatusInfo
)

@JsonClass(generateAdapter = true)
data class ApiStatusInfo(
    val long: String?,
    val short: String?, // "NS", "1H", "HT", "2H", "FT", "AET", "PEN"
    val elapsed: Int?
)

@JsonClass(generateAdapter = true)
data class ApiTeamsInfo(
    val home: ApiTeamInfo,
    val away: ApiTeamInfo
)

@JsonClass(generateAdapter = true)
data class ApiTeamInfo(
    val id: Int,
    val name: String,
    val logo: String?
)

@JsonClass(generateAdapter = true)
data class ApiGoalsInfo(
    val home: Int?,
    val away: Int?
)

@JsonClass(generateAdapter = true)
data class ApiScoreInfo(
    val halftime: ApiGoalsInfo?,
    val fulltime: ApiGoalsInfo?,
    val extratime: ApiGoalsInfo?,
    val penalty: ApiGoalsInfo?
)

// Statistics Responses
@JsonClass(generateAdapter = true)
data class ApiStatsResponse(
    val response: List<ApiTeamStatsItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiTeamStatsItem(
    val team: ApiTeamInfo,
    val statistics: List<ApiStatValue> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiStatValue(
    val type: String, // e.g. "Shots on Goal", "Possession", "Fouls", "Yellow Cards", "Red Cards"
    val value: Any? // moshi supports parsing to String/Double/Boolean/null
)

// Lineups Responses
@JsonClass(generateAdapter = true)
data class ApiLineupsResponse(
    val response: List<ApiLineupItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiLineupItem(
    val team: ApiTeamInfo,
    val coach: ApiCoachInfo?,
    val formation: String?,
    val startXI: List<ApiPlayerItem> = emptyList(),
    val substitutes: List<ApiPlayerItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiCoachInfo(
    val name: String?
)

@JsonClass(generateAdapter = true)
data class ApiPlayerItem(
    val player: ApiPlayerDetails
)

@JsonClass(generateAdapter = true)
data class ApiPlayerDetails(
    val id: Int?,
    val name: String,
    val number: Int?,
    val pos: String? // "G", "D", "M", "F"
)

// Events Responses
@JsonClass(generateAdapter = true)
data class ApiEventsResponse(
    val response: List<ApiEventItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiEventItem(
    val time: ApiEventTime,
    val team: ApiTeamInfo,
    val player: ApiEventPlayer,
    val assist: ApiEventPlayer?,
    val type: String, // "Goal", "Card", "subst"
    val detail: String? // "Yellow Card", "Normal Goal", "Own Goal"
)

@JsonClass(generateAdapter = true)
data class ApiEventTime(
    val elapsed: Int,
    val extra: Int? = null
)

@JsonClass(generateAdapter = true)
data class ApiEventPlayer(
    val id: Int?,
    val name: String?
)

// Standings Responses
@JsonClass(generateAdapter = true)
data class ApiStandingsResponse(
    val response: List<ApiStandingsItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiStandingsItem(
    val league: ApiLeagueStandings
)

@JsonClass(generateAdapter = true)
data class ApiLeagueStandings(
    val id: Int,
    val name: String,
    val standings: List<List<ApiStandingRow>> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiStandingRow(
    val rank: Int,
    val team: ApiTeamInfo,
    val group: String, // "Group A"
    val points: Int,
    val goalsDiff: Int,
    val all: ApiStandingStats
)

@JsonClass(generateAdapter = true)
data class ApiStandingStats(
    val played: Int,
    val win: Int,
    val draw: Int,
    val lose: Int,
    val goals: ApiStandingGoals
)

@JsonClass(generateAdapter = true)
data class ApiStandingGoals(
    @Json(name = "for") val goalsFor: Int,
    val against: Int
)
