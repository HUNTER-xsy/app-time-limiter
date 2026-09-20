package com.hunterxsy.timelimiter

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class LimitInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val limitMinutes: Int,
    val usedMinutes: Int
)

class LimitListAdapter(
    private val limits: List<LimitInfo>
) : RecyclerView.Adapter<LimitListAdapter.LimitViewHolder>() {

    class LimitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgLimitIcon)
        val appName: TextView = view.findViewById(R.id.txtLimitAppName)
        val usage: TextView = view.findViewById(R.id.txtLimitUsage)
        val percent: TextView = view.findViewById(R.id.txtLimitPercent)
        val progress: ProgressBar = view.findViewById(R.id.progressLimit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LimitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_limit, parent, false)
        return LimitViewHolder(view)
    }

    override fun onBindViewHolder(holder: LimitViewHolder, position: Int) {
        val item = limits[position]
        holder.icon.setImageDrawable(item.icon)
        holder.appName.text = item.appName
        holder.usage.text = "${item.usedMinutes}মি / ${item.limitMinutes}মি"

        val percent = if (item.limitMinutes > 0) {
            ((item.usedMinutes * 100) / item.limitMinutes).coerceAtMost(100)
        } else 0

        holder.percent.text = "$percent%"
        holder.progress.progress = percent
    }

    override fun getItemCount() = limits.size

    companion object {
        fun loadLimits(pm: PackageManager, prefs: android.content.SharedPreferences): List<LimitInfo> {
            val all = prefs.all
            val limitKeys = all.filterKeys { it.startsWith("limit_") }

            val dayFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val today = dayFormat.format(java.util.Date())

            return limitKeys.mapNotNull { (key, value) ->
                val packageName = key.removePrefix("limit_")
                val limitMinutes = value as? Int ?: return@mapNotNull null
                val usedSeconds = prefs.getInt("usage_${packageName}_$today", 0)
                val usedMinutes = usedSeconds / 60

                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    LimitInfo(
                        packageName = packageName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo),
                        limitMinutes = limitMinutes,
                        usedMinutes = usedMinutes
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }.sortedByDescending { it.usedMinutes }
        }
    }
}
