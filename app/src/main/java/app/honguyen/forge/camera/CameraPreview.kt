package app.honguyen.forge.camera

import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.honguyen.forge.engine.ForgeEngine

@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                var cameraSession: Camera2Session? = null
                var surfaceTexture: SurfaceTexture? = null

                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        ForgeEngine.nativeSurfaceCreated(holder.surface)

                        val texId = ForgeEngine.nativeCreateOesTexture()
                        if (texId < 0) return

                        val st = SurfaceTexture(texId).also { surfaceTexture = it }
                        st.setOnFrameAvailableListener({ tex ->
                            tex.updateTexImage()
                            val texMatrix = FloatArray(16)
                            tex.getTransformMatrix(texMatrix)
                            ForgeEngine.nativeDrawFrame(texMatrix)
                        }, Handler(Looper.getMainLooper()))
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int,
                    ) {
                        val st = surfaceTexture ?: return

                        val (size, sensorOrientation) = Camera2Session.selectPreviewSize(ctx, width, height)
                        st.setDefaultBufferSize(size.width, size.height)

                        // Compute effective portrait dimensions accounting for sensor rotation
                        val (camPortraitW, camPortraitH) = if (sensorOrientation % 180 == 90)
                            size.height to size.width
                        else
                            size.width to size.height

                        ForgeEngine.nativeSetViewport(camPortraitW, camPortraitH, width, height)

                        cameraSession?.close()
                        cameraSession = Camera2Session(ctx).also { it.open(Surface(st)) }
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        cameraSession?.close()
                        surfaceTexture?.release()
                        ForgeEngine.nativeSurfaceDestroyed()
                    }
                })
            }
        },
        modifier = modifier,
    )
}
