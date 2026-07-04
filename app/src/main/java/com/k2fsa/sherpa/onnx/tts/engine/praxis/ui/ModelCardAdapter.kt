package com.k2fsa.sherpa.onnx.tts.engine.praxis.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.k2fsa.sherpa.onnx.tts.engine.R
import java.util.Locale

class ModelCardAdapter(
    context: Context,
    private val models: List<String>,
    private val modelType: String,
) : ArrayAdapter<String>(context, R.layout.list_item_model_card, models) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_model_card, parent, false)
        val model = models[position]
        val parts = model.split("-")
        val langCode = parts.getOrElse(0) { model }.replace("_", "-")
        val rawName = parts.getOrElse(1) { "" }
        val quality = parts.getOrElse(2) { "" }

        val voiceName = if (rawName.isEmpty()) model
        else rawName.split("_").joinToString(" ") { word ->
            word.replaceFirstChar { it.titlecase(Locale.ROOT) }
        }

        val locale = try { Locale.forLanguageTag(langCode) } catch (_: Exception) { Locale.ROOT }
        val langDisplay = locale.getDisplayName(Locale.ENGLISH).ifEmpty { langCode }

        view.findViewById<TextView>(R.id.model_voice_name).text = voiceName
        view.findViewById<TextView>(R.id.model_lang).text = langDisplay
        view.findViewById<TextView>(R.id.model_type).text = modelType
        view.findViewById<TextView>(R.id.model_quality).text = quality
        return view
    }
}
