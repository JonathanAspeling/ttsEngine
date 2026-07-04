package com.k2fsa.sherpa.onnx.tts.engine.praxis.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.k2fsa.sherpa.onnx.tts.engine.R
import com.k2fsa.sherpa.onnx.tts.engine.ThemeUtil
import com.k2fsa.sherpa.onnx.tts.engine.praxis.normalizer.PronunciationOverrides

class PronunciationActivity : AppCompatActivity() {

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pronunciation)
        ThemeUtil.setStatusBarAppearance(this)
        listView = findViewById(R.id.pronunciation_list)
        refreshList()
        findViewById<Button>(R.id.button_add_pronunciation).setOnClickListener { showAddDialog() }
    }

    private fun refreshList() {
        val entries = PronunciationOverrides.getAll()
        val items = entries.map { (word, expansion) -> "$word  →  $expansion" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        listView.adapter = adapter
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val word = entries[position].first
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.remove_pronunciation, word))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    PronunciationOverrides.remove(word)
                    refreshList()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            true
        }
    }

    private fun showAddDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 24, 64, 0)
        }
        val wordInput = EditText(this).apply { hint = getString(R.string.pronunciation_word_hint) }
        val expansionInput = EditText(this).apply { hint = getString(R.string.pronunciation_expansion_hint) }
        layout.addView(wordInput)
        layout.addView(expansionInput)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_pronunciation))
            .setView(layout)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val word = wordInput.text.toString().trim()
                val expansion = expansionInput.text.toString().trim()
                if (word.isNotEmpty() && expansion.isNotEmpty()) {
                    PronunciationOverrides.add(word, expansion)
                    refreshList()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
