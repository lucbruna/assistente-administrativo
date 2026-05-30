package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private val CardSurface = Color(0xFF2B2930)
private val BorderColor = Color(0xFF49454F)
private val AccentNeonTeal = Color(0xFF80D8CC)
private val AccentElectricBlue = Color(0xFFD0BCFF)

@Composable
fun OcrResultDialog(
    result: OcrResult,
    onDismiss: () -> Unit,
    onGenerateReport: () -> Unit,
    onUseAsMinutes: () -> Unit,
    onSaveDocument: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState())
            ) {
                Text(
                    "RESULTADO DO OCR • CORREÇÃO GRAMATICAL",
                    fontSize = 12.sp, fontWeight = FontWeight.Black, color = AccentNeonTeal
                )
                Spacer(modifier = Modifier.height(14.dp))

                Text("📄 TEXTO ORIGINAL (RECONHECIDO)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1B1F), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(result.rawText, fontSize = 12.sp, color = Color(0xFFFF8A80), lineHeight = 18.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (result.corrections.isNotEmpty()) {
                    Text("🔧 CORREÇÕES APLICADAS (${result.corrections.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    result.corrections.forEach { (wrong, right) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("\"$wrong\"", fontSize = 11.sp, color = Color(0xFFFF8A80), textDecoration = TextDecoration.LineThrough)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = AccentNeonTeal, modifier = Modifier.size(14.dp))
                            Text("\"$right\"", fontSize = 11.sp, color = AccentNeonTeal)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text("✅ TEXTO CORRIGIDO E POLIDO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1B1F), RoundedCornerShape(8.dp))
                        .border(1.dp, AccentNeonTeal.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(result.correctedText, fontSize = 12.sp, color = Color.White, lineHeight = 18.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onUseAsMinutes,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentElectricBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Assignment, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Criar Ata de Reunião", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onGenerateReport,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AccentNeonTeal)
                ) {
                    Icon(Icons.Filled.Assessment, null, tint = AccentNeonTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gerar Relatório Completo", color = AccentNeonTeal, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveDocument,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, AccentNeonTeal)
                    ) {
                        Icon(Icons.Filled.Save, null, tint = AccentNeonTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salvar", color = AccentNeonTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7D5260))
                    ) {
                        Icon(Icons.Filled.Delete, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Fechar", color = Color.Gray)
                }
            }
        }
    }
}
