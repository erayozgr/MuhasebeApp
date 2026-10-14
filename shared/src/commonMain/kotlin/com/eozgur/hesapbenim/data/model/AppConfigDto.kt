package com.eozgur.hesapbenim.data.model

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
    val maintenanceMessage: String? = null,

    @SerialName("usdTryRate")
    val usdTryRate: Double? = null,

    @SerialName("eurTryRate")
    val eurTryRate: Double? = null,

    @SerialName("exchangeRateDate")
    val exchangeRateDate: String? = null
)
