package com.example.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AmbientPalette(
    val dominant: Color = Color(0xFFFA2D48),
    val vibrant: Color = Color(0xFF1DB954),
    val muted: Color = Color(0xFF14151B)
)

@Composable
fun rememberAmbientPalette(
    imageUrl: String?,
    defaultPrimary: Color = Color(0xFFFA2D48),
    defaultSecondary: Color = Color(0xFF1DB954)
): AmbientPalette {
    val context = LocalContext.current
    val paletteState = remember(imageUrl) {
        mutableStateOf(AmbientPalette(dominant = defaultPrimary, vibrant = defaultSecondary))
    }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .size(128)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        val palette = Palette.from(bitmap).generate()
                        val domInt = palette.getDominantColor(defaultPrimary.hashCode())
                        val vibInt = palette.getVibrantColor(palette.getMutedColor(defaultSecondary.hashCode()))
                        val mutInt = palette.getDarkMutedColor(0xFF14151B.toInt())
                        
                        withContext(Dispatchers.Main) {
                            paletteState.value = AmbientPalette(
                                dominant = Color(domInt),
                                vibrant = Color(vibInt),
                                muted = Color(mutInt)
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback to default palette gracefully
            }
        }
    }

    return paletteState.value
}
