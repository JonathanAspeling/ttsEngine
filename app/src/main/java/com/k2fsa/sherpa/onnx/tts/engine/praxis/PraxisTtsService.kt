package com.k2fsa.sherpa.onnx.tts.engine.praxis

import android.os.Bundle
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import com.k2fsa.sherpa.onnx.tts.engine.PreferenceHelper
import com.k2fsa.sherpa.onnx.tts.engine.TtsService
import com.k2fsa.sherpa.onnx.tts.engine.praxis.normalizer.PronunciationOverrides
import com.k2fsa.sherpa.onnx.tts.engine.praxis.normalizer.TextNormalizer

/**
 * Extends the upstream TtsService to inject the TextNormalizer pipeline before synthesis.
 *
 * SynthesisRequest is a final Android class with a hidden mParams field. We read that
 * field via reflection (read-only, reliable on API 29+) so we can create a fresh
 * SynthesisRequest with the normalized text while preserving all other parameters
 * (language, pitch, speech rate). If reflection fails for any reason the original
 * request is passed through unchanged — no normalization, but synthesis still works.
 */
class PraxisTtsService : TtsService() {

    override fun onCreate() {
        super.onCreate()
        PronunciationOverrides.init("${filesDir.absolutePath}/pronunciations.txt")
    }

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null) {
            super.onSynthesizeText(null, callback)
            return
        }
        praxisPitch = PreferenceHelper(this).getPitch() * 100f
        val raw = request.charSequenceText.toString()
        val normalized = TextNormalizer.normalize(PronunciationOverrides.apply(raw))
        val effective = if (normalized != raw) patchRequest(request, normalized) else request
        super.onSynthesizeText(effective, callback)
    }

    private fun patchRequest(original: SynthesisRequest, normalizedText: String): SynthesisRequest {
        return try {
            val paramsField = SynthesisRequest::class.java.getDeclaredField("mParams")
            paramsField.isAccessible = true
            val params = paramsField.get(original) as? Bundle ?: Bundle()
            SynthesisRequest(normalizedText, params)
        } catch (e: Exception) {
            original
        }
    }
}
