package app.honguyen.forge.camera

import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

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
