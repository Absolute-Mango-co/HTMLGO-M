package com.absolutemango.expressiveui.byok

import android.content.Context
import android.util.Log
import com.absolutemango.expressiveui.HtmlDetector
import com.absolutemango.expressiveui.ZipArchiveSystem
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

sealed interface ProjectRunState {
    object None : ProjectRunState
    data class SingleHtml(val html: String) : ProjectRunState
    data class ZipProject(
        val sessionId: String,
        val entryPointFile: File,
        val zipFile: File?,
        val fileCount: Int
    ) : ProjectRunState
    data class InvalidZip(val reason: String) : ProjectRunState
}

object ProjectZipDetector {
    private const val TAG = "ProjectZipDetector"
    const val PROJECT_FLAG = "<!-- FORGE_PROJECT: zip -->"

    // Matches ```language filename="path" ... ``` or ```filename="path" ... ```
    private val BLOCK_REGEX = Regex(
        """```(?:[a-zA-Z0-9_-]+)?\s+filename=["']?([^"'\s>]+)["']?\s*\r?\n([\s\S]*?)(?:```|$)""",
        RegexOption.IGNORE_CASE
    )

    fun hasProjectFlag(content: String): Boolean {
        return content.contains(PROJECT_FLAG, ignoreCase = true)
    }

    /**
     * Resolves and verifies that target file stays safely within [baseDir].
     * Throws [SecurityException] if Zip Slip path traversal is attempted.
     */
    fun resolveSafeFile(baseDir: File, relativePath: String): File {
        val cleanRelative = relativePath.trim().replace('\\', '/')
        if (cleanRelative.startsWith("/") || cleanRelative.contains("..")) {
            val candidate = File(baseDir, cleanRelative)
            val canonicalBase = baseDir.canonicalPath + File.separator
            val canonicalTarget = candidate.canonicalPath
            if (!canonicalTarget.startsWith(canonicalBase) && canonicalTarget != baseDir.canonicalPath) {
                throw SecurityException("Zip Slip path traversal attempt: $relativePath")
            }
            return candidate
        }
        val target = File(baseDir, cleanRelative)
        val canonicalBase = baseDir.canonicalPath + File.separator
        val canonicalTarget = target.canonicalPath
        if (!canonicalTarget.startsWith(canonicalBase) && canonicalTarget != baseDir.canonicalPath) {
            throw SecurityException("Zip Slip path traversal attempt: $relativePath")
        }
        return target
    }

    /**
     * Compresses contents of [unpackedDir] into [zipOutputFile].
     */
    fun compileZip(unpackedDir: File, zipOutputFile: File) {
        try {
            val parent = zipOutputFile.parentFile
            if (parent != null && !parent.exists()) parent.mkdirs()

            ZipOutputStream(FileOutputStream(zipOutputFile)).use { zos ->
                unpackedDir.walkTopDown().filter { it.isFile && it.canonicalPath != zipOutputFile.canonicalPath }.forEach { file ->
                    val relPath = file.relativeTo(unpackedDir).path.replace('\\', '/')
                    val entry = ZipEntry(relPath)
                    zos.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error compiling project ZIP", e)
        }
    }

    /**
     * Analyzes assistant message content and manages unpacked file staging.
     */
    fun detectProjectState(
        context: Context,
        sessionId: String,
        content: String,
        htmlPayload: String? = null
    ): ProjectRunState {
        if (content.isBlank()) return ProjectRunState.None

        if (hasProjectFlag(content)) {
            val projectDir = File(context.cacheDir, "ai_forge_projects/$sessionId")
            val unpackedDir = File(projectDir, "unpacked").apply { if (!exists()) mkdirs() }

            val matches = BLOCK_REGEX.findAll(content).toList()
            if (matches.isEmpty()) {
                return ProjectRunState.InvalidZip("Scanning project code blocks...")
            }

            var stagedFilesCount = 0
            for (match in matches) {
                val relPath = match.groupValues[1].trim()
                val code = match.groupValues[2].trimEnd()
                if (relPath.isEmpty()) continue

                try {
                    val safeFile = resolveSafeFile(unpackedDir, relPath)
                    val parent = safeFile.parentFile
                    if (parent != null && !parent.exists()) parent.mkdirs()
                    safeFile.writeText(code, Charsets.UTF_8)
                    stagedFilesCount++
                } catch (se: SecurityException) {
                    Log.e(TAG, "Security exception for $relPath", se)
                    return ProjectRunState.InvalidZip("Path traversal security violation: $relPath")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write staged file $relPath", e)
                }
            }

            val indexFile = ZipArchiveSystem.findHtmlFile(unpackedDir) ?: File(unpackedDir, "index.html")
            if (indexFile.exists() && indexFile.length() > 0) {
                val zipFile = File(projectDir, "project.zip")
                compileZip(unpackedDir, zipFile)
                return ProjectRunState.ZipProject(
                    sessionId = sessionId,
                    entryPointFile = indexFile,
                    zipFile = if (zipFile.exists() && zipFile.length() > 0) zipFile else null,
                    fileCount = stagedFilesCount
                )
            } else {
                return ProjectRunState.InvalidZip("Missing index.html entry point")
            }
        }

        // Fallback to single-file HTML detection
        val cleanHtml = htmlPayload ?: HtmlDetector.extractCleanHtml(content)
        return if (!cleanHtml.isNullOrBlank()) {
            ProjectRunState.SingleHtml(cleanHtml)
        } else {
            ProjectRunState.None
        }
    }
}
