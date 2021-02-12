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
 * Configuration on how to make a bitmap icon adaptive.
 * Too lazy to download GSON, since this is the only class I need to (de)serialize I am quite happy
 * with using stock JSON library from android.
 */

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import org.json.JSONObject

data class IconConfig(
    val packageName: String,
    val resType: String,
    val resEntry: String,
    var resId: Int = 0,
    var enabled: Boolean = false,
    var cropRadius: Int = 36,
    var scale: Float = 1f,
    var unicolor: Boolean = false,
    var backgroundColor: Int = Color.WHITE
) {
    companion object {
        fun fromJSON(json: String): IconConfig {
            JSONObject(json).apply {
                return IconConfig(
                    getString("packageName"),
                    getString("resType"),
                    getString("resEntry"),
                    if (has("resId")) getInt("resId") else 0,
                    if (has("enabled")) getBoolean("enabled") else false,
                    if (has("cropRadius")) getInt("cropRadius") else 36,
                    if (has("scale")) getDouble("scale").toFloat() else 0f,
                    if (has("unicolor")) getBoolean("unicolor") else false,
                    if (has("backgroundColor")) getInt("backgroundColor") else Color.WHITE
                )
            }
        }
    }

    fun toJSON(): String {
        JSONObject().apply {
            put("packageName", packageName)
            put("resType", resType)
            put("resEntry", resEntry)
            put("resId", resId)
            put("enabled", enabled)
            put("cropRadius", cropRadius)
            put("scale", scale)
            put("unicolor", unicolor)
            put("backgroundColor", backgroundColor)
            return toString()
        }
    }

    private fun updateResourceId(packageManager: PackageManager) {
        val res = packageManager.getResourcesForApplication(packageName)
        resId = res.getIdentifier(resEntry, resType, packageName)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun getRawIcon(packageManager: PackageManager): Drawable {
        updateResourceId(packageManager)
        return packageManager
            .getResourcesForApplication(packageName)
            .getDrawable(resId, null)
    }
}