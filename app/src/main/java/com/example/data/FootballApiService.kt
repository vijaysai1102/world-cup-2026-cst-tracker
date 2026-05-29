package com.example.data

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface FootballApiService {

    @GET("v3/fixtures")
    suspend fun getFixtures(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("league") leagueId: Int = 1, // Placeholder league ID for World Cup
        @Query("season") season: Int = 2026
    ): ApiFootballResponse

    @GET("v3/fixtures")
    suspend fun getLiveFixtures(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("league") leagueId: Int = 1,
        @Query("live") live: String = "all"
    ): ApiFootballResponse

    @GET("v3/fixtures/statistics")
    suspend fun getMatchStatistics(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("fixture") fixtureId: Int
    ): ApiStatsResponse

    @GET("v3/fixtures/lineups")
    suspend fun getMatchLineups(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("fixture") fixtureId: Int
    ): ApiLineupsResponse

    @GET("v3/fixtures/events")
    suspend fun getMatchEvents(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("fixture") fixtureId: Int
    ): ApiEventsResponse

    @GET("v3/standings")
    suspend fun getStandings(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("league") leagueId: Int = 1,
        @Query("season") season: Int = 2026
    ): ApiStandingsResponse
}
