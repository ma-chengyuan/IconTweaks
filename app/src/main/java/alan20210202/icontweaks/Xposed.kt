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
 * The Xposed side of the app.
 */

import android.app.AndroidAppHelper
import android.content.Context
import android.content.res.Resources
import android.content.res.XResources
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.collection.ArrayMap
import androidx.core.content.res.ResourcesCompat
import com.crossbowffs.remotepreferences.RemotePreferences
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage


class Xposed : IXposedHookLoadPackage, IXposedHookInitPackageResources {
    data class ResourceName(val id: Int, val packageName: String)

    companion object {
        private const val MY_OWN_PACKAGE_NAME = "alan20210202.icontweaks"
        private var iconConfigs: MutableMap<String, IconConfig> = mutableMapOf()
        private val cache: ArrayMap<ResourceName, AdaptiveIconDrawable> = ArrayMap()
        private var prefsLoaded = false

        // Google didn't promise the thread safety of ArrayMap. These helper functions are here to
        // ensure it is indeed thread-safe.

        @Synchronized
        private fun addToCache(key: ResourceName, value: AdaptiveIconDrawable) {
            cache[key] = value
        }

        @Synchronized
        private fun getFromCache(key: ResourceName) = cache[key]
    }

    /**
     * Reading [RemotePreferences] directly in [handleInitPackageResources] seems to cause troubles,
     * so instead we read everything from it here and put it into a [MutableMap].
     */
    private fun reloadPrefs(context: Context) {
        val prefs = RemotePreferences(context, "alan20210202.icontweaks.preferences", "icon_configs")
        iconConfigs.clear()
        for ((key, value) in prefs.all) {
            if (value is String) {
                iconConfigs[key] = IconConfig.fromJSON(value)
            }
        }
        prefsLoaded = true
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam == null) return
        if (lpparam.packageName == MY_OWN_PACKAGE_NAME) {
            XposedHelpers.findAndHookMethod("alan20210202.icontweaks.MainActivity",
                lpparam.classLoader, "isXposedModuleEnabled", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        super.afterHookedMethod(param)
                        param.result = true
                    }
                }
            )
        } else {
            /*
            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader,
                "attach", Context::class.java, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        super.afterHookedMethod(param)
                        if (context != null || param.args[0] == null) return
                        context = param.args[0] as Context
                        context = context?.applicationContext ?: context
                        reloadPrefs()
                    }
                }
            )
            // The above hook will not triggered by com.google.android.gms
             */
            XposedHelpers.findAndHookMethod("android.content.ContextWrapper", lpparam.classLoader,
                "getApplicationContext", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        super.afterHookedMethod(param)
                        if (prefsLoaded || param.result == null) return
                        reloadPrefs(param.result as Context)
                    }
                }
            )
        }
    }

    private fun copyBitmapDrawable(
        bitmapDrawable: BitmapDrawable,
        resources: Resources
    ): BitmapDrawable {
        val bitmap = bitmapDrawable.bitmap
        val copied = bitmap.copy(bitmap.config, true)
        return BitmapDrawable(resources, copied)
    }

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam?) {
        if (AndroidAppHelper.currentPackageName() == MY_OWN_PACKAGE_NAME) return
        if (resparam?.packageName == AndroidAppHelper.currentPackageName()) return
        iconConfigs[resparam?.packageName]?.let {
            if (!it.enabled) return
            resparam?.res?.setReplacement(it.packageName, it.resType, it.resEntry,
                object : XResources.DrawableLoader() {
                    override fun newDrawable(res: XResources, id: Int): Drawable {
                        // val drawable = res.getDrawable(id, null)
                        val drawable = ResourcesCompat.getDrawable(res, id, null)!!
                        if (drawable !is BitmapDrawable) return drawable

                        val resourceName = ResourceName(id, res.packageName)
                        val cache = getFromCache(resourceName)
                        // Though we still need to copy, this is faster than re-running the
                        // algorithm again.
                        cache?.apply {
                            return AdaptiveIconDrawable(
                                copyBitmapDrawable(background as BitmapDrawable, res),
                                copyBitmapDrawable(foreground as BitmapDrawable, res)
                            )
                        }
                        val ret = makeBitmapAdaptive(drawable, it, res)
                        addToCache(resourceName, ret)
                        return ret
                    }
                }
            )
            XposedBridge.log("Resource replaced ${it.packageName} ${it.resEntry}")
        }
    }
}