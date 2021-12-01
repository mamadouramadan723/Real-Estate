package com.rmd.realstate.view_model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel_Property: ViewModel() {

    var my_property = MutableLiveData<String>()

    fun set_property_id(property_id: String) {
        my_property.value = property_id
    }
}