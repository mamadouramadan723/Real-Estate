package com.rmd.realstate.model

data class PropertyPlace(
    var placeId: String = "",
    var placeName: String = "",
    var placeAddress: String = "",
    var placeLng: LatLong? = LatLong(0.0, 0.0),
)