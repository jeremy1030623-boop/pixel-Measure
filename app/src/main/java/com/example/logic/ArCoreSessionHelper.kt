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
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import java.util.EnumSet
import com.google.ar.core.exceptions.*

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import com.example.ui.viewmodel.MeasureViewModel

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
            val sessionInstance = Session(context)
            session = sessionInstance
            
            // 嘗試設定 60 FPS 的相機輸出規格
            try {
                val filter = CameraConfigFilter(sessionInstance)
                filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_60))
                val cameraConfigs = sessionInstance.getSupportedCameraConfigs(filter)
                if (cameraConfigs.isNotEmpty()) {
                    sessionInstance.setCameraConfig(cameraConfigs[0])
                    Log.d("ArCoreSessionHelper", "Successfully configured ARCore camera config for 60 FPS!")
                } else {
                    Log.d("ArCoreSessionHelper", "60 FPS is not supported by ARCore on this device, using default configs")
                }
            } catch (e: Exception) {
                Log.e("ArCoreSessionHelper", "Failed to filter or apply 60 FPS CameraConfig", e)
            }
            
            // 配置 Session
            val config = Config(sessionInstance)
            // 開啟自動對焦
            config.focusMode = Config.FocusMode.AUTO
            // 開啟深度與平面追蹤
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            if (sessionInstance.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.depthMode = Config.DepthMode.AUTOMATIC
            }
            
            sessionInstance.configure(config)
            sessionInstance.setCameraTextureName(0)
            sessionInstance.resume()
            
            return sessionInstance
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

/**
 * 自訂的 GLSurfaceView，用於以高效率 60 FPS 且不與 CameraX 衝突的方式渲染 ARCore 相機畫面，
 * 同時作為主要的 AR 渲染與 Session frame 驅動源。
 */
class ArCoreGLView(
    context: Context,
    private val session: Session,
    private val viewModel: MeasureViewModel
) : GLSurfaceView(context), GLSurfaceView.Renderer {

    private var textureId = -1
    private var program = -1
    private var positionAttrib = -1
    private var texCoordAttrib = -1

    private val vertices = floatArrayOf(
        -1.0f, -1.0f, 0.0f,
         1.0f, -1.0f, 0.0f,
        -1.0f,  1.0f, 0.0f,
         1.0f,  1.0f, 0.0f
    )

    private val texCoords = floatArrayOf(
        0.0f, 1.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f
    )

    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertices)
            position(0)
        }

    private val texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(texCoords)
            position(0)
        }

    private val transformedTexCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        // 產生相機外部 OES 紋理
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // 設定 ARCore 的相機紋理
        session.setCameraTextureName(textureId)

        // 編譯頂點與片段著色器 (外部 OES 紋理專用)
        val vertexShaderSource = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """.trimIndent()

        val fragmentShaderSource = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES sTexture;
            void main() {
                gl_FragColor = texture2D(sTexture, v_TexCoord);
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        positionAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        texCoordAttrib = GLES20.glGetAttribLocation(program, "a_TexCoord")
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val activity = context as? android.app.Activity
        activity?.let {
            val display = it.windowManager.defaultDisplay
            viewModel.updateDisplayGeometry(display.rotation, width, height)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (textureId == -1 || program == -1) return

        try {
            // 更新並取得最新的 ARCore 畫面
            val frame = session.update()
            
            // 轉換紋理座標以避免拉伸 (適應當前寬高比)
            frame.transformDisplayUvCoords(texCoordBuffer, transformedTexCoordBuffer)
            
            // 將 Frame 更新上傳給 ViewModel，以供對應點雲、幾何和平面運算
            post {
                viewModel.updateArFrame(frame)
            }
        } catch (e: Exception) {
            return
        }

        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        GLES20.glEnableVertexAttribArray(positionAttrib)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordAttrib)
        GLES20.glVertexAttribPointer(texCoordAttrib, 2, GLES20.GL_FLOAT, false, 0, transformedTexCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionAttrib)
        GLES20.glDisableVertexAttribArray(texCoordAttrib)
    }
}
