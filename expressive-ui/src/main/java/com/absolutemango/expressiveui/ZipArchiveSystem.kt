package com.absolutemango.expressiveui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Universal Archive System copied from MainActivity.kt and enhanced for AI Forge.
 * Handles robust decompression, recursive HTML entrypoint discovery, SAF workspace extraction,
 * and isolated execution staging for playing multi-file .zip web applications with complete
 * asset resolution (CSS, JS, images, audio, subdirectories) in GeckoView and WebView.
 */
object ZipArchiveSystem {

    private const val TAG = "ZipArchiveSystem"

    /**
     * Resolves the display file name of a given Uri.
     */
    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) result = cursor.getString(index)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query display name for $uri", e)
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result ?: "unknown_file"
    }

    /**
     * Decompresses an entire .zip archive (from a content:// or file:// Uri) into [targetDir].
     * Preserves nested directory hierarchies and prevents Zip Slip path traversal vulnerabilities.
     * Returns the entrypoint HTML file found via [findHtmlFile], or null if decompression failed.
     */
    fun decompressZipToDir(context: Context, zipUri: Uri, targetDir: File): File? {
        return try {
            if (targetDir.exists()) targetDir.deleteRecursively()
            targetDir.mkdirs()

            val inputStream = context.contentResolver.openInputStream(zipUri) ?: return null
            val zis = ZipInputStream(BufferedInputStream(inputStream))
            var entry = zis.nextEntry
            val buffer = ByteArray(2048)
            val canonicalBase = targetDir.canonicalPath + File.separator

            while (entry != null) {
                val file = File(targetDir, entry.name)
                val canonicalTarget = file.canonicalPath
                if (!canonicalTarget.startsWith(canonicalBase) && canonicalTarget != targetDir.canonicalPath) {
                    throw SecurityException("Zip Slip path traversal attempt: ${entry.name}")
                }

                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        var count: Int
                        while (zis.read(buffer, 0, 2048).also { count = it } != -1) {
                            fos.write(buffer, 0, count)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()
            findHtmlFile(targetDir)
        } catch (e: Exception) {
            Log.e(TAG, "Error decompressing zip to dir", e)
            null
        }
    }

    /**
     * Recursively traverses [dir] to locate the primary HTML entrypoint.
     * Prefers "index.html", otherwise falls back to the first *.html or *.htm file discovered.
     */
    fun findHtmlFile(dir: File): File? {
        val files = dir.listFiles() ?: return null
        var fallbackHtml: File? = null
        for (file in files) {
            if (file.isDirectory) {
                val found = findHtmlFile(file)
                if (found != null) {
                    if (found.name.equals("index.html", ignoreCase = true)) return found
                    if (fallbackHtml == null) fallbackHtml = found
                }
            } else if (file.name.endsWith(".html", ignoreCase = true) || file.name.endsWith(".htm", ignoreCase = true)) {
                if (file.name.equals("index.html", ignoreCase = true)) return file
                if (fallbackHtml == null) fallbackHtml = file
            }
        }
        return fallbackHtml
    }

    /**
     * Decompresses a .zip archive directly into a SAF tree directory, creating intermediate subdirectories.
     */
    fun decompressZipToFolderSAF(context: Context, zipUri: Uri, targetFolderUri: Uri): String {
        return try {
            val rootDoc = DocumentFile.fromTreeUri(context, targetFolderUri) ?: return "Failed to access target folder"
            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                val zis = ZipInputStream(BufferedInputStream(inputStream))
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val segments = entry.name.split("/").filter { it.isNotBlank() }
                        var currentDir = rootDoc
                        for (i in 0 until segments.size - 1) {
                            currentDir = currentDir.findFile(segments[i]) ?: currentDir.createDirectory(segments[i]) ?: currentDir
                        }
                        val fileName = segments.last()
                        val file = currentDir.findFile(fileName) ?: currentDir.createFile("application/octet-stream", fileName)
                        file?.let { context.contentResolver.openOutputStream(it.uri)?.use { out -> zis.copyTo(out) } }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                zis.close()
            }
            "Project decompressed to workspace!"
        } catch (e: Exception) {
            Log.e(TAG, "Error decompressing zip to SAF folder", e)
            "Error decompressing: ${e.message}"
        }
    }

    /**
     * Compresses a list of file URIs into a target .zip file using SAF.
     */
    fun compressUrisToZipSAF(context: Context, uris: List<Uri>, zipFileUri: Uri): String {
        return try {
            context.contentResolver.openOutputStream(zipFileUri)?.use { outputStream ->
                val zos = ZipOutputStream(BufferedOutputStream(outputStream))
                uris.forEach { uri ->
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bis = BufferedInputStream(inputStream)
                        val fileName = getFileName(context, uri)
                        val entry = ZipEntry(fileName)
                        zos.putNextEntry(entry)
                        bis.copyTo(zos)
                        zos.closeEntry()
                    }
                }
                zos.close()
            }
            "Files compressed to ZIP successfully!"
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing URIs to ZIP", e)
            "Error compressing: ${e.message}"
        }
    }

    /**
     * Prepares and stages an entire .zip archive for playback in HtmlViewer (GeckoView / WebView).
     * Decompresses the archive into a dedicated cache execution directory and returns the file:// Uri
     * of the index.html entrypoint. Because all extracted assets exist relative to index.html,
     * CSS, JavaScript, fonts, and images resolve completely without missing-file errors.
     */
    fun openZipForPlayback(context: Context, zipUri: Uri, prefix: String = "ai_forge_extracted"): Uri? {
        val tempDir = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}")
        val htmlFile = decompressZipToDir(context, zipUri, tempDir) ?: return null
        return Uri.fromFile(htmlFile)
    }

    /**
     * Prepares playback from an existing unpacked directory (such as AI Forge's active session unpacked directory).
     * If an index file exists in the directory, returns its file:// Uri directly so all sibling assets resolve.
     */
    fun prepareUnpackedPlayback(unpackedDir: File): Uri? {
        if (!unpackedDir.exists()) return null
        val htmlFile = findHtmlFile(unpackedDir) ?: return null
        return Uri.fromFile(htmlFile)
    }
}
