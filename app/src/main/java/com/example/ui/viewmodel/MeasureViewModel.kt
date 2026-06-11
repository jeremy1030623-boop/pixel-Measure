package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.MeasureDatabase
import com.example.data.model.MeasureRecord
import com.example.data.repository.MeasureRepository
import com.example.logic.ARMeasureEngine
import com.example.logic.ArCoreSessionHelper
import com.example.logic.TrigonometricMeasureEngine
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

class MeasureViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val database = MeasureDatabase.getDatabase(application)
    private val repository = MeasureRepository(database.measureDao())
    
    // 測量引擎實作（預設使用三角運算）
    private val measureEngine: ARMeasureEngine = TrigonometricMeasureEngine()
    
    // ARCore Session 助手
    private var arCoreSessionHelper: ArCoreSessionHelper? = null
    val arSession: Session? get() = arCoreSessionHelper?.session
    
    private var latestFrame: Frame? = null

    fun updateDisplayGeometry(rotation: Int, width: Int, height: Int) {
        arCoreSessionHelper?.setDisplayGeometry(rotation, width, height)
    }
    
    private val _arTrackingState = MutableStateFlow(TrackingState.STOPPED)
    val arTrackingState: StateFlow<TrackingState> = _arTrackingState.asStateFlow()
    
    private val _arPointCloud = MutableStateFlow<FloatArray?>(null)
    val arPointCloud: StateFlow<FloatArray?> = _arPointCloud.asStateFlow()

    private val _arPlanes = mutableStateListOf<Plane>()
    val arPlanes: List<Plane> get() = _arPlanes

    private val _viewMatrix = MutableStateFlow(FloatArray(16))
    val viewMatrix: StateFlow<FloatArray> = _viewMatrix.asStateFlow()

    private val _projectionMatrix = MutableStateFlow(FloatArray(16))
    val projectionMatrix: StateFlow<FloatArray> = _projectionMatrix.asStateFlow()

    fun updateArFrame(frame: Frame) {
        latestFrame = frame
        _arTrackingState.value = frame.camera.trackingState
        
        // Update matrices
        val camera = frame.camera
        camera.getViewMatrix(_viewMatrix.value, 0)
        camera.getProjectionMatrix(_projectionMatrix.value, 0, 0.1f, 100.0f)
        
        // Update Point Cloud
        try {
            val pointCloud = frame.acquirePointCloud()
            val points = FloatArray(pointCloud.points.remaining())
            pointCloud.points.get(points)
            _arPointCloud.value = points
            pointCloud.release()
        } catch (e: Exception) {}
        
        // Update Planes & Auto-calibrate Height
        val allPlanes = arSession?.getAllTrackables(Plane::class.java)
        if (allPlanes != null) {
            _arPlanes.clear()
            val trackingPlanes = allPlanes.filter { it.trackingState == TrackingState.TRACKING }
            _arPlanes.addAll(trackingPlanes)
            
            // Auto-calibration: detect ground plane to fix holding height
            val groundPlane = trackingPlanes.firstOrNull { 
                it.type == Plane.Type.HORIZONTAL_UPWARD_FACING && 
                it.centerPose.ty() < frame.camera.pose.ty() // Must be below camera
            }
            
            if (groundPlane != null) {
                // Distance from camera Y to plane Y
                val verticalDist = abs(frame.camera.pose.ty() - groundPlane.centerPose.ty())
                if (verticalDist in 0.5..2.5) { // Safe range for human height
                    val newHeightCm = (verticalDist * 100.0).toFloat()
                    // Smooth update
                    _cameraHeightCm.value = _cameraHeightCm.value * 0.95f + newHeightCm * 0.05f
                }
            }
        }
    }

    // Saved database records
    val savedRecords: StateFlow<List<MeasureRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // App state flows
    private val _currentMode = MutableStateFlow(0) // 0: 相機 AR 測量, 1: 螢幕直尺, 2: 泡泡水平儀
    val currentMode: StateFlow<Int> = _currentMode.asStateFlow()

    private val _selectedUnit = MutableStateFlow("cm") // "cm", "m", "in", "ft"
    val selectedUnit: StateFlow<String> = _selectedUnit.asStateFlow()

    private val _cameraHeightCm = MutableStateFlow(140f) // 預設持手機高度 140 公分
    val cameraHeightCm: StateFlow<Float> = _cameraHeightCm.asStateFlow()

    private val _sensorAlpha = MutableStateFlow(0.2f)
    val sensorAlpha: StateFlow<Float> = _sensorAlpha.asStateFlow()

    private val _vibrateOnAlignment = MutableStateFlow(true)
    val vibrateOnAlignment: StateFlow<Boolean> = _vibrateOnAlignment.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(true)
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    private val _isFirstTimeUser = MutableStateFlow(true)
    val isFirstTimeUser: StateFlow<Boolean> = _isFirstTimeUser.asStateFlow()

    private val _showSplashScreen = MutableStateFlow(true)
    val showSplashScreen: StateFlow<Boolean> = _showSplashScreen.asStateFlow()

    // 測量模式: 0 = 水平地面投影測量 (Ground Plane), 1 = 垂直高度測量 (Vertical Height Tool)
    private val _cameraMeasureSubMode = MutableStateFlow(0)
    val cameraMeasureSubMode: StateFlow<Int> = _cameraMeasureSubMode.asStateFlow()

    // 垂直高度鎖定的底座距離
    private val _lockedBaseDistance = MutableStateFlow<Double?>(null)
    val lockedBaseDistance: StateFlow<Double?> = _lockedBaseDistance.asStateFlow()

    // ARCore Engine states
    private val _arCoreState = MutableStateFlow("UNKNOWN") // "CHECKING", "SUPPORTED_INSTALLED", "UNSUPPORTED", "APK_NOT_INSTALLED"
    val arCoreState: StateFlow<String> = _arCoreState.asStateFlow()

    private val _arCoreActive = MutableStateFlow(true) // Auto enable standard AR space positioning
    val arCoreActive: StateFlow<Boolean> = _arCoreActive.asStateFlow()

    private fun isEmulator(): Boolean {
        val brand = android.os.Build.BRAND
        val device = android.os.Build.DEVICE
        val model = android.os.Build.MODEL
        val product = android.os.Build.PRODUCT
        val hardware = android.os.Build.HARDWARE
        val fingerprint = android.os.Build.FINGERPRINT
        return brand.startsWith("generic") || 
                device.startsWith("generic") || 
                model.contains("google_sdk") || 
                model.contains("Emulator") || 
                model.contains("Android SDK built for x86") || 
                product.contains("sdk_google") || 
                hardware.contains("goldfish") || 
                hardware.contains("ranchu") || 
                fingerprint.startsWith("generic")
    }

    fun checkArCoreSupport(context: Context) {
        if (isEmulator()) {
            _arCoreState.value = "UNSUPPORTED"
            return
        }

        if (arCoreSessionHelper == null) {
            arCoreSessionHelper = ArCoreSessionHelper(context)
        }
        
        viewModelScope.launch {
            try {
                val availability = ArCoreApk.getInstance().checkAvailability(context)
                if (availability.isTransient) {
                    _arCoreState.value = "CHECKING"
                } else if (availability.isSupported) {
                    if (availability.name == "SUPPORTED_INSTALLED") {
                        val session = arCoreSessionHelper?.createSession()
                        if (session != null) {
                            _arCoreState.value = "SUPPORTED_INSTALLED"
                        } else {
                            _arCoreState.value = "UNSUPPORTED"
                        }
                    } else {
                        _arCoreState.value = "APK_NOT_INSTALLED"
                    }
                } else {
                    _arCoreState.value = "UNSUPPORTED"
                }
            } catch (e: Exception) {
                _arCoreState.value = "UNSUPPORTED"
            }
        }
    }

    fun onResume() {
        arCoreSessionHelper?.resume()
        startListening()
    }

    fun onPause() {
        arCoreSessionHelper?.pause()
        stopListening()
    }

    fun destroyArCore() {
        arCoreSessionHelper?.destroy()
        arCoreSessionHelper = null
    }

    fun setArCoreActive(active: Boolean) {
        _arCoreActive.value = active
    }

    // Sensor calibration offsets
    private val _pitchOffset = MutableStateFlow(0f)
    private val _rollOffset = MutableStateFlow(0f)
    private val _yawOffset = MutableStateFlow(0f)

    private val prefs = application.getSharedPreferences("measure_prefs", Context.MODE_PRIVATE)

    init {
        // Load calibration data
        _pitchOffset.value = prefs.getFloat("pitch_offset", 0f)
        _rollOffset.value = prefs.getFloat("roll_offset", 0f)
        _yawOffset.value = prefs.getFloat("yaw_offset", 0f)

        // Load settings data
        _selectedUnit.value = prefs.getString("selected_unit", "cm") ?: "cm"
        _cameraHeightCm.value = prefs.getFloat("default_camera_height_cm", 140f)
        _sensorAlpha.value = prefs.getFloat("sensor_alpha", 0.2f)
        _vibrateOnAlignment.value = prefs.getBoolean("vibrate_on_alignment", true)
        _dynamicColorEnabled.value = prefs.getBoolean("dynamic_color_enabled", true)
        _isFirstTimeUser.value = prefs.getBoolean("is_first_time_user", true)
    }

    fun calibrateSensors() {
        // If we calibrate, unfreeze for 0.5s to capture new zero
        _isLevelFrozen.value = false
        
        // Get the current values to use as the new zero-reference
        // Since _pitch etc are already calibrated, we add back the old offset to get the raw value
        val rawPitch = _pitch.value + _pitchOffset.value
        val rawRoll = _roll.value + _rollOffset.value
        val rawYaw = _yaw.value + _yawOffset.value

        _pitchOffset.value = rawPitch
        _rollOffset.value = rawRoll
        _yawOffset.value = rawYaw
        
        // Immediate UI feedback: reset flows to 0 so it doesn't wait for smoothing
        _pitch.value = 0f
        _roll.value = 0f
        _yaw.value = 0f
        
        viewModelScope.launch {
            delay(500)
            if (_currentMode.value == 2) {
                _isLevelFrozen.value = true
            }
        }

        prefs.edit().apply {
            putFloat("pitch_offset", _pitchOffset.value)
            putFloat("roll_offset", _rollOffset.value)
            putFloat("yaw_offset", _yawOffset.value)
            apply()
        }
    }

    fun resetCalibration() {
        _pitchOffset.value = 0f
        _rollOffset.value = 0f
        _yawOffset.value = 0f
        // Let the next sensor reading update the flows naturally
        prefs.edit().clear().apply()
    }

    // Raw Sensor state
    private val _pitch = MutableStateFlow(0f)  // degree (-90 is straight down, 0 is flat looking ahead)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _roll = MutableStateFlow(0f)   // degree (tilting left/right)
    val roll: StateFlow<Float> = _roll.asStateFlow()

    private val _yaw = MutableStateFlow(0f)     // azimuth heading (yaw angle)
    val yaw: StateFlow<Float> = _yaw.asStateFlow()

    // Current camera measurement points clicked
    val capturedPoints = mutableStateListOf<Point3D>()

    // Total length of the path formed by captured points
    val totalPathDistance: Double
        get() {
            if (capturedPoints.size < 2) return 0.0
            var sum = 0.0
            for (i in 0 until capturedPoints.size - 1) {
                sum += measureEngine.calculateDistance(capturedPoints[i], capturedPoints[i+1])
            }
            return sum
        }

    // Pocket Ruler Callipers (State in DP offset from center)
    private val _rulerCaliperLeft = MutableStateFlow(-120.0f)
    val rulerCaliperLeft: StateFlow<Float> = _rulerCaliperLeft.asStateFlow()

    private val _rulerCaliperRight = MutableStateFlow(120.0f)
    val rulerCaliperRight: StateFlow<Float> = _rulerCaliperRight.asStateFlow()

    // Sensor support structure
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val processedRotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    
    private val fastAlpha = 0.8f // Faster response when moving
    
    private var hasAccelerometer = false
    private var hasMagnetometer = false
    private var hasRotationVector = false
    private var hasGravity = false

    private val _isLevelFrozen = MutableStateFlow(false)
    val isLevelFrozen: StateFlow<Boolean> = _isLevelFrozen.asStateFlow()

    fun setMode(mode: Int) {
        _currentMode.value = mode
        
        // Specific requirement: Surface level only moves for 0.5s then freezes
        if (mode == 2) {
            _isLevelFrozen.value = false
            viewModelScope.launch {
                delay(500)
                _isLevelFrozen.value = true
            }
        } else {
            _isLevelFrozen.value = false
        }
    }

    fun setUnit(unit: String) {
        _selectedUnit.value = unit
        prefs.edit().putString("selected_unit", unit).apply()
    }

    fun setCameraHeight(height: Float) {
        _cameraHeightCm.value = height
        prefs.edit().putFloat("default_camera_height_cm", height).apply()
    }

    fun setSensorAlpha(alpha: Float) {
        _sensorAlpha.value = alpha
        prefs.edit().putFloat("sensor_alpha", alpha).apply()
    }

    fun setVibrateOnAlignment(enabled: Boolean) {
        _vibrateOnAlignment.value = enabled
        prefs.edit().putBoolean("vibrate_on_alignment", enabled).apply()
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        _dynamicColorEnabled.value = enabled
        prefs.edit().putBoolean("dynamic_color_enabled", enabled).apply()
    }

    fun setFirstTimeUser(enabled: Boolean) {
        _isFirstTimeUser.value = enabled
        prefs.edit().putBoolean("is_first_time_user", enabled).apply()
    }

    fun dismissSplashScreen() {
        _showSplashScreen.value = false
    }

    fun setCameraMeasureSubMode(subMode: Int) {
        _cameraMeasureSubMode.value = subMode
        clearActivePoints()
    }

    fun updateRulerCalipers(left: Float, right: Float) {
        _rulerCaliperLeft.value = left
        _rulerCaliperRight.value = right
    }

    // Capture standard point (Accepts specific hit coordinates in pixels)
    fun addPoint(pixelX: Float? = null, pixelY: Float? = null) {
        val currentPitch = _pitch.value
        val currentYaw = _yaw.value
        val camHeightM = cameraHeightCm.value / 100.0
        
        // 嘗試使用 ARCore HitTest
        var arPoint: Point3D? = null
        if (_arCoreActive.value && _arCoreState.value == "SUPPORTED_INSTALLED") {
            val session = arSession
            val frame = latestFrame
            
            if (session != null && frame != null) {
                // If coordinates are provided, use them; otherwise default to center of frame
                val hX = pixelX ?: (frame.camera.imageIntrinsics.imageDimensions[0] / 2f)
                val hY = pixelY ?: (frame.camera.imageIntrinsics.imageDimensions[1] / 2f)
                
                val hits = frame.hitTest(hX, hY)
                val hit = hits.firstOrNull { 
                    val trackable = it.trackable
                    (trackable is Plane && trackable.isPoseInPolygon(it.hitPose)) || 
                    (it.trackable is com.google.ar.core.Point)
                }
                
                if (hit != null) {
                    val pose = hit.hitPose
                    arPoint = Point3D(
                        x = pose.tx().toDouble(),
                        y = pose.ty().toDouble(),
                        z = pose.tz().toDouble(),
                        pitch = currentPitch,
                        yaw = currentYaw,
                        isArPrecision = true
                    )
                }
            }
        }

        val point = arPoint ?: if (_cameraMeasureSubMode.value == 0) {
            // Ground project
            measureEngine.calculateGroundPoint(currentPitch, currentYaw, camHeightM)
        } else {
            // Height mode: base locking
            val baseDist = _lockedBaseDistance.value
            if (baseDist == null) {
                // First click: Lock the base
                val groundPoint = measureEngine.calculateGroundPoint(currentPitch, currentYaw, camHeightM)
                _lockedBaseDistance.value = sqrt(groundPoint.x.pow(2) + groundPoint.y.pow(2))
                groundPoint
            } else {
                // Second click: lock top altitude
                val zHeight = measureEngine.calculateVerticalHeight(baseDist, currentPitch, camHeightM)
                val yawRad = Math.toRadians(currentYaw.toDouble())
                Point3D(
                    x = baseDist * sin(yawRad),
                    y = baseDist * cos(yawRad),
                    z = zHeight,
                    pitch = currentPitch,
                    yaw = currentYaw
                )
            }
        }
        
        capturedPoints.add(point)
    }

    fun removeLastPoint() {
        if (capturedPoints.isNotEmpty()) {
            capturedPoints.removeAt(capturedPoints.size - 1)
            if (capturedPoints.isEmpty()) {
                _lockedBaseDistance.value = null
            }
        }
    }

    fun clearActivePoints() {
        capturedPoints.clear()
        _lockedBaseDistance.value = null
    }

    fun updatePointLabel(index: Int, newLabel: String) {
        if (index in capturedPoints.indices) {
            capturedPoints[index] = capturedPoints[index].copy(label = newLabel)
        }
    }

    // Realtime distance estimation to current crosshair
    fun getLiveDistanceText(): String {
        val currentPitch = _pitch.value
        val currentYaw = _yaw.value
        val camHeightM = cameraHeightCm.value / 100.0
        
        if (_cameraMeasureSubMode.value == 0) {
            // Horizontal multi-point distance
            val currentGroundPoint = measureEngine.calculateGroundPoint(currentPitch, currentYaw, camHeightM)
            
            if (capturedPoints.isEmpty()) {
                // Distance to target point on ground
                val d = sqrt(currentGroundPoint.x.pow(2) + currentGroundPoint.y.pow(2))
                return formatLengthValue(d)
            } else {
                // Distance from last point to live target
                val lastPoint = capturedPoints.last()
                val liveDist = measureEngine.calculateDistance(lastPoint, currentGroundPoint)
                return formatLengthValue(liveDist)
            }
        } else {
            // Altitude height meter
            val baseDist = _lockedBaseDistance.value
            if (baseDist == null) {
                return "請瞄準底部並點擊 +"
            } else {
                val zHeight = measureEngine.calculateVerticalHeight(baseDist, currentPitch, camHeightM)
                return formatLengthValue(zHeight)
            }
        }
    }

    fun saveCurrentMeasurement(customLabel: String? = null, customNotes: String? = null) {
        viewModelScope.launch {
            val label = customLabel ?: if (_cameraMeasureSubMode.value == 0) {
                if (capturedPoints.size >= 2) "測量長度" else "地面距離"
            } else "高度"
            
            var measuredValue = 0.0
            if (_cameraMeasureSubMode.value == 0) {
                if (capturedPoints.size >= 2) {
                    // Sum distances up
                    var accum = 0.0
                    for (i in 0 until capturedPoints.size - 1) {
                        val p1 = capturedPoints[i]
                        val p2 = capturedPoints[i + 1]
                        accum += measureEngine.calculateDistance(p1, p2)
                    }
                    measuredValue = accum
                } else if (capturedPoints.size == 1) {
                    val p = capturedPoints[0]
                    measuredValue = sqrt(p.x * p.x + p.y * p.y)
                }
            } else {
                // Height mode
                if (capturedPoints.size >= 2) {
                    // Difference between bottom and top Z
                    val bottomVal = capturedPoints.first().z
                    val topVal = capturedPoints.last().z
                    measuredValue = abs(topVal - bottomVal)
                } else if (capturedPoints.size == 1) {
                    // Just relative to locked ground base
                    val currentPitch = _pitch.value
                    val camHeightM = cameraHeightCm.value / 100.0
                    val base = _lockedBaseDistance.value ?: 0.0
                    measuredValue = measureEngine.calculateVerticalHeight(base, currentPitch, camHeightM)
                }
            }

            if (measuredValue > 0.0) {
                // Convert from base meters to CM
                val centimeters = measuredValue * 100.0
                repository.insert(
                    MeasureRecord(
                        title = label,
                        value = convertCmToSelected(centimeters, _selectedUnit.value),
                        unit = _selectedUnit.value,
                        type = "CAM",
                        notes = customNotes,
                        pointsData = capturedPoints.serializePoints()
                    )
                )
                clearActivePoints()
            }
        }
    }

    fun saveRulerMeasurement(title: String, cmVal: Double) {
        viewModelScope.launch {
            repository.insert(
                MeasureRecord(
                    title = title,
                    value = convertCmToSelected(cmVal, _selectedUnit.value),
                    unit = _selectedUnit.value,
                    type = "RULER"
                )
            )
        }
    }

    fun deleteRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllRecords() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // Helper functions for unit conversion
    fun convertCmToSelected(cm: Double, unit: String): Double {
        return when (unit) {
            "cm" -> cm
            "m" -> cm / 100.0
            "in" -> cm / 2.54
            "ft" -> cm / 30.48
            else -> cm
        }
    }

    fun formatLengthValue(meters: Double): String {
        val cm = meters * 100.0
        val converted = convertCmToSelected(cm, _selectedUnit.value)
        return String.format("%.1f %s", converted, _selectedUnit.value)
    }

    private var isListening = false

    // Sensor Registration Lifecycle
    fun startListening() {
        if (isListening) return
        isListening = true
        
        // Pixel Optimization: Prioritize high-precision Fused Rotation Vector
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVector != null) {
            sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_FASTEST)
            hasRotationVector = true
            return 
        }

        // Alternative optimization: Geomagnetic Rotation Vector
        val geoRotation = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        if (geoRotation != null) {
            sensorManager.registerListener(this, geoRotation, SensorManager.SENSOR_DELAY_FASTEST)
            hasRotationVector = true
            return
        }

        // Fallback to Gravity (more stable than Accelerometer for level tools)
        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (gravity != null) {
            sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_FASTEST)
            hasGravity = true
        } else {
            val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accel != null) {
                sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST)
                hasAccelerometer = true
            }
        }

        val magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (magnet != null) {
            sensorManager.registerListener(this, magnet, SensorManager.SENSOR_DELAY_FASTEST)
            hasMagnetometer = true
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        isListening = false
        hasRotationVector = false
        hasAccelerometer = false
        hasMagnetometer = false
        hasGravity = false
    }

    private fun applyLowPassFilter(input: FloatArray, output: FloatArray) {
        val alpha = _sensorAlpha.value
        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR || 
            event.sensor.type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            remapAndProcess()
            return
        }

        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                applyLowPassFilter(event.values, accelerometerReading)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                applyLowPassFilter(event.values, magnetometerReading)
            }
        }

        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        if (success) {
            remapAndProcess()
        } else {
            processFallbackOrientation()
        }
    }

    private fun remapAndProcess() {
        val display = (getApplication<Application>().getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay
        val rotation = display.rotation
        
        var axisX = SensorManager.AXIS_X
        var axisY = SensorManager.AXIS_Y
        
        when (rotation) {
            android.view.Surface.ROTATION_0 -> {
                axisX = SensorManager.AXIS_X
                axisY = SensorManager.AXIS_Y
            }
            android.view.Surface.ROTATION_90 -> {
                axisX = SensorManager.AXIS_Y
                axisY = SensorManager.AXIS_MINUS_X
            }
            android.view.Surface.ROTATION_180 -> {
                axisX = SensorManager.AXIS_MINUS_X
                axisY = SensorManager.AXIS_MINUS_Y
            }
            android.view.Surface.ROTATION_270 -> {
                axisX = SensorManager.AXIS_MINUS_Y
                axisY = SensorManager.AXIS_X
            }
        }
        
        if (SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, processedRotationMatrix)) {
            processRotationMatrix(processedRotationMatrix)
        } else {
            processRotationMatrix(rotationMatrix)
        }
    }

    private fun processRotationMatrix(matrix: FloatArray) {
        // If surface level is frozen, skip updates
        if (_currentMode.value == 2 && _isLevelFrozen.value) return
        
        SensorManager.getOrientation(matrix, orientationAngles)
        
        // azimuth (yaw)
        var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        if (azimuth < 0) azimuth += 360f
        
        val newYaw = azimuth - _yawOffset.value
        val yawDiff = abs(newYaw - _yaw.value)
        val yawAlpha = if (yawDiff > 10f) fastAlpha else _sensorAlpha.value
        _yaw.value = _yaw.value + yawAlpha * (newYaw - _yaw.value)

        // pitch (around X axis)
        val newPitchRaw = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val calibratedPitch = newPitchRaw - _pitchOffset.value
        val pitchDiff = abs(calibratedPitch - _pitch.value)
        val pitchAlpha = if (pitchDiff > 5f) fastAlpha else _sensorAlpha.value
        _pitch.value = _pitch.value + pitchAlpha * (calibratedPitch - _pitch.value)
        
        // roll (around Y axis)
        val newRollRaw = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
        val calibratedRoll = newRollRaw - _rollOffset.value
        val rollDiff = abs(calibratedRoll - _roll.value)
        val rollAlpha = if (rollDiff > 5f) fastAlpha else _sensorAlpha.value
        _roll.value = _roll.value + rollAlpha * (calibratedRoll - _roll.value)
    }

    private fun processFallbackOrientation() {
        val ax = accelerometerReading[0]
        val ay = accelerometerReading[1]
        val az = accelerometerReading[2]
        val norm = sqrt((ax * ax + ay * ay + az * az).toDouble())
        if (norm > 0) {
            val x = ax / norm
            val y = ay / norm
            val z = az / norm

            val alpha = _sensorAlpha.value
            val computedPitch = -asin(y) * (180.0 / Math.PI)
            val calibratedPitch = computedPitch.toFloat() - _pitchOffset.value
            _pitch.value = _pitch.value + alpha * (calibratedPitch - _pitch.value)

            val computedRoll = atan2(x, z) * (180.0 / Math.PI)
            val calibratedRoll = computedRoll.toFloat() - _rollOffset.value
            _roll.value = _roll.value + alpha * (calibratedRoll - _roll.value)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
