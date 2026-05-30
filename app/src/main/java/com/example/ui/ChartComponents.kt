package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardSurface = Color(0xFF2B2930)
private val BorderColor = Color(0xFF49454F)
private val AccentNeonTeal = Color(0xFF80D8CC)
private val AccentElectricBlue = Color(0xFFD0BCFF)

enum class ChartType { BAR, LINE }

data class ChartDataPoint(
    val label: String,
    val value: Float
)

fun parseChartData(csvContent: String): List<ChartDataPoint> {
    val lines = csvContent.split("\n").filter { it.isNotBlank() }
    if (lines.size < 2) return emptyList()

    val header = lines.first().split(",")
    val valueColIndex = header.indexOfLast { h ->
        val lower = h.lowercase()
        lower.contains("valor") || lower.contains("total") || lower.contains("preço") ||
        lower.contains("preco") || lower.contains("saldo") || lower.contains("entrada") ||
        lower.contains("saída") || lower.contains("saida") || lower.contains("quantidade")
    }
    val labelColIndex = 0

    if (valueColIndex < 0 || labelColIndex < 0) return emptyList()

    return lines.drop(1).mapNotNull { line ->
        val cols = line.split(",")
        if (cols.size > valueColIndex) {
            val label = cols.getOrElse(labelColIndex) { "" }.trim()
            val rawValue = cols[valueColIndex].trim()
                .replace("R\$", "").replace("$", "").replace(".", "").replace(",", ".")
                .replace("%", "").trim()
            val value = rawValue.toFloatOrNull()
            if (value != null && label.isNotBlank()) ChartDataPoint(label, value)
            else null
        } else null
    }
}

@Composable
fun ChartView(
    data: List<ChartDataPoint>,
    chartType: ChartType = ChartType.BAR
) {
    if (data.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text(
                "📊 GRÁFICO DE DESEMPENHO",
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF938F99), letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            val maxValue = data.maxOf { it.value }.coerceAtLeast(1f)
            val barColor = AccentNeonTeal
            val lineColor = AccentElectricBlue

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val paddingLeft = 60f
                val paddingRight = 20f
                val paddingTop = 10f
                val paddingBottom = 40f
                val chartWidth = size.width - paddingLeft - paddingRight
                val chartHeight = size.height - paddingTop - paddingBottom

                // Grid lines
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = paddingTop + (chartHeight / gridLines) * i
                    drawLine(
                        color = BorderColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width - paddingRight, y),
                        strokeWidth = 0.5f
                    )
                    val labelValue = maxValue - (maxValue / gridLines) * i
                    drawContext.canvas.nativeCanvas.drawText(
                        "R$ ${String.format("%.0f", labelValue)}",
                        4f, y + 4f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 20f
                        }
                    )
                }

                val barWidth = (chartWidth / data.size) * 0.6f
                val spacing = (chartWidth / data.size) * 0.4f

                when (chartType) {
                    ChartType.BAR -> {
                        data.forEachIndexed { index, point ->
                            val barHeight = (point.value / maxValue) * chartHeight
                            val x = paddingLeft + index * (barWidth + spacing) + spacing / 2
                            val y = paddingTop + chartHeight - barHeight

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )

                            // Label
                            drawContext.canvas.nativeCanvas.drawText(
                                point.label.take(6),
                                x + barWidth / 2 - 15f,
                                paddingTop + chartHeight + 16f,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 18f
                                }
                            )
                        }
                    }

                    ChartType.LINE -> {
                        if (data.size >= 2) {
                            val path = Path()
                            data.forEachIndexed { index, point ->
                                val x = paddingLeft + index * (chartWidth / (data.size - 1).coerceAtLeast(1))
                                val y = paddingTop + chartHeight - (point.value / maxValue) * chartHeight
                                if (index == 0) path.moveTo(x, y)
                                else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = lineColor,
                                style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            // Shadow area
                            val shadowPath = Path()
                            shadowPath.addPath(path)
                            val lastX = paddingLeft + (data.size - 1) * (chartWidth / (data.size - 1).coerceAtLeast(1))
                            shadowPath.lineTo(lastX, paddingTop + chartHeight)
                            shadowPath.lineTo(paddingLeft, paddingTop + chartHeight)
                            shadowPath.close()
                            drawPath(
                                path = shadowPath,
                                color = lineColor.copy(alpha = 0.15f)
                            )

                            // Dots
                            data.forEachIndexed { index, point ->
                                val x = paddingLeft + index * (chartWidth / (data.size - 1).coerceAtLeast(1))
                                val y = paddingTop + chartHeight - (point.value / maxValue) * chartHeight
                                drawCircle(color = lineColor, radius = 5f, center = Offset(x, y))
                                drawCircle(color = Color.White, radius = 2.5f, center = Offset(x, y))

                                drawContext.canvas.nativeCanvas.drawText(
                                    point.label.take(6),
                                    x - 15f,
                                    paddingTop + chartHeight + 16f,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 18f
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
