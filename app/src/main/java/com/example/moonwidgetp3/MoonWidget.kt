package com.example.moonwidgetp3

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.*
import android.view.View
import android.widget.RemoteViews
import java.util.*
import kotlin.math.*

class MoonWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) updateAppWidget(context, appWidgetManager, id)
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val prefs     = context.getSharedPreferences("moon_widget_prefs", Context.MODE_PRIVATE)
    val bgColor   = Color.parseColor(prefs.getString("bg_color", "#0D0D1A") ?: "#0D0D1A")
    val showFull  = prefs.getBoolean("show_full_moon", true)
    val showPhase = prefs.getBoolean("show_phase_name", true)
    val showIllum = prefs.getBoolean("show_illumination", true)
    val darkHour  = prefs.getBoolean("dark_hour", false)

    val moon  = getMoonData()
    val views = RemoteViews(context.packageName, R.layout.moon_widget)

    views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)
    views.setImageViewBitmap(R.id.iv_moon, drawMoon(moon.phase, 200, bgColor, darkHour))

    views.setTextViewText(R.id.tv_illumination,   "${moon.illumination}%")
    views.setTextViewText(R.id.tv_phase_name,     moon.phaseName)
    views.setTextViewText(R.id.tv_next_full_moon, "🌕 ${moon.nextFullMoon}")

    views.setViewVisibility(R.id.tv_next_full_moon,
        if (showFull)  View.VISIBLE else View.GONE)
    views.setViewVisibility(R.id.tv_phase_name,
        if (showPhase) View.VISIBLE else View.GONE)
    views.setViewVisibility(R.id.tv_illumination,
        if (showIllum) View.VISIBLE else View.GONE)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

// ── Moon math ──────────────────────────────────────────────

data class MoonData(
    val phase: Double,
    val illumination: Int,
    val phaseName: String,
    val nextFullMoon: String
)

fun getMoonData(): MoonData {
    val now = Calendar.getInstance()
    val jd  = toJulianDay(now)

    val knownNewMoon  = 2451550.1
    val synodicPeriod = 29.53058867

    val phase        = (((jd - knownNewMoon) % synodicPeriod) / synodicPeriod + 1) % 1
    val illumination = ((1 - cos(phase * 2 * PI)) / 2 * 100).roundToInt()

    val phaseName = when {
        phase < 0.025 || phase >= 0.975 -> "NEW MOON"
        phase < 0.25  -> "WAXING CRESCENT"
        phase < 0.275 -> "FIRST QUARTER"
        phase < 0.5   -> "WAXING GIBBOUS"
        phase < 0.525 -> "FULL MOON"
        phase < 0.75  -> "WANING GIBBOUS"
        phase < 0.775 -> "LAST QUARTER"
        else          -> "WANING CRESCENT"
    }

    val daysToFull = if (phase <= 0.5) (0.5 - phase) * synodicPeriod
    else              (1.5 - phase) * synodicPeriod

    val fullCal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysToFull.roundToInt())
    }
    val months  = arrayOf("JAN","FEB","MAR","APR","MAY","JUN",
        "JUL","AUG","SEP","OCT","NOV","DEC")
    val nextFull = "${fullCal.get(Calendar.DAY_OF_MONTH)} ${months[fullCal.get(Calendar.MONTH)]}"

    return MoonData(phase, illumination, phaseName, nextFull)
}

fun toJulianDay(cal: Calendar): Double {
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH) +
            cal.get(Calendar.HOUR_OF_DAY) / 24.0 +
            cal.get(Calendar.MINUTE) / 1440.0
    val a  = (14 - m) / 12
    val yr = y + 4800 - a
    val mo = m + 12 * a - 3
    return d + (153 * mo + 2) / 5 + 365 * yr + yr / 4 - yr / 100 + yr / 400 - 32045.0
}

// ── Moon drawing ───────────────────────────────────────────

fun drawMoon(
    phase: Double,
    size: Int,
    bgColor: Int = Color.parseColor("#0D0D1A"),
    darkHour: Boolean = false
): Bitmap {
    val bmp    = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint  = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = size / 2f
    val cy = size / 2f

    val strokeW = size * 0.055f   // толщина белой обводки
    val gap     = size * 0.04f    // отступ между обводкой и луной
    val outerR  = size / 2f - 2f  // внешний радиус обводки
    val r       = outerR - strokeW - gap  // радиус самой луны

    val isFullMoon = phase in 0.46..0.54

    // Определяем цвет луны: Dark Hour → зеленый при полнолунии
    val moonColor = if (darkHour && isFullMoon)
        Color.parseColor("#00FF88")
    else
        Color.parseColor("#FFD700")

    // 1. Белая обводка (stroke)
    paint.style       = Paint.Style.STROKE
    paint.strokeWidth = strokeW
    paint.color       = if (darkHour && isFullMoon)
        Color.parseColor("#00FF88") else Color.WHITE
    canvas.drawCircle(cx, cy, outerR - strokeW / 2f, paint)

    // 2. Клип — только внутри луны
    val clip = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
    canvas.save()
    canvas.clipPath(clip)

    // 3. Фон луны
    paint.style = Paint.Style.FILL
    paint.color = bgColor
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

    // 4. Освещённая часть
    when {
        phase < 0.025 || phase >= 0.975 -> { /* new moon */ }
        phase < 0.5 -> {
            paint.color = moonColor
            canvas.drawRect(cx, cy - r, cx + r, cy + r, paint)
            val t    = phase / 0.5
            val ellW = r * abs(1f - 2f * t.toFloat())
            paint.color = if (phase < 0.25) bgColor else moonColor
            canvas.drawOval(cx - ellW, cy - r, cx + ellW, cy + r, paint)
        }
        else -> {
            paint.color = moonColor
            canvas.drawRect(cx - r, cy - r, cx, cy + r, paint)
            val t    = (phase - 0.5) / 0.5
            val ellW = r * abs(1f - 2f * t.toFloat())
            paint.color = if (phase < 0.75) moonColor else bgColor
            canvas.drawOval(cx - ellW, cy - r, cx + ellW, cy + r, paint)
        }
    }

    // 5. Dark Hour: зеленое свечение из центра поверх луны
    if (darkHour && isFullMoon) {
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val gradient  = RadialGradient(
            cx, cy, r,
            intArrayOf(
                Color.argb(180, 0, 255, 100),   // яркий центр
                Color.argb(60,  0, 200, 80),
                Color.argb(0,   0, 150, 60)      // прозрачный край
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = gradient
        canvas.drawCircle(cx, cy, r, glowPaint)
    }

    canvas.restore()
    return bmp
}