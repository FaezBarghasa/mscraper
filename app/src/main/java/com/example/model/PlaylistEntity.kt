package com.example.model

data class PlaylistEntity(
    val id: String,
    val name: String,
    val description: String?,
    val isSmart: Boolean = false,
    val smartRules: String? = null, // Rules stored as structured text/JSON, e.g., "RECENT_ADDED" or "TOP_PLAYED"
    val dateCreated: Long = System.currentTimeMillis()
)
