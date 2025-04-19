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
    private val textureView: TextureView
) {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var lensFacing = CameraSelector.LENS_FACING_FRONT // Default to front camera

    companion object {
        private const val TAG = "CameraHelper"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        /**
         * Check if all permissions are granted
         */
        fun allPermissionsGranted(context: Context): Boolean {
            return REQUIRED_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    /**
     * Start the camera preview
     */
    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                // Get camera provider
                val cameraProvider = cameraProviderFuture.get()

                // Set up preview use case
                val preview = Preview.Builder().build()

                // Set up camera selector
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                // Unbind any bound use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
                preview.setSurfaceProvider { request ->

                    val surfaceTexture = textureView.surfaceTexture ?: return@setSurfaceProvider

                    // Create a Surface with the SurfaceTexture
                    val surface = Surface(surfaceTexture)

                    // Configure the output
                    val resolution = request.resolution
                    surfaceTexture.setDefaultBufferSize(resolution.width, resolution.height)

                    // Provide the surface to CameraX
                    request.provideSurface(surface, cameraExecutor) {
                        surface.release()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Switch between front and back camera
     */
    fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }

        // Restart camera with new lens direction
        startCamera()
    }

    /**
     * Clean up resources
     */
    fun shutdown() {
        cameraExecutor.shutdown()
    }
}