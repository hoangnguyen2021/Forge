package app.honguyen.forge.camera

import android.view.Surface

/*
 * Contract for a camera session: open a camera stream into a Surface, then close it.
 *
 * Implementations are responsible for:
 *   - delivering frames into the provided Surface continuously after open()
 *   - releasing all camera and thread resources on close()
 *   - keeping camera callbacks off the main thread
 *
 * Callers are responsible for:
 *   - ensuring CAMERA permission is granted before calling open()
 *   - calling close() when the session is no longer needed (e.g. onPause, onDestroy)
 */
interface CameraSession {
    /*
     * Opens the camera and starts streaming frames into the provided Surface.
     * The call is asynchronous — frames begin arriving shortly after, not immediately.
     * The Surface must remain valid for the lifetime of the session.
     */
    fun open(surface: Surface)

    /*
     * Stops the camera stream and releases all associated resources.
     * Safe to call even if open() was never called or already failed.
     */
    fun close()
}
