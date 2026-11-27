package ai.bondhu.assistant.data.prefs

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("bondhu_settings")

private val KEY_GEMINI = stringPreferencesKey("gemini_key_enc")
private val KEY_LANG = stringPreferencesKey("language")
private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
private val KEY_LOCAL_MODE = booleanPreferencesKey("local_voice_mode")

class SettingsStore(private val context: Context) {
    val language: Flow<String> = context.dataStore.data.map { it[KEY_LANG] ?: "bn-BD" }
    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDED] ?: false }
    val localMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_LOCAL_MODE] ?: true }

    suspend fun setOnboarded(value: Boolean) = context.dataStore.edit { it[KEY_ONBOARDED] = value }
    suspend fun setLanguage(value: String) = context.dataStore.edit { it[KEY_LANG] = value }
    suspend fun setLocalMode(value: Boolean) = context.dataStore.edit { it[KEY_LOCAL_MODE] = value }

    suspend fun saveGeminiKey(key: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val enc = cipher.doFinal(key.toByteArray())
        context.dataStore.edit {
            it[KEY_GEMINI] = android.util.Base64.encodeToString(
                cipher.iv + enc, android.util.Base64.NO_WRAP
            )
        }
    }

    suspend fun readGeminiKey(): String? {
        val raw = context.dataStore.data.map { it[KEY_GEMINI] }.firstOrNull() ?: return null
        val bytes = android.util.Base64.decode(raw, android.util.Base64.NO_WRAP)
        if (bytes.size < 13) return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)))
    }

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "bondhu_gemini_key"
        if (!ks.containsAlias(alias)) {
            val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            kg.init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            kg.generateKey()
        }
        return ks.getKey(alias, null) as SecretKey
    }
}
