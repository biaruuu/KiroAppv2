package com.kiro.app.ui

/** An AniList genre. [name] must match AniList's genre collection exactly (it is sent as-is). */
data class Genre(val name: String)

val BrowseGenres = listOf(
    Genre("Action"),
    Genre("Comedy"),
    Genre("Romance"),
    Genre("Fantasy"),
    Genre("Sports"),
    Genre("Sci-Fi"),
    Genre("Slice of Life"),
    Genre("Supernatural"),
    Genre("Adventure"),
    Genre("Drama"),
    Genre("Mystery"),
    Genre("Mahou Shoujo"),
)
