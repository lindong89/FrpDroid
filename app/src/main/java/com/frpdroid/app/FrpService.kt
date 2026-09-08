package com.frpdroid.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONArray

class FrpService : Service() {

    private var frpJob: Job? = null
    private var frpProc: Process? = null
    private val frpKill = AtomicBoolean(false)
    private var cfJob: Job? = null
    private var cfProc: Process? = null
    private val cfKill = AtomicBoolean(false)
    private var trafficJob: Job? = null

    private var sessionBaseRx = -1L
    private var sessionBaseTx = -1L
    private var todayBaseRx = 0L
    private var todayBaseTx = 0L
    private var lastRx = 0L
    private var lastTx = 0L
    private var lastTime = 0L
    private var smoothRx = 0L
    private var smoothTx = 0L

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY
        when (action) {
            ACTION_START -> {
                val hasPerm = Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") == 0
                if (hasPerm) startForeground(NOTIF_ID, buildNotification("Starting..."))
                val notifEnabled = intent.getBooleanExtra("notif_enabled", true)
                val proxiesJson = intent.getStringExtra("proxies") ?: "[]"
                val addr = intent.getStringExtra("addr") ?: "127.0.0.1"
                val port = intent.getIntExtra("port", 7000)
                val tok = intent.getStringExtra("tok") ?: ""
                val user = intent.getStringExtra("user") ?: ""
                val metadatas = intent.getStringExtra("metadatas") ?: ""
                val notifRunning = intent.getStringExtra("notif_running") ?: "Service Running"
                val notifStopped = intent.getStringExtra("notif_stopped") ?: "Stopped"
                startFrpc(addr, port, tok, user, metadatas, proxiesJson, notifEnabled, notifRunning, notifStopped)
            }
            ACTION_STOP -> {
                stopFrpc()
                maybeSelfStop()
            }
            ACTION_START_CF -> {
                val hasPerm = Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") == 0
                if (hasPerm) startForeground(NOTIF_ID, buildNotification("Starting Cloudflared..."))
                val cfToken = intent.getStringExtra("cf_token") ?: ""
                val notifEnabled = intent.getBooleanExtra("notif_enabled", true)
                val notifRunning = intent.getStringExtra("notif_running") ?: "Cloudflared Running"
                val notifStopped = intent.getStringExtra("notif_stopped") ?: "Stopped"
                startCloudflared(cfToken, notifEnabled, notifRunning, notifStopped)
            }
            ACTION_STOP_CF -> {
                stopCloudflared()
                maybeSelfStop()
            }
        }
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "FrpDroid", NotificationManager.IMPORTANCE_LOW)
            channel.description = "FrpDroid background service"
            val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        launchIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pending = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FrpDroid")
            .setContentText(text)
            .setSmallIcon(R.drawable.frpdroid_lu)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.frpdroid_lu))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pending)
            .build()
    }

    private fun updateNotification(text: String) {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(NOTIF_ID, buildNotification(text))
    }

    private fun startFrpc(
        addr: String, port: Int, tok: String, user: String, metadatas: String, proxiesJson: String,
        notifEnabled: Boolean, notifRunning: String, notifStopped: String,
    ) {
        if (_isRunning.value) {
            Companion.log("frpc already running")
        } else {
            val scope = CoroutineScope(Dispatchers.IO + coroutineHandler)
            frpJob = scope.launch {
                try {
                    val nativeLibDir = applicationInfo.nativeLibraryDir
                    val binary = File(nativeLibDir, "libfrpc.so")
                    binary.setExecutable(true)
                    val confDir = File(filesDir, "conf")
                    confDir.mkdirs()
                    val configFile = File(confDir, "frpc.toml")
                    buildConfig(configFile, addr, port, tok, user, metadatas, proxiesJson)
                    _isRunning.value = true
                    Companion.log("Starting frpc...")
                    Companion.log("Config: $addr:$port")
                    if (notifEnabled) {
                        updateNotification(notifRunning)
                    }
                    val pb = ProcessBuilder(binary.absolutePath, "-c", configFile.absolutePath)
                    pb.directory(filesDir)
                    pb.environment()["HOME"] = filesDir.absolutePath
                    pb.redirectErrorStream(true)
                    frpProc = pb.start()
                    startTrafficSampling()
                    val reader = BufferedReader(InputStreamReader(frpProc!!.inputStream, Charsets.UTF_8))
                    reader.useLines { lines ->
                        for (line in lines) Companion.log(line)
                    }
                    val exitCode = frpProc!!.waitFor()
                    Companion.log("frpc exited: $exitCode")
                    _isRunning.value = false
                    if (notifEnabled) {
                        updateNotification("Stopped (exit: $exitCode)")
                    }
                } catch (e: Exception) {
                    Companion.log("ERR: ${e.message}")
                    _isRunning.value = false
                    if (notifEnabled) {
                        updateNotification("Error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun startCloudflared(
        token: String, notifEnabled: Boolean, notifRunning: String, notifStopped: String,
    ) {
        if (_cfRunning.value) {
            Companion.log("cloudflared already running")
        } else if (token.isBlank()) {
            Companion.log("ERR: cloudflared token is empty")
        } else {
            val scope = CoroutineScope(Dispatchers.IO + coroutineHandler)
            cfJob = scope.launch {
                try {
                    val nativeLibDir = applicationInfo.nativeLibraryDir
                    val binary = File(nativeLibDir, "libcloudflared.so")
                    binary.setExecutable(true)
                    _cfRunning.value = true
                    Companion.log("Starting cloudflared...")
                    if (notifEnabled) {
                        updateNotification(notifRunning)
                    }
                    val pb = ProcessBuilder(binary.absolutePath, "tunnel", "run", "--token", token)
                    pb.directory(filesDir)
                    pb.environment()["HOME"] = filesDir.absolutePath
                    pb.environment()["NO_AUTOUPDATE"] = "1"
                    pb.redirectErrorStream(true)
                    cfProc = pb.start()
                    startTrafficSampling()
                    val reader = BufferedReader(InputStreamReader(cfProc!!.inputStream, Charsets.UTF_8))
                    reader.useLines { lines ->
                        for (line in lines) Companion.log(line)
                    }
                    val exitCode = cfProc!!.waitFor()
                    Companion.log("cloudflared exited: $exitCode")
                    _cfRunning.value = false
                    if (notifEnabled) {
                        updateNotification("Stopped (exit: $exitCode)")
                    }
                } catch (e: Exception) {
                    Companion.log("ERR: ${e.message}")
                    _cfRunning.value = false
                    if (notifEnabled) {
                        updateNotification("Error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun stopCloudflared() {
        if (cfKill.compareAndSet(false, true)) {
            cfJob?.cancel(null)
            _cfRunning.value = false
            stopTrafficSampling()
            try {
                val p = cfProc
                if (p != null && p.isAlive) {
                    p.destroyForcibly()
                    p.waitFor(3, TimeUnit.SECONDS)
                }
            } catch (e: Exception) {
                // ignore
            }
            cfProc = null
            _cfRunning.value = false
            cfKill.set(false)
            Companion.log("cloudflared stopped")
        }
    }

    private fun buildConfig(f: File, addr: String, port: Int, tok: String, user: String, metadatas: String, proxiesJson: String) {
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
        if (metadatas.isNotBlank()) {
            val entries = metadatas.split(",").map { it.trim() }.filter { it.contains("=") }
            if (entries.isNotEmpty()) {
                val kv = entries.joinToString(", ") { e ->
                    val parts = e.split("=", limit = 2)
                    "\"${parts[0].trim()}\" = \"${parts[1].trim()}\""
                }
                sb.append("metadatas = { $kv }\n")
            }
        }
        if (proxiesJson.length > 3) {
            try {
                val arr = JSONArray(proxiesJson)
                val proxies = ArrayList<ProxyConfig>()
                val visitors = ArrayList<ProxyConfig>()
                for (i in 0 until arr.length()) {
                    val pc = ProxyConfig.fromJson(arr.getJSONObject(i))
                    if (pc.name.isBlank()) continue
                    if (pc.isVisitor) visitors.add(pc) else proxies.add(pc)
                }
                for (p in proxies) writeProxy(sb, p)
                for (v in visitors) writeVisitor(sb, v)
            } catch (e: Exception) {
                Companion.log("WARN: proxy parse error: ${e.message}")
                f.writeText(sb.toString())
                Companion.log("Config written")
                return
            }
        }
        f.writeText(sb.toString())
        Companion.log("Config written")
    }

    private fun startTrafficSampling() {
        if (trafficJob != null) return
        val uid = applicationInfo.uid
        val prefs = getSharedPreferences("frp_config", MODE_PRIVATE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val savedDate = prefs.getString("traffic_date", "")
        if (savedDate != today) {
            prefs.edit().putString("traffic_date", today).putLong("traffic_rx", 0L).putLong("traffic_tx", 0L).apply()
            todayBaseRx = 0L
            todayBaseTx = 0L
        } else {
            todayBaseRx = prefs.getLong("traffic_rx", 0L)
            todayBaseTx = prefs.getLong("traffic_tx", 0L)
        }
        sessionBaseRx = TrafficStats.getUidRxBytes(uid)
        sessionBaseTx = TrafficStats.getUidTxBytes(uid)
        lastRx = sessionBaseRx
        lastTx = sessionBaseTx
        lastTime = System.currentTimeMillis()
        smoothRx = 0L
        smoothTx = 0L
        val scope = CoroutineScope(Dispatchers.IO)
        trafficJob = scope.launch {
            while (isActive) {
                sampleTraffic(uid)
                delay(2000L)
            }
        }
    }

    private fun sampleTraffic(uid: Int) {
        val rx = TrafficStats.getUidRxBytes(uid)
        val tx = TrafficStats.getUidTxBytes(uid)
        if (rx < 0 || tx < 0) return
        if (resetRequested) {
            resetRequested = false
            todayBaseRx = 0L
            todayBaseTx = 0L
            sessionBaseRx = rx
            sessionBaseTx = tx
            lastRx = rx
            lastTx = tx
            lastTime = System.currentTimeMillis()
            smoothRx = 0L
            smoothTx = 0L
            return
        }
        if (sessionBaseRx < 0 || sessionBaseTx < 0) return
        val now = System.currentTimeMillis()
        val dtSec = kotlin.math.max(now - lastTime, 1L) / 1000.0
        val speedRx = ((rx - lastRx) / dtSec).toLong()
        val speedTx = ((tx - lastTx) / dtSec).toLong()
        smoothRx = (smoothRx + speedRx) / 2
        smoothTx = (smoothTx + speedTx) / 2
        val sessionRx = rx - sessionBaseRx
        val sessionTx = tx - sessionBaseTx
        _traffic.value = TrafficInfo(
            sessionRx = sessionRx, sessionTx = sessionTx,
            todayRx = todayBaseRx + sessionRx, todayTx = todayBaseTx + sessionTx,
            speedRx = smoothRx, speedTx = smoothTx,
        )
        lastRx = rx
        lastTx = tx
        lastTime = now
    }

    private fun stopTrafficSampling() {
        if (_isRunning.value || _cfRunning.value) return
        trafficJob?.cancel(null)
        trafficJob = null
        val uid = applicationInfo.uid
        val rx = TrafficStats.getUidRxBytes(uid)
        val tx = TrafficStats.getUidTxBytes(uid)
        if (rx >= 0 && tx >= 0 && sessionBaseRx >= 0 && sessionBaseTx >= 0) {
            val sessionRx = rx - sessionBaseRx
            val sessionTx = tx - sessionBaseTx
            val prefs = getSharedPreferences("frp_config", MODE_PRIVATE)
            prefs.edit()
                .putLong("traffic_rx", todayBaseRx + sessionRx)
                .putLong("traffic_tx", todayBaseTx + sessionTx)
                .apply()
            _traffic.value = TrafficInfo(
                todayRx = todayBaseRx + sessionRx, todayTx = todayBaseTx + sessionTx,
            )
        }
        sessionBaseRx = -1L
        sessionBaseTx = -1L
        smoothRx = 0L
        smoothTx = 0L
    }

    private fun stopFrpc() {
        if (frpKill.compareAndSet(false, true)) {
            frpJob?.cancel(null)
            _isRunning.value = false
            stopTrafficSampling()
            try {
                val p = frpProc
                if (p != null && p.isAlive) {
                    p.destroyForcibly()
                    p.waitFor(3, TimeUnit.SECONDS)
                }
            } catch (e: Exception) {
                // ignore
            }
            frpProc = null
            frpKill.set(false)
            Companion.log("Stopped")
        }
    }

    private fun maybeSelfStop() {
        if (!_isRunning.value && !_cfRunning.value) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopFrpc()
        stopCloudflared()
        super.onDestroy()
    }

    private val coroutineHandler = CoroutineExceptionHandler { _, e ->
        Companion.log("ERR: ${e.message}")
    }

    companion object {
        private const val CHANNEL_ID = "frp"
        private const val NOTIF_ID = 1
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val ACTION_START_CF = "START_CF"
        const val ACTION_STOP_CF = "STOP_CF"

        private val _log = MutableStateFlow<List<String>>(emptyList())
        val logLines: StateFlow<List<String>> = _log.asStateFlow()
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
        private val _cfRunning = MutableStateFlow(false)
        val cfRunning: StateFlow<Boolean> = _cfRunning.asStateFlow()
        private val _traffic = MutableStateFlow(TrafficInfo())
        val traffic: StateFlow<TrafficInfo> = _traffic.asStateFlow()

        @Volatile
        private var resetRequested = false
        private var lastStatus = ""

        fun log(s: String) {
            val c = _log.value.toMutableList()
            c.add(s)
            if (c.size > 500) c.removeAt(0)
            _log.value = c
            lastStatus = s
        }

        fun clearLog() {
            _log.value = listOf("--- logs cleared ---")
        }

        fun resetTraffic(ctx: Context) {
            ctx.getSharedPreferences("frp_config", MODE_PRIVATE)
                .edit().putLong("traffic_rx", 0L).putLong("traffic_tx", 0L).apply()
            resetRequested = true
            _traffic.value = TrafficInfo()
        }
    }
}
