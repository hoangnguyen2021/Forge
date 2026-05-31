package app.honguyen.forge.camera

import android.util.Size

/*
 * The orientation is needed alongside the size because width/height are reported in
 * the sensor's frame, not the screen's — a 1920x1080 stream from a 90°-mounted sensor
 * displays as 1080x1920 on a portrait surface. Callers use sensorOrientation to swap
 * width/height before comparing against on-screen dimensions.
 */
data class PreviewSize(
    // the chosen camera output dimensions
    val size: Size,
    // the sensor's mount orientation in degrees
    val sensorOrientation: Int,
)
