package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
    private const val PIPED_BASE_URL = "http://127.0.0.1:8080/"
    private const val SPONSORBLOCK_BASE_URL = "https://sponsor.ajay.app/api/"
    private const val RYD_BASE_URL = "https://returnyoutubedislikeapi.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val pipedRetrofit = Retrofit.Builder()
        .baseUrl(PIPED_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val sponsorBlockRetrofit = Retrofit.Builder()
        .baseUrl(SPONSORBLOCK_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val rydRetrofit = Retrofit.Builder()
        .baseUrl(RYD_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val pipedApi: PipedApiService = pipedRetrofit.create(PipedApiService::class.java)
    val sponsorBlockApi: SponsorBlockApi = sponsorBlockRetrofit.create(SponsorBlockApi::class.java)
    val rydApi: ReturnYouTubeDislikeApi = rydRetrofit.create(ReturnYouTubeDislikeApi::class.java)
    
    suspend fun searchMusic(query: String): List<PipedSearchItem> {
        val response = pipedApi.search(query, "music_songs")
        return response.items
    }
    
    suspend fun getStreamUrl(videoId: String): String? {
        val streams = pipedApi.getStreams(videoId)
        // Prefer m4a or webm audio
        val bestAudio = streams.audioStreams.maxByOrNull { it.bitrate }
        return bestAudio?.url
    }

    suspend fun getSuggestions(query: String): List<String> {
        return try {
            pipedApi.getSuggestions(query)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPlaylist(playlistId: String): PipedPlaylistResponse {
        return pipedApi.getPlaylist(playlistId)
    }

    suspend fun getChannel(channelId: String): PipedChannelResponse {
        return pipedApi.getChannel(channelId)
    }

    suspend fun getTrending(): List<PipedSearchItem> {
        return try {
            pipedApi.getTrending()
        } catch (e: Exception) {
            emptyList()
        }
    }

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

