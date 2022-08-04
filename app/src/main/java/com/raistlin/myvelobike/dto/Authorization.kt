package com.raistlin.myvelobike.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class LoginData(val login: String, val pin: String)

@Serializable
data class Authorization(
    @SerialName("access_token")
    val token: String,
    @SerialName("refresh_token")
    val refreshToken: String
)

@Serializable
data class ProfileResponse(
    @SerialName("Profile")
    val profile: Profile
)

@Serializable
data class Profile(
    @SerialName("Balance")
    val balance: Int
)