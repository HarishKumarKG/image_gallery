package com.harish.gallery.feature.locked

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal

private const val AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

/** True if the phone has a fingerprint, face or at least a PIN / pattern / password set up. */
internal fun canUnlock(context: Context): Boolean =
    context.getSystemService(BiometricManager::class.java)?.canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

/**
 * Android's own unlock sheet (fingerprint / face, with "Use PIN" as a fallback).
 * Uses the built-in framework API, so no extra library is needed.
 */
internal fun askToUnlock(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val prompt = BiometricPrompt.Builder(context)
        .setTitle("Unlock Locked folder")
        .setSubtitle("Use your fingerprint, face or screen lock")
        .setAllowedAuthenticators(AUTHENTICATORS)
        .build()
    prompt.authenticate(CancellationSignal(), context.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errString.toString())
    })
}
