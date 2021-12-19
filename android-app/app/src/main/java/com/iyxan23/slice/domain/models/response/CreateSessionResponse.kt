package com.iyxan23.slice.domain.models.response

import kotlinx.serialization.Serializable

@Serializable
sealed class CreateSessionResponse {
    @Serializable
    data class Success(val token: String, val sessionId: String) : CreateSessionResponse()

    @Serializable
    data class Error(val message: String) : CreateSessionResponse()
}
