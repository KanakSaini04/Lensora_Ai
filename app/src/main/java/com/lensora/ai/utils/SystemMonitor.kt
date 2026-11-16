package com.lensora.ai.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import com.lensora.ai.ai.ModelManager
import java.io.File

data class SystemStats(
    val ramUsedGb: Float,
    val ramTotalGb: Float,
    val ramPercentage: Int,
    val batteryTempCelsius: Float,
    val thermalStatus: String,
    val lamaModelReady: Boolean,
    val esrganModelReady: Boolean,
)

class SystemMonitor(
    private val context: Context,
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val modelManager = ModelManager(context)

    fun getSystemStats(): SystemStats {
        // 1. RAM Usage
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val totalRamGb = if (memoryInfo.totalMem > 0) memoryInfo.totalMem / (1024f * 1024f * 1024f) else 8.0f
        val availRamGb = memoryInfo.availMem / (1024f * 1024f * 1024f)
        val usedRamGb = (totalRamGb - availRamGb).coerceAtLeast(0f)
        val ramPercentage = ((usedRamGb / totalRamGb) * 100).toInt().coerceIn(0, 100)

        // 2. Battery Temperature
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 280
        val batteryTempCelsius = tempTenths / 10.0f

        // 3. Thermal Status (Min SDK 33 >= 29)
        val thermalStatusStr = when (powerManager?.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> "NONE"
            PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
            PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
            PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
            PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
            else -> "NONE"
        }

        // 4. AI Engine Status
        val lamaReady = modelManager.getLamaModelFile()?.exists() == true
        val esrganReady = modelManager.getEnhancementModelFile()?.exists() == true || File(context.filesDir, "models/mobilenet_v1_1.0_224.tflite").exists()

        return SystemStats(
            ramUsedGb = usedRamGb,
            ramTotalGb = totalRamGb,
            ramPercentage = ramPercentage,
            batteryTempCelsius = batteryTempCelsius,
            thermalStatus = thermalStatusStr,
            lamaModelReady = lamaReady,
            esrganModelReady = esrganReady,
        )
    }
}
