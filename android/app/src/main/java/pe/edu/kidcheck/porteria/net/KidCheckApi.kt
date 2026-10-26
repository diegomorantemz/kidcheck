package pe.edu.kidcheck.porteria.net

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import pe.edu.kidcheck.porteria.BuildConfig
import pe.edu.kidcheck.porteria.data.SecurePrefs
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

data class UsuarioDto(val id: Long, val nombre: String, val rol: String)
data class ColegioDto(val id: Long, val nombre: String)

data class LoginRequest(val usuario: String, val password: String)
data class RefreshRequest(val refreshToken: String)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val usuario: UsuarioDto,
    val colegio: ColegioDto
)

data class ColegioInfo(val id: Long, val zonaHoraria: String)
data class EstudianteRoster(
    val idEstudiante: Long,
    val codigoMatricula: String,
    val nombres: String,
    val apellidos: String,
    val tokenHash: String,
    val seccion: String,
    val horaLimite: String?,
    val fotoUrl: String? = null
)

data class RosterResponse(
    val version: String,
    val colegio: ColegioInfo,
    val estudiantes: List<EstudianteRoster>
)

data class SyncItem(
    val clientId: String,
    val idEstudiante: Long,
    val fechaHoraUtc: String,
    val canal: String,
    val observacion: String? = null
)

data class SyncIngresosRequest(val deviceUuid: String, val ingresos: List<SyncItem>)
data class SyncResultado(
    val clientId: String,
    val status: String,
    val idIngreso: Long? = null,
    val horaLocal: String? = null,
    val codigo: String? = null,
    val mensaje: String? = null
)

data class SyncIngresosResponse(val resultados: List<SyncResultado>)

private data class ErrorApi(
    val code: String? = null,
    val detail: String? = null,
    val title: String? = null
)

class ApiException(
    val codigo: String,
    override val message: String,
    val estadoHttp: Int
) : Exception(message)

class SinSesionException : Exception("La sesión expiró. Vuelve a iniciar sesión.")

class KidCheckApi(private val prefs: SecurePrefs) {

    private val gson = Gson()
    private val cliente = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = "application/json; charset=utf-8".toMediaType()

    fun normalizarServidor(valor: String): String {
        var servidor = valor.trim()
        if (servidor.isEmpty()) servidor = BuildConfig.API_URL_POR_DEFECTO
        if (!servidor.startsWith("http://") && !servidor.startsWith("https://")) {
            servidor = "http://$servidor"
        }
        return servidor.trimEnd('/')
    }

    suspend fun iniciarSesion(usuario: String, password: String): LoginResponse =
        withContext(Dispatchers.IO) {
            val cuerpo = gson.toJson(LoginRequest(usuario, password))
            val request = Request.Builder()
                .url("${prefs.servidor}/api/v1/auth/login")
                .post(cuerpo.toRequestBody(json))
                .build()

            cliente.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw errorDe(response)
                parsear(response, LoginResponse::class.java)
                    ?: throw ApiException("RESPUESTA_INVALIDA", "El servidor devolvió una respuesta vacía.", 0)
            }
        }

    suspend fun roster(): RosterResponse = withContext(Dispatchers.IO) {
        val response = solicitar("/api/v1/sync/roster", "GET", null)
        response.use {
            if (!it.isSuccessful) throw errorDe(it)
            parsear(it, RosterResponse::class.java)
                ?: throw ApiException("RESPUESTA_INVALIDA", "No se pudo leer el roster.", 0)
        }
    }

    suspend fun sincronizarIngresos(ingresos: List<SyncItem>): SyncIngresosResponse =
        withContext(Dispatchers.IO) {
            val cuerpo = gson.toJson(SyncIngresosRequest(prefs.deviceUuid, ingresos))
            val response = solicitar("/api/v1/sync/ingresos", "POST", cuerpo)
            response.use {
                if (!it.isSuccessful) throw errorDe(it)
                parsear(it, SyncIngresosResponse::class.java)
                    ?: throw ApiException("RESPUESTA_INVALIDA", "No se pudo leer la respuesta de sincronización.", 0)
            }
        }

    private fun solicitar(ruta: String, metodo: String, cuerpo: String?): Response {
        val token = prefs.accessToken ?: throw SinSesionException()
        var response = ejecutar(ruta, metodo, cuerpo, token)

        if (response.code == 401) {
            response.close()
            if (!refrescarToken()) throw SinSesionException()
            val nuevoToken = prefs.accessToken ?: throw SinSesionException()
            response = ejecutar(ruta, metodo, cuerpo, nuevoToken)
        }
        return response
    }

    private fun ejecutar(ruta: String, metodo: String, cuerpo: String?, token: String): Response {
        val constructor = Request.Builder()
            .url("${prefs.servidor}$ruta")
            .header("Authorization", "Bearer $token")

        when (metodo) {
            "GET" -> constructor.get()
            "POST" -> constructor.post((cuerpo ?: "{}").toRequestBody(json))
            else -> throw IllegalArgumentException("Método no soportado: $metodo")
        }
        return cliente.newCall(constructor.build()).execute()
    }

    private fun refrescarToken(): Boolean {
        val refresh = prefs.refreshToken ?: return false
        val cuerpo = gson.toJson(RefreshRequest(refresh))
        val request = Request.Builder()
            .url("${prefs.servidor}/api/v1/auth/refresh")
            .post(cuerpo.toRequestBody(json))
            .build()

        return try {
            cliente.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "No se pudo renovar la sesión: HTTP ${response.code}")
                    prefs.limpiarSesion()
                    false
                } else {
                    val datos = parsear(response, LoginResponse::class.java)
                    if (datos == null) {
                        prefs.limpiarSesion()
                        false
                    } else {
                        prefs.accessToken = datos.accessToken
                        prefs.refreshToken = datos.refreshToken
                        true
                    }
                }
            }
        } catch (ex: IOException) {
            Log.w(TAG, "Sin red al renovar la sesión", ex)
            false
        }
    }

    private fun <T> parsear(response: Response, clase: Class<T>): T? {
        val texto = response.body?.string() ?: return null
        return try {
            gson.fromJson(texto, clase)
        } catch (ex: JsonSyntaxException) {
            throw ApiException("RESPUESTA_INVALIDA", "Respuesta ilegible del servidor.", response.code)
        }
    }

    private fun errorDe(response: Response): ApiException {
        val texto = response.body?.string()
        val error = try {
            if (texto.isNullOrBlank()) null else gson.fromJson(texto, ErrorApi::class.java)
        } catch (_: Exception) {
            null
        }
        val mensaje = error?.detail ?: error?.title ?: "Error del servidor (HTTP ${response.code})."
        val codigo = error?.code ?: "HTTP_${response.code}"
        return ApiException(codigo, mensaje, response.code)
    }

    private companion object {
        const val TAG = "KidCheckApi"
    }
}
