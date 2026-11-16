package com.lensora.ai.ui.camera

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.lensora.ai.camera.CameraController

@Composable
fun CameraPreview(
    cameraController: CameraController,
    modifier: Modifier = Modifier,
    onTextureViewCreated: ((TextureView) -> Unit)? = null,
) {
    AndroidView(
        factory = { context ->
            TextureView(context).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int,
                    ) {
                        cameraController.openCamera(surface, width, height)
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int,
                    ) {
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        cameraController.closeCamera()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                    }
                }

                if (isAvailable) {
                    surfaceTexture?.let { surface ->
                        cameraController.openCamera(surface, width, height)
                    }
                }
                onTextureViewCreated?.invoke(this)
            }
        },
        modifier = modifier.fillMaxSize(),
    )
}
