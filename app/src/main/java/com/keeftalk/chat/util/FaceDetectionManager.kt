package com.keeftalk.chat.util

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

class FaceDetectionManager {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    suspend fun detectFaces(bitmap: Bitmap): List<android.graphics.Rect> {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            val faces = detector.process(image).await()
            faces.map { it.boundingBox }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
