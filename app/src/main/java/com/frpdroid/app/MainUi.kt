@file:OptIn(ExperimentalMaterial3Api::class)

package com.frpdroid.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

// ---------- 主题状态与配色 ----------
object ThemePrefs {
    var isDark by mutableStateOf(true)
}

private val C_BG: Color get() = if (ThemePrefs.isDark) Color(0xFF0C1119) else Color(0xFFF2F5F9)          // 主背景
private val C_SURFACE: Color get() = if (ThemePrefs.isDark) Color(0xFF151E2E) else Color(0xFFFFFFFF)     // 卡片背景
private val C_SURFACE_2: Color get() = if (ThemePrefs.isDark) Color(0xFF1D2A3E) else Color(0xFFE9EEF5)   // 次级卡片 / 输入框背景
private val C_STROKE: Color get() = if (ThemePrefs.isDark) Color(0xFF27364F) else Color(0xFFD5DEE9)      // 描边
private val C_PRIMARY: Color get() = if (ThemePrefs.isDark) Color(0xFF4D8DFF) else Color(0xFF2F6FE0)     // 品牌蓝
private val C_PRIMARY_DEEP = Color(0xFF2E6BD6)
private val C_ACCENT: Color get() = if (ThemePrefs.isDark) Color(0xFF8B5CF6) else Color(0xFF7C3AED)      // 品牌紫
private val C_CYAN: Color get() = if (ThemePrefs.isDark) Color(0xFF22D3EE) else Color(0xFF0EA5E9)
private val C_GREEN: Color get() = if (ThemePrefs.isDark) Color(0xFF34D399) else Color(0xFF10B981)
private val C_RED: Color get() = if (ThemePrefs.isDark) Color(0xFFF87171) else Color(0xFFEF4444)
private val C_ORANGE: Color get() = if (ThemePrefs.isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)
private val C_TEXT_MAIN: Color get() = if (ThemePrefs.isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)   // 主文本
private val C_TEXT: Color get() = if (ThemePrefs.isDark) Color(0xFF8B98AC) else Color(0xFF64748B)        // 次级文本

private val GRAD_BRAND: Brush get() = Brush.linearGradient(listOf(C_PRIMARY, C_ACCENT))
private val GRAD_RUN: Brush get() = Brush.linearGradient(listOf(C_GREEN, C_CYAN))
private val GRAD_STOP: Brush get() = Brush.linearGradient(listOf(C_PRIMARY, C_ACCENT))

// ---------- 工具函数 ----------

fun savePrefs(
    ctx: Context, addr: String, port: String, tok: String, user: String,
    proxies: List<ProxyConfig>, visitors: List<ProxyConfig>,
    autoStart: Boolean, notifEnabled: Boolean, lang: String,
) {
    val prefs = ctx.getSharedPreferences("frp_config", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("addr", addr)
        .putString("port", port)
        .putString("tok", tok)
        .putString("user", user)
        .putString("proxies", listToJson(proxies))
        .putString("visitors", listToJson(visitors))
        .putBoolean("auto_start", autoStart)
        .putBoolean("notif_enabled", notifEnabled)
        .putString("lang", lang)
        .apply()
}

fun loadProxies(prefs: SharedPreferences, key: String = "proxies"): List<ProxyConfig> = try {
    val arr = JSONArray(prefs.getString(key, "[]") ?: "[]")
    (0 until arr.length()).map { ProxyConfig.fromJson(arr.getJSONObject(it)) }
} catch (e: Exception) {
    emptyList()
}

fun listToJson(list: List<ProxyConfig>): String {
    val arr = JSONArray()
    for (p in list) arr.put(p.toJson())
    return arr.toString()
}

fun serversToJson(list: List<ServerConfig>): String {
    val arr = JSONArray()
    for (s in list) arr.put(s.toJson())
    return arr.toString()
}

fun loadServers(prefs: SharedPreferences): List<ServerConfig> {
    val raw = prefs.getString("servers", null)
    if (!raw.isNullOrBlank()) {
        try {
            val arr = JSONArray(raw)
            return (0 until arr.length()).map { ServerConfig.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            return migrateServers(prefs)
        }
    }
    return migrateServers(prefs)
}

fun migrateServers(prefs: SharedPreferences): List<ServerConfig> {
    val addr = prefs.getString("addr", "") ?: ""
    val port = prefs.getString("port", "7000") ?: "7000"
    val tok = prefs.getString("tok", "") ?: ""
    val user = prefs.getString("user", "") ?: ""
    val proxies = loadProxies(prefs, "proxies")
    val visitors = loadProxies(prefs, "visitors")
    val name = if (addr.isBlank()) "Server 1" else addr
    return listOf(ServerConfig(1, name, addr, port, tok, user, proxies, visitors))
}

fun loadCurrentId(prefs: SharedPreferences, servers: List<ServerConfig>): Int {
    val id = prefs.getInt("current_server", -1)
    if (servers.any { it.id == id }) return id
    return servers.firstOrNull()?.id ?: -1
}

// ---------- TOML 生成 ----------

fun buildToml(
    addr: String, port: String, tok: String, user: String,
    proxies: List<ProxyConfig>, visitors: List<ProxyConfig>,
): String {
    val sb = StringBuilder()
    sb.append("serverAddr = \"$addr\"\n")
    sb.append("serverPort = $port\n")
    if (tok.isNotBlank()) {
        sb.append("\n")
        sb.append("auth.token = \"$tok\"\n")
    }
    if (user.isNotBlank()) {
        sb.append("user = \"$user\"\n")
    }
    for (p in proxies) writeProxy(sb, p)
    for (v in visitors) writeVisitor(sb, v)
    return sb.toString()
}

fun writeProxy(sb: StringBuilder, p: ProxyConfig) {
    sb.append("\n")
    sb.append("[[proxies]]\n")
    sb.append("name = \"${p.name}\"\n")
    sb.append("type = \"${p.type}\"\n")
    if (p.localIP.isNotBlank() && p.type !in listOf("http", "https", "tcpmux")) {
        sb.append("localIP = \"${p.localIP}\"\n")
    }
    if (p.localPort.isNotBlank()) {
        sb.append("localPort = ${p.localPort}\n")
    }
    if (p.remotePort.isNotBlank() && p.type in listOf("tcp", "udp")) {
        sb.append("remotePort = ${p.remotePort}\n")
    }
    if (p.customDomains.isNotBlank() && p.type in listOf("http", "https", "tcpmux")) {
        val domains = p.customDomains.split(",").map { it.trim() }.filter { it.isNotBlank() }
        sb.append("customDomains = [" + domains.joinToString(", ") { "\"$it\"" } + "]\n")
    }
    if (p.multiplexer.isNotBlank() && p.type == "tcpmux") {
        sb.append("multiplexer = \"${p.multiplexer}\"\n")
    }
    if (p.secretKey.isNotBlank() && p.type in listOf("stcp", "xtcp")) {
        sb.append("secretKey = \"${p.secretKey}\"\n")
    }
    if (p.pluginType.isBlank()) return
    sb.append("[proxies.plugin]\n")
    sb.append("type = \"${p.pluginType}\"\n")
    when (p.pluginType) {
        "static_file" -> {
            if (p.localPath.isNotBlank()) sb.append("localPath = \"${p.localPath}\"\n")
            if (p.stripPrefix.isNotBlank()) sb.append("stripPrefix = \"${p.stripPrefix}\"\n")
            if (p.httpUser.isNotBlank()) sb.append("httpUser = \"${p.httpUser}\"\n")
            if (p.httpPassword.isNotBlank()) sb.append("httpPassword = \"${p.httpPassword}\"\n")
        }
        "unix_domain_socket" -> {
            if (p.unixPath.isNotBlank()) sb.append("unixPath = \"${p.unixPath}\"\n")
        }
        "https2http" -> {
            if (p.localAddr.isNotBlank()) sb.append("localAddr = \"${p.localAddr}\"\n")
            if (p.crtPath.isNotBlank()) sb.append("crtPath = \"${p.crtPath}\"\n")
            if (p.keyPath.isNotBlank()) sb.append("keyPath = \"${p.keyPath}\"\n")
            if (p.hostHeaderRewrite.isNotBlank()) sb.append("hostHeaderRewrite = \"${p.hostHeaderRewrite}\"\n")
            val headers = p.requestHeaders.split(",").map { it.trim() }.filter { it.contains("=") }
            for (h in headers) {
                val parts = h.split("=", limit = 2)
                if (parts.size == 2) {
                    sb.append("requestHeaders.set.${parts[0]} = \"${parts[1]}\"\n")
                }
            }
        }
    }
}

fun writeVisitor(sb: StringBuilder, v: ProxyConfig) {
    sb.append("\n")
    sb.append("[[visitors]]\n")
    sb.append("name = \"${v.name}\"\n")
    sb.append("type = \"${v.type}\"\n")
    if (v.serverName.isNotBlank()) sb.append("serverName = \"${v.serverName}\"\n")
    if (v.secretKey.isNotBlank()) sb.append("secretKey = \"${v.secretKey}\"\n")
    if (v.bindAddr.isNotBlank()) sb.append("bindAddr = \"${v.bindAddr}\"\n")
    if (v.bindPort.isNotBlank()) sb.append("bindPort = ${v.bindPort}\n")
    sb.append("keepTunnelOpen = ${v.keepTunnelOpen}\n")
}

// ---------- TOML 解析（导入） ----------

fun parseConfig(content: String): ParsedConfig? {
    try {
        var addr = ""
        var port = ""
        var tok = ""
        var user = ""
        val proxies = ArrayList<ProxyConfig>()
        val visitors = ArrayList<ProxyConfig>()
        var inSection = ""
        var inPlugin = false
        var current = ProxyConfig()
        val lines = content.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            when {
                line.startsWith("[[proxies]]") -> {
                    flushSection(inSection, current, proxies, visitors)
                    inSection = "proxies"
                    inPlugin = false
                    current = ProxyConfig()
                }
                line.startsWith("[[visitors]]") -> {
                    flushSection(inSection, current, proxies, visitors)
                    inSection = "visitors"
                    inPlugin = false
                    current = ProxyConfig(isVisitor = true)
                }
                line.startsWith("[proxies.plugin]") -> {
                    inPlugin = true
                }
                line.startsWith("serverAddr") -> addr = valueOf(line)
                line.startsWith("serverPort") -> port = valueOf(line)
                line.startsWith("auth.token") -> tok = valueOf(line)
                line.startsWith("user") -> user = valueOf(line)
                else -> {
                    if (inSection.isEmpty()) continue
                    val kv = line.split("=", limit = 2)
                    if (kv.size != 2) continue
                    val key = kv[0].trim()
                    val rawVal = kv[1].trim()
                    if (inPlugin) {
                        when (key) {
                            "type" -> current.pluginType = rawVal.trim('"')
                            "localPath" -> current.localPath = rawVal.trim('"')
                            "stripPrefix" -> current.stripPrefix = rawVal.trim('"')
                            "httpUser" -> current.httpUser = rawVal.trim('"')
                            "httpPassword" -> current.httpPassword = rawVal.trim('"')
                            "unixPath" -> current.unixPath = rawVal.trim('"')
                            "localAddr" -> current.localAddr = rawVal.trim('"')
                            "crtPath" -> current.crtPath = rawVal.trim('"')
                            "keyPath" -> current.keyPath = rawVal.trim('"')
                            "hostHeaderRewrite" -> current.hostHeaderRewrite = rawVal.trim('"')
                        }
                    } else {
                        when (key) {
                            "name" -> current.name = rawVal.trim('"')
                            "type" -> current.type = rawVal.trim('"')
                            "localIP" -> current.localIP = rawVal.trim('"')
                            "localPort" -> current.localPort = rawVal.trim('"')
                            "remotePort" -> current.remotePort = rawVal.trim('"')
                            "customDomains" -> current.customDomains = rawVal.trim('"', '[', ']').replace("\"", "")
                            "multiplexer" -> current.multiplexer = rawVal.trim('"')
                            "secretKey" -> current.secretKey = rawVal.trim('"')
                            "serverName" -> current.serverName = rawVal.trim('"')
                            "bindAddr" -> current.bindAddr = rawVal.trim('"')
                            "bindPort" -> current.bindPort = rawVal.trim('"')
                            "keepTunnelOpen" -> current.keepTunnelOpen = rawVal.trim().toBoolean()
                        }
                    }
                }
            }
        }
        flushSection(inSection, current, proxies, visitors)
        if (addr.isEmpty()) return null
        if (port.isEmpty()) port = "7000"
        return ParsedConfig(addr, port, tok, user, proxies, visitors)
    } catch (e: Exception) {
        return null
    }
}

private fun valueOf(line: String): String {
    val idx = line.indexOf('=')
    if (idx < 0) return ""
    return line.substring(idx + 1).trim().trim('"')
}

private fun flushSection(
    section: String, current: ProxyConfig,
    proxies: ArrayList<ProxyConfig>, visitors: ArrayList<ProxyConfig>,
) {
    if (section == "proxies" && current.name.isNotBlank()) proxies.add(current)
    if (section == "visitors" && current.name.isNotBlank()) visitors.add(current)
}

// ---------- 版本 & 格式化 ----------

private var cachedFrpVersion: String? = null
private var cachedCfVersion: String? = null

fun appVersionName(ctx: Context): String = try {
    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "4.0.1"
} catch (e: Exception) {
    "4.0.1"
}

fun frpVersion(ctx: Context): String {
    cachedFrpVersion?.let { return it }
    var v = "frp"
    try {
        val binary = File(ctx.applicationInfo.nativeLibraryDir, "libfrpc.so")
        if (binary.exists()) {
            binary.setExecutable(true)
            val out = ProcessBuilder(binary.absolutePath, "-v")
                .redirectErrorStream(true).start().inputStream
                .bufferedReader().readText().trim()
            var ver = out.substringAfter("v")
            for (i in ver.indices) {
                if (!(ver[i].isDigit() || ver[i] == '.')) {
                    ver = ver.substring(0, i)
                    break
                }
            }
            v = if (ver.isBlank()) (if (out.isBlank()) "frp" else out) else "v$ver"
        }
    } catch (e: Exception) {
    }
    cachedFrpVersion = v
    return v
}

fun cloudflaredVersion(ctx: Context): String {
    cachedCfVersion?.let { return it }
    var v = "cloudflared"
    try {
        val binary = File(ctx.applicationInfo.nativeLibraryDir, "libcloudflared.so")
        if (binary.exists()) {
            binary.setExecutable(true)
            val out = ProcessBuilder(binary.absolutePath, "--version")
                .redirectErrorStream(true).start().inputStream
                .bufferedReader().readText().trim()
            val ver = out.substringAfter("version ")
            v = if (ver.isBlank()) (if (out.isBlank()) "cloudflared" else out) else ver.trim().substringBefore(" (")
        }
    } catch (e: Exception) {
    }
    cachedCfVersion = v
    return v
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes / 1024.0 < 1024.0 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes / 1024.0 / 1024.0 < 1024.0 -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
    else -> String.format(Locale.US, "%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0)
}

fun formatTraffic(bytes: Long, speed: Long): String = "${formatBytes(bytes)}  ·  ${formatBytes(speed)}/s"

fun proxyTypeIcon(type: String): ImageVector = when (type) {
    "tcp" -> Icons.Outlined.SwapHoriz
    "udp" -> Icons.Outlined.Wifi
    "http" -> Icons.Outlined.Public
    "https" -> Icons.Outlined.Lock
    "tcpmux" -> Icons.Outlined.CallSplit
    "stcp" -> Icons.Outlined.Shield
    "xtcp" -> Icons.Outlined.Bolt
    "static_file" -> Icons.Outlined.InsertDriveFile
    "unix_domain_socket" -> Icons.Outlined.Dns
    "https2http" -> Icons.Outlined.SyncAlt
    else -> Icons.Outlined.SwapHoriz
}

fun proxyTypeColor(type: String): Color = when (type) {
    "tcpmux" -> C_ORANGE
    "tcp" -> C_PRIMARY
    "udp" -> C_CYAN
    "http" -> C_RED
    "stcp" -> C_TEXT
    "xtcp" -> C_ACCENT
    "https" -> C_GREEN
    else -> C_TEXT
}

// ---------- UI 主题 ----------

@Composable
fun BlueTheme(content: @Composable () -> Unit) {
    val scheme = if (ThemePrefs.isDark) {
        darkColorScheme(
            primary = C_PRIMARY,
            onPrimary = Color.White,
            background = C_BG,
            onBackground = C_TEXT_MAIN,
            surface = C_SURFACE,
            onSurface = C_TEXT_MAIN,
            surfaceVariant = C_SURFACE_2,
            onSurfaceVariant = C_TEXT,
            outline = C_STROKE,
            secondary = C_ACCENT,
            onSecondary = Color.White,
            error = C_RED,
            onError = Color.White,
        )
    } else {
        lightColorScheme(
            primary = C_PRIMARY,
            onPrimary = Color.White,
            background = C_BG,
            onBackground = C_TEXT_MAIN,
            surface = C_SURFACE,
            onSurface = C_TEXT_MAIN,
            surfaceVariant = C_SURFACE_2,
            onSurfaceVariant = C_TEXT,
            outline = C_STROKE,
            secondary = C_ACCENT,
            onSecondary = Color.White,
            error = C_RED,
            onError = Color.White,
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}

// ---------- 通用卡片 ----------

@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .background(C_SURFACE, shape)
            .padding(20.dp),
        content = content,
    )
}

/** 分组标题（小节标题 + 可选操作） */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C_TEXT)
        if (action != null) action()
    }
}

// ---------- 底部浮动导航 ----------

@Composable
fun FloatingGlassBar(
    tab: Int,
    onTab: (Int) -> Unit,
    items: List<Triple<String, Int, Int>>,
    labelOf: (String) -> String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .shadow(
                10.dp,
                shape,
                ambientColor = if (ThemePrefs.isDark) Color.Black.copy(alpha = 0.35f) else Color(0xFF64748B).copy(alpha = 0.3f),
                spotColor = if (ThemePrefs.isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFF475569).copy(alpha = 0.4f),
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = if (ThemePrefs.isDark)
                        listOf(C_SURFACE_2.copy(alpha = 0.88f), C_SURFACE.copy(alpha = 0.82f))
                    else
                        listOf(Color.White.copy(alpha = 0.9f), Color(0xFFE9EEF5).copy(alpha = 0.85f)),
                ),
                shape = shape,
            )
            .border(1.dp, C_STROKE.copy(alpha = 0.9f), shape)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val selected = tab == index
                val bg by animateColorAsState(
                    if (selected)
                        (if (ThemePrefs.isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.5f))
                    else
                        Color.Transparent,
                    label = "navBg",
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(bg)
                        .clickable { onTab(index) }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Image(
                        painter = painterResource(if (selected) item.second else item.third),
                        contentDescription = labelOf(item.first),
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = labelOf(item.first),
                        fontSize = 11.sp,
                        color = if (selected) C_PRIMARY else C_TEXT,
                    )
                }
            }
        }
    }
}

// ---------- 主界面 ----------

private val tabItems = listOf(
    Triple("home", R.drawable.ic_home, R.drawable.ic_home),
    Triple("config", R.drawable.ic_config, R.drawable.ic_config),
    Triple("log", R.drawable.ic_log, R.drawable.ic_log),
    Triple("settings", R.drawable.ic_settings, R.drawable.ic_settings),
)

@Composable
fun AppMain() {
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("frp_config", Context.MODE_PRIVATE)

    var servers by remember { mutableStateOf(loadServers(prefs)) }
    var currentId by remember { mutableStateOf(loadCurrentId(prefs, servers)) }
    val cur = servers.firstOrNull { it.id == currentId }

    var addr by rememberSaveable { mutableStateOf(prefs.getString("addr", "") ?: "") }
    var port by rememberSaveable { mutableStateOf(prefs.getString("port", "7000") ?: "7000") }
    var tok by rememberSaveable { mutableStateOf(prefs.getString("tok", "") ?: "") }
    var user by rememberSaveable { mutableStateOf(prefs.getString("user", "") ?: "") }
    var proxies by remember { mutableStateOf(loadProxies(prefs, "proxies")) }
    var visitors by remember { mutableStateOf(loadProxies(prefs, "visitors")) }
    var autoStart by rememberSaveable { mutableStateOf(prefs.getBoolean("auto_start", false)) }
    var autoStartCf by rememberSaveable { mutableStateOf(prefs.getBoolean("auto_start_cf", false)) }
    var cfEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("cf_enabled", false)) }
    var darkTheme by rememberSaveable { mutableStateOf(prefs.getBoolean("dark_theme", true)) }
    ThemePrefs.isDark = darkTheme
    var notifEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("notif_enabled", true)) }
    var lang by rememberSaveable { mutableStateOf(prefs.getString("lang", "zh") ?: "zh") }
    var tab by rememberSaveable { mutableStateOf(0) }

    var showProxyDialog by remember { mutableStateOf(false) }
    var editProxy by remember { mutableStateOf<ProxyConfig?>(null) }
    var isVisitor by remember { mutableStateOf(false) }
    var proxyIsNew by remember { mutableStateOf(true) }
    var showAbout by remember { mutableStateOf(false) }
    var showServer by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<ParsedConfig?>(null) }

    val t = T(if (lang == "zh") T.zhMap else emptyMap(), T.enMap)

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val input = ctx.contentResolver.openInputStream(uri)
                val text = input?.bufferedReader()?.use { it.readText() } ?: ""
                val parsed = parseConfig(text)
                if (parsed == null) {
                    Toast.makeText(ctx, t.str("import_fail"), Toast.LENGTH_SHORT).show()
                } else {
                    pendingImport = parsed
                    showServer = false
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, t.str("import_fail"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun commitCurrent() {
        val c = servers.firstOrNull { it.id == currentId } ?: return
        servers = servers.map {
            if (it.id == c.id) it.copy(addr = addr, port = port, tok = tok, user = user, proxies = proxies, visitors = visitors)
            else it
        }
    }

    fun saveAll() {
        commitCurrent()
        prefs.edit().putString("servers", serversToJson(servers)).putInt("current_server", currentId).apply()
        savePrefs(ctx, addr, port, tok, user, proxies, visitors, autoStart, notifEnabled, lang)
    }

    fun switchServer(id: Int) {
        val target = servers.firstOrNull { it.id == id } ?: return
        commitCurrent()
        currentId = id
        prefs.edit().putInt("current_server", id).putString("servers", serversToJson(servers)).apply()
        savePrefs(ctx, target.addr, target.port, target.tok, target.user, target.proxies, target.visitors, autoStart, notifEnabled, lang)
        addr = target.addr
        port = target.port
        tok = target.tok
        user = target.user
        proxies = target.proxies
        visitors = target.visitors
    }

    fun deleteServer(id: Int) {
        if (servers.size <= 1) return
        val newList = servers.filter { it.id != id }
        servers = newList
        if (currentId == id) {
            currentId = newList.first().id
            prefs.edit().putInt("current_server", currentId).apply()
        }
        val c = newList.firstOrNull { it.id == currentId } ?: newList.first()
        savePrefs(ctx, c.addr, c.port, c.tok, c.user, c.proxies, c.visitors, autoStart, notifEnabled, lang)
        addr = c.addr
        port = c.port
        tok = c.tok
        user = c.user
        proxies = c.proxies
        visitors = c.visitors
        prefs.edit().putString("servers", serversToJson(servers)).apply()
    }

    fun doExport() {
        val toml = buildToml(addr, port, tok, user, proxies, visitors)
        var exportPath = ""
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "frpc.toml")
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    ctx.contentResolver.openOutputStream(uri)?.use { it.write(toml.toByteArray()) }
                    exportPath = "${Environment.DIRECTORY_DOWNLOADS}/frpc.toml"
                }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                File(dir, "frpc.toml").writeText(toml)
                exportPath = "${dir.absolutePath}/frpc.toml"
            }
            Toast.makeText(
                ctx,
                if (exportPath.isBlank()) "${t.str("export_ok")} frpc.toml" else "${t.str("export_ok")} $exportPath",
                Toast.LENGTH_SHORT,
            ).show()
        } catch (e: Exception) {
            Toast.makeText(ctx, "Export failed", Toast.LENGTH_SHORT).show()
        }
    }

    BlueTheme {
        Scaffold(
            containerColor = C_BG,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(), // 内容延伸到屏幕底部，悬浮条浮在内容之上
                ) {
                    when (tab) {
                        0 -> HomeTab(t, FrpService.isRunning.collectAsState().value, addr, port, proxies, visitors, ctx, cfEnabled)
                        1 -> ConfigTab(
                            t, addr, port, tok, user, proxies, visitors, cur?.name ?: "",
                            onUpd = { a, p, tk, u, px ->
                                addr = a; port = p; tok = tk; user = u; proxies = px
                                saveAll()
                            },
                            onVisUpd = { vs -> visitors = vs; saveAll() },
                            onAddP = { editProxy = ProxyConfig(); isVisitor = false; proxyIsNew = true; showProxyDialog = true },
                            onAddV = { editProxy = ProxyConfig(); isVisitor = true; proxyIsNew = true; showProxyDialog = true },
                            onEditP = { p -> editProxy = p; isVisitor = false; proxyIsNew = false; showProxyDialog = true },
                            onEditV = { v -> editProxy = v; isVisitor = true; proxyIsNew = false; showProxyDialog = true },
                            onServer = { showServer = true },
                        )
                        2 -> LogTab(t, FrpService.logLines.collectAsState().value, rememberLazyListState())
                        3 -> SettingsTab(
                            t, autoStart, { autoStart = it; saveAll() },
                            autoStartCf, { autoStartCf = it; saveAll() },
                            notifEnabled, { notifEnabled = it; saveAll() },
                            lang, { lang = it; saveAll() },
                            onAbout = { showAbout = true },
                            cfEnabled = cfEnabled,
                            darkTheme = darkTheme,
                            onDarkTheme = {
                                darkTheme = it
                                ThemePrefs.isDark = it
                                prefs.edit().putBoolean("dark_theme", it).apply()
                            },
                        )
                    }
                }
                // 悬浮导航叠加在内容之上（内容可滚到其下方）
                FloatingGlassBar(
                    tab = tab,
                    onTab = { tab = it },
                    items = tabItems,
                    labelOf = { key -> t.str(key) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        if (showProxyDialog) {
            ProxyDialog(
                t = t,
                isVisitor = isVisitor,
                isNew = proxyIsNew,
                initial = editProxy ?: ProxyConfig(),
                onSave = { pc ->
                    if (isVisitor) {
                        val list = visitors.toMutableList()
                        val idx = if (proxyIsNew) -1 else list.indexOfFirst { it.id == pc.id }
                        if (idx >= 0) list[idx] = pc else list.add(pc.copy(id = (list.maxOfOrNull { it.id } ?: 0) + 1))
                        visitors = list
                        saveAll()
                    } else {
                        val list = proxies.toMutableList()
                        val idx = if (proxyIsNew) -1 else list.indexOfFirst { it.id == pc.id }
                        if (idx >= 0) list[idx] = pc else list.add(pc.copy(id = (list.maxOfOrNull { it.id } ?: 0) + 1))
                        proxies = list
                        saveAll()
                    }
                    showProxyDialog = false
                    editProxy = null
                },
                onCancel = {
                    showProxyDialog = false
                    editProxy = null
                },
            )
        }

        if (showAbout) {
            AboutDialog(t, cfEnabled, onEnableCf = {
                cfEnabled = true
                prefs.edit().putBoolean("cf_enabled", true).apply()
            }) { showAbout = false }
        }

        if (showServer) {
            ServerDialog(
                t = t,
                servers = servers,
                currentId = currentId,
                onSwitch = { id -> switchServer(id); showServer = false },
                onNew = {
                    val newId = (servers.maxOfOrNull { it.id } ?: 0) + 1
                    val ns = ServerConfig(newId, t.str("new_server"), "", "7000", "", "", emptyList(), emptyList())
                    servers = servers + ns
                    prefs.edit().putString("servers", serversToJson(servers)).apply()
                    switchServer(newId)
                    showServer = false
                },
                onDelete = { id ->
                    deleteServer(id)
                },
                onRename = { id, name ->
                    servers = servers.map { if (it.id == id) it.copy(name = name) else it }
                    prefs.edit().putString("servers", serversToJson(servers)).apply()
                    val cur = servers.firstOrNull { it.id == currentId }
                    if (cur != null) {
                        addr = cur.addr; port = cur.port; tok = cur.tok; user = cur.user
                        proxies = cur.proxies; visitors = cur.visitors
                    }
                },
                onImport = {
                    importLauncher.launch(arrayOf("text/*", "application/octet-stream", "text/plain"))
                },
                onExport = { doExport() },
            )
        }

        // 导入选择弹窗：作为新服务器 / 覆盖当前
        pendingImport?.let { pi ->
            AlertDialog(
                onDismissRequest = { pendingImport = null },
                containerColor = C_SURFACE,
                titleContentColor = C_TEXT_MAIN,
                textContentColor = C_TEXT_MAIN,
                title = { Text(t.str("import")) },
                text = {
                    Column {
                        Text(
                            "${pi.addr}:${pi.port}  ·  ${pi.proxies.size} ${t.str("proxies")} / ${pi.visitors.size} ${t.str("visitors")}",
                            fontSize = 13.sp,
                            color = C_TEXT,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val newId = (servers.maxOfOrNull { it.id } ?: 0) + 1
                                val ns = ServerConfig(newId, pi.addr, pi.addr, pi.port, pi.tok, pi.user, pi.proxies, pi.visitors)
                                servers = servers + ns
                                prefs.edit().putString("servers", serversToJson(servers)).apply()
                                switchServer(newId)
                                pendingImport = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = C_PRIMARY, contentColor = Color.White),
                        ) {
                            Text(t.str("import_as_new"))
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                addr = pi.addr; port = pi.port; tok = pi.tok; user = pi.user
                                proxies = pi.proxies; visitors = pi.visitors
                                saveAll()
                                pendingImport = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = C_PRIMARY),
                            border = androidx.compose.foundation.BorderStroke(1.dp, C_STROKE),
                        ) {
                            Text(t.str("import_overwrite"))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { pendingImport = null }) {
                        Text(t.str("cancel"), color = C_TEXT)
                    }
                },
            )
        }
    }
}

// ---------- 主页 ----------

@Composable
fun HomeTab(
    t: T, running: Boolean, addr: String, port: String,
    proxies: List<ProxyConfig>, visitors: List<ProxyConfig>, ctx: Context,
    cfEnabled: Boolean = false,
) {
    val traffic by FrpService.traffic.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // 品牌标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.frpdroid_lu),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(t.str("launcher"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C_TEXT_MAIN)
            }
        }

        // Hero：FRP 隧道状态卡（与 Cloudflare 卡同构）
        GroupCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (running) GRAD_RUN else GRAD_BRAND,
                                RoundedCornerShape(13.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_tunnel),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.str("frp_tunnel"), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = C_TEXT_MAIN)
                        Text(
                            if (running) t.str("running") else t.str("stopped"),
                            fontSize = 12.sp,
                            color = if (running) C_GREEN else C_ORANGE,
                        )
                    }
                    // 状态指示灯
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (running) C_GREEN else C_ORANGE, CircleShape),
                    )
                }
                if (addr.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_server),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("$addr:$port", fontSize = 14.sp, color = C_TEXT_MAIN, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${t.str("proxies")}: ${proxies.size}  ·  ${t.str("visitors")}: ${visitors.size}",
                    fontSize = 12.sp,
                    color = C_TEXT,
                )

                Spacer(Modifier.height(16.dp))

                // 启停按钮
                Button(
                    onClick = {
                        if (running) {
                            ctx.startService(Intent(ctx, FrpService::class.java).setAction(FrpService.ACTION_STOP))
                        } else {
                            if (addr.isBlank()) {
                                Toast.makeText(ctx, "${t.str("address")} is empty", Toast.LENGTH_SHORT).show()
                            } else {
                                val prefs = ctx.getSharedPreferences("frp_config", Context.MODE_PRIVATE)
                                val intent = Intent(ctx, FrpService::class.java).setAction(FrpService.ACTION_START)
                                intent.putExtra("addr", addr)
                                intent.putExtra("port", (prefs.getString("port", "7000") ?: "7000").toIntOrNull() ?: 7000)
                                intent.putExtra("tok", prefs.getString("tok", "") ?: "")
                                intent.putExtra("user", prefs.getString("user", "") ?: "")
                                intent.putExtra("proxies", prefs.getString("proxies", "[]") ?: "[]")
                                intent.putExtra("notif_enabled", prefs.getBoolean("notif_enabled", true))
                                intent.putExtra("notif_running", if ((prefs.getString("lang", "zh") ?: "zh") == "zh") "服务运行中" else "Service Running")
                                intent.putExtra("notif_stopped", if ((prefs.getString("lang", "zh") ?: "zh") == "zh") "已停止" else "Stopped")
                                ContextCompat.startForegroundService(ctx, intent)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (running) Color(0xFF3A2A12) else C_PRIMARY,
                        contentColor = if (running) C_ORANGE else Color.White,
                    ),
                    border = if (running) androidx.compose.foundation.BorderStroke(1.dp, C_ORANGE) else null,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_start),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (running) t.str("stop") else t.str("start"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (cfEnabled) {
            CloudflaredCard(t, ctx)

            Spacer(Modifier.height(16.dp))
        }

        // 流量仪表
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(t.str("traffic"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C_TEXT_MAIN)
            TextButton(onClick = { FrpService.resetTraffic(ctx) }) {
                Image(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(t.str("reset"), fontSize = 13.sp, color = C_TEXT)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            // 下载卡
            TrafficMiniCard(
                label = t.str("download"),
                icon = R.drawable.ic_download,
                color = C_PRIMARY,
                today = traffic.todayRx,
                speed = traffic.speedRx,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            // 上传卡
            TrafficMiniCard(
                label = t.str("upload"),
                icon = R.drawable.ic_upload,
                color = C_ACCENT,
                today = traffic.todayTx,
                speed = traffic.speedTx,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun TrafficMiniCard(
    label: String,
    icon: Int,
    color: Color,
    today: Long,
    speed: Long,
    modifier: Modifier = Modifier,
) {
    GroupCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(label, fontSize = 13.sp, color = C_TEXT)
            }
            Spacer(Modifier.height(12.dp))
            Text(formatBytes(today), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C_TEXT_MAIN)
            Spacer(Modifier.height(2.dp))
            Text("${formatBytes(speed)}/s", fontSize = 12.sp, color = color)
        }
    }
}

@Composable
fun CloudflaredCard(t: T, ctx: Context) {
    val cfRunning by FrpService.cfRunning.collectAsState()
    val prefs = ctx.getSharedPreferences("frp_config", Context.MODE_PRIVATE)
    var cfToken by remember { mutableStateOf(prefs.getString("cf_token", "") ?: "") }

    GroupCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (cfRunning) GRAD_RUN else Brush.linearGradient(listOf(C_ORANGE, Color(0xFFF97316))),
                            RoundedCornerShape(13.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(if (cfRunning) R.drawable.ic_connected else R.drawable.ic_disconnected),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(t.str("cloudflared"), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = C_TEXT_MAIN)
                    Text(
                        if (cfRunning) t.str("cf_connected") else t.str("cf_disconnected"),
                        fontSize = 12.sp,
                        color = if (cfRunning) C_GREEN else C_TEXT,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (cfRunning) C_GREEN else C_TEXT, CircleShape),
                )
            }

            Spacer(Modifier.height(14.dp))

            if (!cfRunning) {
                OutlinedTextField(
                    value = cfToken,
                    onValueChange = { cfToken = it; prefs.edit().putString("cf_token", it).apply() },
                    label = { Text(t.str("cf_token")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = C_PRIMARY,
                        unfocusedBorderColor = C_STROKE,
                        focusedLabelColor = C_PRIMARY,
                        cursorColor = C_PRIMARY,
                    ),
                )
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    if (cfRunning) {
                        ctx.startService(Intent(ctx, FrpService::class.java).setAction(FrpService.ACTION_STOP_CF))
                    } else {
                        if (cfToken.isBlank()) {
                            Toast.makeText(ctx, t.str("cf_token_desc"), Toast.LENGTH_SHORT).show()
                        } else {
                            val intent = Intent(ctx, FrpService::class.java).setAction(FrpService.ACTION_START_CF)
                            intent.putExtra("cf_token", cfToken)
                            intent.putExtra("notif_enabled", prefs.getBoolean("notif_enabled", true))
                            intent.putExtra("notif_running", if ((prefs.getString("lang", "zh") ?: "zh") == "zh") "Cloudflare 运行中" else "Cloudflare Running")
                            intent.putExtra("notif_stopped", if ((prefs.getString("lang", "zh") ?: "zh") == "zh") "已停止" else "Stopped")
                            ContextCompat.startForegroundService(ctx, intent)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (cfRunning) Color(0xFF3A2A12) else C_PRIMARY,
                    contentColor = if (cfRunning) C_ORANGE else Color.White,
                ),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_start),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(if (cfRunning) t.str("stop") else t.str("start"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------- 配置页 ----------

@Composable
fun ConfigTab(
    t: T,
    addr: String, port: String, tok: String, user: String,
    proxies: List<ProxyConfig>, visitors: List<ProxyConfig>,
    serverName: String,
    onUpd: (String, String, String, String, List<ProxyConfig>) -> Unit,
    onVisUpd: (List<ProxyConfig>) -> Unit,
    onAddP: () -> Unit,
    onAddV: () -> Unit,
    onEditP: (ProxyConfig) -> Unit,
    onEditV: (ProxyConfig) -> Unit,
    onServer: () -> Unit,
) {
    var a by rememberSaveable(addr) { mutableStateOf(addr) }
    var p by rememberSaveable(port) { mutableStateOf(port) }
    var tk by rememberSaveable(tok) { mutableStateOf(tok) }
    var u by rememberSaveable(user) { mutableStateOf(user) }
    var px by remember(proxies) { mutableStateOf(proxies) }
    var vs by remember(visitors) { mutableStateOf(visitors) }
    var seg by rememberSaveable { mutableStateOf(0) } // 0=代理 1=访问者
    val ctx = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // 服务器选择卡
            GroupCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onServer),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(GRAD_BRAND, RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_server),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.str("server"), fontSize = 12.sp, color = C_TEXT)
                        Text(serverName.ifBlank { t.str("select_server") }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C_TEXT_MAIN)
                    }
                    Image(
                        painter = painterResource(R.drawable.ic_config),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 连接信息 + 认证信息（合一卡）
            GroupCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = a,
                            onValueChange = { a = it },
                            label = { Text(t.str("address")) },
                            singleLine = true,
                            modifier = Modifier.weight(2f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = C_PRIMARY,
                                unfocusedBorderColor = C_STROKE,
                                focusedLabelColor = C_PRIMARY,
                                cursorColor = C_PRIMARY,
                            ),
                        )
                        OutlinedTextField(
                            value = p,
                            onValueChange = { if (it.length <= 6) p = it },
                            label = { Text(t.str("port")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = C_PRIMARY,
                                unfocusedBorderColor = C_STROKE,
                                focusedLabelColor = C_PRIMARY,
                                cursorColor = C_PRIMARY,
                            ),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tk,
                        onValueChange = { tk = it },
                        label = { Text(t.str("token_opt")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = C_PRIMARY,
                            unfocusedBorderColor = C_STROKE,
                            focusedLabelColor = C_PRIMARY,
                            cursorColor = C_PRIMARY,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = u,
                        onValueChange = { u = it },
                        label = { Text(t.str("user_opt")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = C_PRIMARY,
                            unfocusedBorderColor = C_STROKE,
                            focusedLabelColor = C_PRIMARY,
                            cursorColor = C_PRIMARY,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 保存按钮（放在代理卡片上方）
            Button(
                onClick = {
                    onUpd(a, p, tk, u, px)
                    Toast.makeText(ctx, t.str("saved"), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = C_PRIMARY, contentColor = Color.White),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_save),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(t.str("save"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))

            // 代理 / 访问者（合一卡，分栏切换）
            GroupCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SegChip(t.str("proxies"), selected = seg == 0, modifier = Modifier.weight(1f)) { seg = 0 }
                        SegChip(t.str("visitors"), selected = seg == 1, modifier = Modifier.weight(1f)) { seg = 1 }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (seg == 0) {
                        if (px.isEmpty()) {
                            EmptyHint(t.str("no_proxy"), t.str("add_hint"))
                        } else {
                            px.forEach { proxy ->
                                ProxyCard(t, proxy, onEdit = { onEditP(proxy) }, onDelete = {
                                    onUpd(a, p, tk, u, px.filterNot { it.id == proxy.id })
                                })
                            }
                        }
                    } else {
                        if (vs.isEmpty()) {
                            EmptyHint(t.str("no_visitor"), t.str("visitor_hint"))
                        } else {
                            vs.forEach { visitor ->
                                ProxyCard(t, visitor, onEdit = { onEditV(visitor) }, onDelete = {
                                    onVisUpd(vs.filterNot { it.id == visitor.id })
                                })
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Spacer(Modifier.height(120.dp))
        }

        // 右下角圆形悬浮添加按钮
        FloatingActionButton(
            onClick = { if (seg == 0) onAddP() else onAddV() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 20.dp, end = 20.dp, bottom = 130.dp),
            containerColor = C_PRIMARY,
            contentColor = Color.White,
            shape = CircleShape,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = if (seg == 0) t.str("add_proxy") else t.str("add_visitor"),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** 分栏切换胶囊（代理/访问者） */
@Composable
private fun SegChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) C_PRIMARY else C_SURFACE_2, label = "seg$text")
    val fg by animateColorAsState(if (selected) Color.White else C_TEXT, label = "segFg$text")
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = fg)
    }
}

@Composable
private fun EmptyHint(title: String, hint: String) {
    GroupCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 14.sp, color = C_TEXT)
            Text(hint, fontSize = 11.sp, color = C_TEXT.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun ProxyCard(t: T, proxy: ProxyConfig, onEdit: () -> Unit, onDelete: () -> Unit) {
    GroupCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(proxyTypeColor(proxy.type).copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    proxyTypeIcon(proxy.type),
                    contentDescription = proxy.type,
                    tint = proxyTypeColor(proxy.type),
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(proxy.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C_TEXT_MAIN)
                Text(
                    "${proxy.type}  ·  ${if (proxy.type in listOf("http", "https", "tcpmux")) proxy.customDomains.ifBlank { "-" } else "${proxy.localIP}:${proxy.localPort} → ${proxy.remotePort}"}",
                    fontSize = 11.sp,
                    color = C_TEXT,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = t.str("edit_proxy"),
                    modifier = Modifier.size(17.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = t.str("delete_server"),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

// ---------- 日志页 ----------

@Composable
fun LogTab(t: T, logs: List<String>, ls: LazyListState) {
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            ls.animateScrollToItem(logs.lastIndex)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 100.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(C_SURFACE_2, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_log),
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(t.str("output"), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = C_TEXT_MAIN)
            }
            TextButton(onClick = { FrpService.clearLog() }) {
                Image(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(t.str("clear"), fontSize = 13.sp, color = C_TEXT)
            }
        }

        // 终端窗口
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    if (ThemePrefs.isDark) Color(0xFF0A0F18) else Color(0xFFFFFFFF),
                    RoundedCornerShape(18.dp),
                )
                .border(
                    1.dp,
                    if (ThemePrefs.isDark) Color.Transparent else C_STROKE,
                    RoundedCornerShape(18.dp),
                )
                .clip(RoundedCornerShape(18.dp)),
        ) {
            if (logs.isEmpty()) {
                Text(
                    t.str("no_logs"),
                    fontSize = 13.sp,
                    color = C_TEXT.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            } else {
                LazyColumn(
                    state = ls,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 120.dp),
                ) {
                    items(logs) { line ->
                        Text(
                            line,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = if (line.startsWith("ERR")) C_RED else C_TEXT_MAIN.copy(alpha = 0.85f),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------- 设置页 ----------

@Composable
fun SettingsTab(
    t: T,
    autoStart: Boolean, onAutoStart: (Boolean) -> Unit,
    autoStartCf: Boolean, onAutoStartCf: (Boolean) -> Unit,
    notifEnabled: Boolean, onNotifEnabled: (Boolean) -> Unit,
    lang: String, onLang: (String) -> Unit,
    onAbout: () -> Unit,
    cfEnabled: Boolean = false,
    darkTheme: Boolean = true, onDarkTheme: (Boolean) -> Unit = {},
) {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SectionHeader(t.str("settings"))
        GroupCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column {
                SettingRow(
                    icon = R.drawable.ic_boot,
                    color = C_PRIMARY,
                    title = t.str("auto_start_frp"),
                    checked = autoStart,
                    onChecked = onAutoStart,
                )
                if (cfEnabled) {
                    Divider(color = C_STROKE.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                    SettingRow(
                        icon = R.drawable.ic_cloud,
                        color = C_ACCENT,
                        title = t.str("auto_start_cf"),
                        checked = autoStartCf,
                        onChecked = onAutoStartCf,
                    )
                }
                Divider(color = C_STROKE.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                SettingRow(
                    icon = R.drawable.ic_notif,
                    color = C_ORANGE,
                    title = t.str("notif_on"),
                    checked = notifEnabled,
                    onChecked = onNotifEnabled,
                )
                Divider(color = C_STROKE.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                SettingRow(
                    icon = R.drawable.ic_settings,
                    color = C_ACCENT,
                    title = t.str("theme"),
                    checked = darkTheme,
                    onChecked = onDarkTheme,
                )
                Divider(color = C_STROKE.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(C_SURFACE_2, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_lang),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(t.str("language"), fontSize = 15.sp, color = C_TEXT_MAIN, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LangChip("中文", selected = lang == "zh") { onLang("zh") }
                    LangChip("English", selected = lang == "en") { onLang("en") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionHeader(t.str("about"))
        // 关于：单行，点击进入关于弹窗
        GroupCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAbout),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(C_SURFACE_2, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_about),
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(t.str("about"), fontSize = 15.sp, color = C_TEXT_MAIN, modifier = Modifier.weight(1f))
                Text(appVersionName(ctx), fontSize = 14.sp, color = C_TEXT, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun SettingRow(
    icon: Int,
    color: Color,
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 15.sp, color = C_TEXT_MAIN, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun LangChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) C_PRIMARY else C_SURFACE_2, label = "lang")
    val fg by animateColorAsState(if (selected) Color.White else C_TEXT, label = "langFg")
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
    ) {
        Text(text, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = fg)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = C_TEXT)
        Text(value, fontSize = 14.sp, color = C_TEXT_MAIN, fontWeight = FontWeight.Medium)
    }
}

// ---------- 对话框 ----------

@Composable
fun ProxyDialog(
    t: T,
    isVisitor: Boolean,
    isNew: Boolean,
    initial: ProxyConfig,
    onSave: (ProxyConfig) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var type by remember { mutableStateOf(initial.type) }
    var localIP by remember { mutableStateOf(initial.localIP) }
    var localPort by remember { mutableStateOf(initial.localPort) }
    var remotePort by remember { mutableStateOf(initial.remotePort) }
    var customDomains by remember { mutableStateOf(initial.customDomains) }
    var multiplexer by remember { mutableStateOf(initial.multiplexer) }
    var secretKey by remember { mutableStateOf(initial.secretKey) }
    var serverName by remember { mutableStateOf(initial.serverName) }
    var bindAddr by remember { mutableStateOf(initial.bindAddr) }
    var bindPort by remember { mutableStateOf(initial.bindPort) }
    var keepTunnelOpen by remember { mutableStateOf(initial.keepTunnelOpen) }
    var pluginType by remember { mutableStateOf(initial.pluginType) }
    var unixPath by remember { mutableStateOf(initial.unixPath) }
    var localPath by remember { mutableStateOf(initial.localPath) }
    var stripPrefix by remember { mutableStateOf(initial.stripPrefix) }
    var httpUser by remember { mutableStateOf(initial.httpUser) }
    var httpPassword by remember { mutableStateOf(initial.httpPassword) }
    var localAddr by remember { mutableStateOf(initial.localAddr) }
    var crtPath by remember { mutableStateOf(initial.crtPath) }
    var keyPath by remember { mutableStateOf(initial.keyPath) }
    var hostHeaderRewrite by remember { mutableStateOf(initial.hostHeaderRewrite) }
    var requestHeaders by remember { mutableStateOf(initial.requestHeaders) }

    val types = listOf("tcp", "udp", "http", "https", "tcpmux", "stcp", "xtcp", "unix_domain_socket")

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = C_SURFACE,
        titleContentColor = C_TEXT_MAIN,
        textContentColor = C_TEXT_MAIN,
        title = {
            Text(
                when {
                    isNew && isVisitor -> t.str("add_visitor")
                    isNew -> t.str("add_proxy")
                    isVisitor -> t.str("edit_visitor")
                    else -> t.str("edit_proxy")
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 480.dp),
            ) {
                DarkField(name, { name = it }, t.str("name"))
                Spacer(Modifier.height(10.dp))
                // 类型下拉菜单
                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it },
                ) {
                    OutlinedTextField(
                        value = t.str(type),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(t.str("type")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = C_PRIMARY,
                            unfocusedBorderColor = C_STROKE,
                            focusedLabelColor = C_PRIMARY,
                            cursorColor = C_PRIMARY,
                        ),
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false },
                    ) {
                        types.forEach { tp ->
                            DropdownMenuItem(
                                text = { Text(t.str(tp)) },
                                onClick = {
                                    type = tp
                                    typeExpanded = false
                                },
                            )
                        }
                    }
                }
                if (!isVisitor && type !in listOf("http", "https", "tcpmux")) {
                    Spacer(Modifier.height(10.dp))
                    DarkField(localIP, { localIP = it }, t.str("local_ip"))
                }
                if (!isVisitor && type !in listOf("http", "https", "tcpmux")) {
                    Spacer(Modifier.height(10.dp))
                    DarkField(localPort, { localPort = it }, t.str("local_port"))
                }
                if (!isVisitor && type in listOf("tcp", "udp")) {
                    Spacer(Modifier.height(10.dp))
                    DarkField(remotePort, { remotePort = it }, t.str("remote_port"))
                }
                if (!isVisitor && type in listOf("http", "https", "tcpmux")) {
                    Spacer(Modifier.height(10.dp))
                    DarkField(customDomains, { customDomains = it }, t.str("custom_domains"))
                }
                if (!isVisitor && type == "tcpmux") {
                    Spacer(Modifier.height(10.dp))
                    DarkField(multiplexer, { multiplexer = it }, t.str("multiplexer"))
                }
                if (type in listOf("stcp", "xtcp") || isVisitor) {
                    Spacer(Modifier.height(10.dp))
                    DarkField(secretKey, { secretKey = it }, t.str("secret_key"))
                }
                if (isVisitor) {
                    Spacer(Modifier.height(10.dp))
                    DarkField(serverName, { serverName = it }, t.str("server_name"))
                    Spacer(Modifier.height(10.dp))
                    DarkField(bindAddr, { bindAddr = it }, t.str("bind_addr"))
                    Spacer(Modifier.height(10.dp))
                    DarkField(bindPort, { bindPort = it }, t.str("bind_port"))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { keepTunnelOpen = !keepTunnelOpen }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(t.str("keep_open"), fontSize = 14.sp, color = C_TEXT_MAIN)
                        Switch(checked = keepTunnelOpen, onCheckedChange = { keepTunnelOpen = it })
                    }
                }
                if (!isVisitor) {
                    Spacer(Modifier.height(10.dp))
                    DarkField(pluginType, { pluginType = it }, t.str("plugin"))
                    when (pluginType) {
                        "static_file" -> {
                            Spacer(Modifier.height(10.dp))
                            DarkField(localPath, { localPath = it }, t.str("local_path"))
                            Spacer(Modifier.height(10.dp))
                            DarkField(stripPrefix, { stripPrefix = it }, t.str("strip_prefix"))
                            Spacer(Modifier.height(10.dp))
                            DarkField(httpUser, { httpUser = it }, t.str("http_user"))
                            Spacer(Modifier.height(10.dp))
                            DarkField(httpPassword, { httpPassword = it }, t.str("http_password"))
                        }
                        "unix_domain_socket" -> {
                            Spacer(Modifier.height(10.dp))
                            DarkField(unixPath, { unixPath = it }, t.str("unix_path"))
                        }
                        "https2http" -> {
                            Spacer(Modifier.height(10.dp))
                            DarkField(localAddr, { localAddr = it }, t.str("local_addr"))
                            Spacer(Modifier.height(10.dp))
                            DarkField(crtPath, { crtPath = it }, t.str("crt_path"))
                            Spacer(Modifier.height(10.dp))
                            DarkField(keyPath, { keyPath = it }, t.str("key_path"))
                            Spacer(Modifier.height(10.dp))
                            DarkField(hostHeaderRewrite, { hostHeaderRewrite = it }, t.str("host_rewrite"))
                            Spacer(Modifier.height(10.dp))
                            DarkField(requestHeaders, { requestHeaders = it }, t.str("req_headers"))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initial.copy(
                            name = name,
                            type = type,
                            localIP = localIP,
                            localPort = localPort,
                            remotePort = remotePort,
                            customDomains = customDomains,
                            multiplexer = multiplexer,
                            secretKey = secretKey,
                            serverName = serverName,
                            bindAddr = bindAddr,
                            bindPort = bindPort,
                            keepTunnelOpen = keepTunnelOpen,
                            pluginType = pluginType,
                            unixPath = unixPath,
                            localPath = localPath,
                            stripPrefix = stripPrefix,
                            httpUser = httpUser,
                            httpPassword = httpPassword,
                            localAddr = localAddr,
                            crtPath = crtPath,
                            keyPath = keyPath,
                            hostHeaderRewrite = hostHeaderRewrite,
                            requestHeaders = requestHeaders,
                            isVisitor = isVisitor,
                        )
                    )
                },
            ) {
                Text(t.str("save"), color = C_PRIMARY, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(t.str("cancel"), color = C_TEXT)
            }
        },
    )
}

@Composable
private fun DarkField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = C_PRIMARY,
            unfocusedBorderColor = C_STROKE,
            focusedLabelColor = C_PRIMARY,
            cursorColor = C_PRIMARY,
        ),
    )
}

@Composable
fun AboutDialog(t: T, cfEnabled: Boolean, onEnableCf: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C_SURFACE,
        titleContentColor = C_TEXT_MAIN,
        textContentColor = C_TEXT_MAIN,
        title = { Text(t.str("about")) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.frpdroid_lu),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(t.str("launcher"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(t.str("author") + ": LINDONG", fontSize = 12.sp, color = C_TEXT)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Divider(color = C_STROKE.copy(alpha = 0.5f))
                Spacer(Modifier.height(10.dp))
                InfoRow(t.str("version"), appVersionName(ctx))
                Spacer(Modifier.height(8.dp))
                InfoRow(t.str("frp_version"), frpVersion(ctx))
                Spacer(Modifier.height(8.dp))
                // Cloudflare 启用开关：未启用显示"启用"，启用后显示版本号（无关闭按钮）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !cfEnabled, onClick = onEnableCf)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(t.str("cf_version"), fontSize = 14.sp, color = C_TEXT)
                    Text(
                        if (cfEnabled) cloudflaredVersion(ctx) else t.str("cf_enable"),
                        fontSize = 14.sp,
                        color = if (cfEnabled) C_TEXT_MAIN else C_PRIMARY,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(t.str("ok"), color = C_PRIMARY, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

@Composable
fun ServerDialog(
    t: T,
    servers: List<ServerConfig>,
    currentId: Int,
    onSwitch: (Int) -> Unit,
    onNew: () -> Unit,
    onDelete: (Int) -> Unit,
    onRename: (Int, String) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    var renameId by remember { mutableStateOf<Int?>(null) }
    var renameText by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = C_SURFACE,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 18.dp)) {
                Text(
                    t.str("select_server"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = C_TEXT_MAIN,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(max = 380.dp),
                ) {
                    servers.forEach { s ->
                        val selected = s.id == currentId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) C_PRIMARY.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { onSwitch(s.id) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.name, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = C_TEXT_MAIN)
                                Text("${s.addr}:${s.port}", fontSize = 12.sp, color = C_TEXT)
                            }
                            if (selected) {
                                Text(t.str("current_server"), fontSize = 11.sp, color = C_PRIMARY)
                            }
                            IconButton(onClick = { renameId = s.id; renameText = s.name }, modifier = Modifier.size(32.dp)) {
                                Image(
                                    painter = painterResource(R.drawable.ic_modify),
                                    contentDescription = t.str("rename_server"),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            if (servers.size > 1) {
                                IconButton(onClick = { onDelete(s.id) }, modifier = Modifier.size(32.dp)) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = t.str("delete_server"),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                    TextButton(onClick = onNew, modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${t.str("new_server")}", color = C_PRIMARY)
                    }
                    Row {
                        TextButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                            Image(
                                painter = painterResource(R.drawable.ic_import),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(t.str("import"), color = C_PRIMARY)
                        }
                        TextButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                            Image(
                                painter = painterResource(R.drawable.ic_export),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(t.str("export"), color = C_PRIMARY)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { onSwitch(currentId) }) {
                        Text(t.str("ok"), color = C_PRIMARY, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    renameId?.let { id ->
        val target = servers.firstOrNull { it.id == id }
        if (target != null) {
            Dialog(
                onDismissRequest = { renameId = null },
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = C_SURFACE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            t.str("rename_server"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = C_TEXT_MAIN,
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            label = { Text(t.str("server_label")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = C_PRIMARY,
                                unfocusedBorderColor = C_STROKE,
                                focusedLabelColor = C_PRIMARY,
                                cursorColor = C_PRIMARY,
                            ),
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { renameId = null }) {
                                Text(t.str("cancel"), color = C_TEXT)
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = {
                                onRename(id, renameText.trim().ifBlank { target.name })
                                renameId = null
                            }) {
                                Text(t.str("ok"), color = C_PRIMARY, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
