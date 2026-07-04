package com.k2fsa.sherpa.onnx.tts.engine.praxis.ui

import android.app.AlertDialog
import android.content.Context

object OnboardingDialog {
    fun show(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Welcome to SherpaTTS")
            .setMessage(
                "SherpaTTS is a text-to-speech engine — it gives voice to other apps " +
                "rather than speaking on its own.\n\n" +
                "To start using it:\n" +
                "  1. Make sure a voice model is downloaded\n" +
                "  2. Go to Settings → Accessibility → Text-to-speech\n" +
                "  3. Select SherpaTTS as your preferred engine\n\n" +
                "After that, any app that uses text-to-speech will speak through SherpaTTS."
            )
            .setPositiveButton("Got it", null)
            .show()
    }
}
