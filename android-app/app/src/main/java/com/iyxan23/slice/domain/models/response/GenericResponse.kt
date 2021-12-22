package com.iyxan23.slice.domain.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class GenericResponse {
    @Serializable
    @SerialName("success")
    object Success : GenericResponse()

    @Serializable
    @SerialName("error")
    data class Error(val message: String) : GenericResponse()
}