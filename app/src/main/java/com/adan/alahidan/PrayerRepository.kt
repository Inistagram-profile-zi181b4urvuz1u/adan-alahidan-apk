package com.adan.alahidan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MoroccanCity(val id: String, val nameAr: String, val nameEn: String, val lat: Double, val lon: Double)

data class PrayerTimesDay(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val hijriDate: String,
    val gregorianDate: String
)

object PrayerRepository {
    val CITIES = listOf(
        MoroccanCity("casablanca", "الدار البيضاء", "Casablanca", 33.5731, -7.5898),
        MoroccanCity("rabat", "الرباط", "Rabat", 34.0209, -6.8416),
        MoroccanCity("marrakech", "مراكش", "Marrakech", 31.6295, -7.9811),
        MoroccanCity("fes", "فاس", "Fes", 34.0181, -5.0078),
        MoroccanCity("tangier", "طنجة", "Tangier", 35.7595, -5.8340),
        MoroccanCity("agadir", "أكادير", "Agadir", 30.4278, -9.5981),
        MoroccanCity("meknes", "مكناس", "Meknes", 33.8935, -5.5473),
        MoroccanCity("oujda", "وجدة", "Oujda", 34.6814, -1.9086),
        MoroccanCity("kenitra", "القنيطرة", "Kenitra", 34.2610, -6.5802),
        MoroccanCity("tetouan", "تطوان", "Tetouan", 35.5889, -5.3626),
        MoroccanCity("safi", "آسفي", "Safi", 32.2994, -9.2372),
        MoroccanCity("el_jadida", "الجديدة", "El Jadida", 33.2316, -8.5007),
        MoroccanCity("beni_mellal", "بني ملال", "Beni Mellal", 32.3373, -6.3498),
        MoroccanCity("nador", "الناظور", "Nador", 35.1681, -2.9335),
        MoroccanCity("settat", "سطات", "Settat", 33.0010, -7.6166),
        MoroccanCity("laayoune", "العيون", "Laayoune", 27.1253, -13.1625),
        MoroccanCity("dakhla", "الداخلة", "Dakhla", 23.6848, -15.9570)
    )

    fun getSelectedCity(context: Context): MoroccanCity {
        val prefs = context.getSharedPreferences("adan_prefs", Context.MODE_PRIVATE)
        val id = prefs.getString("city_id", "casablanca") ?: "casablanca"
        return CITIES.find { it.id == id } ?: CITIES[0]
    }

    fun setSelectedCity(context: Context, city: MoroccanCity) {
        context.getSharedPreferences("adan_prefs", Context.MODE_PRIVATE)
            .edit().putString("city_id", city.id).apply()
    }

    fun isPrayerEnabled(context: Context, key: String): Boolean {
        return context.getSharedPreferences("adan_prefs", Context.MODE_PRIVATE)
            .getBoolean("bell_$key", true)
    }

    fun setPrayerEnabled(context: Context, key: String, enabled: Boolean) {
        context.getSharedPreferences("adan_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("bell_$key", enabled).apply()
    }

    fun getCachedOrFallback(context: Context, city: MoroccanCity): PrayerTimesDay {
        val prefs = context.getSharedPreferences("adan_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("cached_${city.id}", null)
        if (jsonStr != null) {
            try {
                val obj = JSONObject(jsonStr)
                return PrayerTimesDay(
                    fajr = obj.getString("fajr"),
                    sunrise = obj.getString("sunrise"),
                    dhuhr = obj.getString("dhuhr"),
                    asr = obj.getString("asr"),
                    maghrib = obj.getString("maghrib"),
                    isha = obj.getString("isha"),
                    hijriDate = obj.optString("hijri", ""),
                    gregorianDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
                )
            } catch (_: Exception) {}
        }
        return PrayerTimesDay(
            fajr = "05:35",
            sunrise = "07:02",
            dhuhr = "13:22",
            asr = "16:28",
            maghrib = "18:52",
            isha = "20:10",
            hijriDate = "المغرب • الحبوس",
            gregorianDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        )
    }

    fun fetchAndCacheToday(context: Context, city: MoroccanCity): PrayerTimesDay {
        try {
            val url = URL("https://api.aladhan.com/v1/timings?latitude=${city.lat}&longitude=${city.lon}&method=21")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            val text = conn.inputStream.bufferedReader().readText()
            val data = JSONObject(text).getJSONObject("data")
            val timings = data.getJSONObject("timings")
            val hijri = data.getJSONObject("date").getJSONObject("hijri")
            val hijriStr = "${hijri.getString("day")} ${hijri.getJSONObject("month").getString("ar")} ${hijri.getString("year")}"

            val clean = { s: String -> s.substring(0, 5) }
            val day = PrayerTimesDay(
                fajr = clean(timings.getString("Fajr")),
                sunrise = clean(timings.getString("Sunrise")),
                dhuhr = clean(timings.getString("Dhuhr")),
                asr = clean(timings.getString("Asr")),
                maghrib = clean(timings.getString("Maghrib")),
                isha = clean(timings.getString("Isha")),
                hijriDate = hijriStr,
                gregorianDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
            )

            val saveObj = JSONObject().apply {
                put("fajr", day.fajr)
                put("sunrise", day.sunrise)
                put("dhuhr", day.dhuhr)
                put("asr", day.asr)
                put("maghrib", day.maghrib)
                put("isha", day.isha)
                put("hijri", day.hijriDate)
            }
            context.getSharedPreferences("adan_prefs", Context.MODE_PRIVATE)
                .edit().putString("cached_${city.id}", saveObj.toString()).apply()

            return day
        } catch (_: Exception) {
            return getCachedOrFallback(context, city)
        }
    }

    fun scheduleAlarms(context: Context, day: PrayerTimesDay) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prayers = listOf(
            Triple("fajr", "الفجر", day.fajr),
            Triple("dhuhr", "الظهر", day.dhuhr),
            Triple("asr", "العصر", day.asr),
            Triple("maghrib", "المغرب", day.maghrib),
            Triple("isha", "العشاء", day.isha)
        )

        val now = System.currentTimeMillis()
        var idx = 100
        for ((key, nameAr, timeStr) in prayers) {
            if (!isPrayerEnabled(context, key)) continue
            val parts = timeStr.split(":")
            if (parts.size != 2) continue
            val h = parts[0].toIntOrNull() ?: continue
            val m = parts[1].toIntOrNull() ?: continue

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val intent = Intent(context, AdhanReceiver::class.java).apply {
                putExtra("prayer_name", nameAr)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                idx++,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val showIntent = Intent(context, MainActivity::class.java)
                    val showPending = PendingIntent.getActivity(
                        context, 0, showIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(cal.timeInMillis, showPending),
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                }
            } catch (_: Exception) {}
        }
    }
}
