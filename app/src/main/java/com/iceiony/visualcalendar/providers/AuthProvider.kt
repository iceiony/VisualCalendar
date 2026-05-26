package com.iceiony.visualcalendar.providers

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.Flow

fun generateQrCode(content: String, sizePx: Int): Bitmap {
    val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val bmp = createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
    for (x in 0 until sizePx)
        for (y in 0 until sizePx)
            bmp[x, y] = if (bits[x, y]) Color.BLACK else Color.WHITE
    return bmp
}
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
    ) {
        var qrContent = "${verificationUrl}?user_code=${userCode}"
        val qrBitmap:Bitmap = generateQrCode(qrContent, 400)
    }

}
