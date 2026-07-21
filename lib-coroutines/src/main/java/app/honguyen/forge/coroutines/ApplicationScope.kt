package app.honguyen.forge.coroutines

import javax.inject.Qualifier

/**
 * Qualifies the process-lifetime [kotlinx.coroutines.CoroutineScope] — work that should
 * outlive any one screen, such as DataStore's own write coordination.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
