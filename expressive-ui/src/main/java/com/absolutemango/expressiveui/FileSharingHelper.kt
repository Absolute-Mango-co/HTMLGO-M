package com.absolutemango.expressiveui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileSharingHelper {

    /**
     * Shares in-memory HTML text as either an .html file or bundled into a .zip archive.
     */
    fun shareHtmlContent(
        context: Context,
        htmlContent: String,
        suggestedFileName: String = "index.html",
        asZip: Boolean = false
    ) {
        try {
            val exportDir = File(context.cacheDir, "shared_exports").apply { if (!exists()) mkdirs() }

            val targetFile = if (asZip) {
                val zipBaseName = suggestedFileName.substringBeforeLast(".", "index")
                val zipFile = File(exportDir, "${zipBaseName}.zip")
                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    val entry = ZipEntry(if (suggestedFileName.endsWith(".html", ignoreCase = true)) suggestedFileName else "$suggestedFileName.html")
                    zos.putNextEntry(entry)
                    zos.write(htmlContent.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
                zipFile
            } else {
                val cleanName = if (suggestedFileName.endsWith(".html", ignoreCase = true)) suggestedFileName else "$suggestedFileName.html"
                val htmlFile = File(exportDir, cleanName)
                htmlFile.writeText(htmlContent, Charsets.UTF_8)
                htmlFile
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, targetFile)

            val mimeType = if (asZip) "application/zip" else "text/html"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, targetFile.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share ${if (asZip) "ZIP Archive" else "HTML File"}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error preparing file for sharing: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares a file referenced by Uri (e.g. from last opened recents), with on-the-fly ZIP packing if requested.
     */
    fun shareFileFromUri(
        context: Context,
        fileUri: Uri,
        fileName: String,
        asZip: Boolean = false
    ) {
        try {
            val exportDir = File(context.cacheDir, "shared_exports").apply { if (!exists()) mkdirs() }

            // Handle dummy developer files with fallback content
            if (fileUri == Uri.EMPTY || fileUri.toString().isBlank()) {
                val fallbackHtml = "<!DOCTYPE html>\n<html>\n<head><title>$fileName</title></head>\n<body>\n<h1>$fileName</h1>\n<p>Exported from HTMLGO-M.</p>\n</body>\n</html>"
                shareHtmlContent(context, fallbackHtml, fileName, asZip)
                return
            }

            val inputStream = context.contentResolver.openInputStream(fileUri)
            if (inputStream == null) {
                Toast.makeText(context, "Could not read file from storage", Toast.LENGTH_SHORT).show()
                return
            }

            val fileBytes = inputStream.use { it.readBytes() }

            val targetFile = if (asZip && !fileName.endsWith(".zip", ignoreCase = true)) {
                val zipBaseName = fileName.substringBeforeLast(".", "project")
                val zipFile = File(exportDir, "${zipBaseName}.zip")
                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    val entry = ZipEntry(fileName)
                    zos.putNextEntry(entry)
                    zos.write(fileBytes)
                    zos.closeEntry()
                }
                zipFile
            } else {
                val directFile = File(exportDir, fileName)
                directFile.writeBytes(fileBytes)
                directFile
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, targetFile)

            val mimeType = if (targetFile.name.endsWith(".zip", ignoreCase = true)) "application/zip" else "text/html"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, targetFile.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share ${if (mimeType == "application/zip") "ZIP Archive" else "HTML File"}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares a pre-existing File (such as a compiled project .zip) via FileProvider.
     */
    fun shareExistingFile(
        context: Context,
        file: File,
        mimeType: String = "application/zip",
        chooserTitle: String = "Share Project ZIP"
    ) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "File does not exist: ${file.name}", Toast.LENGTH_SHORT).show()
                return
            }
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        }
    }
}
