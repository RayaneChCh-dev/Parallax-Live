package com.example.parallaxlive.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Helper class to manage camera preview
 */
class CameraHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var cameraProvider: ProcessCameraProvider? = null

    companion object {
        private const val TAG = "CameraHelper"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        fun allPermissionsGranted(context: Context): Boolean {
            return REQUIRED_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun startCamera() {
        Log.d(TAG, "Starting camera setup")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                // Get camera provider
                cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "Camera provider obtained")

                // Set up preview use case
                val preview = Preview.Builder().build()

                // Set up camera selector
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                // Unbind any bound use cases before rebinding
                cameraProvider?.unbindAll()
                Log.d(TAG, "Unbound previous camera uses")

                // Bind use cases to camera
                val camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )

                // Connect preview to the PreviewView
                preview.setSurfaceProvider(previewView.surfaceProvider)
                Log.d(TAG, "Preview connected to surface provider")

            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }

        // Restart camera with new lens direction
        startCamera()
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}