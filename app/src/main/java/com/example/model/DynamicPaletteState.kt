package com.example.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DynamicPaletteState(
    val containerColor: Color = Color(0xFF1A1618),
    val glowColor: Color = Color(0xFF8E3B46),
    val secondaryGlowColor: Color = Color(0xFF3E4A61),
    val onContainerColor: Color = Color(0xFFF7F2F3),
    val mutedOnContainerColor: Color = Color(0xFFC8BFC2),
    val accentColor: Color = Color(0xFFFFB3AC),
    val onAccentColor: Color = Color(0xFF38090C),
    val miniBarColor: Color = Color(0xFF282124)
)

object CoverPaletteExtractor {
    private val cache = LinkedHashMap<String, DynamicPaletteState>(24, 0.75f, true)

    suspend fun extract(
        context: Context,
        coverUrl: String?
    ): DynamicPaletteState {
        val cacheKey = coverUrl ?: "__default_cover__"
        synchronized(cache) {
            cache[cacheKey]?.let { return it }
        }

        val bitmap: Bitmap? = withContext(Dispatchers.IO) {
            if (!coverUrl.isNullOrBlank()) {
                loadBitmapFromUrl(context, coverUrl)
            } else {
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.img_default_cover_1795712994276
                )
            }
        }

        val state = if (bitmap != null) {
            withContext(Dispatchers.Default) {
                buildPaletteState(bitmap)
            }
        } else {
            DynamicPaletteState()
        }

        synchronized(cache) {
            if (cache.size > 32) {
                val eldestKey = cache.keys.firstOrNull()
                if (eldestKey != null) cache.remove(eldestKey)
            }
            cache[cacheKey] = state
        }
        return state
    }

    private suspend fun loadBitmapFromUrl(context: Context, url: String): Bitmap? {
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .size(256)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val drawable = result.drawable
                if (drawable is android.graphics.drawable.BitmapDrawable) {
                    drawable.bitmap
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun buildPaletteState(bitmap: Bitmap): DynamicPaletteState {
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()

        val dominant = palette.darkMutedSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
            ?: 0xFF1F1A1D.toInt()

        val vibrant = palette.vibrantSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: 0xFFD94A4A.toInt()

        val secondary = palette.darkVibrantSwatch?.rgb
            ?: palette.lightMutedSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: dominant

        // Deepen container color so immersive player surface feels rich and legible
        val baseDark = blendTowardDark(dominant, 0.58f)
        val miniSurface = blendTowardDark(dominant, 0.42f)
        val glow = ensureMinimumBrightness(vibrant, minLuminance = 0.16f)
        val accent = ensureAccentContrast(vibrant, baseDark)

        val onContainer = Color(0xFFF9F4F5)
        val mutedOnContainer = Color(ColorUtils.blendARGB(onContainer.toArgb(), baseDark, 0.34f))
        val onAccent = if (ColorUtils.calculateLuminance(accent) > 0.45) {
            Color(0xFF1A1314)
        } else {
            Color(0xFFFFFFFF)
        }

        return DynamicPaletteState(
            containerColor = Color(baseDark),
            glowColor = Color(glow),
            secondaryGlowColor = Color(blendTowardDark(secondary, 0.25f)),
            onContainerColor = onContainer,
            mutedOnContainerColor = mutedOnContainer,
            accentColor = Color(accent),
            onAccentColor = onAccent,
            miniBarColor = Color(miniSurface)
        )
    }

    private fun blendTowardDark(colorInt: Int, ratio: Float): Int {
        return ColorUtils.blendARGB(colorInt, 0xFF100E11.toInt(), ratio.coerceIn(0f, 1f))
    }

    private fun ensureMinimumBrightness(colorInt: Int, minLuminance: Float): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(colorInt, hsl)
        if (hsl[2] < minLuminance) {
            hsl[2] = minLuminance
        }
        hsl[1] = hsl[1].coerceIn(0.32f, 0.85f)
        return ColorUtils.HSLToColor(hsl)
    }

    private fun ensureAccentContrast(colorInt: Int, againstBg: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(colorInt, hsl)
        hsl[2] = hsl[2].coerceAtLeast(0.66f)
        hsl[1] = hsl[1].coerceIn(0.38f, 0.88f)
        var candidate = ColorUtils.HSLToColor(hsl)
        var attempts = 0
        while (ColorUtils.calculateContrast(candidate, againstBg) < 3.5 && attempts < 6) {
            hsl[2] = (hsl[2] + 0.05f).coerceAtMost(0.95f)
            candidate = ColorUtils.HSLToColor(hsl)
            attempts++
        }
        return candidate
    }
}
