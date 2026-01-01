package com.emotion.detection

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var emotionText: TextView
    private lateinit var confidenceText: TextView
    private lateinit var allConfidencesText: TextView
    private lateinit var startButton: Button

    private var classifier: EmotionClassifier? = null
    private var detectionEnabled = false
    private var modelReady = false

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val mlScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var frameCounter = 0
    private val frameSkip = 1 // analyze every frame for more active recognition

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        emotionText = findViewById(R.id.emotionText)
        confidenceText = findViewById(R.id.confidenceText)
        allConfidencesText = findViewById(R.id.allConfidences)
        startButton = findViewById(R.id.startButton)

        startButton.isEnabled = false
        emotionText.text = "Loading model..."

        // ⚡ Preload and warm-up model while UI and camera load
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                classifier = EmotionClassifier.create(this@MainActivity)

                // warm up once
                val dummy = Bitmap.createBitmap(
                    classifier!!.inputSize,
                    classifier!!.inputSize,
                    Bitmap.Config.ARGB_8888
                )
                classifier!!.classify(dummy)

                withContext(Dispatchers.Main) {
                    emotionText.text = "Model ready. Tap Start."
                    startButton.isEnabled = true
                    modelReady = true
                }
                Log.i("EmotionApp", "✅ Model preloaded and warmed up.")
            } catch (e: Exception) {
                Log.e("EmotionApp", "❌ Failed to preload model", e)
            }
        }

        startButton.setOnClickListener {
            detectionEnabled = !detectionEnabled
            startButton.text = if (detectionEnabled) "Stop Detection" else "Start Detection"

            if (!detectionEnabled) {
                emotionText.text = "Emotion:"
                confidenceText.text = "Confidence:"
                allConfidencesText.text = ""
                overlayView.clear()
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetResolution(Size(640, 480))
                .build()
                .apply { setSurfaceProvider(previewView.surfaceProvider) }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(480, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val detectorOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()

            val faceDetector = FaceDetection.getClient(detectorOptions)

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                if (!detectionEnabled || !modelReady) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                frameCounter++
                if (frameCounter % frameSkip != 0) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                try {
                    val grayBitmap =
                        ImageUtils.imageProxyToGrayscaleBitmap(imageProxy, 480, 480)
                    val inputImage =
                        InputImage.fromBitmap(grayBitmap, imageProxy.imageInfo.rotationDegrees)

                    faceDetector.process(inputImage)
                        .addOnSuccessListener { faces ->
                            if (faces.isEmpty()) {
                                runOnUiThread { overlayView.clear() }
                                imageProxy.close()
                                return@addOnSuccessListener
                            }

                            val face = faces[0].boundingBox
                            runOnUiThread {
                                overlayView.setFaces(
                                    listOf(face),
                                    grayBitmap.width,
                                    grayBitmap.height,
                                    previewView
                                )
                            }

                            mlScope.launch {
                                try {
                                    val x = face.left.coerceAtLeast(0)
                                    val y = face.top.coerceAtLeast(0)
                                    val w = face.width().coerceAtMost(grayBitmap.width - x)
                                    val h = face.height().coerceAtMost(grayBitmap.height - y)

                                    if (w > 10 && h > 10) {
                                        val faceBitmap = Bitmap.createBitmap(grayBitmap, x, y, w, h)
                                        val resized = Bitmap.createScaledBitmap(
                                            faceBitmap,
                                            classifier!!.inputSize,
                                            classifier!!.inputSize,
                                            true
                                        )

                                        val confidences = classifier!!.classify(resized)
                                        val maxIndex =
                                            confidences.indices.maxByOrNull { confidences[it] } ?: 0
                                        val label = classifier!!.labels[maxIndex]
                                        val maxValue = confidences[maxIndex]

                                        withContext(Dispatchers.Main) {
                                            emotionText.text = "Emotion: $label"
                                            confidenceText.text =
                                                String.format("Confidence: %.1f%%", maxValue * 100)

                                            val sb = StringBuilder()
                                            confidences.forEachIndexed { i, c ->
                                                sb.append(
                                                    "${classifier!!.labels[i]}: ${
                                                        String.format("%.1f%%", c * 100)
                                                    }\n"
                                                )
                                            }
                                            allConfidencesText.text = sb.toString().trim()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("EmotionApp", "Error in classification", e)
                                } finally {
                                    imageProxy.close()
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("EmotionApp", "Face detection failed", e)
                            imageProxy.close()
                        }
                } catch (e: Exception) {
                    Log.e("EmotionApp", "Frame analysis failed", e)
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("EmotionApp", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdownNow()
        mlScope.cancel()
        classifier?.close()
    }
}
