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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    
    private val _arTrackingState = MutableStateFlow(TrackingState.STOPPED)
    val arTrackingState: StateFlow<TrackingState> = _arTrackingState.asStateFlow()
    
    fun updateArFrame(frame: Frame) {
        latestFrame = frame
        _arTrackingState.value = frame.camera.trackingState
    }

    // Saved database records
    val savedRecords: StateFlow<List<MeasureRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // App state flows
    private val _currentMode = MutableStateFlow(0) // 0: 相機 AR 測量, 1: 螢幕直尺, 2: 泡泡水平儀, 3: AI 智慧測量 (Web API)
    val currentMode: StateFlow<Int> = _currentMode.asStateFlow()

    private val _aiResult = MutableStateFlow<String?>(null)
    val aiResult: StateFlow<String?> = _aiResult.asStateFlow()

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _selectedUnit = MutableStateFlow("cm") // "cm", "m", "in", "ft"
    val selectedUnit: StateFlow<String> = _selectedUnit.asStateFlow()

    private val _cameraHeightCm = MutableStateFlow(140f) // 預設持手機高度 140 公分
    val cameraHeightCm: StateFlow<Float> = _cameraHeightCm.asStateFlow()

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

    fun checkArCoreSupport(context: Context) {
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
                        _arCoreState.value = "SUPPORTED_INSTALLED"
                        // 如果支援且已安裝，則嘗試啟動 Session
                        arCoreSessionHelper?.createSession()
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
    }

    fun calibrateSensors() {
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
    
    private val sensorAlpha = 0.2f // Base smoothing
    private val fastAlpha = 0.8f // Faster response when moving
    
    private var hasAccelerometer = false
    private var hasMagnetometer = false
    private var hasRotationVector = false
    private var hasGravity = false

    fun setMode(mode: Int) {
        _currentMode.value = mode
        if (mode != 3) _aiResult.value = null
    }

    // AI Distance Estimation (Real via Gemini)
    fun estimateDistanceByAi(base64Image: String? = null) {
        viewModelScope.launch {
            _isAiProcessing.value = true
            try {
                if (base64Image != null) {
                    val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                    val prompt = "請估算圖中點擊位置物體的距離。請僅返回一個估計的數值與單位（例如：1.5 公尺），不要返回其他文字。"
                    val request = com.example.logic.GenerateContentRequest(
                        contents = listOf(
                            com.example.logic.Content(
                                parts = listOf(
                                    com.example.logic.Part(text = prompt),
                                    com.example.logic.Part(inlineData = com.example.logic.InlineData(mimeType = "image/jpeg", data = base64Image))
                                )
                            )
                        ),
                        generationConfig = com.example.logic.GenerationConfig(temperature = 0.4f)
                    )
                    val response = com.example.logic.RetrofitClient.service.generateContent(apiKey, request)
                    val textResult = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    _aiResult.value = textResult ?: "無法估算距離"
                } else {
                    val d = getLiveDistanceText()
                    _aiResult.value = "預估距離: $d"
                }
            } catch (e: Exception) {
                _aiResult.value = "AI 服務暫時不可用: ${e.message}"
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun setUnit(unit: String) {
        _selectedUnit.value = unit
    }

    fun setCameraHeight(height: Float) {
        _cameraHeightCm.value = height
    }

    fun setCameraMeasureSubMode(subMode: Int) {
        _cameraMeasureSubMode.value = subMode
        clearActivePoints()
    }

    fun updateRulerCalipers(left: Float, right: Float) {
        _rulerCaliperLeft.value = left
        _rulerCaliperRight.value = right
    }

    // Capture standard point
    fun addPoint(screenWidth: Float? = null, screenHeight: Float? = null) {
        val currentPitch = _pitch.value
        val currentYaw = _yaw.value
        val camHeightM = cameraHeightCm.value / 100.0
        
        // 嘗試使用 ARCore HitTest
        var arPoint: Point3D? = null
        if (_arCoreActive.value && _arCoreState.value == "SUPPORTED_INSTALLED") {
            val session = arSession
            val frame = latestFrame
            if (session != null && frame != null && screenWidth != null && screenHeight != null) {
                val hits = frame.hitTest(screenWidth / 2f, screenHeight / 2f)
                val hit = hits.firstOrNull { 
                    val trackable = it.trackable
                    trackable is Plane && trackable.isPoseInPolygon(it.hitPose)
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

    // Sensor Registration Lifecycle
    fun startListening() {
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
        hasRotationVector = false
        hasAccelerometer = false
        hasMagnetometer = false
    }

    private fun applyLowPassFilter(input: FloatArray, output: FloatArray) {
        for (i in input.indices) {
            output[i] = output[i] + sensorAlpha * (input[i] - output[i])
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
        SensorManager.getOrientation(matrix, orientationAngles)
        
        // azimuth (yaw)
        var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        if (azimuth < 0) azimuth += 360f
        
        val newYaw = azimuth - _yawOffset.value
        val yawDiff = abs(newYaw - _yaw.value)
        val yawAlpha = if (yawDiff > 10f) fastAlpha else sensorAlpha
        _yaw.value = _yaw.value + yawAlpha * (newYaw - _yaw.value)

        // pitch (around X axis)
        val newPitchRaw = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val calibratedPitch = newPitchRaw - _pitchOffset.value
        val pitchDiff = abs(calibratedPitch - _pitch.value)
        val pitchAlpha = if (pitchDiff > 5f) fastAlpha else sensorAlpha
        _pitch.value = _pitch.value + pitchAlpha * (calibratedPitch - _pitch.value)
        
        // roll (around Y axis)
        val newRollRaw = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
        val calibratedRoll = newRollRaw - _rollOffset.value
        val rollDiff = abs(calibratedRoll - _roll.value)
        val rollAlpha = if (rollDiff > 5f) fastAlpha else sensorAlpha
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

            val computedPitch = -asin(y) * (180.0 / Math.PI)
            val calibratedPitch = computedPitch.toFloat() - _pitchOffset.value
            _pitch.value = _pitch.value + sensorAlpha * (calibratedPitch - _pitch.value)

            val computedRoll = atan2(x, z) * (180.0 / Math.PI)
            val calibratedRoll = computedRoll.toFloat() - _rollOffset.value
            _roll.value = _roll.value + sensorAlpha * (calibratedRoll - _roll.value)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
