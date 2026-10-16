package com.harish.gallery.core.domain.edit

/**
 * Colour changes as 4x5 colour matrices (20 numbers, row by row) — the same layout Android's
 * ColorMatrix and Compose's ColorMatrix use, so the preview and the saved photo match exactly.
 *
 *   R' = a*R + b*G + c*B + d*A + e      (one row per channel: R, G, B, A)
 *
 * Kept as plain FloatArrays here so the maths can be unit-tested without a phone.
 */
object ColorMatrices {

    fun identity(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    /** amount -1..1: darker .. brighter (adds up to ±80 to every channel). */
    fun brightness(amount: Float): FloatArray {
        val shift = 80f * amount
        return floatArrayOf(
            1f, 0f, 0f, 0f, shift,
            0f, 1f, 0f, 0f, shift,
            0f, 0f, 1f, 0f, shift,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** amount -1..1: flatter .. punchier (stretches colours around mid-grey). */
    fun contrast(amount: Float): FloatArray {
        val scale = 1f + amount
        val shift = 128f * (1f - scale)
        return floatArrayOf(
            scale, 0f, 0f, 0f, shift,
            0f, scale, 0f, 0f, shift,
            0f, 0f, scale, 0f, shift,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** amount -1..1: black & white .. extra colourful. */
    fun saturation(amount: Float): FloatArray {
        val s = 1f + amount
        // How bright each colour looks to the eye (sRGB luminance)
        val r = 0.2126f * (1 - s)
        val g = 0.7152f * (1 - s)
        val b = 0.0722f * (1 - s)
        return floatArrayOf(
            r + s, g, b, 0f, 0f,
            r, g + s, b, 0f, 0f,
            r, g, b + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** amount -1..1: cooler (bluer) .. warmer (more orange). */
    fun warmth(amount: Float): FloatArray {
        val shift = 30f * amount
        return floatArrayOf(
            1f, 0f, 0f, 0f, shift,
            0f, 1f, 0f, 0f, shift * 0.3f,
            0f, 0f, 1f, 0f, -shift,
            0f, 0f, 0f, 1f, 0f
        )
    }

    fun sepia(): FloatArray = floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    /** "Apply [second] after [first]" as one matrix. */
    fun concat(first: FloatArray, second: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (row in 0 until 4) {
            for (col in 0 until 5) {
                var sum = 0f
                for (k in 0 until 4) sum += second[row * 5 + k] * first[k * 5 + col]
                if (col == 4) sum += second[row * 5 + 4]
                out[row * 5 + col] = sum
            }
        }
        return out
    }

    /** Runs a matrix on one colour (0..255 each) — used by the tests and handy for debugging. */
    fun apply(matrix: FloatArray, r: Float, g: Float, b: Float, a: Float = 255f): FloatArray {
        val input = floatArrayOf(r, g, b, a)
        return FloatArray(4) { row ->
            var sum = matrix[row * 5 + 4]
            for (k in 0 until 4) sum += matrix[row * 5 + k] * input[k]
            sum.coerceIn(0f, 255f)
        }
    }
}

/** Ready-made looks, like the filters row in Google Photos. */
enum class PhotoFilter(val label: String) {
    NONE("Original"),
    VIVID("Vivid"),
    WARM("Warm"),
    COOL("Cool"),
    MONO("Mono"),
    NOIR("Noir"),
    SEPIA("Sepia"),
    FADE("Fade");

    fun matrix(): FloatArray = when (this) {
        NONE -> ColorMatrices.identity()
        VIVID -> ColorMatrices.concat(ColorMatrices.saturation(0.35f), ColorMatrices.contrast(0.12f))
        WARM -> ColorMatrices.warmth(0.6f)
        COOL -> ColorMatrices.warmth(-0.6f)
        MONO -> ColorMatrices.saturation(-1f)
        NOIR -> ColorMatrices.concat(ColorMatrices.saturation(-1f), ColorMatrices.contrast(0.35f))
        SEPIA -> ColorMatrices.sepia()
        FADE -> ColorMatrices.concat(ColorMatrices.contrast(-0.25f), ColorMatrices.brightness(0.12f))
    }
}

/** The Adjust sliders, each -1..1 (0 = unchanged). */
data class Adjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f
) {
    val isNeutral: Boolean get() = brightness == 0f && contrast == 0f && saturation == 0f && warmth == 0f

    fun matrix(): FloatArray = listOf(
        ColorMatrices.brightness(brightness),
        ColorMatrices.contrast(contrast),
        ColorMatrices.saturation(saturation),
        ColorMatrices.warmth(warmth)
    ).reduce(ColorMatrices::concat)
}

/** Filter first, then your slider tweaks on top. */
fun editColorMatrix(filter: PhotoFilter, adjustments: Adjustments): FloatArray =
    ColorMatrices.concat(filter.matrix(), adjustments.matrix())
