package com.frpdroid.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 应用更新检查：从 GitHub Release 获取最新版本信息并下载 APK。
 *
 * 说明：Release 需为公开仓库才能匿名读取；若仓库为私有，接口会返回 404。
 */
object UpdateChecker {

    const val REPO = "lindong89/FrpDroid"
    const val REPO_URL = "https://github.com/lindong89/FrpDroid"
    private const val API_LATEST = "https://api.github.com/repos/$REPO/releases/latest"
    private const val UA = "FrpDroid-Android"

    /** 最新版本信息 */
    data class Info(
        val version: String,
        val notes: String,
        val apkUrl: String?,
        val htmlUrl: String,
        val apkName: String,
        val apkSize: Long,
    )

    /** 检查结果 */
    sealed class Result {
        /** 已是最新 */
        object UpToDate : Result()

        /** 有新版本 */
        data class Available(val info: Info) : Result()

        /** 检查失败 */
        data class Failed(val message: String) : Result()
    }

    /** 版本号比较：latest 是否比 current 新 */
    fun isNewer(latest: String, current: String): Boolean {
        val l = parseVersion(latest)
        val c = parseVersion(current)
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }

    private fun parseVersion(v: String): List<Int> = v.trim()
        .removePrefix("v")
        .removePrefix("V")
        .split(".")
        .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

    /** 检查更新（阻塞调用，请放在 IO 线程） */
    fun check(currentVersion: String): Result {
        return try {
            val conn = open(API_LATEST, accept = "application/vnd.github+json")
            try {
                if (conn.responseCode != 200) {
                    val code = conn.responseCode
                    return Result.Failed(
                        when (code) {
                            404 -> "HTTP 404 (仓库私有或暂无 Release)"
                            403 -> "HTTP 403 (API 访问受限)"
                            else -> "HTTP $code"
                        }
                    )
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val o = JSONObject(text)
                val tag = o.optString("tag_name", "")
                val version = tag.removePrefix("v").removePrefix("V")
                val notes = o.optString("body", "")
                val htmlUrl = o.optString("html_url", REPO_URL).ifBlank { REPO_URL }
                var apkUrl: String? = null
                var apkName = ""
                var apkSize = 0L
                val assets = o.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        val name = a.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = a.optString("browser_download_url", "").ifBlank { null }
                            apkName = name
                            apkSize = a.optLong("size", 0L)
                            break
                        }
                    }
                }
                when {
                    version.isBlank() -> Result.Failed("版本号为空")
                    isNewer(version, currentVersion) -> Result.Available(
                        Info(version, notes, apkUrl, htmlUrl, apkName, apkSize)
                    )
                    else -> Result.UpToDate
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.Failed(e.message ?: "网络错误")
        }
    }

    /** 下载 APK（阻塞调用，请放在 IO 线程），带重试，返回下载后的文件 */
    fun download(url: String, dest: File, onProgress: (Int) -> Unit): File {
        var lastError: Exception? = null
        for (attempt in 1..3) {
            try {
                downloadOnce(url, dest, onProgress)
                return dest
            } catch (e: Exception) {
                lastError = e
                if (dest.exists()) dest.delete()
                if (attempt < 3) {
                    onProgress(0)
                    Thread.sleep(1500L * attempt)
                }
            }
        }
        throw lastError ?: IllegalStateException("download failed")
    }

    private fun downloadOnce(url: String, dest: File, onProgress: (Int) -> Unit) {
        val conn = open(url)
        try {
            if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            dest.parentFile?.mkdirs()
            conn.inputStream.use { input ->
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var lastPct = -1
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) {
                            val pct = (done * 100 / total).toInt().coerceIn(0, 100)
                            if (pct != lastPct) {
                                lastPct = pct
                                onProgress(pct)
                            }
                        }
                    }
                    out.flush()
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    /** 打开连接并跟随重定向 */
    private fun open(url: String, accept: String? = null): HttpURLConnection {
        var current = url
        var hops = 0
        while (true) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 60000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", UA)
                if (accept != null) setRequestProperty("Accept", accept)
            }
            val code = conn.responseCode
            if (code in 300..399 && hops < 5) {
                val loc = conn.getHeaderField("Location")
                conn.disconnect()
                if (loc.isNullOrBlank()) return conn
                current = if (loc.startsWith("http")) loc else URL(URL(current), loc).toString()
                hops++
                continue
            }
            return conn
        }
    }

    /** 是否已获得"安装未知应用"权限 */
    fun canInstall(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ctx.packageManager.canRequestPackageInstalls()

    /** 跳转到"安装未知应用"授权页面 */
    fun openInstallSettings(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }

    /** 调起系统安装器 */
    fun install(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    /** 用浏览器打开链接 */
    fun openUrl(ctx: Context, url: String) {
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }
}
