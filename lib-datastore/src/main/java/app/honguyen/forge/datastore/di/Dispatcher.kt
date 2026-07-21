package app.honguyen.forge.datastore.di

import javax.inject.Qualifier

/**
 * Qualifies which [ForgeDispatcher] to inject. Dispatchers are injected rather than
 * referenced directly so tests can substitute a deterministic one.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(
    val dispatcher: ForgeDispatcher,
)
