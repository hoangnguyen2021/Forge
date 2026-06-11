package app.honguyen.forge.camera

import android.util.Size

/*
 * The orientation is needed alongside the size because width/height are reported in
 * the sensor's frame, not the screen's — a 1920x1080 stream from a 90°-mounted sensor
 * displays as 1080x1920 on a portrait surface. portraitWidth/portraitHeight do that
 * swap so callers get dimensions in the device's portrait frame.
 */
data class PreviewSize(
    // the chosen camera output dimensions, in the sensor's frame
    val size: Size,
    // the sensor's mount orientation in degrees
    val sensorOrientation: Int,
) {
    // Chosen size mapped into the device's portrait frame: width/height are swapped
    // when the sensor is mounted sideways, left as-is otherwise.
    val portraitWidth: Int
        get() = if (isSensorSideways(sensorOrientation)) size.height else size.width
    val portraitHeight: Int
        get() = if (isSensorSideways(sensorOrientation)) size.width else size.height

    companion object {
        private const val HALF_TURN_DEGREES = 180
        private const val QUARTER_TURN_DEGREES = 90

        // A sensor at 90° or 270° is mounted sideways relative to the device's natural
        // orientation, so its frame's width and height are swapped relative to the screen.
        fun isSensorSideways(degrees: Int): Boolean = degrees % HALF_TURN_DEGREES == QUARTER_TURN_DEGREES
    }
}
