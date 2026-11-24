package com.example.network

import com.example.core.ffi.AudioSource
import com.example.core.ffi.MmDlpApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// SponsorBlock Models
data class SponsorSegment(
    val segment: List<Double>, // [start_seconds, end_seconds]
    val category: String,
    val UUID: String
)

interface SponsorBlockApi {
    @GET("skipSegments")
    suspend fun getSkipSegments(
        @Query("videoID") videoId: String,
        @Query("categories") categories: String = "[\"sponsor\",\"intro\",\"outro\",\"selfpromo\",\"interaction\",\"preview\",\"music_offtopic\",\"filler\"]"
    ): List<SponsorSegment>
}

// ReturnYouTubeDislike Models
data class RydVotes(
    val id: String?,
    val likes: Long?,
    val dislikes: Long?,
    val rating: Double?
)

interface ReturnYouTubeDislikeApi {
    @GET("votes")
    suspend fun getVotes(
        @Query("videoId") videoId: String
    ): RydVotes
}

object MediaApiManager {
    private const val SPONSORBLOCK_BASE_URL = "https://sponsor.ajay.app/api/"
    private const val RYD_BASE_URL = "https://returnyoutubedislikeapi.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val sponsorBlockRetrofit = Retrofit.Builder()
        .baseUrl(SPONSORBLOCK_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val rydRetrofit = Retrofit.Builder()
        .baseUrl(RYD_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val sponsorBlockApi: SponsorBlockApi = sponsorBlockRetrofit.create(SponsorBlockApi::class.java)
    val rydApi: ReturnYouTubeDislikeApi = rydRetrofit.create(ReturnYouTubeDislikeApi::class.java)
    private val ffiApi = MmDlpApi()

    fun formatDuration(seconds: ULong?): String {
        if (seconds == null || seconds == 0UL) return "--:--"
        val s = seconds.toLong()
        val mins = s / 60
        val secs = s % 60
        return String.format(java.util.Locale.US, "%d:%02d", mins, secs)
    }

    suspend fun searchMusic(query: String): List<com.example.model.Track> = try {
        val results = ffiApi.search(query, AudioSource.YOU_TUBE_MUSIC)
        results.map {
            com.example.model.Track(
                id = it.trackId,
                title = it.title,
                artist = it.artist,
                duration = formatDuration(it.durationSeconds),
                imageUrl = it.albumArtUrl ?: "",
                filePath = ""
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun getStreamUrl(videoId: String): String? = try {
        ffiApi.getStreamUrl(videoId, AudioSource.YOU_TUBE_MUSIC)
    } catch (e: Exception) {
        null
    }

    suspend fun getSuggestions(query: String): List<String> = try {
        val results = ffiApi.search(query, AudioSource.YOU_TUBE_MUSIC)
        results.take(10).map { "${it.title} - ${it.artist}" }
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun getTrending(): List<com.example.model.Track> = searchMusic("Trending Music")

    suspend fun getSponsorSegments(videoId: String): List<SponsorSegment> {
        return try {
            sponsorBlockApi.getSkipSegments(videoId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getVotes(videoId: String): RydVotes? {
        return try {
            rydApi.getVotes(videoId)
        } catch (e: Exception) {
            null
        }
    }
}


