package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.OfficeItem

private val CardSurface = Color(0xFF2B2930)
private val BorderColor = Color(0xFF49454F)
private val AccentNeonTeal = Color(0xFF80D8CC)
private val AccentElectricBlue = Color(0xFFD0BCFF)

@Composable
fun FusionDocumentPicker(
    allItems: List<OfficeItem>,
    selectedDocs: List<OfficeItem>,
    onToggleDoc: (OfficeItem) -> Unit,
    onGenerate: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }
    val filteredItems = allItems.filter { it.type != "SPREADSHEET" || it.type == "SPREADSHEET" }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "FUSÃO CORPORATIVA DE DOCUMENTOS",
                    fontSize = 12.sp, fontWeight = FontWeight.Black,
                    color = AccentNeonTeal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Selecione QUANTOS documentos quiser (planilhas, PDFs, textos, atas) para gerar um relatório completo consolidado",
                    fontSize = 10.sp, color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título do relatório", fontSize = 11.sp) },
                    placeholder = { Text("Ex: Relatório Consolidado Maio") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentNeonTeal, unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "${selectedDocs.size} documento(s) selecionado(s)",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = if (selectedDocs.isNotEmpty()) AccentNeonTeal else Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (allItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nenhum documento disponível. Crie documentos primeiro.", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(allItems, key = { it.id }) { doc ->
                            val isSelected = selectedDocs.any { it.id == doc.id }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleDoc(doc) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) AccentNeonTeal.copy(alpha = 0.15f) else Color(0xFF1C1B1F)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) AccentNeonTeal else BorderColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        null,
                                        tint = if (isSelected) AccentNeonTeal else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(doc.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(doc.type, fontSize = 10.sp, color = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = observations,
                    onValueChange = { observations = it },
                    label = { Text("Observações adicionais", fontSize = 11.sp) },
                    placeholder = { Text("Instruções extras para o relatório...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentNeonTeal, unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onGenerate(title, observations) },
                        enabled = selectedDocs.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentNeonTeal)
                    ) { Text("Gerar Relatório Fusão", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
