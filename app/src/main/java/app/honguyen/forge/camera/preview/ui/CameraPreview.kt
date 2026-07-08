package app.honguyen.forge.camera.preview.ui

import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.honguyen.forge.camera.preview.session.CameraSurfaceCallback

@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(CameraSurfaceCallback(context))
            }
        },
        modifier = modifier,
    )
}
