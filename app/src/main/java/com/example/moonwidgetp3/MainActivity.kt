package com.example.moonwidgetp3

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var bgColor          = Color.parseColor("#0D0D1A")
    private var showFullMoon     = true
    private var showPhaseName    = true
    private var showIllumination = true
    private var darkHour         = false
    private var updatingFromSlider = false
    private var updatingFromHex    = false

    private lateinit var previewCard: View
    private lateinit var previewMoon: ImageView
    private lateinit var previewPhaseName: TextView
    private lateinit var previewIllumination: TextView
    private lateinit var previewFullMoon: TextView

    private lateinit var previewCardFull: View
    private lateinit var previewMoonFull: ImageView
    private lateinit var previewPhaseNameFull: TextView
    private lateinit var previewIlluminationFull: TextView
    private lateinit var previewFullMoonFull: TextView

    private lateinit var colorSwatch: View
    private lateinit var etHex: EditText
    private lateinit var seekR: SeekBar
    private lateinit var seekG: SeekBar
    private lateinit var seekB: SeekBar

    private val presets = listOf(
        "#303030", "#0D0D1A", "#1A0D2E", "#0A1628", "#000000"
    )
    private val presetIds = listOf(
        R.id.preset_1, R.id.preset_2, R.id.preset_3, R.id.preset_4, R.id.preset_5
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("moon_widget_prefs", Context.MODE_PRIVATE)
        bgColor          = Color.parseColor(prefs.getString("bg_color", "#0D0D1A") ?: "#0D0D1A")
        showFullMoon     = prefs.getBoolean("show_full_moon", true)
        showPhaseName    = prefs.getBoolean("show_phase_name", true)
        showIllumination = prefs.getBoolean("show_illumination", true)
        darkHour         = prefs.getBoolean("dark_hour", false)

        // Обычное превью
        previewCard         = findViewById(R.id.preview_card)
        previewMoon         = findViewById(R.id.preview_moon)
        previewPhaseName    = findViewById(R.id.preview_phase_name)
        previewIllumination = findViewById(R.id.preview_illumination)
        previewFullMoon     = findViewById(R.id.preview_full_moon)

        // Превью полнолуния
        previewCardFull         = findViewById(R.id.preview_card_full)
        previewMoonFull         = findViewById(R.id.preview_moon_full)
        previewPhaseNameFull    = findViewById(R.id.preview_phase_name2)
        previewIlluminationFull = findViewById(R.id.preview_illumination2)
        previewFullMoonFull     = findViewById(R.id.preview_full_moon2)

        colorSwatch = findViewById(R.id.color_preview_swatch)
        etHex       = findViewById(R.id.et_hex)
        seekR       = findViewById(R.id.seek_r)
        seekG       = findViewById(R.id.seek_g)
        seekB       = findViewById(R.id.seek_b)

        seekR.progress = Color.red(bgColor)
        seekG.progress = Color.green(bgColor)
        seekB.progress = Color.blue(bgColor)

        val seekListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, fromUser: Boolean) {
                if (!fromUser || updatingFromHex) return
                bgColor = Color.rgb(seekR.progress, seekG.progress, seekB.progress)
                updatingFromSlider = true
                etHex.setText("%02X%02X%02X".format(
                    Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor)))
                etHex.setSelection(etHex.text.length)
                updatingFromSlider = false
                refreshPreview()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }
        seekR.setOnSeekBarChangeListener(seekListener)
        seekG.setOnSeekBarChangeListener(seekListener)
        seekB.setOnSeekBarChangeListener(seekListener)

        etHex.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (updatingFromSlider) return
                val hex = s?.toString() ?: return
                if (hex.length == 6) {
                    try {
                        val c = Color.parseColor("#$hex")
                        bgColor = c
                        updatingFromHex = true
                        seekR.progress = Color.red(c)
                        seekG.progress = Color.green(c)
                        seekB.progress = Color.blue(c)
                        updatingFromHex = false
                        refreshPreview()
                    } catch (_: Exception) {}
                }
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        etHex.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) { etHex.clearFocus(); true } else false
        }

        presetIds.forEachIndexed { i, id ->
            findViewById<View>(id).setOnClickListener { applyColor(presets[i]) }
        }

        val swFull     = findViewById<Switch>(R.id.switch_full_moon)
        val swPhase    = findViewById<Switch>(R.id.switch_phase_name)
        val swIllum    = findViewById<Switch>(R.id.switch_illumination)
        val swDarkHour = findViewById<Switch>(R.id.switch_dark_hour)

        swFull.isChecked     = showFullMoon
        swPhase.isChecked    = showPhaseName
        swIllum.isChecked    = showIllumination
        swDarkHour.isChecked = darkHour

        swFull.setOnCheckedChangeListener     { _, c -> showFullMoon     = c; refreshPreview() }
        swPhase.setOnCheckedChangeListener    { _, c -> showPhaseName    = c; refreshPreview() }
        swIllum.setOnCheckedChangeListener    { _, c -> showIllumination = c; refreshPreview() }
        swDarkHour.setOnCheckedChangeListener { _, c -> darkHour         = c; refreshPreview() }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val hex = "#%02X%02X%02X".format(
                Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
            getSharedPreferences("moon_widget_prefs", Context.MODE_PRIVATE).edit()
                .putString("bg_color", hex)
                .putBoolean("show_full_moon", showFullMoon)
                .putBoolean("show_phase_name", showPhaseName)
                .putBoolean("show_illumination", showIllumination)
                .putBoolean("dark_hour", darkHour)
                .apply()

            val mgr = AppWidgetManager.getInstance(this)
            val ids = mgr.getAppWidgetIds(ComponentName(this, MoonWidget::class.java))
            for (id in ids) updateAppWidget(this, mgr, id)

            Toast.makeText(this, "✓ Виджет обновлён", Toast.LENGTH_SHORT).show()
        }

        etHex.setText("%02X%02X%02X".format(
            Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor)))
        refreshPreview()
    }

    private fun applyColor(hex: String) {
        bgColor = Color.parseColor(hex)
        updatingFromHex = true
        seekR.progress = Color.red(bgColor)
        seekG.progress = Color.green(bgColor)
        seekB.progress = Color.blue(bgColor)
        updatingFromHex = false
        updatingFromSlider = true
        etHex.setText(hex.removePrefix("#").uppercase())
        etHex.setSelection(etHex.text.length)
        updatingFromSlider = false
        refreshPreview()
    }

    private fun refreshPreview() {
        val moon = getMoonData()

        // Обычное превью — текущая фаза
        previewCard.setBackgroundColor(bgColor)
        colorSwatch.setBackgroundColor(bgColor)
        previewMoon.setImageBitmap(drawMoon(moon.phase, 240, bgColor, darkHour))
        previewPhaseName.text    = moon.phaseName
        previewIllumination.text = "${moon.illumination}%"
        previewFullMoon.text     = "🌕 ${moon.nextFullMoon}"
        previewFullMoon.visibility     = if (showFullMoon)     View.VISIBLE else View.INVISIBLE
        previewPhaseName.visibility    = if (showPhaseName)    View.VISIBLE else View.INVISIBLE
        previewIllumination.visibility = if (showIllumination) View.VISIBLE else View.INVISIBLE

        // Превью полнолуния — phase = 0.5 принудительно
        previewCardFull.setBackgroundColor(bgColor)
        previewMoonFull.setImageBitmap(drawMoon(0.5, 240, bgColor, darkHour))
        previewPhaseNameFull.text    = "FULL MOON"
        previewIlluminationFull.text = "100%"
        previewFullMoonFull.text     = "🌕 ${moon.nextFullMoon}"
        previewFullMoonFull.visibility     = if (showFullMoon)     View.VISIBLE else View.INVISIBLE
        previewPhaseNameFull.visibility    = if (showPhaseName)    View.VISIBLE else View.INVISIBLE
        previewIlluminationFull.visibility = if (showIllumination) View.VISIBLE else View.INVISIBLE
    }
}