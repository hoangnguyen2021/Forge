package app.honguyen.forge

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Substitutes [HiltTestApplication] for [ForgeApp] in instrumented tests, which is how
 * Hilt builds a component that test modules can replace bindings in.
 */
class ForgeTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
