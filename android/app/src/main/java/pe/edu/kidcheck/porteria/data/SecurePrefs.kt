package pe.edu.kidcheck.porteria.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import pe.edu.kidcheck.porteria.BuildConfig
import pe.edu.kidcheck.porteria.net.LoginResponse
import java.security.SecureRandom
import java.util.UUID

/**
 * Secretos de la tablet (tokens, clave de SQLCipher, PIN) cifrados con una
 * clave del Android Keystore. Si el Keystore no está disponible en un
 * emulador antiguo, se degrada a preferencias privadas para no bloquear la
 * demo; el camino normal siempre es cifrado.
 */
class SecurePrefs(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            ARCHIVO,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (ex: Exception) {
        Log.e(TAG, "Keystore no disponible; se usan preferencias privadas", ex)
        context.getSharedPreferences("${ARCHIVO}_respaldo", Context.MODE_PRIVATE)
    }

    init {
        // Si el equipo tenía guardada la URL del emulador (10.0.2.2) y este build
        // apunta a otra, la reemplazamos: en un celular o tablet real esa IP
        // nunca resuelve y el login falla con "no se pudo conectar".
        val guardado = prefs.getString(CLAVE_SERVIDOR, null)
        if (guardado == SERVIDOR_EMULADOR && SERVIDOR_POR_DEFECTO != SERVIDOR_EMULADOR) {
            prefs.edit().putString(CLAVE_SERVIDOR, SERVIDOR_POR_DEFECTO).apply()
        }
    }

    var servidor: String
        get() = prefs.getString(CLAVE_SERVIDOR, SERVIDOR_POR_DEFECTO) ?: SERVIDOR_POR_DEFECTO
        set(valor) {
            prefs.edit().putString(CLAVE_SERVIDOR, valor.trim().trimEnd('/')).apply()
        }

    var accessToken: String?
        get() = prefs.getString(CLAVE_ACCESS, null)
        set(valor) {
            prefs.edit().putString(CLAVE_ACCESS, valor).apply()
        }

    var refreshToken: String?
        get() = prefs.getString(CLAVE_REFRESH, null)
        set(valor) {
            prefs.edit().putString(CLAVE_REFRESH, valor).apply()
        }

    var usuarioNombre: String
        get() = prefs.getString(CLAVE_USUARIO, "") ?: ""
        set(valor) {
            prefs.edit().putString(CLAVE_USUARIO, valor).apply()
        }

    var colegioNombre: String
        get() = prefs.getString(CLAVE_COLEGIO, "") ?: ""
        set(valor) {
            prefs.edit().putString(CLAVE_COLEGIO, valor).apply()
        }

    val deviceUuid: String
        get() {
            val actual = prefs.getString(CLAVE_DEVICE, null)
            if (actual != null) return actual
            val nuevo = UUID.randomUUID().toString()
            prefs.edit().putString(CLAVE_DEVICE, nuevo).apply()
            return nuevo
        }

    var ultimaSyncRoster: Long
        get() = prefs.getLong(CLAVE_SYNC_ROSTER, 0L)
        set(valor) {
            prefs.edit().putLong(CLAVE_SYNC_ROSTER, valor).apply()
        }

    var ultimaSyncCola: Long
        get() = prefs.getLong(CLAVE_SYNC_COLA, 0L)
        set(valor) {
            prefs.edit().putLong(CLAVE_SYNC_COLA, valor).apply()
        }

    val estaAutenticado: Boolean
        get() = !accessToken.isNullOrBlank()

    fun guardarSesion(respuesta: LoginResponse) {
        accessToken = respuesta.accessToken
        refreshToken = respuesta.refreshToken
        usuarioNombre = respuesta.usuario.nombre
        colegioNombre = respuesta.colegio.nombre
    }

    fun limpiarSesion() {
        prefs.edit()
            .remove(CLAVE_ACCESS)
            .remove(CLAVE_REFRESH)
            .remove(CLAVE_USUARIO)
            .remove(CLAVE_COLEGIO)
            .apply()
    }

    fun claveBaseDatos(): ByteArray {
        val guardada = prefs.getString(CLAVE_DB, null)
        if (guardada != null) {
            return Base64.decode(guardada, Base64.NO_WRAP)
        }
        val nueva = ByteArray(32)
        SecureRandom().nextBytes(nueva)
        prefs.edit().putString(CLAVE_DB, Base64.encodeToString(nueva, Base64.NO_WRAP)).apply()
        return nueva
    }

    fun pinValido(pin: String): Boolean =
        pin == (prefs.getString(CLAVE_PIN, PIN_POR_DEFECTO) ?: PIN_POR_DEFECTO)

    fun cambiarPin(nuevo: String) {
        if (nuevo.length in 4..8) {
            prefs.edit().putString(CLAVE_PIN, nuevo).apply()
        }
    }

    private companion object {
        const val TAG = "KidCheckPrefs"
        const val ARCHIVO = "kidcheck_porteria_seguro"
        const val CLAVE_SERVIDOR = "servidor"
        const val CLAVE_ACCESS = "access_token"
        const val CLAVE_REFRESH = "refresh_token"
        const val CLAVE_USUARIO = "usuario_nombre"
        const val CLAVE_COLEGIO = "colegio_nombre"
        const val CLAVE_DEVICE = "device_uuid"
        const val CLAVE_SYNC_ROSTER = "sync_roster"
        const val CLAVE_SYNC_COLA = "sync_cola"
        const val CLAVE_DB = "db_key"
        const val CLAVE_PIN = "pin_kiosco"
        const val PIN_POR_DEFECTO = "1234"
        val SERVIDOR_POR_DEFECTO = BuildConfig.API_URL_POR_DEFECTO
        const val SERVIDOR_EMULADOR = "http://10.0.2.2:8080"
    }
}
