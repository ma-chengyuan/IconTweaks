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
 * The editor activity in which icon configurations are modified and previews are shown.
 */

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.flag.FlagView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.sliders.AbstractSlider
import java.util.*
import kotlin.properties.Delegates


class IconEditorActivity : AppCompatActivity() {
    private lateinit var icon: BitmapDrawable

    private lateinit var imageForeground: ImageView
    private lateinit var imageOriginal: ImageView
    private lateinit var imageScaledCropped: ImageView
    private lateinit var imageAdaptive: ImageView

    private lateinit var textCropRadius: TextView
    private lateinit var barCropRadius: SeekBar

    private lateinit var textScale: TextView
    private lateinit var barScale: SeekBar

    private lateinit var switchEnabled: Switch
    private lateinit var switchUnicolor: Switch
    private lateinit var imageColor: ImageView

    private var position by Delegates.notNull<Int>()
    private var color by Delegates.notNull<Int>()
    private lateinit var iconConfig: IconConfig
    private var enabled: Boolean
        get() = switchEnabled.isChecked
        set(value) {
            switchEnabled.isChecked = value
        }
    private var unicolor: Boolean
        get() = switchUnicolor.isChecked
        set(value) {
            switchUnicolor.isChecked = value
        }
    private var cropRadius
        get() = barCropRadius.progress
        set(value) = barCropRadius.setProgress(value, false)
    private var scale
        get() = barScale.progress / 100f
        set(value) = barScale.setProgress((value * 100).toInt(), false)

    @ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_icon_editor)
        iconConfig = IconConfig.fromJSON(intent.getStringExtra("config")!!)
        position = intent.getIntExtra("position", 0)
        supportActionBar!!.title = getString(R.string.title_icon_editor)
        icon = iconConfig.getRawIcon(packageManager) as BitmapDrawable

        imageOriginal = findViewById(R.id.image_original)
        imageScaledCropped = findViewById(R.id.image_scaled_cropped)
        imageForeground = findViewById(R.id.image_foreground)
        imageAdaptive = findViewById(R.id.image_adaptive)

        switchEnabled = findViewById(R.id.switch_enabled)
        enabled = iconConfig.enabled
        switchEnabled.setOnCheckedChangeListener { _, _ -> updateEverything() }

        textCropRadius = findViewById(R.id.text_crop_radius)
        barCropRadius = findViewById(R.id.bar_crop_radius)
        cropRadius = iconConfig.cropRadius
        barCropRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(b: SeekBar, p: Int, f: Boolean) = updateEverything()
            override fun onStartTrackingTouch(b: SeekBar) {}
            override fun onStopTrackingTouch(b: SeekBar) {}
        })

        textScale = findViewById(R.id.text_scale)
        barScale = findViewById(R.id.bar_scale)
        scale = iconConfig.scale
        barScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(b: SeekBar, p: Int, f: Boolean) = updateEverything()
            override fun onStartTrackingTouch(b: SeekBar) {}
            override fun onStopTrackingTouch(b: SeekBar) {}
        })
        color = iconConfig.backgroundColor
        switchUnicolor = findViewById(R.id.switch_unicolor)
        switchUnicolor.setOnClickListener { updateEverything() }
        unicolor = iconConfig.unicolor
        imageColor = findViewById(R.id.image_color)
        imageColor.setOnClickListener {
            val dialog = ColorPickerDialog.Builder(this)
                .setTitle(getString(R.string.title_color_picker))
                .setPositiveButton(getString(R.string.text_select), object : ColorEnvelopeListener {
                    override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                        color = envelope.color
                        updateEverything()
                    }
                })
                .setNegativeButton(getString(R.string.text_cancel),
                    DialogInterface.OnClickListener { _, _ -> return@OnClickListener })
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
            val colorPicker = dialog.colorPickerView

            val size = icon.intrinsicWidth
            val palette = Bitmap.createBitmap(6 * size, 6 * size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(palette)
            canvas.drawARGB(0xFF, 0xFF, 0xFF, 0xFF)
            val scaledUp = Bitmap.createScaledBitmap(icon.bitmap, 4 * size, 4 * size, false)
            canvas.drawBitmap(scaledUp, size.toFloat(), size.toFloat(), null)
            colorPicker.setPaletteDrawable(BitmapDrawable(resources, palette))
            colorPicker.flagView = CustomFlagView(this, R.layout.flag_color_picker)
            colorPicker.setSelectorDrawable(getDrawable(R.drawable.ic_selector)!!)
            val f = AbstractSlider::class.java.getDeclaredField("selectorDrawable")
            f.isAccessible = true
            f.set(colorPicker.alphaSlideBar, getDrawable(R.drawable.ic_selector))
            dialog.show()
        }

        updateEverything()
        imageOriginal.setImageBitmap(padBitmapToAdaptiveSize(icon.bitmap, resources))

        findViewById<Button>(R.id.button_save).setOnClickListener {
            val intent = Intent()
            intent.putExtra("position", position)
            intent.putExtra("config", iconConfig.toJSON())
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    @ExperimentalUnsignedTypes
    private fun updateEverything() {
        Log.i("Icon Editor", unicolor.toString())

        textCropRadius.text = getString(R.string.text_crop_radius, cropRadius)
        textScale.text = getString(R.string.text_scale, scale)

        iconConfig.enabled = enabled
        iconConfig.cropRadius = cropRadius
        iconConfig.scale = scale
        iconConfig.unicolor = unicolor
        iconConfig.backgroundColor = color

        barCropRadius.isEnabled = enabled
        barScale.isEnabled = enabled

        switchUnicolor.isEnabled = enabled
        switchUnicolor.text = if (unicolor) getString(
            R.string.text_unicolor,
            "#" + color.toUInt().toString(16).toUpperCase(Locale.ROOT)
        ) else getString(R.string.text_radial_ext)

        if (unicolor && enabled) {
            val gd = GradientDrawable()
            gd.setColor(color)
            gd.setStroke(2, Color.BLACK)
            imageColor.setImageDrawable(gd)
        } else
            imageColor.setImageDrawable(getDrawable(R.drawable.ic_disabled_72))


        if (enabled) {
            val adaptiveIcon = makeBitmapAdaptive(icon, iconConfig, resources)
            imageScaledCropped.setImageBitmap(
                padBitmapToAdaptiveSize(
                    cropAndScaleBitmap(icon, cropRadius, scale), resources
                )
            )
            if (unicolor)
                imageForeground.setImageDrawable(getDrawable(R.drawable.ic_disabled_108))
            else imageForeground.setImageBitmap(
                (adaptiveIcon.foreground as BitmapDrawable).bitmap
            )
            this.imageAdaptive.setImageDrawable(adaptiveIcon)
        } else {
            imageScaledCropped.setImageDrawable(
                resources.getDrawable(
                    R.drawable.ic_disabled_108, null
                )
            )
            imageForeground.setImageDrawable(
                resources.getDrawable(
                    R.drawable.ic_disabled_108, null
                )
            )
            imageAdaptive.setImageDrawable(
                resources.getDrawable(
                    R.drawable.ic_disabled_72, null
                )
            )
        }
    }

    inner class CustomFlagView(context: Context, layout: Int) : FlagView(context, layout) {
        private val colorTile: LinearLayout = findViewById(R.id.color_tile)
        private val textColorCode: TextView = findViewById(R.id.text_color_code)

        @SuppressLint("SetTextI18n")
        override fun onRefresh(colorEnvelope: ColorEnvelope) {
            colorTile.setBackgroundColor(colorEnvelope.color)
            textColorCode.text = "#${colorEnvelope.hexCode}"
        }
    }
}