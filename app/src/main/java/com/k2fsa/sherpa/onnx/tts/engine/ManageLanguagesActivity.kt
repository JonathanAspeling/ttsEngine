package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.k2fsa.sherpa.onnx.tts.engine.databinding.ActivityManageLanguagesBinding
import com.k2fsa.sherpa.onnx.tts.engine.praxis.ui.DownloadConfirm
import com.k2fsa.sherpa.onnx.tts.engine.praxis.ui.ModelCardAdapter
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class ManageLanguagesActivity : AppCompatActivity() {
    private var binding: ActivityManageLanguagesBinding? = null

    private var modelFileUri: Uri? = null
    private var tokensFileUri: Uri? = null
    private var langCodeForInstallation: String = ""
    private var langCode: String = ""
    private var modelName: String = ""

    private val allPiperModels = mutableListOf<String>()
    private val allCoquiModels = mutableListOf<String>()
    private var showPiperModels = mutableListOf<String>()
    private var showCoquiModels = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageLanguagesBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        ThemeUtil.setStatusBarAppearance(this)

        val piperRes: Array<String> = resources.getStringArray(R.array.piper_models)
        val coquiRes: Array<String> = resources.getStringArray(R.array.coqui_models)

        val db = LangDB.getInstance(this)
        val installedLanguages = db.allInstalledLanguages
        val installedLangCodes = installedLanguages.map { it.lang }

        val storageBytes = installedLanguages.sumOf { lang ->
            val dir = File(getExternalFilesDir(null), "${lang.lang}${lang.country}")
            if (dir.exists()) dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L
        }
        if (storageBytes > 0) {
            val count = installedLanguages.size
            binding!!.storageIndicator.text =
                "$count voice${if (count != 1) "s" else ""} installed · ${DownloadConfirm.formatBytes(storageBytes)} used"
            binding!!.storageIndicator.visibility = View.VISIBLE
        }

        for (model in piperRes) {
            val lang = Locale(model.split("_")[0]).isO3Language
            if (!installedLangCodes.contains(lang)) allPiperModels.add(model)
        }
        for (model in coquiRes) {
            val lang = Locale(model.split("_")[0]).isO3Language
            if (!installedLangCodes.contains(lang)) allCoquiModels.add(model)
        }
        showPiperModels = allPiperModels.toMutableList()
        showCoquiModels = allCoquiModels.toMutableList()

        setupFilterChips()
        refreshAdapters()
        setupClickListeners()
    }

    private fun setupFilterChips() {
        val langCodes = (allPiperModels + allCoquiModels)
            .map { it.substring(0, 2) }.distinct().sorted()
        if (langCodes.size < 2) return

        val chipGroup = binding!!.filterChipGroup
        langCodes.forEach { code ->
            val chip = Chip(this).apply {
                text = Locale(code).displayLanguage
                isCheckable = true
                tag = code
            }
            chipGroup.addView(chip)
        }
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val code = if (checkedIds.isEmpty()) null
                else (group.findViewById<Chip>(checkedIds[0]))?.tag as? String
            applyFilter(code)
        }
        binding!!.filterScroll.visibility = View.VISIBLE
    }

    private fun applyFilter(langCode: String?) {
        showPiperModels = if (langCode == null) allPiperModels.toMutableList()
            else allPiperModels.filter { it.startsWith(langCode) }.toMutableList()
        showCoquiModels = if (langCode == null) allCoquiModels.toMutableList()
            else allCoquiModels.filter { it.startsWith(langCode) }.toMutableList()
        refreshAdapters()
    }

    private fun refreshAdapters() {
        binding!!.piperModelList.adapter = ModelCardAdapter(this, showPiperModels, "Piper")
        binding!!.coquiModelList.adapter = ModelCardAdapter(this, showCoquiModels, "Coqui")
    }

    private fun hideListsForDownload() {
        binding!!.piperModelList.visibility = View.GONE
        binding!!.coquiModelList.visibility = View.GONE
        binding!!.buttonTestVoices.visibility = View.GONE
        binding!!.piperHeader.visibility = View.GONE
        binding!!.coquiHeader.visibility = View.GONE
        binding!!.filterScroll.visibility = View.GONE
        binding!!.downloadSize.setText("")
    }

    private fun setupClickListeners() {
        binding!!.piperModelList.setOnItemClickListener { _, _, position, _ ->
            val model = showPiperModels[position]
            val type = "vits-piper"
            val onnxUrl = "https://huggingface.co/csukuangfj/$type-$model/resolve/main/$model.onnx"
            val country = model.substring(3, 5)
            val lang = Locale(model.substring(0, 2)).isO3Language
            DownloadConfirm.checkSizeAndConfirm(this, model, onnxUrl) {
                hideListsForDownload()
                Downloader.downloadModels(this, binding, model, lang, country, type)
            }
        }
        binding!!.coquiModelList.setOnItemClickListener { _, _, position, _ ->
            val model = showCoquiModels[position]
            val type = "vits-coqui"
            val onnxUrl = "https://huggingface.co/csukuangfj/$type-$model/resolve/main/model.onnx"
            val lang = Locale(model.substring(0, 2)).isO3Language
            DownloadConfirm.checkSizeAndConfirm(this, model, onnxUrl) {
                hideListsForDownload()
                Downloader.downloadModels(this, binding, model, lang, "", type)
            }
        }
    }

    fun startMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }

    fun testVoices(view: View) {startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/spaces/k2-fsa/text-to-speech/")))}
    
    fun installFromSD(view: View) {
        sdInstall()
    }
    fun sdInstall() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_install_custom_model, null)

        val langInput = dialogView.findViewById<EditText>(R.id.editTextLangCode)
        val modelInput = dialogView.findViewById<EditText>(R.id.editTextModelName)
        val selectModelBtn = dialogView.findViewById<Button>(R.id.buttonSelectModel)
        val selectTokensBtn = dialogView.findViewById<Button>(R.id.buttonSelectTokens)
        val installBtn = dialogView.findViewById<Button>(R.id.buttonInstall)

        // Initialize button text/visibility 
        selectModelBtn.setOnClickListener {
            modelPickerLauncher.launch(arrayOf("application/octet-stream"))
        }

        selectTokensBtn.setOnClickListener {
            tokensPickerLauncher.launch(arrayOf("text/plain"))
        }

        installBtn.setOnClickListener {
            langCode = langInput.text.toString().trim()
            modelName = modelInput.text.toString().trim()

            // Validate
            if (langCode.length != 3) {
                Toast.makeText(this, R.string.language_code_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (modelName.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_model_name), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (modelFileUri == null) {
                Toast.makeText(this, getString(R.string.select_model_file), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (tokensFileUri == null) {
                Toast.makeText(this, getString(R.string.select_tokens_file), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check duplicates
            val db = LangDB.getInstance(this)
            if (db.allInstalledLanguages.any { it.lang == langCode }) {
                Toast.makeText(this, R.string.language_already_installed, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Proceed with install
            installCustomModel(langCode, modelFileUri!!, tokensFileUri!!)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.install_from_sd))
            .setView(dialogView)
            .setNegativeButton(getString(android.R.string.cancel)) { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()

        dialog.show()
    }

    private fun installCustomModel(langCode: String, modelUri: Uri, tokensUri: Uri) {
        // Create directory for the language (country code is empty as requested)
        val directory = File(this.getExternalFilesDir(null), "/$langCode/")
        if (!directory.exists() && !directory.mkdirs()) {
            Toast.makeText(this, R.string.error_copying_files, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Copy model.onnx file
        val modelDest = File(directory, Downloader.onnxModel)
        try {
            copyFile(modelUri, modelDest)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_copying_files, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Copy tokens.txt file
        val tokensDest = File(directory, Downloader.tokens)
        try {
            copyFile(tokensUri, tokensDest)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_copying_files, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Add to database as vits-piper model with empty country code
        val db = LangDB.getInstance(this)
        db.addLanguage(modelName, langCode, "", 0, 1.0f, 1.0f, "vits-piper")
        
        // Show success message
        Toast.makeText(this, "+ \"$langCode\" = \"$modelName\" ", Toast.LENGTH_SHORT).show()
        val preferenceHelper = PreferenceHelper(this)
        preferenceHelper.setCurrentLanguage(langCode)
        // Reset file URIs and lang code for next use
        modelFileUri = null
        tokensFileUri = null
        langCodeForInstallation = ""
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }
    
    private fun copyFile(sourceUri: Uri, destFile: File) {
        this.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    // Register for file pickers 
    private val modelPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            modelFileUri = uri
        }
    }

    private val tokensPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            tokensFileUri = uri
        }
    }
}
