package app.honguyen.forge.composeutils.modifiers

import androidx.compose.ui.Modifier

/**
 * Appends the modifiers built by [factory] when [condition] holds, and returns the chain
 * untouched otherwise.
 *
 * [factory] builds onto an empty [Modifier] rather than onto the receiver, so writing
 * either `thenIf(c) { padding(4.dp) }` or `thenIf(c) { Modifier.padding(4.dp) }` appends
 * correctly instead of silently discarding the chain built up so far.
 *
 * Toggling [condition] restructures the modifier chain, which invalidates the layout node
 * and forces a remeasure. Reserve this for conditions that change on user interaction; for
 * per-frame values reach for a modifier that reads state at draw time, such as
 * `graphicsLayer` or `drawBehind`.
 */
inline fun Modifier.thenIf(
    condition: Boolean,
    factory: Modifier.() -> Modifier,
): Modifier = if (condition) then(Modifier.factory()) else this

/**
 * Appends the modifiers built by [factory] when [value] is non-null, handing it to
 * [factory] already smart-cast, and returns the chain untouched otherwise.
 *
 * Carries the same chain-restructuring caveat as [thenIf].
 */
inline fun <T : Any> Modifier.thenIfNotNull(
    value: T?,
    factory: Modifier.(T) -> Modifier,
): Modifier = if (value != null) then(Modifier.factory(value)) else this
