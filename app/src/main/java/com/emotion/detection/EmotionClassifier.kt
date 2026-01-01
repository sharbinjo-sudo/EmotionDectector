package com.emotion.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class EmotionClassifier private constructor(
    private val interpreter: Interpreter,
    val inputSize: Int,
    private val inputByteSize: Int,
    val labels: List<String>
) : Closeable {

    private val runMutex = Mutex()
    private val inputBuffer: ByteBuffer = ByteBuffer.allocateDirect(inputByteSize).order(ByteOrder.nativeOrder())
    private val outputArray: Array<FloatArray> = Array(1) { FloatArray(labels.size) }

    companion object {
        private const val FLOAT_TYPE_SIZE = 4
        private const val PIXEL_SIZE = 1

        fun create(
            context: Context,
            modelAssetName: String = "emotion_detector.tflite",
            labels: List<String>? = null
        ): EmotionClassifier {
            val modelBuffer = loadModelFile(context, modelAssetName)

            val options = Interpreter.Options().apply {
                numThreads = 2
                useXNNPACK = true
            }

            var gpuDelegate: GpuDelegate? = null
            try {
                gpuDelegate = GpuDelegate()
                options.addDelegate(gpuDelegate)
                Log.i("EmotionClassifier", "✅ GPU delegate added successfully.")
            } catch (e: Exception) {
                Log.w("EmotionClassifier", "⚠️ GPU delegate unavailable, using CPU fallback.")
            }

            val interpreter: Interpreter = try {
                Interpreter(modelBuffer, options)
            } catch (e: Exception) {
                Log.e("EmotionClassifier", "⚠️ Failed to create interpreter with GPU — fallback to CPU.", e)
                gpuDelegate?.close()
                val cpuOptions = Interpreter.Options().apply {
                    numThreads = 2
                    useXNNPACK = true
                }
                Interpreter(modelBuffer, cpuOptions)
            }

            val inputShape = interpreter.getInputTensor(0).shape()
            val inputWidth = inputShape[1]
            val inputHeight = inputShape[2]
            val channels = inputShape[3].coerceAtLeast(1)
            val inputByteSize = FLOAT_TYPE_SIZE * inputWidth * inputHeight * channels

            val finalLabels = labels ?: listOf("Angry", "Happy", "Sad", "Surprise")
            return EmotionClassifier(interpreter, inputWidth, inputByteSize, finalLabels)
        }

        private fun loadModelFile(context: Context, assetName: String): MappedByteBuffer {
            val afd = context.assets.openFd(assetName)
            FileInputStream(afd.fileDescriptor).use { input ->
                val channel = input.channel
                return channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
            }
        }
    }

    private fun runInferenceInternal(bitmap: Bitmap): FloatArray {
        inputBuffer.rewind()
        fillBufferFromBitmap(bitmap, inputBuffer)
        inputBuffer.rewind()

        for (i in outputArray[0].indices) outputArray[0][i] = 0f
        interpreter.run(inputBuffer, outputArray)

        val scores = outputArray[0]
        val total = scores.sum().takeIf { it > 0f } ?: 1f
        return FloatArray(scores.size) { i -> scores[i] / total }
    }

    suspend fun classify(bitmap: Bitmap): FloatArray = withContext(Dispatchers.Default) {
        runMutex.withLock {
            runInferenceInternal(bitmap)
        }
    }

    override fun close() {
        interpreter.close()
    }

    private fun fillBufferFromBitmap(bitmap: Bitmap, buffer: ByteBuffer) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val gray = ((r + g + b) / 3f) / 255f
            buffer.putFloat(gray)
        }
    }
}
