package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object MediaApiManager {
    private const val PIPED_BASE_URL = "https://pipedapi.kavin.rocks/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val pipedRetrofit = Retrofit.Builder()
        .baseUrl(PIPED_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val pipedApi: PipedApiService = pipedRetrofit.create(PipedApiService::class.java)
    
    // For now, we will map SoundCloud and Spotify to use Piped (YouTube Music) under the hood
    // as free public APIs for full streams on SC/Spotify are restricted.
    
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
}
