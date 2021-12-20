package com.iyxan23.slice.domain.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ConnectSessionResponse {
    @Serializable
    @SerialName("success")
    data class Success(val token: String) : ConnectSessionResponse()

    @Serializable
    @SerialName("error")
    data class Error(val message: String) : ConnectSessionResponse()
}