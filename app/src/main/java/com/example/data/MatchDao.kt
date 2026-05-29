package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Query("SELECT * FROM matches ORDER BY id ASC")
    fun getAllMatchesFlow(): Flow<List<Match>>

    @Query("SELECT * FROM matches ORDER BY id ASC")
    suspend fun getAllMatches(): List<Match>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: Int): Match?

    @Query("SELECT * FROM matches WHERE isFavorite = 1")
    fun getFavoriteMatchesFlow(): Flow<List<Match>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(matches: List<Match>)

    @Update
    suspend fun updateMatch(match: Match)

    @Query("UPDATE matches SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFav: Boolean)

    @Query("SELECT COUNT(*) FROM matches")
    suspend fun getCount(): Int
}
