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
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateAppWidget(context, appWidgetManager, id)
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val prefs     = context.getSharedPreferences("moon_widget_prefs", Context.MODE_PRIVATE)
    val bgColor   = Color.parseColor(prefs.getString("bg_color", "#0D0D1A") ?: "#0D0D1A")
    val showFull  = prefs.getBoolean("show_full_moon", true)
    val showPhase = prefs.getBoolean("show_phase_name", true)
    val showIllum = prefs.getBoolean("show_illumination", true)
    val darkHour  = prefs.getBoolean("dark_hour", false)
    val lang      = prefs.getString("language", "EN") ?: "EN"

    val moon  = getMoonData(lang)
    val views = RemoteViews(context.packageName, R.layout.moon_widget)

    views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)
    views.setImageViewBitmap(R.id.iv_moon, drawMoon(moon.phase, 220, bgColor, darkHour))
    views.setTextViewText(R.id.tv_illumination,   "${moon.illumination}%")
    views.setTextViewText(R.id.tv_phase_name,     moon.phaseName)
    views.setTextViewText(R.id.tv_next_full_moon, "🌕 ${moon.nextFullMoon}")
    views.setViewVisibility(R.id.tv_next_full_moon, if (showFull)  View.VISIBLE else View.GONE)
    views.setViewVisibility(R.id.tv_phase_name,     if (showPhase) View.VISIBLE else View.GONE)
    views.setViewVisibility(R.id.tv_illumination,   if (showIllum) View.VISIBLE else View.GONE)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

data class MoonData(val phase: Double, val illumination: Int, val phaseName: String, val nextFullMoon: String)

fun getMoonData(lang: String = "EN"): MoonData {
    val now = Calendar.getInstance()
    val jd  = toJulianDay(now)
    val phase        = (((jd - 2451550.1) % 29.53058867) / 29.53058867 + 1) % 1
    val illumination = ((1 - cos(phase * 2 * PI)) / 2 * 100).roundToInt()
    val phaseIndex   = when {
        phase < 0.025 || phase >= 0.975 -> 0
        phase < 0.25  -> 1; phase < 0.275 -> 2; phase < 0.5  -> 3
        phase < 0.525 -> 4; phase < 0.75  -> 5; phase < 0.775 -> 6
        else          -> 7
    }
    val phaseName = getPhaseNames(lang)[phaseIndex]
    val daysToFull = if (phase <= 0.5) (0.5 - phase) * 29.53058867 else (1.5 - phase) * 29.53058867
    val fullCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, daysToFull.roundToInt()) }
    val day = fullCal.get(Calendar.DAY_OF_MONTH)
    val mon = fullCal.get(Calendar.MONTH)
    val monthsEN = arrayOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC")
    val monthsRU = arrayOf("ЯНВ","ФЕВ","МАР","АПР","МАЙ","ИЮН","ИЮЛ","АВГ","СЕН","ОКТ","НОЯ","ДЕК")
    val monthsJP = arrayOf("1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月")
    val nextFull = when(lang) {
        "RU" -> "$day ${monthsRU[mon]}"
        "JP" -> "${monthsJP[mon]}${day}日"
        else -> "$day ${monthsEN[mon]}"
    }
    return MoonData(phase, illumination, phaseName, nextFull)
}

fun getPhaseNames(lang: String): Array<String> = when(lang) {
    "RU" -> arrayOf("НОВОЛУНИЕ","РАСТУЩИЙ СЕРП","ПЕРВАЯ ЧЕТВЕРТЬ","РАСТУЩАЯ ЛУНА","ПОЛНОЛУНИЕ","УБЫВАЮЩАЯ ЛУНА","ПОСЛЕДНЯЯ ЧЕТВЕРТЬ","УБЫВАЮЩИЙ СЕРП")
    "JP" -> arrayOf("新月","三日月","上弦の月","十三夜月","満月","居待月","下弦の月","有明月")
    else -> arrayOf("NEW MOON","WAXING CRESCENT","FIRST QUARTER","WAXING GIBBOUS","FULL MOON","WANING GIBBOUS","LAST QUARTER","WANING CRESCENT")
}

fun toJulianDay(cal: Calendar): Double {
    val y = cal.get(Calendar.YEAR); val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH) + cal.get(Calendar.HOUR_OF_DAY)/24.0 + cal.get(Calendar.MINUTE)/1440.0
    val a = (14-m)/12; val yr = y+4800-a; val mo = m+12*a-3
    return d + (153*mo+2)/5 + 365*yr + yr/4 - yr/100 + yr/400 - 32045.0
}

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
    val strokeW = size * 0.055f
    val gap     = size * 0.04f
    val outerR  = size / 2f - 2f
    val r       = outerR - strokeW - gap

    val isFullMoon = phase in 0.46..0.54
    val moonColor  = if (darkHour && isFullMoon) Color.parseColor("#00FF88")
    else Color.parseColor("#FFD700")

    // Белая обводка
    paint.style       = Paint.Style.STROKE
    paint.strokeWidth = strokeW
    paint.color       = Color.WHITE
    canvas.drawCircle(cx, cy, outerR - strokeW / 2f, paint)

    // Клип внутри луны
    val clip = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
    canvas.save()
    canvas.clipPath(clip)

    // Фон
    paint.style = Paint.Style.FILL
    paint.color = bgColor
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

    // Освещённая часть
    when {
        phase < 0.025 || phase >= 0.975 -> {}
        isFullMoon -> { paint.color = moonColor; canvas.drawCircle(cx, cy, r, paint) }
        phase < 0.5 -> {
            paint.color = moonColor
            canvas.drawRect(cx, cy - r, cx + r, cy + r, paint)
            val ellW = r * abs(1f - 2f * (phase / 0.5).toFloat())
            paint.color = if (phase < 0.25) bgColor else moonColor
            canvas.drawOval(cx - ellW, cy - r, cx + ellW, cy + r, paint)
        }
        else -> {
            paint.color = moonColor
            canvas.drawRect(cx - r, cy - r, cx, cy + r, paint)
            val ellW = r * abs(1f - 2f * ((phase - 0.5) / 0.5).toFloat())
            paint.color = if (phase < 0.75) moonColor else bgColor
            canvas.drawOval(cx - ellW, cy - r, cx + ellW, cy + r, paint)
        }
    }

    canvas.restore()
    return bmp
}