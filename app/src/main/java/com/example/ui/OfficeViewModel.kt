package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.OpenRouterClient
import com.example.api.OpenRouterMessage
import com.example.api.OpenRouterRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.AgendaItem
import com.example.data.OfficeItem
import com.example.data.OfficeRepository
import com.example.utils.OfflineAssistantEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val sender: String,
    val text: String,
    val type: String? = null
)

data class OcrResult(
    val rawText: String,
    val correctedText: String,
    val corrections: List<Pair<String, String>> = emptyList()
)

class OfficeViewModel(
    application: Application,
    private val repository: OfficeRepository
) : AndroidViewModel(application) {

    val items: StateFlow<List<OfficeItem>> = repository.allItems
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val agendaItems: StateFlow<List<AgendaItem>> = repository.allAgendaItems
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("AI", "Olá! Sou o seu Assistente de Escritório Inteligente de Alta Performance. Como posso te auxiliar hoje?\n\nAqui realizamos redações, relatórios, atas, planilhas estruturadas e tiramos dúvidas corporativas!"))
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _activeEditItem = MutableStateFlow<OfficeItem?>(null)
    val activeEditItem: StateFlow<OfficeItem?> = _activeEditItem.asStateFlow()

    private val _selectedProvider = MutableStateFlow("GEMINI")
    val selectedProvider: StateFlow<String> = _selectedProvider.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Document fusion state
    private val _selectedFusionDocs = MutableStateFlow<List<OfficeItem>>(emptyList())
    val selectedFusionDocs: StateFlow<List<OfficeItem>> = _selectedFusionDocs.asStateFlow()

    // OCR state
    private val _ocrResult = MutableStateFlow<OcrResult?>(null)
    val ocrResult: StateFlow<OcrResult?> = _ocrResult.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        initSpeechRecognizer(application)
    }

    fun setLoading(loading: Boolean) { _isLoading.value = loading }

    private fun initSpeechRecognizer(context: Context) {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }
        } catch (e: Exception) {
            Log.e("OfficeViewModel", "Speech init failed: ${e.message}")
        }
    }

    fun setSourceMode(online: Boolean) { _isOnline.value = online }
    fun setAiProvider(provider: String) { _selectedProvider.value = provider }
    fun clearError() { _error.value = null }
    fun selectEditItem(item: OfficeItem?) { _activeEditItem.value = item }

    fun updateActiveItemContent(newContent: String, isCsvData: Boolean = false) {
        val current = _activeEditItem.value ?: return
        _activeEditItem.value = if (isCsvData) current.copy(csvData = newContent, content = newContent)
        else current.copy(content = newContent)
    }

    fun saveActiveEditItem(title: String) {
        val active = _activeEditItem.value ?: return
        viewModelScope.launch {
            try {
                repository.updateItem(active.copy(title = title, timestamp = System.currentTimeMillis()))
                _activeEditItem.value = active.copy(title = title, timestamp = System.currentTimeMillis())
            } catch (e: Exception) { _error.value = "Erro ao guardar: ${e.message}" }
        }
    }

    fun deleteItem(item: OfficeItem) {
        viewModelScope.launch {
            try {
                repository.deleteItem(item)
                if (_activeEditItem.value?.id == item.id) _activeEditItem.value = null
            } catch (e: Exception) { _error.value = "Erro ao excluir: ${e.message}" }
        }
    }

    fun insertCustomItem(title: String, type: String, content: String, notes: String? = null) {
        viewModelScope.launch {
            try {
                repository.insertItem(OfficeItem(title = title, type = type, content = content, csvData = if (type == "SPREADSHEET") content else null, notes = notes))
            } catch (e: Exception) { _error.value = "Erro ao salvar: ${e.message}" }
        }
    }

    // === AGENDA METHODS ===
    fun addAgendaItem(title: String, description: String, dateTime: Long, priority: String, location: String) {
        viewModelScope.launch {
            try {
                repository.insertAgendaItem(AgendaItem(title = title, description = description, dateTime = dateTime, priority = priority, location = location))
            } catch (e: Exception) { _error.value = "Erro ao criar compromisso: ${e.message}" }
        }
    }

    fun toggleAgendaComplete(item: AgendaItem) {
        viewModelScope.launch {
            try {
                repository.setAgendaItemCompleted(item.id, !item.isCompleted)
            } catch (e: Exception) { _error.value = "Erro ao atualizar: ${e.message}" }
        }
    }

    fun deleteAgendaItem(item: AgendaItem) {
        viewModelScope.launch {
            try {
                repository.deleteAgendaItem(item)
            } catch (e: Exception) { _error.value = "Erro ao excluir: ${e.message}" }
        }
    }

    // === DOCUMENT FUSION ===
    fun toggleFusionDoc(item: OfficeItem) {
        val current = _selectedFusionDocs.value.toMutableList()
        if (current.any { it.id == item.id }) current.removeAll { it.id == item.id }
        else current.add(item)
        _selectedFusionDocs.value = current
    }

    fun clearFusionDocs() { _selectedFusionDocs.value = emptyList() }

    fun generateFusionReport(title: String, observations: String, onResult: (String) -> Unit) {
        val docs = _selectedFusionDocs.value
        if (docs.isEmpty()) { _error.value = "Selecione ao menos um documento"; return }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fusionContent = buildString {
                    appendLine("=== FUSÃO CORPORATIVA DE DOCUMENTOS ===")
                    appendLine("Observações: $observations")
                    appendLine()
                    docs.forEachIndexed { i, doc ->
                        appendLine("--- Documento ${i + 1}: ${doc.title} (${doc.type}) ---")
                        appendLine(doc.content.take(2000))
                        appendLine()
                    }
                }

                val prompt = "Com base nos documentos fornecidos, gere um relatório executivo completo e bem estruturado em português: \n\n$fusionContent"
                val response = generateWithAI(prompt, "REPORT")
                onResult(response)
            } catch (e: Exception) {
                val fallback = OfflineAssistantEngine.generateLocalResponse("Relatório fusão corporativa: $observations", "REPORT")
                onResult(fallback)
            } finally { _isLoading.value = false }
        }
    }

    // === OCR METHODS ===
    fun processOcrText(rawText: String) {
        val corrections = mutableListOf<Pair<String, String>>()
        var corrected = rawText

        // Simulated grammar corrections for common Portuguese errors
        val grammarRules = listOf(
            "nos" to "nós", "pra" to "para", "ta" to "está", "tá" to "está",
            "vc" to "você", "tb" to "também", "pq" to "porque",
            "q" to "que", "d" to "de", "num" to "em um",
            "pro" to "para o", "duma" to "de uma", "nessa" to "nesta",
            "agnt" to "a gente", "gnt" to "gente", "mt" to "muito",
            "obg" to "obrigado", "blz" to "beleza", "hj" to "hoje"
        )
        for ((wrong, right) in grammarRules) {
            val pattern = Regex("\\b$wrong\\b", RegexOption.IGNORE_CASE)
            if (pattern.containsMatchIn(corrected)) {
                corrections.add(wrong to right)
                corrected = corrected.replace(pattern, right)
            }
        }

        // Capitalization
        corrected = corrected.replaceFirstChar { it.uppercase() }
        corrected = corrected.replace(Regex("(?<=\\.\\s)\\w")) { it.value.uppercase() }
        corrected = corrected.trim()

        _ocrResult.value = OcrResult(rawText = rawText, correctedText = corrected, corrections = corrections)
    }

    fun clearOcrResult() { _ocrResult.value = null }

    // === SPEECH ===
    private var accumulatedSpeech = ""
    private var speechCallback: ((String) -> Unit)? = null

    fun startListening(onResult: (String) -> Unit) {
        val recognizer = speechRecognizer ?: run { onResult(""); return }
        _isRecording.value = true
        accumulatedSpeech = ""
        speechCallback = onResult

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    // Auto-restart if still recording (user hasn't stopped manually)
                    if (_isRecording.value) {
                        recognizer.startListening(intent)
                    }
                }
                override fun onError(error: Int) {
                    if (_isRecording.value) {
                        // Restart on error if still in recording mode
                        recognizer.startListening(intent)
                    }
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        accumulatedSpeech += if (accumulatedSpeech.isEmpty()) text else " $text"
                        speechCallback?.invoke(accumulatedSpeech)
                    }
                    // Keep listening if still recording
                    if (_isRecording.value) {
                        recognizer.startListening(intent)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        speechCallback?.invoke(accumulatedSpeech + matches[0])
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer.startListening(intent)
        } catch (e: Exception) { _isRecording.value = false }
    }

    fun stopListening() {
        _isRecording.value = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        // Reinitialize for next use
        speechRecognizer = try {
            SpeechRecognizer.createSpeechRecognizer(getApplication())
        } catch (e: Exception) { null }
        speechCallback = null
    }

    // === CHAT / AI ===
    fun askQuestion(prompt: String) {
        if (prompt.trim().isEmpty()) return
        val updatedChat = _chatMessages.value.toMutableList()
        updatedChat.add(ChatMessage("USER", prompt))
        _chatMessages.value = updatedChat
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val documentType = detectDocumentType(prompt)
                val response = generateWithAI(prompt, documentType)
                _chatMessages.value = _chatMessages.value + ChatMessage("AI", response, documentType)
            } catch (e: Exception) {
                val fallbackType = detectDocumentType(prompt)
                val fallback = OfflineAssistantEngine.generateLocalResponse(prompt, fallbackType)
                _chatMessages.value = _chatMessages.value + ChatMessage("AI", "⚠️ [Modo Offline]\n\n$fallback", fallbackType)
            } finally { _isLoading.value = false }
        }
    }

    private suspend fun generateWithAI(prompt: String, docType: String): String {
        if (!_isOnline.value) return OfflineAssistantEngine.generateLocalResponse(prompt, docType)

        val systemPrompt = "Você é um Assistente de IA completo e versátil, capaz de responder QUALQUER assunto global: " +
            "escritório, tecnologia, ciência, história, cultura, matemática, programação, finanças, saúde, viagens, " +
            "entretenimento, esportes, política, educação, direito, engenharia, medicina, arte, música, filosofia e muito mais. " +
            "Atue em português (PT-BR) de forma natural, solta e completa, como uma conversa real. " +
            "Se o usuário pedir para gerar uma planilha/tabela, responda no formato CSV separado por vírgula. " +
            "Se o usuário pedir conselhos, opiniões ou sugestões, responda à vontade e sem restrições. " +
            "Você NÃO precisa se limitar — responda com profundidade, criatividade e personalidade. " +
            "Para perguntas gerais, responda de forma natural e informativa como um assistente pessoal e amigável."

        return when (_selectedProvider.value) {
            "OPENROUTER" -> generateWithOpenRouter(prompt, systemPrompt)
            else -> generateWithGemini(prompt, systemPrompt)
        }
    }

    private suspend fun generateWithGemini(prompt: String, systemPrompt: String): String {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            return OfflineAssistantEngine.generateLocalResponse(prompt, detectDocumentType(prompt))
        }
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )
        val response = withContext(Dispatchers.IO) {
            RetrofitClient.service.generateContent(key, request)
        }
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            ?: throw Exception("Sem resposta da IA (Gemini)")
    }

    private suspend fun generateWithOpenRouter(prompt: String, systemPrompt: String): String {
        val key = BuildConfig.OPENROUTER_API_KEY
        if (key.isEmpty() || key == "MY_OPENROUTER_API_KEY") {
            return OfflineAssistantEngine.generateLocalResponse(prompt, detectDocumentType(prompt))
        }
        val request = OpenRouterRequest(
            model = "openai/gpt-4o",
            messages = listOf(
                OpenRouterMessage(role = "system", content = systemPrompt),
                OpenRouterMessage(role = "user", content = prompt)
            )
        )
        val response = withContext(Dispatchers.IO) {
            OpenRouterClient.service.generateContent("Bearer $key", request)
        }
        return response.choices?.firstOrNull()?.message?.content?.trim()
            ?: throw Exception("Sem resposta da IA (OpenRouter)")
    }

    private fun detectDocumentType(prompt: String): String {
        val lower = prompt.lowercase(Locale.getDefault())
        return when {
            lower.contains("bom dia") || lower.contains("boa tarde") || lower.contains("boa noite") ||
                lower.contains("ola") || lower.contains("olá") || lower.contains("oi") || lower.contains("hey") ||
                lower.contains("tudo bem") || lower.contains("como vai") || lower.contains("e aí") ||
                lower.contains("blz") || lower.contains("beleza") || lower.contains("opa") ||
                lower.contains("qual seu nome") || lower.contains("quem é você") ||
                lower.contains("obrigado") || lower.contains("valeu") || lower.contains("brigado") ||
                lower.equals("oi") || lower.equals("ola") || lower.equals("olá") || lower.equals("hey") ||
                lower.startsWith("bom") || lower.startsWith("boa") || lower.length < 8 -> "GENERAL"
            lower.contains("planilha") || lower.contains("tabela") || lower.contains("dados") || lower.contains("inventario") || lower.contains("estoque") -> "SPREADSHEET"
            lower.contains("relatorio") || lower.contains("relatório") || lower.contains("fusão") || lower.contains("fusao") -> "REPORT"
            lower.contains("ata") || lower.contains("reuni") -> "MINUTE"
            lower.contains("redação") || lower.contains("redacao") || lower.contains("texto") || lower.contains("oficio") || lower.contains("memorando") -> "TEXT"
            else -> "GENERAL"
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
    }
}

class OfficeViewModelFactory(
    private val application: Application,
    private val repository: OfficeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OfficeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return OfficeViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
