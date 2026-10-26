package com.morado.topwaylauncher.data

/**
 * Color ambiental a partir de una carátula: el tono dominante, ajustado para teñir el fondo
 * sin deslumbrar ni restar contraste al texto. Kotlin puro (colores ARGB en Int) para poder
 * probarlo sin Android.
 */
object AmbientColor {

    private const val HUE_BUCKETS = 12
    private const val MIN_SATURATION = 0.18f
    private const val MIN_VALUE = 0.18f

    // Rango al que se lleva el color: vivo pero contenido, nunca neón ni casi negro.
    private const val TONE_MIN_SATURATION = 0.35f
    private const val TONE_MAX_SATURATION = 0.70f
    private const val TONE_MIN_VALUE = 0.50f
    private const val TONE_MAX_VALUE = 0.75f

    /**
     * Tono dominante de [pixels]: se agrupan por matiz, pesando más los colores vivos, y se
     * promedia el grupo más fuerte. null si la imagen es gris, negra o blanca (no hay un
     * color que valga la pena: el fondo se queda con su halo de siempre).
     */
    fun dominant(pixels: IntArray): Int? {
        val weight = FloatArray(HUE_BUCKETS)
        val red = FloatArray(HUE_BUCKETS)
        val green = FloatArray(HUE_BUCKETS)
        val blue = FloatArray(HUE_BUCKETS)
        for (pixel in pixels) {
            if (alpha(pixel) < 128) continue
            val (h, s, v) = toHsv(pixel)
            if (s < MIN_SATURATION || v < MIN_VALUE) continue
            val bucket = (h / 360f * HUE_BUCKETS).toInt().coerceIn(0, HUE_BUCKETS - 1)
            val w = s * v
            weight[bucket] += w
            red[bucket] += red(pixel) * w
            green[bucket] += green(pixel) * w
            blue[bucket] += blue(pixel) * w
        }
        val best = weight.indices.maxBy { weight[it] }
        val total = weight[best]
        // Menos de ~3 % de la imagen con color: se considera sin color.
        if (total < pixels.size * 0.03f) return null
        return rgb((red[best] / total).toInt(), (green[best] / total).toInt(), (blue[best] / total).toInt())
    }

    /** Lleva [color] a un rango de saturación y brillo apto para teñir el fondo. */
    fun tone(color: Int): Int {
        val (h, s, v) = toHsv(color)
        return fromHsv(
            h,
            s.coerceIn(TONE_MIN_SATURATION, TONE_MAX_SATURATION),
            v.coerceIn(TONE_MIN_VALUE, TONE_MAX_VALUE),
        )
    }

    private fun alpha(c: Int) = (c ushr 24) and 0xFF
    private fun red(c: Int) = (c shr 16) and 0xFF
    private fun green(c: Int) = (c shr 8) and 0xFF
    private fun blue(c: Int) = c and 0xFF
    private fun rgb(r: Int, g: Int, b: Int) =
        (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    /** Matiz en grados [0, 360), saturación y brillo en [0, 1]. */
    private fun toHsv(c: Int): Triple<Float, Float, Float> {
        val r = red(c) / 255f
        val g = green(c) / 255f
        val b = blue(c) / 255f
        val max = maxOf(r, g, b)
        val delta = max - minOf(r, g, b)
        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta).mod(6f))
            max == g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }
        val saturation = if (max == 0f) 0f else delta / max
        return Triple(hue, saturation, max)
    }

    private fun fromHsv(h: Float, s: Float, v: Float): Int {
        val c = v * s
        val x = c * (1 - kotlin.math.abs((h / 60f).mod(2f) - 1))
        val m = v - c
        val (r, g, b) = when ((h / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return rgb(((r + m) * 255).toInt(), ((g + m) * 255).toInt(), ((b + m) * 255).toInt())
    }
}
