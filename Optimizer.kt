package com.shinnie.optimize

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.Settings

data class DeviceStats(
    val ramUsedPercent: Int,
    val ramFreeMb: Long,
    val storageUsedPercent: Int,
    val batteryPercent: Int,
    val isCharging: Boolean
)

object Optimizer {

    // Free Fire (normal) and Free Fire MAX
    private val GAME_PACKAGES = listOf("com.dts.freefireth", "com.dts.freefiremax")

    private fun memInfo(context: Context): ActivityManager.MemoryInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    }

    fun readStats(context: Context): DeviceStats {
        val mem = memInfo(context)
        val ramUsed = (((mem.totalMem - mem.availMem).toDouble() / mem.totalMem) * 100).toInt()

        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.blockCountLong * stat.blockSizeLong
        val free = stat.availableBlocksLong * stat.blockSizeLong
        val storageUsed = (((total - free).toDouble() / total) * 100).toInt()

        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        return DeviceStats(
            ramUsedPercent = ramUsed,
            ramFreeMb = mem.availMem / (1024 * 1024),
            storageUsedPercent = storageUsed,
            batteryPercent = if (level >= 0) level * 100 / scale else 0,
            isCharging = charging
        )
    }

    /**
     * Asks Android to stop background processes of other user-installed apps.
     * Android limits this (no root = no force-kill), so results vary by device.
     * Returns approx. MB of RAM freed.
     */
    fun boost(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val before = memInfo(context).availMem
        context.packageManager.getInstalledApplications(0)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { it.packageName }
            .filter { it != context.packageName && it !in GAME_PACKAGES }
            .forEach { am.killBackgroundProcesses(it) }
        val after = memInfo(context).availMem
        return ((after - before) / (1024 * 1024)).coerceAtLeast(0)
    }

    /** Opens Free Fire (or Free Fire MAX). Returns false if neither is installed. */
    fun launchGame(context: Context): Boolean {
        for (pkg in GAME_PACKAGES) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            }
        }
        return false
    }

    /** Lets the user turn on Do Not Disturb so calls/notifications don't interrupt a match. */
    fun openDndSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
