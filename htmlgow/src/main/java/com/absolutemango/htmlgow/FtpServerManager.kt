package com.absolutemango.htmlgow

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.Authority
import org.apache.ftpserver.ftplet.DefaultFtplet
import org.apache.ftpserver.ftplet.FtpRequest
import org.apache.ftpserver.ftplet.FtpSession
import org.apache.ftpserver.ftplet.FtpletResult
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale

private const val PREFS_NAME = "htmlgow_prefs"
private const val KEY_FTP_ANONYMOUS = "ftp_anonymous"
private const val KEY_FTP_USER = "ftp_user"
private const val KEY_FTP_PASS = "ftp_pass"

object FtpServerManager {
    @Volatile
    private var ftpServer: FtpServer? = null

    @Volatile
    private var wifiLock: WifiManager.WifiLock? = null

    @Volatile
    var isRunning: Boolean = false
        private set

    fun getSharedDir(context: Context): File {
        return File(context.filesDir, "shared").apply {
            if (!exists()) mkdirs()
        }
    }

    fun startServer(context: Context, port: Int = 2121): Boolean {
        if (isRunning) return true
        val rootDir = getSharedDir(context)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val anonymousMode = prefs.getBoolean(KEY_FTP_ANONYMOUS, true)
        val ftpUser = prefs.getString(KEY_FTP_USER, "htmlgo") ?: "htmlgo"
        val ftpPass = prefs.getString(KEY_FTP_PASS, "1234") ?: "1234"

        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "HtmlGoW:FtpLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }

            val serverFactory = FtpServerFactory()
            val listenerFactory = ListenerFactory()

            listenerFactory.port = port
            listenerFactory.serverAddress = "0.0.0.0"

            serverFactory.addListener("default", listenerFactory.createListener())
            serverFactory.ftplets = mapOf("solidExplorerHelper" to SolidExplorerFtpHelper(rootDir))

            val authorities: List<Authority> = listOf(WritePermission())

            if (anonymousMode) {
                val anonUser = BaseUser().apply {
                    name = "anonymous"
                    homeDirectory = rootDir.absolutePath
                    this.authorities = authorities
                }
                serverFactory.userManager.save(anonUser)
            } else {
                val user = BaseUser().apply {
                    name = ftpUser
                    password = ftpPass
                    homeDirectory = rootDir.absolutePath
                    this.authorities = authorities
                }
                serverFactory.userManager.save(user)
            }

            val server = serverFactory.createServer()
            server.start()
            ftpServer = server
            isRunning = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            stopServer()
            false
        }
    }

    fun stopServer() {
        isRunning = false
        try {
            wifiLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        wifiLock = null

        try {
            ftpServer?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ftpServer = null
    }

    fun getLocalIpAddress(context: Context): String {
        try {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val activeNetwork = cm.activeNetwork
                val linkProperties = cm.getLinkProperties(activeNetwork)
                if (linkProperties != null) {
                    for (linkAddress in linkProperties.linkAddresses) {
                        val inetAddress = linkAddress.address
                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            val host = inetAddress.hostAddress
                            if (!host.isNullOrEmpty() && host != "127.0.0.1" && host != "0.0.0.0") {
                                return host
                            }
                        }
                    }
                }
            }

            val candidateIps = mutableListOf<String>()
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: ""
                        if (host.isNotEmpty() && host != "127.0.0.1" && host != "0.0.0.0") {
                            if (intf.name.lowercase(Locale.US).contains("wlan") || intf.name.lowercase(Locale.US).contains("eth")) {
                                return host
                            }
                            candidateIps.add(host)
                        }
                    }
                }
            }
            if (candidateIps.isNotEmpty()) {
                return candidateIps.first()
            }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                val ipStr = String.format(
                    Locale.US,
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
                if (ipStr != "0.0.0.0" && ipStr != "127.0.0.1") return ipStr
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    fun getFtpUrl(context: Context, port: Int = 2121): String {
        val ip = getLocalIpAddress(context)
        return if (ip != "127.0.0.1" && ip != "0.0.0.0") "ftp://$ip:$port" else "No Wi-Fi Connected"
    }

    private class SolidExplorerFtpHelper(private val rootDir: File) : DefaultFtplet() {
        override fun onRenameStart(session: FtpSession, request: FtpRequest): FtpletResult {
            try {
                val targetRel = request.argument ?: return FtpletResult.DEFAULT
                val userFileSystem = session.fileSystemView ?: return FtpletResult.DEFAULT
                val currentFtpFile = userFileSystem.workingDirectory ?: return FtpletResult.DEFAULT
                val currentDir = File(rootDir, currentFtpFile.absolutePath)

                val targetFile = if (targetRel.startsWith("/")) {
                    File(rootDir, targetRel.substring(1))
                } else {
                    File(currentDir, targetRel)
                }

                // On Android POSIX filesystem, File.renameTo fails if target file already exists.
                // Explicitly delete target file if it already exists before RNTO completes!
                if (targetFile.exists()) {
                    targetFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return FtpletResult.DEFAULT
        }

        override fun onUploadEnd(session: FtpSession, request: FtpRequest): FtpletResult {
            try {
                val userFileSystem = session.fileSystemView ?: return FtpletResult.DEFAULT
                val currentFtpFile = userFileSystem.workingDirectory ?: return FtpletResult.DEFAULT
                val currentDir = File(rootDir, currentFtpFile.absolutePath)
                val files = currentDir.listFiles() ?: return FtpletResult.DEFAULT

                // Clean up / auto-rename any orphan Solid Explorer SE_<HEX>_<realName> temporary files
                for (f in files) {
                    val name = f.name
                    if (name.startsWith("SE_") && name.contains("_")) {
                        val underscoreIndex = name.indexOf('_', 3)
                        if (underscoreIndex != -1 && underscoreIndex + 1 < name.length) {
                            val realName = name.substring(underscoreIndex + 1)
                            val realFile = File(currentDir, realName)
                            if (realFile.exists()) {
                                realFile.delete()
                            }
                            f.renameTo(realFile)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return FtpletResult.DEFAULT
        }
    }
}
