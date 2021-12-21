package com.iyxan23.slice.domain.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class CreateSessionResponse {
    @Serializable
    @SerialName("success")
    data class Success(val token: String, @SerialName("session_id") val sessionId: String) : CreateSessionResponse()

    @Serializable
    @SerialName("error")
    data class Error(val message: String) : CreateSessionResponse()
}
