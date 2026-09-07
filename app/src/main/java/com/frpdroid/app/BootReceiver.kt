package com.frpdroid.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = ctx.getSharedPreferences("frp_config", Context.MODE_PRIVATE)
        val lang = prefs.getString("lang", "zh") ?: "zh"
        val notifEnabled = prefs.getBoolean("notif_enabled", true)

        // FRP 隧道自启
        if (prefs.getBoolean("auto_start", false)) {
            val addr = prefs.getString("addr", "") ?: ""
            if (addr.isNotBlank()) {
                val startIntent = Intent(ctx, FrpService::class.java)
                startIntent.action = FrpService.ACTION_START
                startIntent.putExtra("addr", addr)
                startIntent.putExtra("port", (prefs.getString("port", "7000") ?: "7000").toIntOrNull() ?: 7000)
                startIntent.putExtra("tok", prefs.getString("tok", "") ?: "")
                startIntent.putExtra("user", prefs.getString("user", "") ?: "")
                startIntent.putExtra("proxies", prefs.getString("proxies", "[]") ?: "[]")
                startIntent.putExtra("notif_enabled", notifEnabled)
                startIntent.putExtra(
                    "notif_running",
                    if (lang == "zh") "服务运行中" else "Service Running",
                )
                startIntent.putExtra(
                    "notif_stopped",
                    if (lang == "zh") "已停止" else "Stopped",
                )
                ContextCompat.startForegroundService(ctx.applicationContext, startIntent)
            }
        }

        // Cloudflare 隧道自启
        if (prefs.getBoolean("auto_start_cf", false)) {
            val cfToken = prefs.getString("cf_token", "") ?: ""
            if (cfToken.isNotBlank()) {
                val cfIntent = Intent(ctx, FrpService::class.java)
                cfIntent.action = FrpService.ACTION_START_CF
                cfIntent.putExtra("cf_token", cfToken)
                cfIntent.putExtra("notif_enabled", notifEnabled)
                cfIntent.putExtra(
                    "notif_running",
                    if (lang == "zh") "Cloudflare 运行中" else "Cloudflare Running",
                )
                cfIntent.putExtra(
                    "notif_stopped",
                    if (lang == "zh") "已停止" else "Stopped",
                )
                ContextCompat.startForegroundService(ctx.applicationContext, cfIntent)
            }
        }
    }
}
