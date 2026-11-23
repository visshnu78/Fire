package com.meh.fire;

import android.app.Activity;
import android.app.AlertDialog;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Ends a lock early. Uses fingerprint or face unlock when the phone has one set up,
 * otherwise falls back to typing the emergency sentence.
 */
final class Unlocker {
    private Unlocker() {}

    /** Typing this exactly ends a lock on phones without fingerprint unlock. */
    static final String EMERGENCY_PHRASE = "I really need to unlock this right now";

    interface Done {
        void unlocked();
    }

    static void unlock(Activity a, Done done) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            fingerprint(a, done);
        } else {
            phrase(a, done);
        }
    }

    private static void fingerprint(Activity a, Done done) {
        BiometricPrompt.Builder builder = new BiometricPrompt.Builder(a)
                .setTitle("Unlock Fire")
                .setSubtitle("Use your fingerprint or face. Ends the lock now.")
                .setNegativeButton("Stay locked", a.getMainExecutor(), (d, w) -> { });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Face unlock: accept it without an extra "Confirm" tap.
            builder.setConfirmationRequired(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // "Weak" biometrics = fingerprint AND face unlock (any face unlock Android lets apps use).
            builder.setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK);
        }
        BiometricPrompt prompt = builder.build();
        prompt.authenticate(new CancellationSignal(), a.getMainExecutor(),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        Lock.clear(a);
                        Toast.makeText(a, "Unlocked", Toast.LENGTH_SHORT).show();
                        done.unlocked();
                    }

                    @Override
                    public void onAuthenticationError(int code, CharSequence message) {
                        switch (code) {
                            case BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT:
                            case BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                            case BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS:
                                // No fingerprint set up on this phone: use the sentence instead.
                                phrase(a, done);
                                break;
                            case BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED:
                            case BiometricPrompt.BIOMETRIC_ERROR_CANCELED:
                                break;
                            default:
                                Toast.makeText(a, message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private static void phrase(Activity a, Done done) {
        EditText input = new EditText(a);
        input.setHint(EMERGENCY_PHRASE);
        new AlertDialog.Builder(a)
                .setTitle("Unlock Fire")
                .setMessage("Type exactly:\n\n" + EMERGENCY_PHRASE)
                .setView(input)
                .setPositiveButton("Unlock", (d, w) -> {
                    if (EMERGENCY_PHRASE.equals(input.getText().toString().trim())) {
                        Lock.clear(a);
                        Toast.makeText(a, "Unlocked", Toast.LENGTH_SHORT).show();
                        done.unlocked();
                    } else {
                        Toast.makeText(a, "Didn't match. Still locked.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Stay locked", null)
                .show();
    }
}
