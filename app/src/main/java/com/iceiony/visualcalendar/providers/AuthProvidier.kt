package com.iceiony.visualcalendar.providers

import kotlinx.coroutines.flow.Flow

interface AuthProvidier {

    fun requestDeviceCode(): Flow<DeviceCodeInfo>
    data class DeviceCodeInfo(
        val deviceCode: String,
        val userCode: String,
        val verificationUrl: String,
        val intervalSeconds: Int,
        val expiresIn : Long
    )

}
