package com.example.logic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.ui.viewmodel.Point3D
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*

/**
 * 助手類別，用於管理 ARCore Session 的生命週期與配置
 */
class ArCoreSessionHelper(private val context: Context) {
    var session: Session? = null
        private set

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 初始化或恢復 ARCore Session
     */
    fun createSession(): Session? {
        if (!hasCameraPermission()) {
            Log.e("ArCoreSessionHelper", "Cannot create session without camera permission")
            return null
        }
        if (session != null) return session

        try {
            // 檢查 ARCore 狀態
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            if (!availability.isSupported) {
                Log.e("ArCoreSessionHelper", "ARCore is not supported on this device")
                return null
            }

            // 建立 Session
            session = Session(context)
            
            // 配置 Session
            val config = Config(session)
            // 開啟自動對焦
            config.focusMode = Config.FocusMode.AUTO
            // 開啟深度與平面追蹤
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            if (session?.isDepthModeSupported(Config.DepthMode.AUTOMATIC) == true) {
                config.depthMode = Config.DepthMode.AUTOMATIC
            }
            
            session?.configure(config)
            session?.resume()
            
            return session
        } catch (e: Exception) {
            Log.e("ArCoreSessionHelper", "Failed to create ARCore session", e)
            return null
        }
    }

    fun pause() {
        session?.pause()
    }

    fun resume() {
        if (!hasCameraPermission()) return
        try {
            session?.resume()
        } catch (e: Exception) {
            Log.e("ArCoreSessionHelper", "Failed to resume session", e)
        }
    }

    fun setDisplayGeometry(rotation: Int, width: Int, height: Int) {
        session?.setDisplayGeometry(rotation, width, height)
    }

    fun destroy() {
        session?.close()
        session = null
    }
}
