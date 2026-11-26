package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object DiscoverRoute

@Serializable
data object VideosRoute

@Serializable
data object LibraryRoute

@Serializable
data object SearchRoute

@Serializable
data object AccountRoute

@Serializable
data class PlaylistDetailRoute(
    val playlistId: Long,
    val title: String,
    val coverUrl: String = ""
)
