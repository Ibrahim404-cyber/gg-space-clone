package com.ibrahim404.ggspace.data

data class Game(
    val packageName: String,
    val label: String,
    val isFavorite: Boolean = false,
    val lastPlayed: Long = 0L
)
