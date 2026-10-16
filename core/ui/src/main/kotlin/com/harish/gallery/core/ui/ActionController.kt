package com.harish.gallery.core.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harish.gallery.core.data.actions.ActionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Runs file actions for a ViewModel and handles Android's "Allow?" popups in one place.
 *
 * In a ViewModel:
 *     val actions = ActionController(viewModelScope)
 *     fun delete(items) = actions.run(onSuccess = ::clearSelection) { mediaActions.moveToTrash(items) }
 *
 * In the screen:
 *     ActionEffects(viewModel.actions, snackbarHostState)
 */
class ActionController(private val scope: CoroutineScope) {

    /** A popup waiting to be shown (kept here so a screen rotation doesn't lose it). */
    class PendingConsent internal constructor(
        val result: ActionResult.NeedsConsent,
        val onSuccess: () -> Unit,
        var launched: Boolean = false
    )

    private val _pending = MutableStateFlow<PendingConsent?>(null)
    val pending: StateFlow<PendingConsent?> = _pending.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun run(onSuccess: () -> Unit = {}, action: suspend () -> ActionResult) {
        scope.launch {
            _busy.value = true
            try {
                handle(action(), onSuccess)
            } finally {
                _busy.value = false
            }
        }
    }

    fun message(text: String) {
        _messages.tryEmit(text)
    }

    private suspend fun handle(result: ActionResult, onSuccess: () -> Unit) {
        when (result) {
            is ActionResult.Done -> {
                result.message?.let(_messages::tryEmit)
                onSuccess()
            }
            is ActionResult.Failed -> _messages.tryEmit(result.message)
            is ActionResult.NeedsConsent ->
                if (_pending.value == null) _pending.value = PendingConsent(result, onSuccess)
                else {
                    // One popup at a time: cancel the new request cleanly instead of losing the first one
                    result.onDenied()
                    _messages.tryEmit("Please finish the previous action first")
                }
        }
    }

    /** Called with the popup's answer. */
    fun onConsentResult(granted: Boolean) {
        val pending = _pending.value ?: return
        _pending.value = null
        scope.launch {
            _busy.value = true
            try {
                if (granted) handle(pending.result.onGranted(), pending.onSuccess)
                else pending.result.onDenied()
            } finally {
                _busy.value = false
            }
        }
    }
}

/**
 * Shows Android's consent popups and the result messages ("3 items moved to Trash")
 * for an [ActionController]. Messages go to [snackbarHostState], or a Toast when there's none.
 */
@Composable
fun ActionEffects(controller: ActionController, snackbarHostState: SnackbarHostState? = null) {
    val context = LocalContext.current
    val pending by controller.pending.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        controller.onConsentResult(result.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(pending) {
        val p = pending ?: return@LaunchedEffect
        if (!p.launched) {
            p.launched = true
            launcher.launch(IntentSenderRequest.Builder(p.result.request).build())
        }
    }
    LaunchedEffect(controller, snackbarHostState) {
        controller.messages.collect { text ->
            if (snackbarHostState != null) snackbarHostState.showSnackbar(text)
            else Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}
