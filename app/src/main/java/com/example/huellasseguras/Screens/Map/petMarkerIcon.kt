package com.example.huellasseguras.Screens.Map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberPetMarkerIcon(
    context: Context,
    photoUrl: String?,
    circleSize: Int =150,        // px
    borderColor: Int = Color.parseColor("#FF6B35"),
    anchorColor: Int = Color.parseColor("#FF6B35"),
    borderWidth: Float = 6f,
    stemHeight: Int = 30,
    stemWidth: Float = 5f,
    anchorRadius: Float = 15f,
): BitmapDescriptor? {
    var descriptor by remember(photoUrl) { mutableStateOf<BitmapDescriptor?>(null) }

    LaunchedEffect(photoUrl) {
        withContext(Dispatchers.IO) {
            try {
                // Descargar imagen con Coil
                val request = ImageRequest.Builder(context)
                    .data("$photoUrl?t=${System.currentTimeMillis()}")
                    .size(circleSize, circleSize)
                    .allowHardware(false)
                    .build()
                val result = ImageLoader(context).execute(request)
                val photoBitmap = (result as? SuccessResult)?.drawable?.toBitmap(
                    circleSize, circleSize, Bitmap.Config.ARGB_8888
                ) ?: return@withContext

                // Crear canvas total
                val totalHeight = circleSize + stemHeight + (anchorRadius * 2).toInt()
                val output = Bitmap.createBitmap(circleSize, totalHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)

                val cx = circleSize / 2f
                val cy = circleSize / 2f
                // Dibujar foto recortada en círculo (centrada y escalada)
                val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val shader = BitmapShader(
                    photoBitmap,
                    Shader.TileMode.CLAMP,
                    Shader.TileMode.CLAMP
                )
                // Escalar y centrar el bitmap dentro del círculo (tipo ContentScale.Crop)
                val radioUtil = circleSize / 2f - borderWidth
                val scale = (radioUtil * 2) / minOf(photoBitmap.width, photoBitmap.height).toFloat()
                val offsetX = (radioUtil * 2 - photoBitmap.width * scale) / 2f
                val offsetY = (radioUtil * 2 - photoBitmap.height * scale) / 2f

                val matrix = Matrix()
                matrix.setScale(scale, scale)
                matrix.postTranslate(offsetX + borderWidth, offsetY + borderWidth)
                shader.setLocalMatrix(matrix)
                photoPaint.shader = shader
                canvas.drawCircle(cx, cy, radioUtil, photoPaint)

                //Dibujar borde del círculo
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = borderColor
                    style = Paint.Style.STROKE
                    strokeWidth = borderWidth
                }
                canvas.drawCircle(cx, cy, circleSize / 2f - borderWidth / 2, borderPaint)

                // Dibujar tallo
                val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = anchorColor
                    style = Paint.Style.FILL
                }
                val stemLeft = cx - stemWidth / 2
                canvas.drawRect(
                    stemLeft,
                    circleSize.toFloat(),
                    stemLeft + stemWidth,
                    circleSize + stemHeight.toFloat(),
                    stemPaint
                )

                // Dibujar punto ancla
                canvas.drawCircle(
                    cx,
                    circleSize + stemHeight + anchorRadius,
                    anchorRadius,
                    stemPaint
                )

                descriptor = BitmapDescriptorFactory.fromBitmap(output)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    return descriptor
}