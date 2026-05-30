package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File
import java.io.FileOutputStream

private val CardSurface = Color(0xFF2B2930)
private val BorderColor = Color(0xFF49454F)
private val AccentNeonTeal = Color(0xFF80D8CC)

@Composable
fun SignaturePad(
    onSignatureSaved: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val completedPaths = remember { mutableStateListOf<List<Pair<Float, Float>>>() }
    val currentPath = remember { mutableStateListOf<Pair<Float, Float>>() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val context = LocalContext.current

    fun collectAllPaths(): List<List<Pair<Float, Float>>> = buildList {
        addAll(completedPaths)
        if (currentPath.isNotEmpty()) add(currentPath.toList())
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "ASSINATURA DIGITAL",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentNeonTeal
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.White)
                        .border(1.dp, BorderColor)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { canvasSize = it }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPath.clear()
                                        currentPath.add(offset.x to offset.y)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentPath.add(change.position.x to change.position.y)
                                    },
                                    onDragEnd = {
                                        if (currentPath.isNotEmpty()) {
                                            completedPaths.add(currentPath.toList())
                                            currentPath.clear()
                                        }
                                    },
                                    onDragCancel = {
                                        currentPath.clear()
                                    }
                                )
                            }
                    ) {
                        val allPaths = collectAllPaths()
                        for (pathPoints in allPaths) {
                            if (pathPoints.size >= 2) {
                                val path = Path().apply {
                                    moveTo(pathPoints[0].first, pathPoints[0].second)
                                    for (i in 1 until pathPoints.size) {
                                        lineTo(pathPoints[i].first, pathPoints[i].second)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color.Black,
                                    style = Stroke(
                                        width = 4f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            completedPaths.clear()
                            currentPath.clear()
                        }
                    ) {
                        Text("Limpar", color = AccentNeonTeal)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            val allPaths = collectAllPaths()
                            if (allPaths.isNotEmpty() && canvasSize.width > 0 && canvasSize.height > 0) {
                                val bitmap = pathsToBitmap(allPaths, canvasSize.width, canvasSize.height)
                                saveAndVerify(bitmap, context)
                                onSignatureSaved(bitmap)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentNeonTeal)
                    ) {
                        Text(
                            "Confirmar Assinatura",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

private fun pathsToBitmap(
    paths: List<List<Pair<Float, Float>>>,
    width: Int,
    height: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(maxOf(width, 1), maxOf(height, 1), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 6f
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }
    for (pathPoints in paths) {
        if (pathPoints.size >= 2) {
            val path = android.graphics.Path().apply {
                moveTo(pathPoints[0].first, pathPoints[0].second)
                for (i in 1 until pathPoints.size) {
                    lineTo(pathPoints[i].first, pathPoints[i].second)
                }
            }
            canvas.drawPath(path, paint)
        }
    }
    return bitmap
}

private fun saveAndVerify(bitmap: Bitmap, context: Context) {
    try {
        val tempFile = File(context.cacheDir, "signature_${System.currentTimeMillis()}.png")
        FileOutputStream(tempFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        val fileSize = tempFile.length()
        Toast.makeText(context, "Assinatura salva: ${fileSize} bytes", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
