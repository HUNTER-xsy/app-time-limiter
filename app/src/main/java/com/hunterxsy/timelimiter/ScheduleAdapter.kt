package com.hunterxsy.timelimiter

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ScheduleInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
)

class ScheduleAdapter(
    private val schedules: List<ScheduleInfo>,
    private val onDelete: (ScheduleInfo) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    class ScheduleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgScheduleIcon)
        val appName: TextView = view.findViewById(R.id.txtScheduleAppName)
        val timeRange: TextView = view.findViewById(R.id.txtScheduleTimeRange)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteSchedule)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val item = schedules[position]
        holder.icon.setImageDrawable(item.icon)
        holder.appName.text = item.appName

        val startStr = String.format("%02d:%02d", item.startHour, item.startMinute)
        val endStr = String.format("%02d:%02d", item.endHour, item.endMinute)
        holder.timeRange.text = "$startStr - $endStr"

        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = schedules.size

    companion object {
        fun loadSchedules(pm: PackageManager, prefs: android.content.SharedPreferences): List<ScheduleInfo> {
            val all = prefs.all
            val startKeys = all.filterKeys { it.startsWith("sched_start_") }

            return startKeys.mapNotNull { (key, value) ->
                val packageName = key.removePrefix("sched_start_")
                val startMinutesOfDay = value as? Int ?: return@mapNotNull null
                val endMinutesOfDay = prefs.getInt("sched_end_$packageName", -1)
                if (endMinutesOfDay < 0) return@mapNotNull null

                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    ScheduleInfo(
                        packageName = packageName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo),
                        startHour = startMinutesOfDay / 60,
                        startMinute = startMinutesOfDay % 60,
                        endHour = endMinutesOfDay / 60,
                        endMinute = endMinutesOfDay % 60
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }.sortedBy { it.appName.lowercase() }
        }
    }
}
