package com.example.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class PipedSearchResponse(
    val items: List<PipedSearchItem>
)

data class PipedSearchItem(
    val url: String,
    val title: String,
    val thumbnail: String,
    val uploaderName: String?,
    val uploaderUrl: String?,
    val uploadedDate: String?,
    val views: Long?,
    val duration: Long?, // in seconds
    val isShort: Boolean?
)

data class PipedStreamResponse(
    val title: String,
    val description: String,
    val uploader: String,
    val thumbnailUrl: String,
    val audioStreams: List<PipedAudioStream>,
    val duration: Long
)

data class PipedAudioStream(
    val url: String,
    val format: String,
    val quality: String,
    val mimeType: String,
    val bitrate: Long
)

data class PipedPlaylistResponse(
    val name: String,
    val thumbnailUrl: String,
    val description: String?,
    val uploader: String,
    val uploaderUrl: String?,
    val videos: Int,
    val relatedItems: List<PipedSearchItem>
)

data class PipedChannelResponse(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val subscriberCount: Long,
    val description: String?,
    val relatedPlaylists: List<PipedRelatedPlaylist>
)

data class PipedRelatedPlaylist(
    val title: String,
    val playlistId: String
)

interface PipedApiService {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("filter") filter: String = "music_songs"
    ): PipedSearchResponse

    @GET("streams/{videoId}")
    suspend fun getStreams(
        @Path("videoId") videoId: String
    ): PipedStreamResponse

    @GET("suggestions")
    suspend fun getSuggestions(
        @Query("query") query: String
    ): List<String>

    @GET("playlists/{playlistId}")
    suspend fun getPlaylist(
        @Path("playlistId") playlistId: String
    ): PipedPlaylistResponse

    @GET("channel/{channelId}")
    suspend fun getChannel(
        @Path("channelId") channelId: String
    ): PipedChannelResponse

    @GET("trending")
    suspend fun getTrending(
        @Query("region") region: String = "US"
    ): List<PipedSearchItem>
}

