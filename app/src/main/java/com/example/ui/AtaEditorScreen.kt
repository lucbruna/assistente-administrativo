package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OfficeItem
import com.example.utils.DocumentSharingUtils
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardSurface = Color(0xFF2B2930)
private val BorderColor = Color(0xFF49454F)
private val AccentNeonTeal = Color(0xFF80D8CC)
private val AccentElectricBlue = Color(0xFFD0BCFF)
private val DarkBackground = Color(0xFF1C1B1F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtaEditorScreen(
    item: OfficeItem,
    viewModel: OfficeViewModel,
    onClose: () -> Unit,
    onDelete: (OfficeItem) -> Unit = {}
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(item.title) }
    var pauta by remember { mutableStateOf("") }
    var data by remember { mutableStateOf("") }
    var local by remember { mutableStateOf("") }
    var participantes by remember { mutableStateOf("") }
    var discussoes by remember { mutableStateOf("") }
    var encaminhamentos by remember { mutableStateOf("") }

    LaunchedEffect(item.id) {
        val json = try {
            JSONObject(item.content)
        } catch (e: Exception) {
            JSONObject().apply {
                put("discussoes", item.content)
                put("data", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
            }
        }
        pauta = json.optString("pauta", "")
        data = json.optString("data", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
        local = json.optString("local", "")
        participantes = json.optString("participantes", "")
        discussoes = json.optString("discussoes", "")
        encaminhamentos = json.optString("encaminhamentos", "")
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val save: () -> Unit = {
        val json = JSONObject().apply {
            put("pauta", pauta)
            put("data", data)
            put("local", local)
            put("participantes", participantes)
            put("discussoes", discussoes)
            put("encaminhamentos", encaminhamentos)
        }.toString()
        viewModel.updateActiveItemContent(json)
        viewModel.saveActiveEditItem(title)
        Toast.makeText(context, "Ata salva com sucesso!", Toast.LENGTH_SHORT).show()
    }

    val shareText = buildString {
        appendLine("ATA DE REUNIÃO")
        appendLine("Título: $title")
        appendLine("Pauta: $pauta")
        appendLine("Data: $data")
        appendLine("Local: $local")
        appendLine()
        appendLine("Participantes:")
        appendLine(participantes)
        appendLine()
        appendLine("Discussões / Deliberações:")
        appendLine(discussoes)
        appendLine()
        appendLine("Encaminhamentos / Ações:")
        appendLine(encaminhamentos)
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AccentElectricBlue,
                    unfocusedBorderColor = BorderColor,
                    cursorColor = AccentElectricBlue
                ),
                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
            )
            IconButton(onClick = save) {
                Icon(Icons.Default.Save, "Salvar", tint = AccentElectricBlue)
            }
            IconButton(onClick = { onDelete(item) }) {
                Icon(Icons.Default.Delete, "Excluir", tint = Color(0xFFEF5350))
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Pauta", color = AccentNeonTeal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pauta,
                        onValueChange = { pauta = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Assunto da reunião", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentElectricBlue,
                            unfocusedBorderColor = BorderColor,
                            cursorColor = AccentElectricBlue
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Data", color = AccentNeonTeal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.DateRange, null, tint = AccentNeonTeal, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = data,
                        onValueChange = { data = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("dd/mm/aaaa", color = Color.Gray) },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, "Selecionar data", tint = AccentElectricBlue)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentElectricBlue,
                            unfocusedBorderColor = BorderColor,
                            cursorColor = AccentElectricBlue
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Local", color = AccentNeonTeal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = local,
                        onValueChange = { local = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Local da reunião", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentElectricBlue,
                            unfocusedBorderColor = BorderColor,
                            cursorColor = AccentElectricBlue
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Participantes", color = AccentNeonTeal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = participantes,
                        onValueChange = { participantes = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                        maxLines = 3,
                        placeholder = { Text("Digite os participantes, um por linha", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentElectricBlue,
                            unfocusedBorderColor = BorderColor,
                            cursorColor = AccentElectricBlue
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Discussões / Deliberações", color = AccentNeonTeal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = discussoes,
                        onValueChange = { discussoes = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                        placeholder = { Text("Descreva as discussões da reunião...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentElectricBlue,
                            unfocusedBorderColor = BorderColor,
                            cursorColor = AccentElectricBlue
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Encaminhamentos / Ações", color = AccentNeonTeal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = encaminhamentos,
                        onValueChange = { encaminhamentos = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
                        placeholder = { Text("Liste os encaminhamentos e responsáveis...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentElectricBlue,
                            unfocusedBorderColor = BorderColor,
                            cursorColor = AccentElectricBlue
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, title)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartilhar Ata"))
                },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, AccentElectricBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Share, null, tint = AccentElectricBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartilhar", color = AccentElectricBlue)
            }
            OutlinedButton(
                onClick = {
                    DocumentSharingUtils.exportToPdf(context, title, shareText)?.let { uri ->
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Exportar PDF"))
                    }
                },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, AccentNeonTeal),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, null, tint = AccentNeonTeal, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportar PDF", color = AccentNeonTeal)
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        data = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = AccentElectricBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
