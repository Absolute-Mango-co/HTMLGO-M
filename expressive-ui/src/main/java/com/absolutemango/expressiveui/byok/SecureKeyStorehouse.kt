package com.absolutemango.expressiveui.byok

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException

open class SecureKeyStorehouse(private val context: Context? = null) {

    companion object {
        private const val TAG = "SecureKeyStorehouse"
        const val PREFS_FILE_NAME = "ai_forge_secure_keys"
        private const val SUFFIX_PRIMARY = "_primary_key"
        private const val SUFFIX_BACKUP = "_backup_key"
    }

    private var sharedPreferences: SharedPreferences? = null

    init {
        if (context != null) {
            try {
                initializeVault()
            } catch (_: Exception) {
                // Ignored in headless unit test environments
            }
        }
    }

    @Synchronized
    private fun initializeVault() {
        val ctx = context ?: return
        try {
            val masterKey = MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            sharedPreferences = EncryptedSharedPreferences.create(
                ctx,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            when (e) {
                is GeneralSecurityException, is IOException -> {
                    Log.e(TAG, "Keystore or cryptographic corruption detected during initialization. Wiping vault.", e)
                    wipeCorruptedVault()
                    try {
                        val masterKey = MasterKey.Builder(ctx)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build()
                        sharedPreferences = EncryptedSharedPreferences.create(
                            ctx,
                            PREFS_FILE_NAME,
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        )
                    } catch (retryEx: Exception) {
                        Log.e(TAG, "Failed to re-initialize EncryptedSharedPreferences after wiping vault.", retryEx)
                        sharedPreferences = null
                    }
                }
                else -> {
                    Log.e(TAG, "Unexpected error initializing SecureKeyStorehouse", e)
                    wipeCorruptedVault()
                }
            }
        }
    }

    @Synchronized
    open fun storeKeys(profileId: String, primaryKey: String, backupKey: String?) {
        val prefs = sharedPreferences ?: run {
            initializeVault()
            sharedPreferences
        } ?: return

        prefs.edit().apply {
            putString("${profileId}$SUFFIX_PRIMARY", primaryKey)
            if (!backupKey.isNullOrBlank()) {
                putString("${profileId}$SUFFIX_BACKUP", backupKey)
            } else {
                remove("${profileId}$SUFFIX_BACKUP")
            }
            apply()
        }
    }

    @Synchronized
    open fun getPrimaryKey(profileId: String): String? {
        val prefs = sharedPreferences ?: run {
            initializeVault()
            sharedPreferences
        } ?: return null

        return prefs.getString("${profileId}$SUFFIX_PRIMARY", null)
    }

    @Synchronized
    open fun getBackupKey(profileId: String): String? {
        val prefs = sharedPreferences ?: run {
            initializeVault()
            sharedPreferences
        } ?: return null

        return prefs.getString("${profileId}$SUFFIX_BACKUP", null)
    }

    @Synchronized
    open fun deleteKeys(profileId: String) {
        val prefs = sharedPreferences ?: run {
            initializeVault()
            sharedPreferences
        } ?: return

        prefs.edit()
            .remove("${profileId}$SUFFIX_PRIMARY")
            .remove("${profileId}$SUFFIX_BACKUP")
            .apply()
    }

    @Synchronized
    open fun wipeCorruptedVault() {
        try {
            sharedPreferences?.edit()?.clear()?.apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear SharedPreferences via editor", e)
        }
        sharedPreferences = null

        val ctx = context ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ctx.deleteSharedPreferences(PREFS_FILE_NAME)
            } else {
                val dataDir = ctx.applicationInfo.dataDir
                val prefsFile = File(dataDir, "shared_prefs/$PREFS_FILE_NAME.xml")
                if (prefsFile.exists()) {
                    prefsFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting corrupted shared preferences file", e)
        }
    }
}
