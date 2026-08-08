package com.keeftalk.chat.ui.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoCapture
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

@Composable
fun CameraPreview(
    onImageCaptured: (Uri) -> Unit,
    onVideoCaptured: (Uri) -> Unit = {},
    onError: (Exception) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    
    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
    }
    val videoCapture: VideoCapture<Recorder> = remember { VideoCapture.withOutput(recorder) }
    var recording: Recording? by remember { mutableStateOf(null) }
    var isRecording by remember { mutableStateOf(false) }

    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )
            } catch (exc: Exception) {
                Log.e("CameraPreview", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

        IconButton(
            onClick = { },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(80.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            takePhoto(
                                context = context,
                                imageCapture = imageCapture,
                                executor = ContextCompat.getMainExecutor(context),
                                onImageCaptured = onImageCaptured,
                                onError = { if (it is Exception) onError(it) }
                            )
                        },
                        onLongPress = {
                            isRecording = true
                            recording = recordVideo(
                                context = context,
                                videoCapture = videoCapture,
                                executor = ContextCompat.getMainExecutor(context),
                                onVideoCaptured = { 
                                    onVideoCaptured(it)
                                    isRecording = false
                                },
                                onError = onError
                            )
                        },
                        onPress = {
                            val released = tryAwaitRelease()
                            if (isRecording) {
                                recording?.stop()
                                recording = null
                                isRecording = false
                            }
                        }
                    )
                },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.Camera, 
                contentDescription = "Capture", 
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

private fun recordVideo(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    executor: Executor,
    onVideoCaptured: (Uri) -> Unit,
    onError: (Exception) -> Unit
): Recording {
    val outputDirectory = getOutputDirectory(context)
    val videoFile = File(
        outputDirectory,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".mp4"
    )

    val outputOptions = FileOutputOptions.Builder(videoFile).build()

    return videoCapture.output
        .prepareRecording(context, outputOptions)
        .start(executor) { event ->
            if (event is VideoRecordEvent.Finalize) {
                if (!event.hasError()) {
                    onVideoCaptured(Uri.fromFile(videoFile))
                } else {
                    onError(RuntimeException("Video recording failed: ${event.error}"))
                }
            }
        }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val outputDirectory = getOutputDirectory(context)
    val photoFile = File(
        outputDirectory,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onImageCaptured(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

private fun getOutputDirectory(context: Context): File {
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
        File(it, context.resources.getString(com.keeftalk.chat.R.string.app_name)).apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
}
