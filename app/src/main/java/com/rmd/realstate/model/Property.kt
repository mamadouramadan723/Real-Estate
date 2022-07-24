package com.rmd.realstate.model

import com.google.android.libraries.places.api.model.Place

data class Property(
    var property_type: String = "",
    var property_size: Int = 0,
    var property_price: Int = 0,
    var number_bedrooms: Int = 0,
    var number_bathrooms: Int = 0,
    var has_bath_place: Boolean = false,
    var has_dinner_room: Boolean = false,
    var has_garage: Boolean = false,
    var has_balcony: Boolean = false,
    var has_bedroom_baby: Boolean = false,
    var has_tv_room: Boolean = false,
    var property_place: Place,
    var property_description: String = "",
    var property_id: String = "",
    var property_user_id: String = "",
    var image_url: ArrayList<String>  = arrayListOf()
)
