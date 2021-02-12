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

@file:Suppress("MemberVisibilityCanBePrivate")

package alan20210202.icontweaks

/**
 * The main activity.
 */

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var listIconConfigs: RecyclerView
    private lateinit var iconConfigs: MutableList<IconConfig>
    private lateinit var iconConfigAdapter: IconConfigAdapter
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        prefs = applicationContext.createDeviceProtectedStorageContext()
            .getSharedPreferences("icon_configs", Context.MODE_PRIVATE)

        Toast.makeText(applicationContext, "Xposed: ${isXposedModuleEnabled()}", Toast.LENGTH_SHORT)
            .show()

        val linearLayoutManager = LinearLayoutManager(this)
        iconConfigs = getIconConfigs()
        iconConfigAdapter = IconConfigAdapter(iconConfigs)

        listIconConfigs = findViewById(R.id.icon_config_recycler)
        listIconConfigs.layoutManager = linearLayoutManager
        listIconConfigs.adapter = iconConfigAdapter
    }

    @SuppressLint("ApplySharedPref")
    private fun getIconConfigs(): MutableList<IconConfig> {
        val ret = arrayListOf<IconConfig>()
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        for (ri in packageManager.queryIntentActivities(intent, 0)) {
            val packageName = ri.activityInfo.packageName
            val icon = ri.loadIcon(packageManager)
            if (icon is BitmapDrawable) {
                val res = packageManager.getResourcesForApplication(packageName)
                val defaultJSON = IconConfig(
                    packageName,
                    res.getResourceTypeName(ri.iconResource),
                    res.getResourceEntryName(ri.iconResource)
                ).toJSON()
                val exists = prefs.contains(packageName)
                ret.add(
                    IconConfig.fromJSON(prefs.getString(packageName, defaultJSON)!!)
                )
                if (!exists)
                    prefs.edit().putString(packageName, defaultJSON).apply()
            }
        }
        return ret
    }

    @SuppressLint("ApplySharedPref")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val position = data!!.getIntExtra("position", 0)
            val json = data.getStringExtra("config")!!
            val newConfig = IconConfig.fromJSON(json)
            iconConfigs[position] = newConfig
            iconConfigAdapter.notifyItemChanged(position)
            prefs.edit().putString(newConfig.packageName, json).apply()
        }
    }

    inner class IconConfigAdapter(private val configs: List<IconConfig>) :
        RecyclerView.Adapter<IconConfigAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textPackageName: TextView = itemView.findViewById(R.id.text_package_name)
            val imageAdaptive: ImageView = itemView.findViewById(R.id.image_adaptive)
            val imageOriginal: ImageView = itemView.findViewById(R.id.image_original)
            val textConfigDesc: TextView = itemView.findViewById(R.id.text_config_desc)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val context = parent.context
            val inflater = LayoutInflater.from(context)
            val appIconView = inflater.inflate(R.layout.item_icon_config, parent, false)
            return ViewHolder(appIconView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val iconConfig = configs[position]
            holder.textPackageName.text = iconConfig.packageName
            val icon = iconConfig.getRawIcon(packageManager)
            holder.imageOriginal.setImageDrawable(icon)
            if (iconConfig.enabled) {
                holder.imageAdaptive.setImageDrawable(
                    makeBitmapAdaptive(icon as BitmapDrawable, iconConfig, resources)
                )
                holder.textConfigDesc.text = getString(
                    R.string.text_config_desc,
                    iconConfig.cropRadius, iconConfig.scale
                )
            } else {
                holder.imageAdaptive.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_disabled_72,
                        null
                    )
                )
                holder.textConfigDesc.text = getString(R.string.text_config_disabled)
            }
            holder.itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, IconEditorActivity::class.java)
                intent.putExtra("config", iconConfig.toJSON())
                intent.putExtra("position", position)
                this@MainActivity.startActivityForResult(intent, 1)
            }
        }

        override fun getItemCount(): Int = configs.size
    }

    fun isXposedModuleEnabled() = false
}

