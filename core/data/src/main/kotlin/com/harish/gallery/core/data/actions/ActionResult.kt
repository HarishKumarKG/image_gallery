package com.harish.gallery.core.data.actions

import android.content.IntentSender

/**
 * What happened when we tried to change files (trash, favourite, move, rename...).
 *
 * Android 11+ asks the user before an app changes photos made by other apps (camera, WhatsApp...).
 * So many actions come back as [NeedsConsent]: the screen shows Android's popup, and
 * [onGranted] / [onDenied] finish the job afterwards.
 *
 * Example: "Move 3 photos to Trash"
 *   moveToTrash() -> NeedsConsent(popup)  -> user taps Allow -> onGranted() -> Done("3 items moved to Trash")
 */
sealed interface ActionResult {
    data class Done(val message: String? = null) : ActionResult

    class NeedsConsent(
        val request: IntentSender,
        val onGranted: suspend () -> ActionResult,
        val onDenied: suspend () -> Unit = {}
    ) : ActionResult

    data class Failed(val message: String) : ActionResult
}

/** "1 item" / "3 items" */
internal fun itemCount(count: Int): String = if (count == 1) "1 item" else "$count items"
