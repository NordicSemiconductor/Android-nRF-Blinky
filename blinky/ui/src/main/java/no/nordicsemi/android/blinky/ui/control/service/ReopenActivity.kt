package no.nordicsemi.android.blinky.ui.control.service

import android.app.Activity
import android.os.Bundle

/**
 * This is a trick to reopen the app from a submodule.
 *
 * It has no access to the MainActivity (which is in :app), so we run a
 * dummy Activity only to close it immediately. This way the previous Activity
 * will be brought to foreground.
 */
class ReopenActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}