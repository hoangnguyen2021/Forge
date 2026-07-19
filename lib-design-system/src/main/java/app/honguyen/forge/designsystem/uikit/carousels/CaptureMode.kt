package app.honguyen.forge.designsystem.uikit.carousels

/**
 * A capture mode offered by [CaptureModeCarousel]. [label] is the user-facing text drawn in the
 * carousel; declaration order is the left-to-right order on screen.
 */
enum class CaptureMode(
    val label: String,
) {
    ActionPan("Action Pan"),
    LongExposure("Long Exposure"),
    AddMe("Add Me"),
    Portrait("Portrait"),
    Photo("Photo"),
    NightSight("Night Sight"),
    Panorama("Panorama"),
}
