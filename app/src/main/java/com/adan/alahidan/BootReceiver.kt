package com.adan.alahidan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val city = PrayerRepository.getSelectedCity(context)
            val day = PrayerRepository.getCachedOrFallback(context, city)
            PrayerRepository.scheduleAlarms(context, day)
        }
    }
}
