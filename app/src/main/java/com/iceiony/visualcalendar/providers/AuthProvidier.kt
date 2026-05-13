package com.iceiony.visualcalendar.providers

interface AuthProvidier {

    suspend fun requestDeviceCode(): AuthProvidier.DeviceCodeInfo
    data class DeviceCodeInfo(
        val deviceCode: String,
        val userCode: String,
        val verificationUrl: String,
        val intervalSeconds: Int,
    )

}
