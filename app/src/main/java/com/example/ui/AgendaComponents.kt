package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AgendaItem
import com.example.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

private val CardSurface = Color(0xFF2B2930)
private val BorderColor = Color(0xFF49454F)
private val AccentNeonTeal = Color(0xFF80D8CC)
private val DarkBackground = Color(0xFF1C1B1F)

@Composable
fun AgendaPanel(
    items: List<AgendaItem>,
    onToggleComplete: (AgendaItem) -> Unit,
    onDelete: (AgendaItem) -> Unit,
    onAddItem: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "AGENDA EXECUTIVA",
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFFD0BCFF), letterSpacing = 1.sp
            )
            Text(
                "${items.size} compromissos",
                fontSize = 11.sp, color = AccentNeonTeal
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Toque para concluir • Deslize ou use o ícone para excluir",
            fontSize = 9.sp, color = Color.Gray
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.EventNote, null, tint = Color.Gray, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nenhum compromisso agendado.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            val sorted = items.sortedBy { it.dateTime }
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sorted, key = { it.id }) { item ->
                    AgendaItemRow(item, onToggleComplete, onDelete)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onAddItem,
            colors = ButtonDefaults.buttonColors(containerColor = AccentNeonTeal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Novo Compromisso", fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun AgendaItemRow(
    item: AgendaItem,
    onToggleComplete: (AgendaItem) -> Unit,
    onDelete: (AgendaItem) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(item.dateTime))
    val timeStr = timeFormat.format(Date(item.dateTime))

    val priorityColor = when (item.priority) {
        "CRITICAL" -> Color(0xFFFF5252)
        "MEDIUM" -> Color(0xFFFFD740)
        else -> AccentNeonTeal
    }

    val bgColor by animateColorAsState(
        targetValue = if (item.isCompleted) Color(0xFF1A3A2A) else CardSurface,
        label = "bg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleComplete(item) },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, if (item.isCompleted) priorityColor.copy(alpha = 0.3f) else BorderColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(priorityColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (item.isCompleted) Modifier.alpha(0.5f) else Modifier
                    )
            ) {
                Text(
                    item.title,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (item.description.isNotEmpty()) {
                    Text(
                        item.description,
                        fontSize = 11.sp, color = Color.LightGray,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$dateStr às $timeStr", fontSize = 10.sp, color = Color.Gray)
                    if (item.location.isNotEmpty()) {
                        Text("• ${item.location}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (item.isCompleted) {
                Icon(Icons.Default.CheckCircle, null, tint = AccentNeonTeal, modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = { onDelete(item) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Delete, "Excluir", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun CreateAgendaDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, dateTime: Long, priority: String, location: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp).border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    "NOVO COMPROMISSO",
                    fontSize = 13.sp, fontWeight = FontWeight.Black,
                    color = AccentNeonTeal
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Título", fontSize = 12.sp, color = Color.Gray) },
                    placeholder = { Text("Ex: Reunião com diretoria") },
                    colors = fieldsColors(), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text("Descrição", fontSize = 12.sp, color = Color.Gray) },
                    placeholder = { Text("Detalhes do compromisso...") },
                    colors = fieldsColors(), modifier = Modifier.fillMaxWidth(), maxLines = 2)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = date, onValueChange = { date = it },
                        label = { Text("Data", fontSize = 11.sp) },
                        placeholder = { Text("dd/MM/yyyy") },
                        colors = fieldsColors(), modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = time, onValueChange = { time = it },
                        label = { Text("Hora", fontSize = 11.sp) },
                        placeholder = { Text("HH:mm") },
                        colors = fieldsColors(), modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = location, onValueChange = { location = it },
                    label = { Text("Local", fontSize = 12.sp, color = Color.Gray) },
                    placeholder = { Text("Sala, endereço...") },
                    colors = fieldsColors(), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ROUTINE" to "Rotina", "MEDIUM" to "Médio", "CRITICAL" to "Crítico").forEach { (value, label) ->
                        FilterChip(
                            selected = priority == value,
                            onClick = { priority = value },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentNeonTeal,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cal = Calendar.getInstance()
                            try {
                                val dateParts = date.split("/")
                                if (dateParts.size >= 3) {
                                    cal.set(Calendar.DAY_OF_MONTH, dateParts[0].toInt())
                                    cal.set(Calendar.MONTH, dateParts[1].toInt() - 1)
                                    cal.set(Calendar.YEAR, dateParts[2].toInt())
                                }
                                val timeParts = time.split(":")
                                if (timeParts.size >= 2) {
                                    cal.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                                    cal.set(Calendar.MINUTE, timeParts[1].toInt())
                                }
                            } catch (_: Exception) {}
                            onConfirm(title.ifEmpty { "Compromisso" }, description, cal.timeInMillis, priority, location)
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentNeonTeal)
                    ) { Text("Agendar", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable private fun fieldsColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentNeonTeal, unfocusedBorderColor = BorderColor,
    focusedTextColor = Color.White, unfocusedTextColor = Color.White
)
