package com.hunterxsy.timelimiter

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class AppBlockerService : AccessibilityService() {

    private var currentPackage: String? = null
    private var trackingTimer: Timer? = null
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            if (packageName == currentPackage) return

            currentPackage = packageName
            stopTracking()

            val prefs = getSharedPreferences("time_limiter_prefs", MODE_PRIVATE)

            if (isOutsideScheduleWindow(prefs, packageName)) {
                blockApp()
                return
            }

            val limitMinutes = prefs.getInt("limit_$packageName", -1)
            if (limitMinutes > 0) {
                if (isOverLimit(prefs, packageName, limitMinutes)) {
                    blockApp()
                    return
                }
            }

            startTracking(packageName, limitMinutes)
        }
    }

    private fun startTracking(packageName: String, limitMinutes: Int) {
        trackingTimer = Timer()
        trackingTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val prefs = getSharedPreferences("time_limiter_prefs", MODE_PRIVATE)

                if (isOutsideScheduleWindow(prefs, packageName)) {
                    blockApp()
                    stopTracking()
                    return
                }

                if (limitMinutes > 0) {
                    addUsageSeconds(prefs, packageName, 5)
                    if (isOverLimit(prefs, packageName, limitMinutes)) {
                        blockApp()
                        stopTracking()
                    }
                }
            }
        }, 5000, 5000)
    }

    private fun stopTracking() {
        trackingTimer?.cancel()
        trackingTimer = null
    }

    private fun getTodayKey(packageName: String): String {
        val today = dayFormat.format(Date())
        return "usage_${packageName}_$today"
    }

    private fun addUsageSeconds(prefs: SharedPreferences, packageName: String, seconds: Int) {
        val key = getTodayKey(packageName)
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + seconds).apply()
    }

    private fun isOverLimit(prefs: SharedPreferences, packageName: String, limitMinutes: Int): Boolean {
        val usedSeconds = prefs.getInt(getTodayKey(packageName), 0)
        return usedSeconds >= limitMinutes * 60
    }

    private fun isOutsideScheduleWindow(prefs: SharedPreferences, packageName: String): Boolean {
        val startMinutes = prefs.getInt("sched_start_$packageName", -1)
        val endMinutes = prefs.getInt("sched_end_$packageName", -1)

        if (startMinutes < 0 || endMinutes < 0) return false

        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        return if (startMinutes <= endMinutes) {
            nowMinutes < startMinutes || nowMinutes > endMinutes
        } else {
            nowMinutes < startMinutes && nowMinutes > endMinutes
        }
    }

    private fun blockApp() {
        Log.d("AppBlockerService", "Blocking app: $currentPackage")
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
    }

    override fun onInterrupt() {
        stopTracking()
    }
}
