package com.example.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.OfficeItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupUtils {

    private const val DB_NAME = "office_assistant_database"
    private const val BACKUP_DIR = "backup"
    private const val SPREADSHEETS_DIR = "spreadsheets"
    private const val DOCUMENTS_DIR = "documents"

    fun createBackup(context: Context): Uri? {
        try {
            val dbFile = context.getDatabasePath(DB_NAME) ?: return null
            if (!dbFile.exists()) return null

            val backupDir = File(context.cacheDir, BACKUP_DIR)
            backupDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val dbCopy = File(backupDir, "backup_${timestamp}.db")
            dbFile.copyTo(dbCopy, overwrite = true)

            val zipFile = File(backupDir, "backup_${timestamp}.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // Add database copy
                zos.putNextEntry(ZipEntry(dbCopy.name))
                FileInputStream(dbCopy).use { fis -> fis.copyTo(zos) }
                zos.closeEntry()
                dbCopy.delete()

                // Add spreadsheets
                val spreadsheetsDir = File(context.cacheDir, SPREADSHEETS_DIR)
                if (spreadsheetsDir.exists()) {
                    spreadsheetsDir.listFiles()?.forEach { file ->
                        zos.putNextEntry(ZipEntry("${SPREADSHEETS_DIR}/${file.name}"))
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }

                // Add documents
                val documentsDir = File(context.cacheDir, DOCUMENTS_DIR)
                if (documentsDir.exists()) {
                    documentsDir.listFiles()?.forEach { file ->
                        zos.putNextEntry(ZipEntry("${DOCUMENTS_DIR}/${file.name}"))
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }

            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun restoreBackup(context: Context, uri: Uri): Boolean {
        try {
            val backupDir = File(context.cacheDir, BACKUP_DIR)
            backupDir.mkdirs()

            val dbFile = context.getDatabasePath(DB_NAME) ?: return false

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (entry.name.endsWith(".db")) {
                            val extracted = File(backupDir, "restored_${entry.name}")
                            FileOutputStream(extracted).use { fos ->
                                zis.copyTo(fos)
                            }
                            // Copy over the existing database
                            extracted.copyTo(dbFile, overwrite = true)
                            extracted.delete()
                            return true
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun exportToJson(context: Context, items: List<OfficeItem>): Uri? {
        try {
            val backupDir = File(context.cacheDir, BACKUP_DIR)
            backupDir.mkdirs()

            val jsonArray = JSONArray()
            for (item in items) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("title", item.title)
                obj.put("type", item.type)
                obj.put("content", item.content)
                obj.put("csvData", item.csvData ?: JSONObject.NULL)
                obj.put("timestamp", item.timestamp)
                obj.put("notes", item.notes ?: JSONObject.NULL)
                jsonArray.put(obj)
            }

            val jsonFile = File(backupDir, "documents_backup.json")
            jsonFile.writeText(jsonArray.toString(2), Charsets.UTF_8)

            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", jsonFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun importFromJson(context: Context, uri: Uri): List<OfficeItem>? {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            } ?: return null

            val jsonArray = JSONArray(jsonString)
            val items = mutableListOf<OfficeItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val item = OfficeItem(
                    id = obj.getInt("id"),
                    title = obj.getString("title"),
                    type = obj.getString("type"),
                    content = obj.getString("content"),
                    csvData = if (obj.isNull("csvData")) null else obj.optString("csvData"),
                    timestamp = obj.getLong("timestamp"),
                    notes = if (obj.isNull("notes")) null else obj.optString("notes")
                )
                items.add(item)
            }
            return items
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportToZip(context: Context, items: List<OfficeItem>): Uri? {
        try {
            val backupDir = File(context.cacheDir, BACKUP_DIR)
            backupDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            // Copy database
            val dbFile = context.getDatabasePath(DB_NAME)
            val dbCopy: File? = if (dbFile?.exists() == true) {
                val copy = File(backupDir, "backup_${timestamp}.db")
                dbFile.copyTo(copy, overwrite = true)
                copy
            } else null

            // Create JSON file
            val jsonArray = JSONArray()
            for (item in items) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("title", item.title)
                obj.put("type", item.type)
                obj.put("content", item.content)
                obj.put("csvData", item.csvData ?: JSONObject.NULL)
                obj.put("timestamp", item.timestamp)
                obj.put("notes", item.notes ?: JSONObject.NULL)
                jsonArray.put(obj)
            }
            val jsonFile = File(backupDir, "documents_backup_${timestamp}.json")
            jsonFile.writeText(jsonArray.toString(2), Charsets.UTF_8)

            val zipFile = File(backupDir, "full_backup_${timestamp}.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // Add database
                dbCopy?.let { copy ->
                    zos.putNextEntry(ZipEntry(copy.name))
                    FileInputStream(copy).use { fis -> fis.copyTo(zos) }
                    zos.closeEntry()
                    copy.delete()
                }

                // Add JSON
                zos.putNextEntry(ZipEntry(jsonFile.name))
                FileInputStream(jsonFile).use { fis -> fis.copyTo(zos) }
                zos.closeEntry()
                jsonFile.delete()

                // Add spreadsheets
                val spreadsheetsDir = File(context.cacheDir, SPREADSHEETS_DIR)
                if (spreadsheetsDir.exists()) {
                    spreadsheetsDir.listFiles()?.forEach { file ->
                        zos.putNextEntry(ZipEntry("${SPREADSHEETS_DIR}/${file.name}"))
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }

                // Add documents
                val documentsDir = File(context.cacheDir, DOCUMENTS_DIR)
                if (documentsDir.exists()) {
                    documentsDir.listFiles()?.forEach { file ->
                        zos.putNextEntry(ZipEntry("${DOCUMENTS_DIR}/${file.name}"))
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }

            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
