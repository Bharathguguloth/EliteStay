package com.example.elitestay.model

data class Property(
    val name: String,
    val location: String, // could also be LatLng
    val price: String,
    val imageUrl: String // optional for now
)
