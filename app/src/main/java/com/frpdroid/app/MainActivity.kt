package com.frpdroid.app

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or not */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = 0
        window.navigationBarColor = 0
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true

        // 保持与服务绑定（用于前台服务生命周期）
        bindService(
            Intent(this, FrpService::class.java),
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
                override fun onServiceDisconnected(name: ComponentName?) {}
            },
            BIND_AUTO_CREATE,
        )

        // Android 13+ 通知权限
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            AppMain()
        }
    }
}
