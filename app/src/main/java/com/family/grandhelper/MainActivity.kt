package com.family.grandhelper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.family.grandhelper.service.OverlayService

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_PERMISSIONS = 100
        private const val REQUEST_OVERLAY = 101
    }

    private lateinit var permissionList: LinearLayout
    private lateinit var btnStart: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 측면 버튼으로 재실행 시: 퍼미션 OK + 서비스 실행 중이면 토글 후 즉시 종료
        if (checkAllPermissions() && OverlayService.isRunning) {
            toggleOverlay()
            return
        }

        setContentView(R.layout.activity_main)

        permissionList = findViewById(R.id.permission_list)
        btnStart = findViewById(R.id.btn_start)

        btnStart.setOnClickListener {
            if (checkAllPermissions()) {
                startOverlayService()
            } else {
                requestMissingPermissions()
            }
        }

        updatePermissionStatus()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (checkAllPermissions() && OverlayService.isRunning) {
            toggleOverlay()
        }
    }

    private fun toggleOverlay() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_TOGGLE
        }
        startForegroundService(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        permissionList.removeAllViews()

        val permissions = listOf(
            PermissionItem(getString(R.string.permission_overlay), Settings.canDrawOverlays(this)),
            PermissionItem(getString(R.string.permission_microphone), hasPermission(Manifest.permission.RECORD_AUDIO)),
            PermissionItem(getString(R.string.permission_notification), hasPermission(Manifest.permission.POST_NOTIFICATIONS)),
            PermissionItem(getString(R.string.permission_contacts), hasPermission(Manifest.permission.READ_CONTACTS)),
            PermissionItem(getString(R.string.permission_phone), hasPermission(Manifest.permission.CALL_PHONE)),
        )

        for (item in permissions) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 16, 0, 16)
            }

            val label = TextView(this).apply {
                text = item.name
                textSize = 18f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_white))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val status = TextView(this).apply {
                text = if (item.granted) getString(R.string.permission_granted) else getString(R.string.permission_denied)
                textSize = 14f
                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        if (item.granted) R.color.success else R.color.primary
                    )
                )
            }

            row.addView(label)
            row.addView(status)
            permissionList.addView(row)
        }
    }

    private fun checkAllPermissions(): Boolean {
        return Settings.canDrawOverlays(this)
            && hasPermission(Manifest.permission.RECORD_AUDIO)
            && hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestMissingPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_OVERLAY)
            return
        }

        val needed = mutableListOf<String>()
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            needed.add(Manifest.permission.READ_CONTACTS)
        }
        if (!hasPermission(Manifest.permission.CALL_PHONE)) {
            needed.add(Manifest.permission.CALL_PHONE)
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updatePermissionStatus()
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        startForegroundService(intent)
        finish()
    }

    private data class PermissionItem(val name: String, val granted: Boolean)
}
