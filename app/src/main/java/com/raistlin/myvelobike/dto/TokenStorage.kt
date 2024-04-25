package com.raistlin.myvelobike.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Authorization(val pwaToken: String, val mobileToken: String)

@Serializable
data class LoginData(
    @SerialName("user")
    val login: String,
    @SerialName("password")
    val pin: String
)

@Serializable
data class TokenStorage(
    @SerialName("token")
    val token: String,
)

@Serializable
data class MobileTokenStorage(
    @SerialName("mobile_token")
    val token: String,
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