/**
 * The MIT License (MIT)
 *
 * Copyright 2020 Chengyuan Ma
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRIN-
 * GEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package alan20210202.icontweaks

/**
 * All the image processing code.
 * The code is rather ugly from my point of view, but it should work.
 */

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import kotlin.math.*

fun padBitmapToAdaptiveSize(bitmap: Bitmap): Bitmap {
    if (BuildConfig.DEBUG && bitmap.width != bitmap.height)
        error("The icon must have equal width and height!")
    val dpi = bitmap.density
    val size = 108 * dpi / 160
    val ret = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(ret)
    canvas.drawARGB(0, 0, 0, 0)
    canvas.drawBitmap(bitmap, (size - bitmap.width) / 2f, (size - bitmap.height) / 2f, null)
    return ret
}

fun cropAndScaleBitmap(
    bitmapDrawable: BitmapDrawable,
    cropRadiusDp: Int,
    scale: Float
): Bitmap {
    if (BuildConfig.DEBUG && bitmapDrawable.intrinsicWidth != bitmapDrawable.intrinsicHeight)
        error("The icon must have equal width and height!")
    val bitmap = bitmapDrawable.bitmap
    val dpi = bitmap.density
    val sizeScaled = (bitmap.width * scale).roundToInt()
    val cropRadius = min(
        cropRadiusDp * scale * dpi / 160,
        sizeScaled * sqrt(0.5f)
    ).roundToInt()
    val cropped = Bitmap.createBitmap(sizeScaled, sizeScaled, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(cropped)
    canvas.drawARGB(0, 0, 0, 0)
    val paint = Paint()
    paint.color = 0xFF000000.toInt()
    canvas.drawCircle(sizeScaled / 2f, sizeScaled / 2f, cropRadius.toFloat(), paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, null, Rect(0, 0, sizeScaled, sizeScaled), paint)
    paint.xfermode = null
    return cropped
}

fun makeBitmapAdaptiveByUnicolorBackground(
    bitmapDrawable: BitmapDrawable,
    cropRadiusDp: Int,
    scale: Float,
    color: Int,
    resources: Resources
): AdaptiveIconDrawable {
    if (BuildConfig.DEBUG && bitmapDrawable.intrinsicWidth != bitmapDrawable.intrinsicHeight)
        error("The icon must have equal width and height!")
    val bitmap = bitmapDrawable.bitmap
    val dpi = bitmap.density
    val size = 108 * dpi / 160
    val cropped = cropAndScaleBitmap(bitmapDrawable, cropRadiusDp, scale)
    val sizeScaled = cropped.width

    val foreground = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    var canvas = Canvas(foreground)

    canvas.drawColor(color)
    val offset = (size / 2f - sizeScaled / 2f).roundToInt()
    canvas.drawBitmap(
        cropped,
        null,
        Rect(offset, offset, offset + sizeScaled, offset + sizeScaled),
        null
    )

    val background = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    canvas = Canvas(background)
    canvas.drawColor(Color.WHITE)

    val backgroundDrawable = BitmapDrawable(resources, background)
    val foregroundDrawable = BitmapDrawable(resources, foreground)
    return AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable)
}

fun makeBitmapAdaptiveByRadialExtrapolation(
    bitmapDrawable: BitmapDrawable,
    cropRadiusDp: Int,
    scale: Float,
    resources: Resources
): AdaptiveIconDrawable {
    if (BuildConfig.DEBUG && bitmapDrawable.intrinsicWidth != bitmapDrawable.intrinsicHeight)
        error("The icon must have equal width and height!")
    val bitmap = bitmapDrawable.bitmap
    val dpi = bitmap.density
    val size = 108 * dpi / 160
    val cropped = cropAndScaleBitmap(bitmapDrawable, cropRadiusDp, scale)
    val sizeScaled = cropped.width
    val cropRadius = min(
        cropRadiusDp * scale * dpi / 160,
        sizeScaled * sqrt(0.5f)
    ).roundToInt()

    val paint = Paint()
    val foreground = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    var canvas = Canvas(foreground)

    var xEnd = 0
    var yEnd = 0
    val halfSize = size / 2f
    val halfSizeScaled = sizeScaled / 2f
    val offset = halfSize - halfSizeScaled
    paint.strokeWidth = sqrt(2f)
    for (step in 0 until 4 * size - 4) { // Basically going around the edge of the foreground bitmap
        val alpha = atan2(yEnd - size / 2f, xEnd - size / 2f)
        var color = 0
        var radius = 0
        for (r in cropRadius downTo 0) {
            val x = (halfSizeScaled + cos(alpha) * r).roundToInt()
            val y = (halfSizeScaled + sin(alpha) * r).roundToInt()
            if (x in 0 until sizeScaled && y in 0 until sizeScaled) {
                val colorHere = cropped.getPixel(x, y)
                if (Color.alpha(colorHere) == 255 || r == 0) {
                    color = colorHere
                    radius = r - 1
                    break
                }
            }
        }
        paint.color = color
        canvas.drawLine(
            xEnd.toFloat(), yEnd.toFloat(),
            (halfSizeScaled + cos(alpha) * radius) + offset,
            (halfSizeScaled + sin(alpha) * radius) + offset, paint
        )

        if (xEnd < size - 1 && yEnd == 0) xEnd++
        else if (xEnd == size - 1 && yEnd < size - 1) yEnd++
        else if (xEnd > 0 && yEnd == size - 1) xEnd--
        else yEnd--
    }
    canvas.drawBitmap(
        cropped,
        null,
        RectF(offset, offset, offset + sizeScaled, offset + sizeScaled),
        null
    )
    val background = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    canvas = Canvas(background)
    canvas.drawARGB(0, 0, 0, 0)

    val backgroundDrawable = BitmapDrawable(resources, background)
    val foregroundDrawable = BitmapDrawable(resources, foreground)
    return AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable)
}

fun makeBitmapAdaptive(
    bitmapDrawable: BitmapDrawable,
    config: IconConfig,
    resources: Resources
): AdaptiveIconDrawable =
    if (config.unicolor)
        makeBitmapAdaptiveByUnicolorBackground(
            bitmapDrawable,
            config.cropRadius,
            config.scale,
            config.backgroundColor,
            resources
        ) else makeBitmapAdaptiveByRadialExtrapolation(
        bitmapDrawable,
        config.cropRadius,
        config.scale,
        resources
    )
