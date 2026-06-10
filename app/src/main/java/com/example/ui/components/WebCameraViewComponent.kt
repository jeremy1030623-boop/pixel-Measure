package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.*
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.viewmodel.MeasureViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WebCameraViewComponent(
    viewModel: MeasureViewModel
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    val aiResult by viewModel.aiResult.collectAsState()
    val isAiProcessing by viewModel.isAiProcessing.collectAsState()

    if (cameraPermissionState.status.isGranted) {
        val webView = remember {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.allowFileAccess = true
                
                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
                        request.grant(request.resources)
                    }
                }
                
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onPointClicked(x: Float, y: Float) {
                        viewModel.estimateDistanceByAi()
                    }
                }, "AndroidApp")

                val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <script src="https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.2" crossorigin="anonymous"></script>
                        <style>
                            body, html { margin: 0; padding: 0; overflow: hidden; background: #000; width: 100%; height: 100%; font-family: sans-serif; }
                            #video { width: 100%; height: 100%; object-fit: cover; position: absolute; }
                            #canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none; }
                            #overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; cursor: crosshair; z-index: 10; }
                            #msg { position: absolute; bottom: 120px; width: 100%; text-align: center; color: #fff; text-shadow: 0 2px 4px rgba(0,0,0,0.8); pointer-events: none; z-index: 20; font-size: 14px; }
                            .loading { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: #fff; z-index: 30; }
                        </style>
                    </head>
                    <body>
                        <video id="video" autoplay playsinline muted></video>
                        <canvas id="canvas"></canvas>
                        <div id="overlay" onclick="handleClick(event)"></div>
                        <div id="msg">正在啟動 MediaPipe 空間感知引擎...</div>
                        <div id="loading" class="loading">加載 AI 模型中...</div>

                        <script>
                            const video = document.getElementById('video');
                            const canvas = document.getElementById('canvas');
                            const ctx = canvas.getContext('2d');
                            const loading = document.getElementById('loading');
                            const msg = document.getElementById('msg');
                            
                            let objectDetector;
                            let runningMode = "IMAGE";

                            async function initialize() {
                                try {
                                    const vision = await createFilesetResolver();
                                    objectDetector = await ObjectDetector.createFromOptions(vision, {
                                        baseOptions: {
                                            modelAssetPath: "https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/float16/1/efficientdet_lite0.tflite",
                                            delegate: "GPU"
                                        },
                                        scoreThreshold: 0.5,
                                        runningMode: "VIDEO"
                                    });
                                    loading.style.display = 'none';
                                    msg.innerText = "MediaPipe AR 已就緒，點擊物體進行測量";
                                    startCamera();
                                } catch (e) {
                                    console.error(e);
                                    msg.innerText = "AI 引擎啟動失敗: " + e.message;
                                }
                            }

                            function startCamera() {
                                navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
                                    .then(stream => { 
                                        window.stream = stream;
                                        video.srcObject = stream; 
                                        video.addEventListener("loadeddata", () => {
                                            canvas.width = video.videoWidth;
                                            canvas.height = video.videoHeight;
                                            predictWebcam();
                                        });
                                    })
                                    .catch(err => { 
                                        console.error('Error accessing camera:', err); 
                                        msg.innerText = "無法訪問相機";
                                    });
                            }

                            let lastVideoTime = -1;
                            async function predictWebcam() {
                                if (video.currentTime !== lastVideoTime) {
                                    lastVideoTime = video.currentTime;
                                    const startTimeMs = performance.now();
                                    const detections = await objectDetector.detectForVideo(video, startTimeMs);
                                    drawDetections(detections);
                                }
                                window.requestAnimationFrame(predictWebcam);
                            }

                            function drawDetections(result) {
                                ctx.clearRect(0, 0, canvas.width, canvas.height);
                                for (const detection of result.detections) {
                                    const { originX, originY, width, height } = detection.boundingBox;
                                    const label = detection.categories[0].categoryName;
                                    
                                    ctx.strokeStyle = "#0EA5E9";
                                    ctx.lineWidth = 4;
                                    ctx.strokeRect(originX, originY, width, height);
                                    
                                    ctx.fillStyle = "#0EA5E9";
                                    ctx.font = "bold 40px sans-serif";
                                    ctx.fillText(label, originX, originY > 50 ? originY - 10 : originY + 40);
                                    
                                    ctx.beginPath();
                                    ctx.setLineDash([10, 10]);
                                    ctx.moveTo(originX, originY + height / 2);
                                    ctx.lineTo(originX + width, originY + height / 2);
                                    ctx.stroke();
                                    ctx.setLineDash([]);
                                }
                            }

                            async function createFilesetResolver() {
                                return await mediapipe.tasks.vision.FilesetResolver.forVisionTasks(
                                    "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.2/wasm"
                                );
                            }

                            const ObjectDetector = mediapipe.tasks.vision.ObjectDetector;

                            function handleClick(e) {
                                const rect = canvas.getBoundingClientRect();
                                const x = (e.clientX - rect.left) * (canvas.width / rect.width);
                                const y = (e.clientY - rect.top) * (canvas.height / rect.height);
                                
                                if (window.AndroidApp) {
                                    window.AndroidApp.onPointClicked(x, y);
                                }
                            }

                            initialize();
                        </script>
                    </body>
                    </html>
                """.trimIndent()
                loadDataWithBaseURL("https://app-measure.local", htmlContent, "text/html", "utf-8", null)
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                webView.evaluateJavascript("""
                    if (window.stream) {
                        window.stream.getTracks().forEach(track => track.stop());
                    }
                    if (typeof video !== 'undefined' && video && video.srcObject) {
                        video.srcObject.getTracks().forEach(track => track.stop());
                    }
                """.trimIndent(), null)
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            }
        }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { webView },
            update = { /* No-op, initialized in factory */ },
            modifier = Modifier.fillMaxSize()
        )

        // Scanning Line Effect
        val infiniteTransition = rememberInfiniteTransition(label = "Scanning")
        val scanProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scan"
        )
        
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val scanY = maxHeight * scanProgress
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .offset(y = scanY)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )
        }

        // AI Status HUD Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = isAiProcessing,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AI 正在分析深度空間中...", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            AnimatedVisibility(
                visible = aiResult != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("AI 測量結果", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = aiResult ?: "",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
    }
}
