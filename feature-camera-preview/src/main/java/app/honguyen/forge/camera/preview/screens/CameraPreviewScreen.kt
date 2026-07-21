package app.honguyen.forge.camera.preview.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.honguyen.forge.camera.preview.ui.CameraPreview
import app.honguyen.forge.composeutils.systembars.HideStatusBar
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.icons.CameraFlip
import app.honguyen.forge.designsystem.theme.icons.CameraSettings
import app.honguyen.forge.designsystem.theme.icons.Tune
import app.honguyen.forge.designsystem.uikit.buttons.CameraMenuButton
import app.honguyen.forge.designsystem.uikit.buttons.ShutterButton
import app.honguyen.forge.designsystem.uikit.buttons.SpinToggleButton
import app.honguyen.forge.designsystem.uikit.carousels.CaptureMode
import app.honguyen.forge.designsystem.uikit.carousels.CaptureModeCarousel
import app.honguyen.forge.designsystem.uikit.switches.CameraMode
import app.honguyen.forge.designsystem.uikit.switches.CameraModeSwitch

private const val PREVIEW_ASPECT_RATIO = 3f / 4f

/**
 * Screen that gates the live camera preview behind the runtime camera permission,
 * requesting it on first entry and showing a fallback message until it is granted.
 */
@Composable
fun CameraPreviewScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var cameraGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> cameraGranted = granted }

    LaunchedEffect(Unit) {
        if (!cameraGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    HideStatusBar()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        if (cameraGranted) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ForgeTheme.dimensions.size14x)
                        // The status bar is hidden on this screen, so only the cutout still
                        // intrudes here; padding for status bars would resolve to zero.
                        .displayCutoutPadding(),
                ) {
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CameraPreview(modifier = Modifier.aspectRatio(PREVIEW_ASPECT_RATIO))
                }
                CameraControls(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .navigationBarsPadding(),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Camera permission required")
            }
        }
    }
}

@Composable
private fun CameraControls(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        CameraControlsRow1(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(
                    horizontal = ForgeTheme.dimensions.size14x,
                    vertical = ForgeTheme.dimensions.size7x,
                ),
        )
        CameraControlsRow2(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )
        CameraControlsRow3(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(
                    horizontal = ForgeTheme.dimensions.size6x,
                    vertical = ForgeTheme.dimensions.size7x,
                ),
        )
    }
}

@Composable
private fun CameraControlsRow1(modifier: Modifier = Modifier) {
    var lensFacingFront by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.size(ForgeTheme.dimensions.size12x))
        ShutterButton(onClick = {})
        SpinToggleButton(
            imageVector = ForgeTheme.icons.CameraFlip,
            checked = lensFacingFront,
            onCheckedChange = { lensFacingFront = it },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CameraControlsRow2(modifier: Modifier = Modifier) {
    var captureMode by remember { mutableStateOf(CaptureMode.Photo) }

    CaptureModeCarousel(
        modes = CaptureMode.entries,
        selectedMode = captureMode,
        onModeSelected = { captureMode = it },
        modifier = modifier,
    )
}

@Composable
private fun CameraControlsRow3(modifier: Modifier = Modifier) {
    var cameraMode by remember { mutableStateOf(CameraMode.Photo) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CameraMenuButton(
            imageVector = ForgeTheme.icons.CameraSettings,
            onClick = {},
            contentDescription = "Camera settings",
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CameraModeSwitch(
            mode = cameraMode,
            onModeChange = { cameraMode = it },
            photoContentDescription = "Photo mode",
            videoContentDescription = "Video mode",
        )
        CameraMenuButton(
            imageVector = ForgeTheme.icons.Tune,
            onClick = {},
            contentDescription = "Tune",
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
