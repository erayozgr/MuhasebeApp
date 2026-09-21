package com.eray.muhasebeapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppConfigDto(
    @SerialName("id")
    val id: Long? = null,

    @SerialName("currentVersion")
    val currentVersion: String,

    @SerialName("minSupportedVersion")
    val minSupportedVersion: String,

    @SerialName("maintenanceMode")
    val isMaintenanceMode: Boolean,

    @SerialName("maintenanceMessage")
    val maintenanceMessage: String? = null
)