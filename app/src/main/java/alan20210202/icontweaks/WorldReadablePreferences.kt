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
 * Adapted from Gravity Box's WorldReadablePrefs.
 * Currently I think stock [SharedPreferences] should suffice, in case I want the changes to take
 * effects immediately instead of after a reboot this will come in handy.
 */

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.FileObserver
import android.util.Log
import java.io.File

class WorldReadablePreferences(private val context: Context, private val name: String) :
    SharedPreferences {

    companion object {
        lateinit var INSTANCE: WorldReadablePreferences private set

        fun initialize(context: Context) {
            var realContext = context.applicationContext ?: context
            if (!realContext.isDeviceProtectedStorage)
                realContext = realContext.createDeviceProtectedStorageContext()
            INSTANCE = WorldReadablePreferences(realContext,
                realContext.packageName + "_preferences")
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(name, 0)
    private var selfAttrChange = false
    private val fileObserver: FileObserver

    init {
        maybePreCreateFile()
        fixPermissions(true)
        fileObserver = object : FileObserver(File(context.dataDir, "shared_prefs"), ATTRIB) {
            override fun onEvent(event: Int, path: String?) {
                if ((event and ATTRIB) != 0)
                    onFileAttributesChanged(path)
            }
        }
    }

    override fun contains(key: String) = prefs.contains(key)

    override fun edit(): Editor = prefs.edit()

    override fun getAll(): Map<String, *> = prefs.all

    override fun getBoolean(key: String, defValue: Boolean) = prefs.getBoolean(key, defValue)

    override fun getFloat(key: String, defValue: Float) = prefs.getFloat(key, defValue)

    override fun getInt(key: String, defValue: Int) = prefs.getInt(key, defValue)

    override fun getLong(key: String, defValue: Long) = prefs.getLong(key, defValue)

    override fun getString(key: String, defValue: String?) = prefs.getString(key, defValue)

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        prefs.getStringSet(key, defValues)

    override fun registerOnSharedPreferenceChangeListener(
        listener: OnSharedPreferenceChangeListener
    ) = prefs.registerOnSharedPreferenceChangeListener(listener)

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: OnSharedPreferenceChangeListener
    ) = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    @SuppressLint("SetWorldReadable")
    private fun maybePreCreateFile() {
        try {
            val sharedPrefsFolder = File(context.dataDir.absolutePath + "/shared_prefs")
            if (!sharedPrefsFolder.exists()) {
                sharedPrefsFolder.mkdir()
                sharedPrefsFolder.setExecutable(true, false)
                sharedPrefsFolder.setReadable(true, false)
            }
            val file = File("${sharedPrefsFolder.absolutePath}/$name.xml")
            if (!file.exists()) {
                file.createNewFile()
                file.setReadable(true, false)
            }
        } catch (e: Exception) {
            Log.e(
                "WorldReadablePreferences",
                "Error pre-creating prefs file $name: ${e.message}"
            )
        }
    }

    @SuppressLint("SetWorldReadable")
    private fun fixPermissions(force: Boolean = false) {
        val sharedPrefsFolder = File(context.dataDir.absolutePath + "/shared_prefs")
        if (sharedPrefsFolder.exists()) {
            sharedPrefsFolder.setExecutable(true, false)
            sharedPrefsFolder.setReadable(true, false)
            Log.i("World Readable Prefs", "Current folder permission: R: ${sharedPrefsFolder.canRead()} E: ${sharedPrefsFolder.canExecute()}")
            val file = File("${sharedPrefsFolder.absolutePath}/$name.xml")
            if (file.exists()) {
                selfAttrChange = !force
                file.setReadable(true, false)
                Log.i("World Readable Prefs", "Current file permission: R: ${file.canRead()}")
            }
        }
        Log.i("World Readable Prefs", "Permission fixed")
    }

    fun onFileAttributesChanged(path: String?) {
        if (path != null && path.endsWith("$name.xml")) {
            if (selfAttrChange) {
                selfAttrChange = false
                return
            }
            fixPermissions()
        }
    }
}