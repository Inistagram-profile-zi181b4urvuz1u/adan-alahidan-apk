package com.adan.alahidan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.util.Calendar
import kotlin.concurrent.thread
import kotlin.math.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    // Colors - Light Parchment
    private val bgColor = Color.parseColor("#F5EFE0")
    private val cardColor = Color.parseColor("#FFFBF0")
    private val borderColor = Color.parseColor("#E3D8BC")
    private val greenColor = Color.parseColor("#17382C")
    private val goldColor = Color.parseColor("#E8C46E")
    private val goldDarkColor = Color.parseColor("#B8872B")
    private val mutedColor = Color.parseColor("#6B6451")

    private lateinit var contentContainer: FrameLayout
    private var currentTab = 0 // 0=Home, 1=Qibla, 2=Mushaf, 3=Adhkar, 4=Settings

    private lateinit var currentCity: MoroccanCity
    private lateinit var currentDay: PrayerTimesDay

    // Qibla sensor
    private var sensorManager: SensorManager? = null
    private var qiblaNeedleView: TextView? = null
    private var qiblaAngleText: TextView? = null
    private var lastAzimuth = 0f

    // Audio player for Mushaf & VIP
    private var mediaPlayer: MediaPlayer? = null
    private var playingTitle: String = ""
    private var isAudioPlaying = false

    // Timer for countdown
    private val handler = Handler(Looper.getMainLooper())
    private var countdownText: TextView? = null
    private var nextPrayerText: TextView? = null

    private val vipList = listOf(
        "I6hIWAYwx-M" to "تلاوة مختارة 1 — الشيخ محمد اللحيدان",
        "_lAWpqQbRT8" to "تلاوة مختارة 2 — الشيخ محمد اللحيدان",
        "GVnkAzaDMu0" to "تلاوة مختارة 3 — الشيخ محمد اللحيدان",
        "dpVVRWtJ5Sk" to "تلاوة مختارة 4 — الشيخ محمد اللحيدان",
        "hs5h_uZ_dL0" to "تلاوة مختارة 5 — الشيخ محمد اللحيدان",
        "WJ1wH6znrT4" to "تلاوة مختارة 6 — الشيخ محمد اللحيدان",
        "1L_nSASB67Q" to "تلاوة مختارة 7 — الشيخ محمد اللحيدان",
        "-vrIKDL5RVU" to "تلاوة مختارة 8 — الشيخ محمد اللحيدان",
        "te9NgfMFipE" to "تلاوة مختارة 9 — الشيخ محمد اللحيدان",
        "TtzCpIgpM0g" to "تلاوة مختارة 10 — الشيخ محمد اللحيدان"
    )

    private val surahNames = listOf(
        "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس",
        "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه",
        "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم",
        "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر",
        "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق",
        "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة",
        "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج",
        "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس",
        "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد",
        "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات",
        "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر",
        "المسد", "الإخلاص", "الفلق", "الناس"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        window.statusBarColor = greenColor

        currentCity = PrayerRepository.getSelectedCity(this)
        currentDay = PrayerRepository.getCachedOrFallback(this, currentCity)
        PrayerRepository.scheduleAlarms(this, currentDay)

        requestPermissionsIfNeeded()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Top Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(greenColor)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleTv = TextView(this).apply {
            text = "أذان ومصحف اللحيدان"
            setTextColor(goldColor)
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val cityBtn = TextView(this).apply {
            text = "📍 ${currentCity.nameAr}"
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = roundedBg(Color.parseColor("#244E3D"), 20)
            setOnClickListener { showCityPicker() }
        }
        header.addView(titleTv)
        header.addView(cityBtn)
        root.addView(header)

        // Content Area
        contentContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(contentContainer)

        // Bottom Navigation (5 Tabs)
        val navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(cardColor)
            setPadding(0, dp(8), 0, dp(10))
            elevation = dp(8).toFloat()
        }
        val tabs = listOf(
            "🏠\nالرئيسية",
            "🧭\nالقبلة",
            "🎧\nالمصحف",
            "📿\nالأذكار",
            "⚙️\nالإعدادات"
        )
        tabs.forEachIndexed { index, label ->
            val btn = TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 11f
                setTextColor(if (index == currentTab) greenColor else mutedColor)
                setTypeface(null, if (index == currentTab) Typeface.BOLD else Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    currentTab = index
                    for (i in 0 until navBar.childCount) {
                        val tv = navBar.getChildAt(i) as TextView
                        tv.setTextColor(if (i == currentTab) greenColor else mutedColor)
                        tv.setTypeface(null, if (i == currentTab) Typeface.BOLD else Typeface.NORMAL)
                    }
                    renderTab()
                }
            }
            navBar.addView(btn)
        }
        root.addView(navBar)

        setContentView(root)
        renderTab()

        // Fetch fresh Habous times in background
        thread {
            val fresh = PrayerRepository.fetchAndCacheToday(this, currentCity)
            currentDay = fresh
            PrayerRepository.scheduleAlarms(this, fresh)
            runOnUiThread {
                if (currentTab == 0) renderTab()
            }
        }

        // Start 1s countdown loop
        handler.post(object : Runnable {
            override fun run() {
                updateCountdown()
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun renderTab() {
        contentContainer.removeAllViews()
        when (currentTab) {
            0 -> renderHome()
            1 -> renderQibla()
            2 -> renderMushaf()
            3 -> renderAdhkar()
            4 -> renderSettings()
        }
    }

    private fun renderHome() {
        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(24))
        }

        // Hero Card
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(greenColor, 20)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val heroRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val sheikhImg = ImageView(this).apply {
            setImageResource(R.drawable.sheikh_luhaidan)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64)).apply {
                marginEnd = dp(14)
            }
            background = roundedBg(goldColor, 32)
            clipToOutline = true
        }
        val heroInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        nextPrayerText = TextView(this).apply {
            text = "الصلاة القادمة"
            setTextColor(goldColor)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
        }
        countdownText = TextView(this).apply {
            text = "--:--:--"
            setTextColor(Color.WHITE)
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
        }
        val dateTv = TextView(this).apply {
            text = "${currentDay.hijriDate}  •  ${currentDay.gregorianDate}"
            setTextColor(Color.parseColor("#D1DFD8"))
            textSize = 12f
        }
        heroInfo.addView(nextPrayerText)
        heroInfo.addView(countdownText)
        heroInfo.addView(dateTv)
        heroRow.addView(sheikhImg)
        heroRow.addView(heroInfo)
        hero.addView(heroRow)
        col.addView(hero)

        // Prayer Times List
        val listCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedCardBg()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
        }

        val prayers = listOf(
            Triple("fajr", "الفجر", currentDay.fajr),
            Triple("sunrise", "الشروق", currentDay.sunrise),
            Triple("dhuhr", "الظهر", currentDay.dhuhr),
            Triple("asr", "العصر", currentDay.asr),
            Triple("maghrib", "المغرب", currentDay.maghrib),
            Triple("isha", "العشاء", currentDay.isha)
        )

        prayers.forEachIndexed { i, (key, name, time) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }
            val nameTv = TextView(this).apply {
                text = name
                setTextColor(greenColor)
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val timeTv = TextView(this).apply {
                text = time
                setTextColor(greenColor)
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 0, dp(12), 0)
            }
            row.addView(nameTv)
            row.addView(timeTv)

            if (key != "sunrise") {
                var enabled = PrayerRepository.isPrayerEnabled(this, key)
                val bell = TextView(this).apply {
                    text = if (enabled) "🔔" else "🔕"
                    textSize = 18f
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        enabled = !enabled
                        PrayerRepository.setPrayerEnabled(this@MainActivity, key, enabled)
                        PrayerRepository.scheduleAlarms(this@MainActivity, currentDay)
                        text = if (enabled) "🔔" else "🔕"
                        Toast.makeText(
                            this@MainActivity,
                            if (enabled) "تم تفعيل أذان $name" else "تم كتم أذان $name",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                row.addView(bell)
            }

            listCard.addView(row)
            if (i < prayers.size - 1) {
                listCard.addView(View(this).apply {
                    setBackgroundColor(borderColor)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                })
            }
        }
        col.addView(listCard)

        // Test Adhan Button
        val testBtn = Button(this).apply {
            text = "🔊 تجربة صوت الأذان الآن"
            setTextColor(Color.WHITE)
            setBackgroundColor(greenColor)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
            setOnClickListener {
                val intent = Intent(this@MainActivity, AdhanService::class.java).apply {
                    putExtra("prayer_name", "تجربة")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
        col.addView(testBtn)

        scroll.addView(col)
        contentContainer.addView(scroll)
        updateCountdown()
    }

    private fun updateCountdown() {
        val nextTv = nextPrayerText ?: return
        val cdTv = countdownText ?: return
        val prayers = listOf(
            "الفجر" to currentDay.fajr,
            "الظهر" to currentDay.dhuhr,
            "العصر" to currentDay.asr,
            "المغرب" to currentDay.maghrib,
            "العشاء" to currentDay.isha
        )
        val now = Calendar.getInstance()
        val nowSec = now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND)

        var chosenName = "الفجر"
        var diffSec = -1

        for ((name, t) in prayers) {
            val p = t.split(":")
            if (p.size == 2) {
                val sec = p[0].toInt() * 3600 + p[1].toInt() * 60
                if (sec > nowSec) {
                    chosenName = name
                    diffSec = sec - nowSec
                    break
                }
            }
        }
        if (diffSec < 0) {
            val p = prayers[0].second.split(":")
            val sec = p[0].toInt() * 3600 + p[1].toInt() * 60
            diffSec = (24 * 3600 - nowSec) + sec
        }

        val h = diffSec / 3600
        val m = (diffSec % 3600) / 60
        val s = diffSec % 60
        nextTv.text = "الصلاة القادمة: $chosenName"
        cdTv.text = String.format("%02d:%02d:%02d", h, m, s)
    }

    private fun renderQibla() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        val title = TextView(this).apply {
            text = "اتجاه القبلة — ${currentCity.nameAr}"
            setTextColor(greenColor)
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
        }
        val qiblaBearing = calculateQiblaBearing(currentCity.lat, currentCity.lon)
        qiblaAngleText = TextView(this).apply {
            text = "زاوية القبلة: ${qiblaBearing.toInt()}°"
            setTextColor(goldDarkColor)
            textSize = 16f
            setPadding(0, dp(8), 0, dp(24))
        }
        qiblaNeedleView = TextView(this).apply {
            text = "🕋\n⬆️"
            textSize = 54f
            gravity = Gravity.CENTER
            setPadding(dp(30), dp(30), dp(30), dp(30))
            background = roundedCardBg()
        }
        val hint = TextView(this).apply {
            text = "ضع الهاتف بشكل أفقي بعيداً عن المعادن"
            setTextColor(mutedColor)
            textSize = 13f
            setPadding(0, dp(20), 0, 0)
        }
        col.addView(title)
        col.addView(qiblaAngleText)
        col.addView(qiblaNeedleView)
        col.addView(hint)
        contentContainer.addView(col)
    }

    private fun calculateQiblaBearing(lat: Double, lon: Double): Double {
        val kaabaLat = Math.toRadians(21.4225)
        val kaabaLon = Math.toRadians(39.8262)
        val userLat = Math.toRadians(lat)
        val dLon = kaabaLon - Math.toRadians(lon)
        val y = sin(dLon) * cos(kaabaLat)
        val x = cos(userLat) * sin(kaabaLat) - sin(userLat) * cos(kaabaLat) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (currentTab != 1 || event == null) return
        if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
            val azimuth = event.values[0]
            lastAzimuth = lastAzimuth + 0.15f * (azimuth - lastAzimuth)
            val bearing = calculateQiblaBearing(currentCity.lat, currentCity.lon).toFloat()
            qiblaNeedleView?.rotation = bearing - lastAzimuth
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        @Suppress("DEPRECATION")
        sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    private fun renderMushaf() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        var subTab = 0 // 0=VIP, 1=114 Surahs, 2=Read
        val subBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
        }
        val subContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        // Player bar with SeekBar
        val playerBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(greenColor, 14)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
        val playerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val nowPlayingTv = TextView(this).apply {
            text = if (playingTitle.isEmpty()) "اختر تلاوة أو سورة" else playingTitle
            setTextColor(Color.WHITE)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val timeTv = TextView(this).apply {
            text = "00:00 / 00:00"
            setTextColor(goldColor)
            textSize = 11f
            setPadding(dp(8), 0, dp(8), 0)
        }
        val playPauseBtn = TextView(this).apply {
            text = if (isAudioPlaying) "⏸️" else "▶️"
            textSize = 20f
            setPadding(dp(6), dp(4), dp(4), dp(4))
            setOnClickListener {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        mp.pause()
                        isAudioPlaying = false
                        text = "▶️"
                    } else {
                        mp.start()
                        isAudioPlaying = true
                        text = "⏸️"
                    }
                }
            }
        }
        playerBar.addView(nowPlayingTv)
        playerBar.addView(timeTv)
        playerBar.addView(playPauseBtn)

        val seekBar = SeekBar(this).apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        mediaPlayer?.seekTo(progress)
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        playerBox.addView(playerBar)
        playerBox.addView(seekBar)

        handler.post(object : Runnable {
            override fun run() {
                if (currentTab == 2) {
                    mediaPlayer?.let { mp ->
                        try {
                            if (mp.duration > 0) {
                                seekBar.max = mp.duration
                                seekBar.progress = mp.currentPosition
                                val curS = mp.currentPosition / 1000
                                val totS = mp.duration / 1000
                                timeTv.text = String.format("%02d:%02d / %02d:%02d", curS / 60, curS % 60, totS / 60, totS % 60)
                            }
                        } catch (_: Exception) {}
                    }
                    handler.postDelayed(this, 500)
                }
            }
        })

        fun renderSub() {
            subContainer.removeAllViews()
            val scroll = ScrollView(this)
            val list = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedCardBg()
            }

            if (subTab == 0) {
                vipList.forEachIndexed { i, (id, title) ->
                    val row = TextView(this).apply {
                        text = "★ ${i + 1}. $title  (أوفلاين ✓)"
                        setTextColor(greenColor)
                        textSize = 14f
                        setTypeface(null, Typeface.BOLD)
                        setPadding(dp(14), dp(14), dp(14), dp(14))
                        setOnClickListener {
                            playAssetAudio("vip/$id.m4a", title, nowPlayingTv, playPauseBtn)
                        }
                    }
                    list.addView(row)
                    list.addView(divider())
                }
            } else if (subTab == 1) {
                surahNames.forEachIndexed { i, name ->
                    val num = i + 1
                    val numStr = num.toString().padStart(3, '0')
                    val url = "https://server8.mp3quran.net/lhdan/$numStr.mp3"
                    val localFile = File(filesDir, "surah_$numStr.mp3")
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(14), dp(12), dp(14), dp(12))
                    }
                    val tv = TextView(this).apply {
                        text = "$num. سورة $name"
                        setTextColor(greenColor)
                        textSize = 15f
                        setTypeface(null, Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        setOnClickListener {
                            val src = if (localFile.exists()) localFile.absolutePath else url
                            playUrlOrFile(src, "سورة $name", nowPlayingTv, playPauseBtn)
                        }
                    }
                    val dl = TextView(this).apply {
                        text = if (localFile.exists()) "✅" else "⬇️"
                        textSize = 16f
                        setPadding(dp(8), dp(4), dp(8), dp(4))
                        setOnClickListener {
                            if (!localFile.exists()) {
                                text = "⏳"
                                thread {
                                    try {
                                        URL(url).openStream().use { input ->
                                            localFile.outputStream().use { output -> input.copyTo(output) }
                                        }
                                        runOnUiThread { text = "✅" }
                                    } catch (_: Exception) {
                                        runOnUiThread { text = "⬇️" }
                                    }
                                }
                            }
                        }
                    }
                    row.addView(tv)
                    row.addView(dl)
                    list.addView(row)
                    list.addView(divider())
                }
            } else {
                val picker = Spinner(this).apply {
                    adapter = ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        surahNames.mapIndexed { i, n -> "${i + 1}. سورة $n" }
                    )
                }
                val ayahTv = TextView(this).apply {
                    setTextColor(greenColor)
                    textSize = 20f
                    setPadding(dp(14), dp(14), dp(14), dp(14))
                    setLineSpacing(0f, 1.5f)
                }
                picker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                        val surahNum = (pos + 1).toString()
                        thread {
                            val jsonStr = assets.open("quran_uthmani.json").bufferedReader().readText()
                            val arr = JSONObject(jsonStr).getJSONArray(surahNum)
                            val sb = StringBuilder()
                            for (j in 0 until arr.length()) {
                                val obj = arr.getJSONObject(j)
                                sb.append(obj.getString("t")).append(" ﴿").append(obj.getInt("n")).append("﴾ ")
                            }
                            runOnUiThread { ayahTv.text = sb.toString() }
                        }
                    }
                    override fun onNothingSelected(p: AdapterView<*>?) {}
                }
                list.addView(picker)
                list.addView(ayahTv)
            }

            scroll.addView(list)
            subContainer.addView(scroll)
        }

        listOf("★ VIP (10)", "المصحف (114)", "القراءة").forEachIndexed { idx, label ->
            val b = TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(dp(6), dp(10), dp(6), dp(10))
                setTextColor(if (idx == 0) Color.WHITE else greenColor)
                background = roundedBg(if (idx == 0) greenColor else cardColor, 18)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(4)
                }
                setOnClickListener {
                    subTab = idx
                    for (k in 0 until subBar.childCount) {
                        val tv = subBar.getChildAt(k) as TextView
                        tv.setTextColor(if (k == subTab) Color.WHITE else greenColor)
                        tv.background = roundedBg(if (k == subTab) greenColor else cardColor, 18)
                    }
                    renderSub()
                }
            }
            subBar.addView(b)
        }

        col.addView(subBar)
        col.addView(playerBox)
        col.addView(subContainer)
        contentContainer.addView(col)
        renderSub()
    }

    private fun playAssetAudio(assetPath: String, title: String, tv: TextView, btn: TextView) {
        try {
            mediaPlayer?.release()
            val afd = assets.openFd(assetPath)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                start()
            }
            playingTitle = title
            isAudioPlaying = true
            tv.text = title
            btn.text = "⏸️"
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر التشغيل: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playUrlOrFile(src: String, title: String, tv: TextView, btn: TextView) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(src)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    isAudioPlaying = true
                    btn.text = "⏸️"
                }
            }
            playingTitle = title
            tv.text = title
            btn.text = "⏳"
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر التشغيل", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderAdhkar() {
        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(24))
        }
        var count = 0
        val tasbihCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = roundedBg(greenColor, 20)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        val countTv = TextView(this).apply {
            text = "0"
            setTextColor(goldColor)
            textSize = 40f
            setTypeface(null, Typeface.BOLD)
        }
        val tapBtn = Button(this).apply {
            text = "سبحان الله وبحمده (اضغط للتسبيح)"
            setTextColor(greenColor)
            setBackgroundColor(goldColor)
            setOnClickListener {
                count++
                countTv.text = count.toString()
            }
        }
        tasbihCard.addView(countTv)
        tasbihCard.addView(tapBtn)
        col.addView(tasbihCard)

        val adhkar = listOf(
            "أصبحنا وأصبح الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له.",
            "اللهم بك أصبحنا، وبك أمسينا، وبك نحيا، وبك نموت، وإليك النشور.",
            "حسبي الله لا إله إلا هو عليه توكلت وهو رب العرش العظيم (7 مرات).",
            "قال رسول الله ﷺ: «من صلى عليّ صلاة صلى الله عليه بها عشراً» (رواه مسلم)."
        )
        adhkar.forEach { textStr ->
            val tv = TextView(this).apply {
                text = textStr
                setTextColor(greenColor)
                textSize = 15f
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = roundedCardBg()
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
            }
            col.addView(tv)
        }
        scroll.addView(col)
        contentContainer.addView(scroll)
    }

    private fun renderSettings() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val cityCard = TextView(this).apply {
            text = "📍 المدينة الحالية: ${currentCity.nameAr} (اضغط للتغيير)"
            setTextColor(greenColor)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedCardBg()
            setOnClickListener { showCityPicker() }
        }
        col.addView(cityCard)
        contentContainer.addView(col)
    }

    private fun showCityPicker() {
        val names = PrayerRepository.CITIES.map { it.nameAr }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("اختر المدينة")
            .setItems(names) { _, which ->
                currentCity = PrayerRepository.CITIES[which]
                PrayerRepository.setSelectedCity(this, currentCity)
                currentDay = PrayerRepository.getCachedOrFallback(this, currentCity)
                renderTab()
                thread {
                    val fresh = PrayerRepository.fetchAndCacheToday(this, currentCity)
                    currentDay = fresh
                    PrayerRepository.scheduleAlarms(this, fresh)
                    runOnUiThread { renderTab() }
                }
            }
            .show()
    }

    private fun requestPermissionsIfNeeded() {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun roundedBg(color: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun roundedCardBg(): GradientDrawable = GradientDrawable().apply {
        setColor(cardColor)
        cornerRadius = dp(18).toFloat()
        setStroke(2, borderColor)
    }

    private fun divider(): View = View(this).apply {
        setBackgroundColor(borderColor)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}
