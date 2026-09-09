package com.example.model

enum class SearchSource {
    YOUTUBE,
    SOUNDCLOUD,
    SPOTIFY;

    val displayName: String
        get() = when (this) {
            YOUTUBE -> "YouTube"
            SOUNDCLOUD -> "SoundCloud"
            SPOTIFY -> "Spotify"
        }
}
