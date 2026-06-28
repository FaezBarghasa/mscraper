package com.example.db

import androidx.room.*
import com.example.model.FavoriteTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteTrackDao {
    @Query("SELECT * FROM favorite_tracks ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteTrack>>

    @Query("SELECT * FROM favorite_tracks ORDER BY timestamp DESC")
    suspend fun getFavoritesList(): List<FavoriteTrack>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(track: FavoriteTrack)

    @Delete
    suspend fun deleteFavorite(track: FavoriteTrack)

    @Query("DELETE FROM favorite_tracks WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)

    @Query("DELETE FROM favorite_tracks")
    suspend fun clearAllFavorites()

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE id = :id)")
    fun isFavorite(id: String): Flow<Boolean>
}
