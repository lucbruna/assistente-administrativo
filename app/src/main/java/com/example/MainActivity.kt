package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.api.GenerateContentRequest
import com.example.api.Content
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.OfficeDatabase
import com.example.data.OfficeItem
import com.example.data.OfficeRepository
import com.example.data.AgendaItem
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.*
import com.example.utils.DateUtils
import com.example.utils.DocumentSharingUtils
import com.example.utils.BackupUtils
import com.example.utils.AlarmUtils
import com.example.utils.OcrProcessor
import com.example.utils.OfflineAssistantEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    val context = LocalContext.current
                val database = createDatabase(context)
                val repository = OfficeRepository(database.officeItemDao(), database.agendaItemDao())
                val factory = OfficeViewModelFactory(application, repository)
                    val viewModel: OfficeViewModel = viewModel(factory = factory)

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        OfficeMainScreen(viewModel)
                    }
                }
            }
        }
    }

    private fun createDatabase(context: Context): OfficeDatabase {
        return OfficeDatabase.getDatabase(context)
    }
}

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "splashAlpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(1400)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { this.alpha = alpha.value }
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AccentElectricBlue, AccentNeonTeal)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "OP",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "OfficePro AI",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Assistente de Escritório Inteligente",
                fontSize = 13.sp,
                color = Color(0xFFCAC4D0),
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = AccentNeonTeal,
                strokeWidth = 2.dp
            )
        }
    }
}

// Custom Premium Color Scheme overrides for beautiful office panel - Elegant Dark Theme
val DarkBackground = Color(0xFF1C1B1F)
val CardSurface = Color(0xFF2B2930)
val AccentNeonTeal = Color(0xFF80D8CC)
val AccentElectricBlue = Color(0xFFD0BCFF)
val BorderColor = Color(0xFF49454F)

@Composable private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = AccentNeonTeal, selectedTextColor = AccentNeonTeal,
    indicatorColor = BorderColor, unselectedIconColor = Color.LightGray,
    unselectedTextColor = Color.LightGray
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeMainScreen(viewModel: OfficeViewModel) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val activeEditItem by viewModel.activeEditItem.collectAsState()
    val appError by viewModel.error.collectAsState()
    val agendaItems by viewModel.agendaItems.collectAsState()
    val selectedFusionDocs by viewModel.selectedFusionDocs.collectAsState()
    val ocrResult by viewModel.ocrResult.collectAsState()

    var activeTab by remember { mutableStateOf("dashboard") }
    var searchText by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var createType by remember { mutableStateOf("REPORT") }
    var createTitle by remember { mutableStateOf("") }
    var createPrompt by remember { mutableStateOf("") }

    var showFusionDialog by remember { mutableStateOf(false) }
    var showOcrCameraDialog by remember { mutableStateOf(false) }
    var showOcrResultDialog by remember { mutableStateOf(false) }
    var showAgendaCreateDialog by remember { mutableStateOf(false) }
    var showRealCamera by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    var fusionResultTitle by remember { mutableStateOf("") }

    var voiceInputState by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    // File pickup launchers
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val text = DocumentSharingUtils.readTextFromUri(context, uri)
                if (text != null) {
                    val name = uri.lastPathSegment?.substringAfterLast("/") ?: "Importado"
                    val isCsv = name.lowercase().contains(".csv") || text.contains(",")
                    val type = if (isCsv) "SPREADSHEET" else "REPORT"
                    viewModel.insertCustomItem(
                        title = "Importado: " + name.substringBeforeLast("."),
                        type = type,
                        content = text,
                        notes = "Arquivo carregado do aparelho local"
                    )
                    Toast.makeText(context, "Documento importado com sucesso!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Não foi possível ler o arquivo.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Speech permissions
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Escutando...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissão de áudio negada.", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery image import for OCR
    val galleryImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val bmp = context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                    if (bmp != null) {
                        val maxSize = 2048
                        val scaled = if (bmp.width > maxSize || bmp.height > maxSize) {
                            val s = maxSize.toFloat() / maxOf(bmp.width, bmp.height)
                            android.graphics.Bitmap.createScaledBitmap(bmp,
                                (bmp.width * s).toInt(), (bmp.height * s).toInt(), true)
                        } else bmp
                        val text = withContext(Dispatchers.IO) { OcrProcessor.recognizeText(scaled) }
                        if (text.isNotBlank()) {
                            viewModel.processOcrText(text)
                            showOcrResultDialog = true
                        } else {
                            Toast.makeText(context, "Nenhum texto reconhecido na imagem.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro ao processar imagem: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Backup export launcher
    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    BackupUtils.exportToZip(context, items)?.let { zipUri ->
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            context.contentResolver.openInputStream(zipUri)?.use { inp ->
                                inp.copyTo(out)
                            }
                        }
                        Toast.makeText(context, "Backup exportado com sucesso!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro no backup: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Backup import launcher
    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val success = BackupUtils.restoreBackup(context, uri)
                    if (success) {
                        Toast.makeText(context, "Backup restaurado! Reinicie o app.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Erro ao restaurar backup.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(appError) {
        appError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    if (showRealCamera) {
        RealOcrCameraScreen(
            onTextExtracted = { rawText ->
                viewModel.processOcrText(rawText)
                showRealCamera = false
                showOcrResultDialog = true
            },
            onDismiss = { showRealCamera = false }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFD0BCFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "OP",
                                color = Color(0xFF381E72),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text(
                                text = "OfficePro AI",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "LOCAL & OFFLINE SECURE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD0BCFF),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                actions = {
                    // Two AI Switcher (Dual engine toggle)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(CardSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.Cloud else Icons.Default.CloudOff,
                            contentDescription = "Estado IA",
                            tint = if (isOnline) AccentNeonTeal else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOnline) "IA Gemini" else "IA Local",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isOnline,
                            onCheckedChange = { viewModel.setSourceMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentNeonTeal,
                                checkedTrackColor = AccentNeonTeal.copy(alpha = 0.4f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = BorderColor
                            ),
                            modifier = Modifier.scale(0.7f)
                        )
                        if (isOnline) {
                            Spacer(modifier = Modifier.width(8.dp))
                            val selectedProvider = viewModel.selectedProvider.collectAsState().value
                            IconButton(
                                onClick = {
                                    val next = if (selectedProvider == "GEMINI") "OPENROUTER" else "GEMINI"
                                    viewModel.setAiProvider(next)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    if (selectedProvider == "GEMINI") Icons.Default.Shield else Icons.Default.Public,
                                    "Trocar provedor IA",
                                    tint = AccentElectricBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = selectedProvider,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentNeonTeal
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardSurface,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CardSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == "dashboard" && activeEditItem == null,
                    onClick = {
                        activeTab = "dashboard"
                        viewModel.selectEditItem(null)
                    },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Painel") },
                    label = { Text("Painel", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentNeonTeal,
                        selectedTextColor = AccentNeonTeal,
                        indicatorColor = BorderColor,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "chat" && activeEditItem == null,
                    onClick = {
                        activeTab = "chat"
                        viewModel.selectEditItem(null)
                    },
                    icon = { Icon(Icons.Default.QuestionAnswer, contentDescription = "Tirar Dúvidas") },
                    label = { Text("IA Chat", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentNeonTeal,
                        selectedTextColor = AccentNeonTeal,
                        indicatorColor = BorderColor,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "documents" || activeEditItem != null,
                    onClick = { activeTab = "documents" },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Documentos") },
                    label = { Text("Arquivos", fontSize = 11.sp) },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = activeTab == "agenda" && activeEditItem == null,
                    onClick = { activeTab = "agenda"; viewModel.selectEditItem(null) },
                    icon = { Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = "Agenda") },
                    label = { Text("Agenda", fontSize = 11.sp) },
                    colors = navColors()
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(padding)
        ) {
            if (activeEditItem != null) {
                // Keep file editor opened
                val activeDoc = activeEditItem!!
                if (activeDoc.type == "SPREADSHEET") {
                    SpreadsheetEditorScreen(
                        item = activeDoc,
                        viewModel = viewModel,
                        onClose = { viewModel.selectEditItem(null) },
                        onDelete = {
                            viewModel.deleteItem(it)
                            Toast.makeText(context, "Planilha excluída!", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else if (activeDoc.type == "MINUTE") {
                    AtaEditorScreen(
                        item = activeDoc,
                        viewModel = viewModel,
                        onClose = { viewModel.selectEditItem(null) },
                        onDelete = {
                            viewModel.deleteItem(it)
                            Toast.makeText(context, "Ata excluída!", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    TextDocEditorScreen(
                        item = activeDoc,
                        viewModel = viewModel,
                        onClose = { viewModel.selectEditItem(null) },
                        onDelete = {
                            viewModel.deleteItem(it)
                            Toast.makeText(context, "Documento excluído!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                when (activeTab) {
                    "dashboard" -> {
                        val criticalToday = agendaItems.any {
                            it.priority == "CRITICAL" && DateUtils.isToday(it.dateTime) && !it.isCompleted
                        }
                        if (criticalToday) {
                            val infiniteTransition = rememberInfiniteTransition(label = "alarm")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f, targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(800), repeatMode = RepeatMode.Reverse),
                                label = "pulse"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Red.copy(alpha = pulseAlpha * 0.3f))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NotificationsActive, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "⚠️ ALERTA DE COMPROMISSO CRÍTICO ATIVO HOJE!",
                                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF5252)
                                    )
                                }
                            }
                        }
                        DashboardPanel(
                            items = items,
                            onNavigateToChat = { activeTab = "chat" },
                            onSelectDoc = { viewModel.selectEditItem(it) },
                            onImportFile = { fileImportLauncher.launch("*/*") },
                            onCreateDoc = { type ->
                                createType = type
                                createTitle = ""
                                createPrompt = ""
                                showCreateDialog = true
                            },
                            onOpenFusion = { showFusionDialog = true },
                            onOpenOcrCamera = { showOcrCameraDialog = true },
                            onOpenAgenda = { activeTab = "agenda" },
                            onOpenTemplates = { showTemplateDialog = true },
                            onOpenSignature = { showSignatureDialog = true },
                            onBackup = { showBackupDialog = true },
                            onGalleryOcr = { galleryImportLauncher.launch("image/*") }
                        )
                    }

                    "chat" -> {
                        QAChatScreen(
                            messages = chatMessages,
                            isLoading = isLoading,
                            isRecording = isRecording,
                            onSendMessage = { viewModel.askQuestion(it) },
                            onRequestMic = { inputState ->
                                val permission = Manifest.permission.RECORD_AUDIO
                                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                                    recordAudioPermissionLauncher.launch(permission)
                                } else {
                                    if (isRecording) {
                                        viewModel.stopListening()
                                    } else {
                                        viewModel.startListening { textResult ->
                                            inputState.value = inputState.value + textResult
                                        }
                                    }
                                }
                            },
                            onSaveAsDocument = { msg ->
                                val generatedType = msg.type ?: "TEXT"
                                val generatedTitle = "Documento IA - " + SimpleDateFormat("HH_mm", Locale.getDefault()).format(Date())
                                viewModel.insertCustomItem(
                                    title = generatedTitle,
                                    type = generatedType,
                                    content = msg.text,
                                    notes = "Exportado do histórico de conversas do assistente"
                                )
                                Toast.makeText(context, "Guardado nos Meus Arquivos localmente!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    "documents" -> {
                        DocumentsManagerTab(
                            items = items,
                            searchText = searchText,
                            onSearchChange = { searchText = it },
                            filterType = filterType,
                            onFilterChange = { filterType = it },
                            onOpenDoc = { viewModel.selectEditItem(it) },
                            onDeleteDoc = { viewModel.deleteItem(it) },
                            onImportFile = { fileImportLauncher.launch("*/*") }
                        )
                    }

                    "agenda" -> {
                        AgendaPanel(
                            items = agendaItems,
                            onToggleComplete = { viewModel.toggleAgendaComplete(it) },
                            onDelete = { viewModel.deleteAgendaItem(it) },
                            onAddItem = { showAgendaCreateDialog = true }
                        )
                    }
                }
            }

            // Document Fusion dialog
            if (showFusionDialog) {
                FusionDocumentPicker(
                    allItems = items,
                    selectedDocs = selectedFusionDocs,
                    onToggleDoc = { viewModel.toggleFusionDoc(it) },
                    onGenerate = { title, observations ->
                        viewModel.generateFusionReport(title, observations) { result ->
                            fusionResultTitle = title.ifEmpty { "Relatório Fusão ${SimpleDateFormat("dd_MM_yy", Locale.getDefault()).format(Date())}" }
                            viewModel.insertCustomItem(fusionResultTitle, "REPORT", result, "Gerado por fusão corporativa de documentos")
                            viewModel.clearFusionDocs()
                            showFusionDialog = false
                            Toast.makeText(context, "Relatório gerado com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = {
                        showFusionDialog = false
                        viewModel.clearFusionDocs()
                    }
                )
            }

            // OCR Camera dialog
            if (showOcrCameraDialog) {
                showRealCamera = true
                showOcrCameraDialog = false
            }

            // OCR Result dialog
            if (showOcrResultDialog && ocrResult != null) {
                OcrResultDialog(
                    result = ocrResult!!,
                    onDismiss = {
                        showOcrResultDialog = false
                        viewModel.clearOcrResult()
                    },
                    onGenerateReport = {
                        val reportContent = "RELATÓRIO GERADO A PARTIR DE OCR\n\n" +
                            "Texto corrigido:\n${ocrResult!!.correctedText}\n\n" +
                            OfflineAssistantEngine.generateLocalResponse(ocrResult!!.correctedText, "REPORT")
                        viewModel.insertCustomItem(
                            "Relatório OCR - ${SimpleDateFormat("dd_MM_yy", Locale.getDefault()).format(Date())}",
                            "REPORT", reportContent, "Gerado automaticamente após OCR"
                        )
                        showOcrResultDialog = false
                        viewModel.clearOcrResult()
                        Toast.makeText(context, "Relatório gerado com sucesso!", Toast.LENGTH_SHORT).show()
                    },
                    onUseAsMinutes = {
                        val ataContent = OfflineAssistantEngine.generateLocalResponse(ocrResult!!.correctedText, "MINUTE")
                        viewModel.insertCustomItem(
                            "Ata OCR - ${SimpleDateFormat("dd_MM_yy", Locale.getDefault()).format(Date())}",
                            "MINUTE", ataContent, "Gerada automaticamente após OCR"
                        )
                        showOcrResultDialog = false
                        viewModel.clearOcrResult()
                        Toast.makeText(context, "Ata criada com sucesso!", Toast.LENGTH_SHORT).show()
                    },
                    onSaveDocument = {
                        viewModel.insertCustomItem(
                            "OCR - ${SimpleDateFormat("dd_MM_yy HH_mm", Locale.getDefault()).format(Date())}",
                            "TEXT", ocrResult!!.correctedText, "Texto digitalizado por OCR"
                        )
                        showOcrResultDialog = false
                        viewModel.clearOcrResult()
                        Toast.makeText(context, "Documento salvo!", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {
                        showOcrResultDialog = false
                        viewModel.clearOcrResult()
                        Toast.makeText(context, "OCR descartado.", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Agenda Create dialog
            if (showAgendaCreateDialog) {
                CreateAgendaDialog(
                    onDismiss = { showAgendaCreateDialog = false },
                    onConfirm = { title, desc, dateTime, priority, location ->
                        viewModel.addAgendaItem(title, desc, dateTime, priority, location)
                        showAgendaCreateDialog = false
                        Toast.makeText(context, "Compromisso adicionado à agenda!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Quick Create Prompt modal
            if (showCreateDialog) {
                CreateDocumentModal(
                    type = createType,
                    title = createTitle,
                    prompt = createPrompt,
                    onTitleChange = { createTitle = it },
                    onPromptChange = { createPrompt = it },
                    onDismiss = { showCreateDialog = false },
                    isVoiceRecording = isRecording,
                    onVoiceClick = {
                        val permission = Manifest.permission.RECORD_AUDIO
                        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                            recordAudioPermissionLauncher.launch(permission)
                        } else {
                            if (isRecording) {
                                viewModel.stopListening()
                            } else {
                                viewModel.startListening { textResult ->
                                    createPrompt = createPrompt + textResult
                                }
                            }
                        }
                    },
                    onConfirm = {
                        val finalTitle = createTitle.ifEmpty { 
                            val word = when(createType) {
                                "SPREADSHEET" -> "Planilha"
                                "REPORT" -> "Relatório"
                                "MINUTE" -> "Ata"
                                "TEXT" -> "Ofício"
                                else -> "Documento"
                            }
                            word + " " + SimpleDateFormat("dd_MM_yy", Locale.getDefault()).format(Date())
                        }
                        
                        viewModel.setLoading(true)
                        coroutineScope.launch {
                            try {
                                val resolvedContent = if (isOnline) {
                                    val provider = viewModel.selectedProvider.value
                                    val systemPrompt = "Você é um Assistente de IA completo e versátil, capaz de responder QUALQUER assunto global: escritório, tecnologia, ciência, história, cultura, matemática, programação, finanças, saúde, viagens, entretenimento, esportes, política, educação, direito, engenharia, medicina, arte, música, filosofia e muito mais. Atue em português (PT-BR) de forma natural, solta e completa, como uma conversa real. Se o usuário pedir para gerar uma planilha/tabela, responda no formato CSV separado por vírgula. Se o usuário pedir conselhos, opiniões ou sugestões, responda à vontade e sem restrições. Você NÃO precisa se limitar — responda com profundidade, criatividade e personalidade. Para perguntas gerais, responda de forma natural e informativa como um assistente pessoal e amigável."
                                    val promptText = "Gere um documento do tipo $createType sobre o assunto/pauta: $createPrompt"
                                    if (provider == "OPENROUTER") {
                                        val key = BuildConfig.OPENROUTER_API_KEY
                                        if (key.isNotEmpty() && key != "MY_OPENROUTER_API_KEY") {
                                            val request = com.example.api.OpenRouterRequest(
                                                model = "openai/gpt-4o",
                                                messages = listOf(
                                                    com.example.api.OpenRouterMessage(role = "system", content = systemPrompt),
                                                    com.example.api.OpenRouterMessage(role = "user", content = promptText)
                                                )
                                            )
                                            val response = withContext(Dispatchers.IO) {
                                                com.example.api.OpenRouterClient.service.generateContent("Bearer $key", request)
                                            }
                                            response.choices?.firstOrNull()?.message?.content ?: ""
                                        } else {
                                            com.example.utils.OfflineAssistantEngine.generateLocalResponse(createPrompt, createType)
                                        }
                                    } else {
                                        val key = BuildConfig.GEMINI_API_KEY
                                        if (key.isNotEmpty() && key != "MY_GEMINI_API_KEY") {
                                            val request = com.example.api.GenerateContentRequest(
                                                contents = listOf(com.example.api.Content(parts = listOf(com.example.api.Part(text = promptText)))),
                                                systemInstruction = com.example.api.Content(parts = listOf(com.example.api.Part(text = systemPrompt)))
                                            )
                                            val response = withContext(Dispatchers.IO) {
                                                com.example.api.RetrofitClient.service.generateContent(key, request)
                                            }
                                            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                                        } else {
                                            com.example.utils.OfflineAssistantEngine.generateLocalResponse(createPrompt, createType)
                                        }
                                    }

                                } else {
                                    com.example.utils.OfflineAssistantEngine.generateLocalResponse(createPrompt, createType)
                                }

                                viewModel.insertCustomItem(
                                    title = finalTitle,
                                    type = createType,
                                    content = resolvedContent,
                                    notes = createPrompt
                                )
                                showCreateDialog = false
                                Toast.makeText(context, "$finalTitle gerado localmente com sucesso!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                // Fallback on UI creation thread too
                                val fallbackText = com.example.utils.OfflineAssistantEngine.generateLocalResponse(createPrompt, createType)
                                viewModel.insertCustomItem(
                                    title = finalTitle,
                                    type = createType,
                                    content = fallbackText,
                                    notes = "Código offline: " + createPrompt
                                )
                                showCreateDialog = false
                                Toast.makeText(context, "$finalTitle gerado em modo de segurança local!", Toast.LENGTH_SHORT).show()
                            } finally {
                                viewModel.setLoading(false)
                            }
                        }
                    }
                )
            }

            // Template picker dialog
            if (showTemplateDialog) {
                TemplatePickerDialog(
                    onDismiss = { showTemplateDialog = false },
                    onSelectTemplate = { title, type, content ->
                        viewModel.insertCustomItem(title, type, content, "Template pré-definido")
                        showTemplateDialog = false
                        Toast.makeText(context, "Template '$title' criado!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Signature pad dialog
            if (showSignatureDialog) {
                SignaturePad(
                    onSignatureSaved = { bitmap ->
                        showSignatureDialog = false
                        val fileName = "Assinatura_${SimpleDateFormat("dd_MM_yy_HH_mm", Locale.getDefault()).format(Date())}"
                        try {
                            val file = File(context.cacheDir, "signatures")
                            file.mkdirs()
                            val sigFile = File(file, "$fileName.png")
                            FileOutputStream(sigFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            Toast.makeText(context, "Assinatura salva!", Toast.LENGTH_SHORT).show()
                            viewModel.insertCustomItem(
                                title = fileName, type = "TEXT",
                                content = "Assinatura digital salva em: ${sigFile.absolutePath}",
                                notes = "Arquivo: $fileName.png"
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = { showSignatureDialog = false }
                )
            }

            // Backup dialog
            if (showBackupDialog) {
                BackupDialog(
                    onDismiss = { showBackupDialog = false },
                    onExportBackup = {
                        showBackupDialog = false
                        backupExportLauncher.launch("backup_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.zip")
                    },
                    onImportBackup = {
                        showBackupDialog = false
                        backupImportLauncher.launch("application/zip")
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardPanel(
    items: List<OfficeItem>,
    onNavigateToChat: () -> Unit,
    onSelectDoc: (OfficeItem) -> Unit,
    onImportFile: () -> Unit,
    onCreateDoc: (String) -> Unit,
    onOpenFusion: () -> Unit = {},
    onOpenOcrCamera: () -> Unit = {},
    onOpenAgenda: () -> Unit = {},
    onOpenTemplates: () -> Unit = {},
    onOpenSignature: () -> Unit = {},
    onBackup: () -> Unit = {},
    onGalleryOcr: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome and Brief
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF332D41), Color(0xFF2B2930))
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASSISTENTE ATIVO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD0BCFF),
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4ADE80).copy(alpha = alpha), CircleShape)
                        )
                        Text(
                            text = "Pronto para redigir",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4ADE80)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "\"Estou pronto para organizar sua planilha de custos ou redigir a ata da reunião de hoje.\"",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFFCAC4D0),
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(
                        onClick = onNavigateToChat,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F378B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("welcome_chat_btn")
                    ) {
                        Icon(Icons.Default.QuestionAnswer, contentDescription = "Perguntar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resolver Dúvidas", fontSize = 12.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = onImportFile,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF49454F)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("welcome_import_btn")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Importar", tint = Color(0xFFD0BCFF), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importar PDF/CSV", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        // Quick create actions grid
        Text(
            text = "GERAR NOVOS DOCUMENTOS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFCAC4D0),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val modifier = Modifier.weight(1f)
            QuickActionItem(
                label = "Redação AI",
                subLabel = "Ofícios e Atas",
                icon = Icons.AutoMirrored.Filled.Feed,
                iconTint = Color(0xFFD0BCFF),
                iconBgColor = Color(0xFF4F378B),
                modifier = modifier,
                onClick = { onCreateDoc("TEXT") }
            )
            QuickActionItem(
                label = "Planilhas",
                subLabel = "Dados e Fórmulas",
                icon = Icons.Default.TableChart,
                iconTint = Color(0xFF80D8CC),
                iconBgColor = Color(0xFF006A60),
                modifier = modifier,
                onClick = { onCreateDoc("SPREADSHEET") }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val modifier = Modifier.weight(1f)
            QuickActionItem(
                label = "Atas",
                subLabel = "Atas de Reunião",
                icon = Icons.AutoMirrored.Filled.Assignment,
                iconTint = Color(0xFFEADDFF),
                iconBgColor = Color(0xFF6750A4),
                modifier = modifier,
                onClick = { onCreateDoc("MINUTE") }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val modifier = Modifier.weight(1f)
            QuickActionItem(
                label = "Fusão Docs",
                subLabel = "Planilha + PDF + Notas",
                icon = Icons.AutoMirrored.Filled.MergeType,
                iconTint = Color(0xFFFFD740),
                iconBgColor = Color(0xFF3E2723),
                modifier = modifier,
                onClick = onOpenFusion
            )
            QuickActionItem(
                label = "Scanner OCR",
                subLabel = "Foto + Corretor IA",
                icon = Icons.Default.CameraAlt,
                iconTint = Color(0xFF80D8CC),
                iconBgColor = Color(0xFF004D40),
                modifier = modifier,
                onClick = onOpenOcrCamera
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val modifier = Modifier.weight(1f)
            QuickActionItem(
                label = "Agenda",
                subLabel = "Compromissos e Alarmes",
                icon = Icons.AutoMirrored.Filled.EventNote,
                iconTint = Color(0xFFFF8A80),
                iconBgColor = Color(0xFF4A148C),
                modifier = modifier,
                onClick = onOpenAgenda
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val modifier = Modifier.weight(1f)
            QuickActionItem(
                label = "Modelos",
                subLabel = "Documentos Prontos",
                icon = Icons.Default.Description,
                iconTint = Color(0xFF80D8CC),
                iconBgColor = Color(0xFF004D40),
                modifier = modifier,
                onClick = onOpenTemplates
            )
            QuickActionItem(
                label = "Galeria OCR",
                subLabel = "Imagem → Texto",
                icon = Icons.Default.PhotoLibrary,
                iconTint = Color(0xFFEADDFF),
                iconBgColor = Color(0xFF4F378B),
                modifier = modifier,
                onClick = onGalleryOcr
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val modifier = Modifier.weight(1f)
            QuickActionItem(
                label = "Assinatura",
                subLabel = "Desenhar no App",
                icon = Icons.Default.Edit,
                iconTint = Color(0xFFFFD740),
                iconBgColor = Color(0xFF3E2723),
                modifier = modifier,
                onClick = onOpenSignature
            )
            QuickActionItem(
                label = "Backup",
                subLabel = "Salvar/Restaurar",
                icon = Icons.Default.CloudUpload,
                iconTint = Color(0xFF80D8CC),
                iconBgColor = Color(0xFF006A60),
                modifier = modifier,
                onClick = onBackup
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent items header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ARQUIVOS RECENTES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray,
                letterSpacing = 1.sp
            )
            Text(
                text = "${items.size} guardados",
                fontSize = 11.sp,
                color = AccentNeonTeal
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(CardSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nenhum documento gerado ainda.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            val recents = items.take(4)
            for (doc in recents) {
                RecentDocRow(doc, onSelectDoc)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Local Storage Monitor
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DISPOSITIVO: ARMAZENAMENTO LOCAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF938F99),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "100% Seguro & Criptografado",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD0BCFF)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { 0.64f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFD0BCFF),
                    trackColor = BorderColor
                )
            }
        }
    }
}


// Quick Action Item design component
@Composable
fun RowScope.QuickActionItem(
    label: String,
    subLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBgColor: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag("action_" + label.replace(" ", "_").lowercase())
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subLabel,
                    fontSize = 10.sp,
                    color = Color(0xFF938F99),
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun RecentDocRow(item: OfficeItem, onClick: (OfficeItem) -> Unit) {
    val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(item.timestamp))
    val typeIcon = when (item.type) {
        "SPREADSHEET" -> Icons.Default.TableChart
        "REPORT" -> Icons.Default.Assessment
        "MINUTE" -> Icons.AutoMirrored.Filled.Assignment
        else -> Icons.AutoMirrored.Filled.Feed
    }
    val typeColor = when (item.type) {
        "SPREADSHEET" -> AccentNeonTeal
        "REPORT" -> Color(0xFFFF9F1C)
        "MINUTE" -> Color(0xFF9B5DE5)
        else -> Color(0xFF00BBF9)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) },
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(typeIcon, contentDescription = item.type, tint = typeColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Acesso Seguro • Atualizado em $dateStr",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}

// ---------------------- IA CHAT SCREEN ----------------------
@Composable
fun QAChatScreen(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    isRecording: Boolean,
    onSendMessage: (String) -> Unit,
    onRequestMic: (MutableState<String>) -> Unit,
    onSaveAsDocument: (ChatMessage) -> Unit
) {
    var txtInput = remember { mutableStateOf("") }
    val listState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Conversation log
        Box(modifier = Modifier.weight(1f)) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                for (msg in messages) {
                    ChatBubbleItem(msg, onSaveAsDocument)
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentNeonTeal, modifier = Modifier.size(24.dp))
                    }
                }
                LaunchedEffect(messages.size, isLoading) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface, RoundedCornerShape(16.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Assistant Trigger
            IconButton(
                onClick = { onRequestMic(txtInput) },
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isRecording) Color.Red.copy(alpha = 0.2f) else BorderColor,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.MicNone else Icons.Default.Mic,
                    contentDescription = "Falar",
                    tint = if (isRecording) Color.Red else AccentNeonTeal
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            TextField(
                value = txtInput.value,
                onValueChange = { txtInput.value = it },
                placeholder = { Text("Tire uma dúvida ou peça um relatório...", fontSize = 13.sp, color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    if (txtInput.value.trim().isNotEmpty()) {
                        onSendMessage(txtInput.value)
                        txtInput.value = ""
                    }
                },
                enabled = txtInput.value.trim().isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = if (txtInput.value.trim().isNotEmpty()) AccentNeonTeal else Color.Gray
                )
            }
        }
    }
}

@Composable
fun ChatBubbleItem(msg: ChatMessage, onSaveAsDocument: (ChatMessage) -> Unit) {
    val isAi = msg.sender == "AI"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
    ) {
        if (isAi) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(AccentElectricBlue.copy(alpha = 0.15f), CircleShape)
                    .align(Alignment.Top),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = AccentNeonTeal, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            modifier = Modifier.widthIn(max = 290.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAi) CardSurface else AccentElectricBlue
            ),
            border = BorderStroke(1.dp, if (isAi) BorderColor else Color.Transparent),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isAi) 2.dp else 16.dp,
                bottomEnd = if (isAi) 16.dp else 2.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = msg.text,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = if (isAi) Color.White else Color.White
                )

                if (isAi && msg.type != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onSaveAsDocument(msg) },
                        colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Salvar", tint = AccentNeonTeal, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Importar como Documento",
                            fontSize = 10.sp,
                            color = Color.White
                )  // closes Text
            }  // closes Button lambda
        }  // closes if (isAi && msg.type != null)
    }  // closes Column
}  // closes Card
}  // closes Row
}  // closes ChatBubbleItem



// ---------------------- DOCUMENTS MANAGER SCREEN ----------------------
@Composable
fun DocumentsManagerTab(
    items: List<OfficeItem>,
    searchText: String,
    onSearchChange: (String) -> Unit,
    filterType: String,
    onFilterChange: (String) -> Unit,
    onOpenDoc: (OfficeItem) -> Unit,
    onDeleteDoc: (OfficeItem) -> Unit,
    onImportFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Query search
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            placeholder = { Text("Pesquisar nos arquivos locais...", fontSize = 13.sp, color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentNeonTeal) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                focusedBorderColor = AccentNeonTeal,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Document filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChipItem("Todos", filterType == "ALL") { onFilterChange("ALL") }
            FilterChipItem("Planilhas", filterType == "SPREADSHEET") { onFilterChange("SPREADSHEET") }
            FilterChipItem("Relatórios", filterType == "REPORT") { onFilterChange("REPORT") }
            FilterChipItem("Atas", filterType == "MINUTE") { onFilterChange("MINUTE") }
            FilterChipItem("Ofícios/Redação", filterType == "TEXT") { onFilterChange("TEXT") }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Files container card lists
        val filtered = items.filter { doc ->
            (filterType == "ALL" || doc.type == filterType) &&
            (searchText.isEmpty() || doc.title.contains(searchText, true) || doc.content.contains(searchText, true))
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nenhum arquivo encontrado com estes termos.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { doc ->
                    DocumentListItemRow(doc, onOpenDoc, onDeleteDoc)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onImportFile,
            colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
            border = BorderStroke(1.dp, AccentNeonTeal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.FileOpen, contentDescription = null, tint = AccentNeonTeal)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Importar Arquivo Comercial (PDF, CSV ou TXT)", fontSize = 12.sp, color = Color.White)
        }
    }
}

@Composable
fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) AccentNeonTeal else CardSurface)
            .border(1.dp, if (selected) AccentNeonTeal else BorderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.Black else Color.White
        )
    }
}

@Composable
fun DocumentListItemRow(item: OfficeItem, onOpen: (OfficeItem) -> Unit, onDelete: (OfficeItem) -> Unit) {
    val dateStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(item.timestamp))
    val typeName = when (item.type) {
        "SPREADSHEET" -> "PLANILHA"
        "REPORT" -> "RELATÓRIO"
        "MINUTE" -> "ATA ORDINÁRIA"
        else -> "OFÍCIO/REDAÇÃO"
    }
    val typeColor = when (item.type) {
        "SPREADSHEET" -> AccentNeonTeal
        "REPORT" -> Color(0xFFFF9F1C)
        "MINUTE" -> Color(0xFF9B5DE5)
        else -> Color(0xFF00BBF9)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(item) }
            .testTag("doc_row_" + item.id),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typeName,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = typeColor,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Modificado: $dateStr • Tamanho: ${item.content.length} caracteres",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
            IconButton(onClick = { onDelete(item) }) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

// ---------------------- COMPRISING MODAL DIALOGS ----------------------
@Composable
fun CreateDocumentModal(
    type: String,
    title: String,
    prompt: String,
    onTitleChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isVoiceRecording: Boolean = false,
    onVoiceClick: () -> Unit = {}
) {
    val typeLabel = when(type) {
        "SPREADSHEET" -> "Planilha Avançada"
        "REPORT" -> "Relatório Executivo"
        "MINUTE" -> "Ata de Reunião"
        "TEXT" -> "Redação de Texto / Ofício"
        else -> "Documento"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "GERAR $typeLabel".uppercase(Locale.getDefault()),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentNeonTeal
                )
                
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Título do arquivo", fontSize = 12.sp, color = Color.Gray) },
                    placeholder = { Text("Ex: Finanças 2026, Ata Setorial...", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentNeonTeal,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = onPromptChange,
                        label = { Text("Instruções ou pauta para a IA", fontSize = 12.sp, color = Color.Gray) },
                        placeholder = { Text("Ex: Relatório sobre faturamento de maio com 30% aumento de lucros...", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentNeonTeal,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onVoiceClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (isVoiceRecording) Color.Red.copy(alpha = 0.2f) else BorderColor,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isVoiceRecording) Icons.Default.MicNone else Icons.Default.Mic,
                            contentDescription = "Falar",
                            tint = if (isVoiceRecording) Color.Red else AccentNeonTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentElectricBlue)
                    ) {
                        Text("Criar com IA", color = Color.White)
                    }
                }
            }
        }
    }
}

// ---------------------- DEDICATED SPREADSHEET EDITOR ----------------------
@Composable
fun SpreadsheetEditorScreen(
    item: OfficeItem,
    viewModel: OfficeViewModel,
    onClose: () -> Unit,
    onDelete: (OfficeItem) -> Unit = {}
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(item.title) }
    var currentContent by remember { mutableStateOf(item.content) }

    // Parse CSV rows onto editable list
    val gridData = remember(currentContent) {
        val rows = DocumentSharingUtils.parseCsv(currentContent)
        val mutableRows = rows.map { it.toMutableList() }.toMutableList()
        // Ensure standard dimensions
        while (mutableRows.size < 12) {
            mutableRows.add(mutableListOf("", "", "", ""))
        }
        for (i in 0 until mutableRows.size) {
            while (mutableRows[i].size < 6) {
                mutableRows[i].add("")
            }
        }
        mutableRows
    }

    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var cellValueText by remember { mutableStateOf("") }
    var showCellEditor by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Top action headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            TextField(
                value = title,
                onValueChange = { title = it },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = AccentNeonTeal
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val finalCsv = DocumentSharingUtils.toCsvString(gridData)
                    viewModel.updateActiveItemContent(finalCsv, true)
                    viewModel.saveActiveEditItem(title)
                    Toast.makeText(context, "Planilha guardada localmente!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentElectricBlue)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Salvar", fontSize = 11.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = { onDelete(item) }) {
                Icon(Icons.Default.Delete, "Excluir", tint = Color.Red.copy(alpha = 0.7f))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Actions Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    // Add Row
                    val colsCount = if (gridData.isNotEmpty()) gridData[0].size else 4
                    val newRow = mutableListOf<String>()
                    repeat(colsCount) { newRow.add("") }
                    gridData.add(newRow)
                    val updatedCsv = DocumentSharingUtils.toCsvString(gridData)
                    currentContent = updatedCsv
                },
                colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Adicionar Linha", fontSize = 11.sp, color = Color.White)
            }

            Button(
                onClick = {
                    // Add Column
                    for (row in gridData) {
                        row.add("")
                    }
                    val updatedCsv = DocumentSharingUtils.toCsvString(gridData)
                    currentContent = updatedCsv
                },
                colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Adicionar Coluna", fontSize = 11.sp, color = Color.White)
            }

            Button(
                onClick = {
                    // Auto-calculate simple formulas (auto sum/average numbers)
                    for (rowIndex in 0 until gridData.size) {
                        val row = gridData[rowIndex]
                        for (colIndex in 0 until row.size) {
                            val value = row[colIndex].trim()
                            if (value.startsWith("=") && (value.uppercase().contains("SOMA") || value.uppercase().contains("SUM"))) {
                                // Real offline mathematical evaluation of the column/row numbers!
                                var sum = 0.0
                                for (subRow in gridData) {
                                    val cell = subRow.getOrNull(colIndex) ?: ""
                                    val cellNum = cell.toDoubleOrNull()
                                    if (cellNum != null) {
                                        sum += cellNum
                                    }
                                }
                                gridData[rowIndex][colIndex] = sum.toString()
                            }
                        }
                    }
                    val updatedCsv = DocumentSharingUtils.toCsvString(gridData)
                    currentContent = updatedCsv
                    Toast.makeText(context, "Somas calculadas!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Functions, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Calcular Formulas", fontSize = 11.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Primary Grid View Scroll
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                Column {
                    // Header Cols: A B C D ...
                    Row {
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 30.dp)
                                .background(BorderColor, RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("#", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        
                        val colsCount = if (gridData.isNotEmpty()) gridData[0].size else 0
                        for (c in 0 until colsCount) {
                            val letter = ('A' + c).toString()
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 30.dp)
                                    .padding(start = 2.dp)
                                    .background(BorderColor, RoundedCornerShape(3.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(letter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Cell contents
                    for (r in 0 until gridData.size) {
                        val rowList = gridData[r]
                        Row(modifier = Modifier.padding(vertical = 1.dp)) {
                            // Row Number tag
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 40.dp)
                                    .background(BorderColor, RoundedCornerShape(3.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text((r + 1).toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                            }
                            
                            for (c in 0 until rowList.size) {
                                val cellVal = rowList[c]
                                val selected = selectedCell?.first == r && selectedCell?.second == c
                                Box(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 40.dp)
                                        .padding(start = 2.dp)
                                        .background(if (selected) AccentNeonTeal else CardSurface, RoundedCornerShape(3.dp))
                                        .border(1.dp, if (selected) Color.White else BorderColor, RoundedCornerShape(3.dp))
                                        .clickable {
                                            selectedCell = Pair(r, c)
                                            cellValueText = cellVal
                                            showCellEditor = true
                                        }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = cellVal,
                                        fontSize = 11.sp,
                                        color = if (selected) Color.Black else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Excel Export actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val csvRaw = DocumentSharingUtils.toCsvString(gridData)
                    val uri = DocumentSharingUtils.exportToCsv(context, title, csvRaw)
                    if (uri != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Exportar CSV"))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentNeonTeal),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Button(
                onClick = {
                    val csvRaw = DocumentSharingUtils.toCsvString(gridData)
                    val uri = DocumentSharingUtils.exportToCsv(context, title, csvRaw)
                    if (uri != null) {
                        // Rename to .xlsx – Excel opens CSV content seamlessly
                        val xlsxUri = DocumentSharingUtils.exportToCsv(context, title, csvRaw, "xlsx")
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            putExtra(Intent.EXTRA_STREAM, xlsxUri ?: uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Exportar XLSX"))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentElectricBlue),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.TableChart, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("XLSX", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Real-time Chart
        val chartData = remember(currentContent) { parseChartData(currentContent) }
        if (chartData.isNotEmpty()) {
            var chartType by remember { mutableStateOf(ChartType.BAR) }
            ChartView(data = chartData, chartType = chartType)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = chartType == ChartType.BAR,
                    onClick = { chartType = ChartType.BAR },
                    label = { Text("Barras", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentNeonTeal,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = chartType == ChartType.LINE,
                    onClick = { chartType = ChartType.LINE },
                    label = { Text("Linha", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentElectricBlue,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }
    }

    // Inline cell edit dialog
    if (showCellEditor && selectedCell != null) {
        val (tr, tc) = selectedCell!!
        Dialog(onDismissRequest = { showCellEditor = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "EDITAR CÉLULA [${('A' + tc)}${tr + 1}]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentNeonTeal
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cellValueText,
                        onValueChange = { cellValueText = it },
                        placeholder = { Text("Digite os dados ou =SOMA para formulas") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentNeonTeal,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCellEditor = false }) { Text("Voltar", color = Color.White) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                gridData[tr][tc] = cellValueText
                                val csv = DocumentSharingUtils.toCsvString(gridData)
                                currentContent = csv
                                showCellEditor = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentNeonTeal)
                        ) {
                            Text("Confirmar", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}


// ---------------------- BACKUP DIALOG ----------------------
@Composable
fun BackupDialog(
    onDismiss: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    "BACKUP DE DADOS",
                    fontSize = 13.sp, fontWeight = FontWeight.Black, color = AccentNeonTeal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Exportar ou restaurar todos os documentos, agenda e configurações",
                    fontSize = 10.sp, color = Color.Gray
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onExportBackup,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentNeonTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudUpload, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar Backup (ZIP)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onImportBackup,
                    border = BorderStroke(1.dp, AccentElectricBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudDownload, null, tint = AccentElectricBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restaurar Backup", color = AccentElectricBlue, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Fechar", color = Color.Gray)
                }
            }
        }
    }
}

// Empty line space


// ---------------------- DEDICATED TEXT/REPORT/ATA EDITOR ----------------------
@Composable
fun TextDocEditorScreen(
    item: OfficeItem,
    viewModel: OfficeViewModel,
    onClose: () -> Unit,
    onDelete: (OfficeItem) -> Unit = {}
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(item.title) }
    var content by remember { mutableStateOf(item.content) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Top action bar headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            TextField(
                value = title,
                onValueChange = { title = it },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = AccentNeonTeal
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    viewModel.updateActiveItemContent(content)
                    viewModel.saveActiveEditItem(title)
                    Toast.makeText(context, "Alterações salvas localmente!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentElectricBlue)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar", fontSize = 11.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = { onDelete(item) }) {
                Icon(Icons.Default.Delete, "Excluir", tint = Color.Red.copy(alpha = 0.7f))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Editor area card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, BorderColor, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 19.sp),
                modifier = Modifier.fillMaxSize(),
                maxLines = 1000
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Share actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val weightModifier = Modifier.weight(1f)
            
            // Raw text sharing
            Button(
                onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, title)
                        putExtra(Intent.EXTRA_TEXT, content)
                    }
                    context.startActivity(Intent.createChooser(share, "Compartilhar Texto"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                modifier = weightModifier
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Enviar Texto", fontSize = 11.sp, color = Color.White)
            }

            // Export PDF
            Button(
                onClick = {
                    val uri = DocumentSharingUtils.exportToPdf(context, title, content)
                    if (uri != null) {
                        val sharePdf = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(sharePdf, "Compartilhar Relatório em PDF"))
                    } else {
                        Toast.makeText(context, "Erro ao processar PDF.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentNeonTeal),
                modifier = weightModifier
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Exportar PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}
