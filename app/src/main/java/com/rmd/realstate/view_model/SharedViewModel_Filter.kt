package com.rmd.realstate.view_model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rmd.realstate.model.Filter

class SharedViewModel_Filter: ViewModel()  {

    var my_filter = MutableLiveData<Filter>()

    fun apply_and_send(applied_filter: Filter) {
        my_filter.value = applied_filter
    }
}