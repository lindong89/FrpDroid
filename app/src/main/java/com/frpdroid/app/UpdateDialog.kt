package com.frpdroid.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 检查更新弹窗：检查 GitHub Release → 展示新版本 → 下载并安装
 */
@Composable
fun UpdateDialog(t: T, currentVersion: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<UpdateChecker.Result?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(-1) }
    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(attempt) {
        result = null
        progress = -1
        result = withContext(Dispatchers.IO) { UpdateChecker.check(currentVersion) }
    }

    fun startDownload(info: UpdateChecker.Info) {
        val url = info.apkUrl ?: return
        downloading = true
        progress = 0
        FrpService.log("Update: start download $url")
        scope.launch {
            try {
                val dir = File(ctx.getExternalFilesDir(null), "update")
                val dest = File(dir, info.apkName.ifBlank { "FrpDroid-${info.version}.apk" })
                if (dest.exists()) dest.delete()
                withContext(Dispatchers.IO) {
                    UpdateChecker.download(url, dest) { p -> progress = p }
                }
                progress = 100
                downloading = false
                FrpService.log("Update: downloaded ${dest.name} (${dest.length() / 1024} KB)")
                if (UpdateChecker.canInstall(ctx)) {
                    UpdateChecker.install(ctx, dest)
                } else {
                    Toast.makeText(ctx, t.str("install_permission"), Toast.LENGTH_LONG).show()
                    UpdateChecker.openInstallSettings(ctx)
                }
            } catch (e: Exception) {
                downloading = false
                progress = -1
                val msg = "${e.javaClass.simpleName}: ${e.message}"
                FrpService.log("Update download failed -> $msg")
                Toast.makeText(ctx, "${t.str("download_fail")}: $msg", Toast.LENGTH_LONG).show()
            }
        }
    }

    val r = result
    val available = r as? UpdateChecker.Result.Available

    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        containerColor = C_SURFACE,
        titleContentColor = C_TEXT_MAIN,
        textContentColor = C_TEXT_MAIN,
        title = {
            Text(
                when {
                    r == null -> t.str("check_update")
                    available != null -> t.str("update_found")
                    r is UpdateChecker.Result.UpToDate -> t.str("check_update")
                    else -> t.str("check_fail")
                }
            )
        },
        text = {
            Column {
                when {
                    // 检查中
                    r == null -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = C_PRIMARY,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(t.str("checking"), fontSize = 14.sp, color = C_TEXT)
                        }
                    }
                    // 已是最新
                    r is UpdateChecker.Result.UpToDate -> {
                        Text(t.str("up_to_date"), fontSize = 14.sp, color = C_GREEN, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t.str("current_version"), fontSize = 13.sp, color = C_TEXT)
                            Text(currentVersion, fontSize = 13.sp, color = C_TEXT_MAIN)
                        }
                    }
                    // 检查失败
                    r is UpdateChecker.Result.Failed -> {
                        Text(t.str("check_fail"), fontSize = 14.sp, color = C_RED, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        Text(r.message, fontSize = 12.sp, color = C_TEXT)
                        Spacer(Modifier.height(10.dp))
                        Text(t.str("private_repo_hint"), fontSize = 11.sp, color = C_TEXT)
                    }
                    // 有新版本
                    available != null -> {
                        val info = available.info
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t.str("latest_version"), fontSize = 13.sp, color = C_TEXT)
                            Text("v${info.version}", fontSize = 13.sp, color = C_PRIMARY, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t.str("current_version"), fontSize = 13.sp, color = C_TEXT)
                            Text(currentVersion, fontSize = 13.sp, color = C_TEXT_MAIN)
                        }
                        if (info.notes.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .background(C_SURFACE_2, RoundedCornerShape(12.dp))
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp),
                            ) {
                                Text(
                                    info.notes.replace("\r\n", "\n").trim(),
                                    fontSize = 12.sp,
                                    color = C_TEXT_MAIN,
                                    lineHeight = 18.sp,
                                )
                            }
                        }
                        if (info.apkUrl == null) {
                            Spacer(Modifier.height(10.dp))
                            Text(t.str("no_apk"), fontSize = 12.sp, color = C_ORANGE)
                        }
                        if (downloading || progress >= 0) {
                            Spacer(Modifier.height(14.dp))
                            LinearProgressIndicator(
                                progress = { if (progress < 0) 0f else progress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = C_PRIMARY,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${t.str("downloading")} $progress%",
                                fontSize = 12.sp,
                                color = C_TEXT,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                available != null && available.info.apkUrl != null -> {
                    TextButton(
                        enabled = !downloading,
                        onClick = { startDownload(available.info) },
                    ) {
                        Text(
                            if (downloading) t.str("downloading") else t.str("download_install"),
                            color = if (downloading) C_TEXT else C_PRIMARY,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                available != null -> {
                    TextButton(onClick = { UpdateChecker.openUrl(ctx, available.info.htmlUrl) }) {
                        Text(t.str("view_release"), color = C_PRIMARY, fontWeight = FontWeight.SemiBold)
                    }
                }
                r is UpdateChecker.Result.Failed -> {
                    TextButton(onClick = { attempt++ }) {
                        Text(t.str("retry"), color = C_PRIMARY, fontWeight = FontWeight.SemiBold)
                    }
                }
                else -> {
                    TextButton(onClick = onDismiss) {
                        Text(t.str("ok"), color = C_PRIMARY, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        dismissButton = {
            when {
                available != null -> {
                    Row {
                        TextButton(onClick = { UpdateChecker.openUrl(ctx, available.info.htmlUrl) }) {
                            Text(t.str("view_release"), color = C_TEXT)
                        }
                        TextButton(enabled = !downloading, onClick = onDismiss) {
                            Text(t.str("later"), color = C_TEXT)
                        }
                    }
                }
                r is UpdateChecker.Result.Failed -> {
                    TextButton(onClick = { UpdateChecker.openUrl(ctx, UpdateChecker.REPO_URL) }) {
                        Text(t.str("view_release"), color = C_TEXT)
                    }
                }
                else -> Unit
            }
        },
    )
}
