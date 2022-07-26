package com.rmd.realstate.model

import com.google.android.libraries.places.api.model.Place

data class Property(
    var propertyId: String = "",
    var propertyType: String = "",
    var propertyDescription: String = "",
    var propertyOwnerUserId: String = "",
    var propertyOwnerPhoneNumber: String = "",
    var propertyPlace: Place? = null,
    var propertySize: Int = 0,
    var propertyPrice: Int = 0,
    var propertyScore: Int = 0,
    var propertyVotersNumber: Int = 0,
    var propertyBedroomsNumber: Int = 0,
    var propertyBathroomsNumber: Int = 0,
    var propertyHasGarage: Boolean = false,
    var propertyHasTVRoom: Boolean = false,
    var propertyHasBalcony: Boolean = false,
    var propertyHasBathPlace: Boolean = false,
    var propertyHasDiningRoom: Boolean = false,
    var propertyHasBabyBedroom: Boolean = false,
    var propertyImagesUrl: ArrayList<String> = arrayListOf()
)
