package com.absolutemango.htmlgow

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object ZipExtractor {

    fun unzipAndFindEntryPoint(zipFile: File, targetDir: File): File? {
        if (!zipFile.exists() || !zipFile.isFile) return null

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        try {
            ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(targetDir, entry.name)

                    // Zip Slip security validation
                    if (!newFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        zis.closeEntry()
                        entry = zis.nextEntry
                        continue
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return findHtmlEntryPoint(targetDir)
    }

    fun findHtmlEntryPoint(dir: File): File? {
        if (!dir.exists() || !dir.isDirectory) return null

        // 1. Check for index.html or index.htm directly in root
        val rootIndexHtml = File(dir, "index.html")
        if (rootIndexHtml.exists() && rootIndexHtml.isFile) return rootIndexHtml

        val rootIndexHtm = File(dir, "index.htm")
        if (rootIndexHtm.exists() && rootIndexHtm.isFile) return rootIndexHtm

        // 2. Search recursively for index.html / index.htm
        var candidate: File? = null
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val nameLower = file.name.lowercase()
                if (nameLower == "index.html" || nameLower == "index.htm") {
                    return file
                }
                if (candidate == null && (nameLower.endsWith(".html") || nameLower.endsWith(".htm"))) {
                    candidate = file
                }
            }
        }

        return candidate
    }
}
