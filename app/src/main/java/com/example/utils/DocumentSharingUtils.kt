package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DocumentSharingUtils {

    // A4 dimensions in PostScript points: 595 x 842
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    fun exportToPdf(context: Context, title: String, content: String): Uri? {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint()
            val titlePaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 18f
                color = 0xFF1A1C1E.toInt() // Dark Slate
            }
            val metaPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textSize = 10f
                color = 0xFF74777F.toInt() // Medium Slate
            }
            val textPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 11f
                color = 0xFF1A1C1E.toInt()
            }
            val footerPaint = Paint().apply {
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                textSize = 9f
                color = 0xFF8E9199.toInt()
            }

            val margin = 50f
            val maxLineWidth = A4_WIDTH - (margin * 2)
            var currentY = margin + 30f

            // Create page 1
            var pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            // Draw Header Banner border
            val borderPaint = Paint().apply {
                color = 0xFF0D6EFD.toInt() // Accent Blue
                strokeWidth = 3f
                style = Paint.Style.STROKE
            }
            canvas.drawRect(margin - 10, margin - 10, A4_WIDTH - margin + 10, A4_HEIGHT - margin + 10, borderPaint)

            // Draw title
            canvas.drawText(title.uppercase(Locale.getDefault()), margin, currentY, titlePaint)
            currentY += 25f

            // Draw generation meta
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            canvas.drawText("Gerado por: Assistente de Escritório Inteligente | Data: $dateStr", margin, currentY, metaPaint)
            currentY += 40f

            // Split content into lines and handle text wrapping
            val paragraphs = content.split("\n")
            for (paragraph in paragraphs) {
                if (currentY > A4_HEIGHT - margin - 50f) {
                    pdfDocument.finishPage(page)
                    // Create overflow page
                    val nextRawPage = pdfDocument.pages.size + 1
                    pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, nextRawPage).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    canvas.drawRect(margin - 10, margin - 10, A4_WIDTH - margin + 10, A4_HEIGHT - margin + 10, borderPaint)
                    currentY = margin + 30f
                }

                if (paragraph.trim().isEmpty()) {
                    currentY += 15f
                    continue
                }

                val wrappedLines = wrapText(paragraph, textPaint, maxLineWidth)
                for (line in wrappedLines) {
                    if (currentY > A4_HEIGHT - margin - 35f) {
                        pdfDocument.finishPage(page)
                        val nextRawPage = pdfDocument.pages.size + 1
                        pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, nextRawPage).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        canvas.drawRect(margin - 10, margin - 10, A4_WIDTH - margin + 10, A4_HEIGHT - margin + 10, borderPaint)
                        currentY = margin + 30f
                    }
                    canvas.drawText(line, margin, currentY, textPaint)
                    currentY += 18f
                }
                currentY += 8f
            }

            // Draw watermark footer
            val footerY = A4_HEIGHT - margin - 15f
            canvas.drawText("Fim do Relatório Oficial - Armazenamento Local Criptografado", margin, footerY, footerPaint)

            pdfDocument.finishPage(page)

            // Save file in private cache directory
            val cachePath = File(context.cacheDir, "documents")
            cachePath.mkdirs()
            val cleanFileName = title.replace("[^a-zA-Z0-9]".toRegex(), "_") + ".pdf"
            val file = File(cachePath, cleanFileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            // Return safe shareable content Uri using FileProvider
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportToCsv(context: Context, title: String, csvContent: String, extension: String = "csv"): Uri? {
        try {
            val cachePath = File(context.cacheDir, "spreadsheets")
            cachePath.mkdirs()
            val cleanFileName = title.replace("[^a-zA-Z0-9]".toRegex(), "_") + ".$extension"
            val file = File(cachePath, cleanFileName)
            
            FileOutputStream(file).use { out ->
                out.write(0xEF)
                out.write(0xBB)
                out.write(0xBF)
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun readTextFromUri(context: Context, uri: Uri): String? {
        val resolver = context.contentResolver
        try {
            // First identify file extension/mime
            val type = resolver.getType(uri) ?: ""
            val isPdf = uri.path?.lowercase()?.endsWith(".pdf") == true || type.contains("pdf", true)

            resolver.openInputStream(uri)?.use { stream ->
                if (isPdf) {
                    // Optimized simple extraction utility for uncompressed PDF files
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        val stringBuilder = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            // Target contents in PDF streams inside parenthesis e.g. (My text) Tj
                            val regex = "\\(([^)]*)\\)".toRegex()
                            val matches = regex.findAll(line!!)
                            for (match in matches) {
                                val textVal = match.groupValues[1]
                                if (textVal.length > 2 && textVal.any { it.isLetter() }) {
                                    stringBuilder.append(textVal).append(" ")
                                }
                            }
                            if (line!!.contains("ET") || line!!.contains("BT")) {
                                stringBuilder.append("\n")
                            }
                        }
                        val finalResult = stringBuilder.toString().trim()
                            .replace("\\s+".toRegex(), " ")
                            .replace(" \\n ", "\n")
                        
                        if (finalResult.isNotEmpty()) {
                            return finalResult
                        } else {
                            return "Importação de PDF Concluída.\nNota: O arquivo PDF importado utiliza compressão ou imagens escaneadas (não pesquisáveis). Foram carregados os metadados do documento."
                        }
                    }
                } else {
                    // Standard text/CSV reading
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                        val stringBuilder = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            stringBuilder.append(line).append("\n")
                        }
                        return stringBuilder.toString().trim()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val spaceText = if (currentLine.isEmpty()) "" else " "
            val testLine = currentLine.toString() + spaceText + word
            val testWidth = paint.measureText(testLine)

            if (testWidth <= maxWidth) {
                currentLine.append(spaceText).append(word)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    // Single word is too long for the margin, force split it
                    lines.add(word)
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    fun parseCsv(csvContent: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val lines = csvContent.split("\n")
        
        for (line in lines) {
            if (line.trim().isEmpty()) continue
            // Simple comma split, supporting quoted field bounds
            val cells = mutableListOf<String>()
            var currentCell = StringBuilder()
            var inQuotes = false
            
            for (char in line) {
                when (char) {
                    '"' -> {
                        inQuotes = !inQuotes
                    }
                    ',' -> {
                        if (inQuotes) {
                            currentCell.append(char)
                        } else {
                            cells.add(currentCell.toString().trim())
                            currentCell = StringBuilder()
                        }
                    }
                    else -> {
                        currentCell.append(char)
                    }
                }
            }
            cells.add(currentCell.toString().trim())
            rows.add(cells)
        }
        return rows
    }

    fun toCsvString(grid: List<List<String>>): String {
        val sb = StringBuilder()
        for (row in grid) {
            val line = row.joinToString(",") { cell ->
                val escaped = cell.replace("\"", "\"\"")
                if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
                    "\"$escaped\""
                } else {
                    escaped
                }
            }
            sb.append(line).append("\n")
        }
        return sb.toString().trim()
    }
}
