package com.example.netflixclone.model

data class MovieDetail(
    val movie: Movie,
    val similars: List<Movie>
)
