package com.rmd.realstate.model

data class Filter(
    var property_type: String = "",
    var min_property_size: Int =0,
    var max_property_size: Int =0,
    var min_property_price: Int =0,
    var max_property_price: Int =0,
    var number_bedrooms: Int =0,
    var has_bath_place: Boolean = false,
    var has_dinner_room: Boolean = false,
    var has_garage: Boolean = false,
    var has_balcony: Boolean = false,
    var has_bedroom_baby: Boolean = false,
    var has_tv_room: Boolean = false,
    var property_region: String = "",
    var property_city: String = "",
)
