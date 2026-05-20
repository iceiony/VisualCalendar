package com.iceiony.visualcalendar.providers

import kotlinx.coroutines.flow.Flow

interface AuthProvider {

    fun requestDeviceCode(): Flow<DeviceCodeInfo>

    suspend fun getValidAccessToken(): String?
    fun isAuthorised(): Boolean

    data class DeviceCodeInfo(
        val deviceCode: String,
        val userCode: String,
        val verificationUrl: String,
        val intervalSeconds: Int,
        val expiresIn : Long,
    )

}
