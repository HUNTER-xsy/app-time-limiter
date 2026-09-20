package com.hunterxsy.timelimiter

import android.app.AlertDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var editPackageName: EditText
    private lateinit var editMinutes: EditText
    private lateinit var recyclerLimits: RecyclerView
    private lateinit var layoutHome: View
    private lateinit var includeStatistics: View
    private lateinit var includeSchedule: View
    private lateinit var includeProfile: View

    private var pendingChooseAppTarget: EditText? = null
    private var scheduleStartMinutes = -1
    private var scheduleEndMinutes = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editPackageName = findViewById(R.id.editPackageName)
        editMinutes = findViewById(R.id.editMinutes)
        recyclerLimits = findViewById(R.id.recyclerLimits)
        recyclerLimits.layoutManager = LinearLayoutManager(this)

        layoutHome = findViewById(R.id.layoutHome)
        includeStatistics = findViewById(R.id.includeStatistics)
        includeSchedule = findViewById(R.id.includeSchedule)
        includeProfile = findViewById(R.id.includeProfile)

        val btnSetLimit: Button = findViewById(R.id.btnSetLimit)
        val btnOpenAccessibility: Button = findViewById(R.id.btnOpenAccessibility)
        val btnChooseApp: Button = findViewById(R.id.btnChooseApp)
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNav)

        btnSetLimit.setOnClickListener { saveLimit() }

        btnOpenAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnChooseApp.setOnClickListener {
            pendingChooseAppTarget = editPackageName
            showAppListDialog()
        }

        val btnNotification: android.widget.ImageButton = findViewById(R.id.btnNotification)
        val btnSettings: android.widget.ImageButton = findViewById(R.id.btnSettings)

        btnNotification.setOnClickListener {
            Toast.makeText(this, "কোনো নতুন নোটিফিকেশন নেই", Toast.LENGTH_SHORT).show()
        }

        btnSettings.setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_profile
        }

        setupScheduleTab()
        setupProfileTab()

        bottomNav.setOnItemSelectedListener { item ->
            layoutHome.visibility = View.GONE
            includeStatistics.visibility = View.GONE
            includeSchedule.visibility = View.GONE
            includeProfile.visibility = View.GONE

            when (item.itemId) {
                R.id.nav_home -> {
                    layoutHome.visibility = View.VISIBLE
                    true
                }
                R.id.nav_statistics -> {
                    includeStatistics.visibility = View.VISIBLE
                    loadStatistics()
                    true
                }
                R.id.nav_schedule -> {
                    includeSchedule.visibility = View.VISIBLE
                    refreshScheduleList()
                    true
                }
                R.id.nav_profile -> {
                    includeProfile.visibility = View.VISIBLE
                    updateProfileStatus()
                    true
                }
                else -> false
            }
        }

        refreshLimitList()
    }

    override fun onResume() {
        super.onResume()
        refreshLimitList()
        if (includeProfile.visibility == View.VISIBLE) {
            updateProfileStatus()
        }
    }

    private fun setupProfileTab() {
        val btnProfileAccessibility: Button = includeProfile.findViewById(R.id.btnProfileAccessibility)
        val btnResetData: Button = includeProfile.findViewById(R.id.btnResetData)

        btnProfileAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnResetData.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("সব ডেটা মুছে ফেলবে?")
                .setMessage("সব লিমিট, শিডিউল এবং ব্যবহারের ইতিহাস স্থায়ীভাবে মুছে যাবে। এটা ফিরিয়ে আনা যাবে না।")
                .setPositiveButton("হ্যাঁ, মুছে ফেলো") { _, _ ->
                    val prefs = getSharedPreferences("time_limiter_prefs", MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    Toast.makeText(this, "সব ডেটা মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                    refreshLimitList()
                }
                .setNegativeButton("বাতিল", null)
                .show()
        }
    }

    private fun updateProfileStatus() {
        val txtStatus: TextView = includeProfile.findViewById(R.id.txtAccessibilityStatus)
        val enabled = isAccessibilityServiceEnabled()
        if (enabled) {
            txtStatus.text = "Enabled"
            txtStatus.setTextColor(getColor(R.color.teal_accent))
        } else {
            txtStatus.text = "Disabled"
            txtStatus.setTextColor(android.graphics.Color.parseColor("#FF5555"))
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "$packageName/${AppBlockerService::class.java.canonicalName}"
        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun setupScheduleTab() {
        val editSchedulePackage: EditText = includeSchedule.findViewById(R.id.editSchedulePackageName)
        val btnScheduleChooseApp: Button = includeSchedule.findViewById(R.id.btnScheduleChooseApp)
        val btnPickStartTime: Button = includeSchedule.findViewById(R.id.btnPickStartTime)
        val btnPickEndTime: Button = includeSchedule.findViewById(R.id.btnPickEndTime)
        val btnAddSchedule: Button = includeSchedule.findViewById(R.id.btnAddSchedule)

        btnScheduleChooseApp.setOnClickListener {
            pendingChooseAppTarget = editSchedulePackage
            showAppListDialog()
        }

        btnPickStartTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                scheduleStartMinutes = hour * 60 + minute
                btnPickStartTime.text = String.format("%02d:%02d", hour, minute)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        btnPickEndTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                scheduleEndMinutes = hour * 60 + minute
                btnPickEndTime.text = String.format("%02d:%02d", hour, minute)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        btnAddSchedule.setOnClickListener {
            val packageName = editSchedulePackage.text.toString().trim()

            if (packageName.isEmpty()) {
                Toast.makeText(this, "একটা অ্যাপ বাছাই করো", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (scheduleStartMinutes < 0 || scheduleEndMinutes < 0) {
                Toast.makeText(this, "শুরু ও শেষের সময় বাছাই করো", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("time_limiter_prefs", MODE_PRIVATE)
            prefs.edit()
                .putInt("sched_start_$packageName", scheduleStartMinutes)
                .putInt("sched_end_$packageName", scheduleEndMinutes)
                .apply()

            Toast.makeText(this, "শিডিউল সেট হয়েছে", Toast.LENGTH_SHORT).show()

            editSchedulePackage.text.clear()
            btnPickStartTime.text = "Select"
            btnPickEndTime.text = "Select"
            scheduleStartMinutes = -1
            scheduleEndMinutes = -1

            refreshScheduleList()
        }
    }

    private fun refreshScheduleList() {
        val prefs = getSharedPreferences("time_limiter_prefs", MODE_PRIVATE)
        val recyclerSchedules: RecyclerView = includeSchedule.findViewById(R.id.recyclerSchedules)
        recyclerSchedules.layoutManager = LinearLayoutManager(this)

        val schedules = ScheduleAdapter.loadSchedules(packageManager, prefs)
        recyclerSchedules.adapter = ScheduleAdapter(schedules) { scheduleToDelete ->
            prefs.edit()
                .remove("sched_start_${scheduleToDelete.packageName}")
                .remove("sched_end_${scheduleToDelete.packageName}")
                .apply()
            refreshScheduleList()
        }
    }

    private fun refreshLimitList() {
        val prefs = getSharedPreferences("time_limiter_prefs", MODE_PRIVATE)
        val limits = LimitListAdapter.loadLimits(packageManager, prefs)
        recyclerLimits.adapter = LimitListAdapter(limits)

        val circularProgress: CircularProgressView = findViewById(R.id.circularProgressHome)
        val txtActiveLimitsCount: TextView = findViewById(R.id.txtActiveLimitsCount)

        val totalMinutesToday = limits.sumOf { it.usedMinutes }
        val hours = totalMinutesToday / 60
        val mins = totalMinutesToday % 60

        val totalLimitMinutes = limits.sumOf { it.limitMinutes }
        val overallPercent = if (totalLimitMinutes > 0) {
            ((totalMinutesToday * 100) / totalLimitMinutes).coerceAtMost(100)
        } else 0

        circularProgress.setProgress(overallPercent, "${hours}h ${mins}m", "Today's Usage")
        txtActiveLimitsCount.text = "${limits.size} App Limits Active"
    }

    private fun loadStatistics() {
        val prefs = getSharedPreferences("time_limiter_prefs", MODE_PRIVATE)

        val txtTotal: TextView = includeStatistics.findViewById(R.id.txtStatsTotalUsage)
        val chart: WeeklyBarChartView = includeStatistics.findViewById(R.id.weeklyChart)
        val recyclerStats: RecyclerView = includeStatistics.findViewById(R.id.recyclerStatsLimits)
        recyclerStats.layoutManager = LinearLayoutManager(this)

        val limits = LimitListAdapter.loadLimits(packageManager, prefs)
        recyclerStats.adapter = LimitListAdapter(limits)

        val totalMinutesToday = limits.sumOf { it.usedMinutes }
        val hours = totalMinutesToday / 60
        val mins = totalMinutesToday % 60
        txtTotal.text = "${hours}ঘ ${mins}মি"

        val weeklyData = getLast7DaysUsage(prefs)
        val dayLabels = getLast7DaysLabels()
        chart.setData(weeklyData, dayLabels)
    }

    private fun getLast7DaysUsage(prefs: SharedPreferences): List<Int> {
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        val allPrefs = prefs.all
        val result = mutableListOf<Int>()

        for (i in 6 downTo 0) {
            val cal = calendar.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dayFormat.format(cal.time)

            var totalSeconds = 0
            for ((key, value) in allPrefs) {
                if (key.startsWith("usage_") && key.endsWith("_$dateStr")) {
                    totalSeconds += (value as? Int) ?: 0
                }
            }
            result.add(totalSeconds / 60)
        }
        return result
    }

    private fun getLast7DaysLabels(): List<String> {
        val labelFormat = SimpleDateFormat("EEE", Locale.US)
        val calendar = Calendar.getInstance()
        val result = mutableListOf<String>()

        for (i in 6 downTo 0) {
            val cal = calendar.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, -i)
            result.add(labelFormat.format(cal.time))
        }
        return result
    }

    private fun showAppListDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_app_list)

        val recyclerView: RecyclerView = dialog.findViewById(R.id.recyclerApps)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfoList = pm.queryIntentActivities(intent, 0)

        val appList = resolveInfoList
            .filter { it.activityInfo.packageName != packageName }
            .map { resolveInfo ->
                AppInfo(
                    name = resolveInfo.loadLabel(pm).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }

        recyclerView.adapter = AppListAdapter(appList) { selectedApp ->
            pendingChooseAppTarget?.setText(selectedApp.packageName)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveLimit() {
        val packageName = editPackageName.text.toString().trim()
        val minutesText = editMinutes.text.toString().trim()

        if (packageName.isEmpty() || minutesText.isEmpty()) {
            Toast.makeText(this, "সব ঘর পূরণ করো", Toast.LENGTH_SHORT).show()
            return
        }

        val minutes = minutesText.toIntOrNull()
        if (minutes == null || minutes <= 0) {
            Toast.makeText(this, "সঠিক মিনিট লেখো", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("time_limiter_prefs", MODE_PRIVATE)
        prefs.edit()
            .putInt("limit_$packageName", minutes)
            .apply()

        Toast.makeText(this, "$packageName এর জন্য $minutes মিনিট সেট হয়েছে", Toast.LENGTH_SHORT).show()
        editPackageName.text.clear()
        editMinutes.text.clear()
        refreshLimitList()
    }
}
