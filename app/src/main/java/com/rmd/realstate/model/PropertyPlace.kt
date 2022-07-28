package com.rmd.realstate.model

data class PropertyPlace(
    var placeId: String = "",
    var placeName: String = "",
    var placeAddress: String = "",
    var placeLat: Double = 0.0,
    var placeLng: Double = 0.0
)
