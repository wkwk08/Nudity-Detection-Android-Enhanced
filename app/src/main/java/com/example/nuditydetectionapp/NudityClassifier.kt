package com.example.nuditydetectionapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NudityClassifier(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private val modelInputSize = 224
    private val labels = arrayOf("Safe", "Nude")
    
    companion object {
        private const val TAG = "NudityClassifier"
        private const val MODEL_NAME = "skin_detector_model.tflite"
    }
    
    init {
        setupInterpreter()
    }
    
    private fun setupInterpreter() {
        try {
            val options = Interpreter.Options().apply {
                setNumThreads(4) // Use 4 threads for better performance
            }
            
            val model = FileUtil.loadMappedFile(context, MODEL_NAME)
            interpreter = Interpreter(model, options)
            
            Log.d(TAG, "Model loaded successfully")
            Log.d(TAG, "Input shape: ${interpreter?.getInputTensor(0)?.shape()?.contentToString()}")
            Log.d(TAG, "Output shape: ${interpreter?.getOutputTensor(0)?.shape()?.contentToString()}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun classifyImage(bitmap: Bitmap): ClassificationResult {
        if (interpreter == null) {
            return ClassificationResult(
                label = "Error",
                confidence = 0.0f,
                isNude = false,
                processingTime = 0L
            )
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Preprocess the image
            val resizedBitmap = Bitmap.createScaledBitmap(
                bitmap,
                modelInputSize,
                modelInputSize,
                true
            )
            
            // Convert bitmap to ByteBuffer
            val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)
            
            // Run inference
            val outputArray = Array(1) { FloatArray(2) } // [batch_size=1][num_classes=2]
            interpreter?.run(inputBuffer, outputArray)
            
            val processingTime = System.currentTimeMillis() - startTime
            
            // Get results
            val probabilities = outputArray[0]
            val safeProb = probabilities[0]
            val nudeProb = probabilities[1]
            
            val isNude = nudeProb > safeProb
            val confidence = if (isNude) nudeProb else safeProb
            val label = if (isNude) "Nude" else "Safe"
            
            Log.d(TAG, "Classification: $label (confidence: ${confidence * 100}%)")
            Log.d(TAG, "Safe: ${safeProb * 100}%, Nude: ${nudeProb * 100}%")
            Log.d(TAG, "Processing time: ${processingTime}ms")
            
            return ClassificationResult(
                label = label,
                confidence = confidence,
                isNude = isNude,
                processingTime = processingTime,
                safeConfidence = safeProb,
                nudeConfidence = nudeProb
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during classification: ${e.message}")
            e.printStackTrace()
            return ClassificationResult(
                label = "Error",
                confidence = 0.0f,
                isNude = false,
                processingTime = 0L
            )
        }
    }
    
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * modelInputSize * modelInputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val pixels = IntArray(modelInputSize * modelInputSize)
        bitmap.getPixels(pixels, 0, modelInputSize, 0, 0, modelInputSize, modelInputSize)
        
        var pixel = 0
        for (i in 0 until modelInputSize) {
            for (j in 0 until modelInputSize) {
                val value = pixels[pixel++]
                
                // Extract RGB and normalize to [0, 1]
                byteBuffer.putFloat(((value shr 16) and 0xFF) / 255.0f) // R
                byteBuffer.putFloat(((value shr 8) and 0xFF) / 255.0f)  // G
                byteBuffer.putFloat((value and 0xFF) / 255.0f)          // B
            }
        }
        
        return byteBuffer
    }
    
    fun close() {
        interpreter?.close()
        interpreter = null
    }
    
    data class ClassificationResult(
        val label: String,
        val confidence: Float,
        val isNude: Boolean,
        val processingTime: Long,
        val safeConfidence: Float = 0f,
        val nudeConfidence: Float = 0f
    )
}