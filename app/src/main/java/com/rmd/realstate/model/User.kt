package com.rmd.realstate.model

data class User(
    var userId: String = "",
    var userName: String = "",
    var userMail: String = "",
    var userImageUrl: String = "",
    var userPhoneNumber: String = "",
    var userScore: Int = 0,
    var userVotersNumber: Int = 0
)