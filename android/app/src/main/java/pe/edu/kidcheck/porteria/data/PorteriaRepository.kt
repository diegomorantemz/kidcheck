package pe.edu.kidcheck.porteria.data

import kotlinx.coroutines.flow.Flow
import pe.edu.kidcheck.porteria.net.EstudianteRoster
import pe.edu.kidcheck.porteria.net.KidCheckApi
import pe.edu.kidcheck.porteria.net.SyncItem
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class ResultadoSync(val enviados: Int, val duplicados: Int, val errores: Int) {
    val total: Int get() = enviados + duplicados + errores
}

sealed interface ResultadoRegistro {
    data class Registrado(val item: ColaEntity) : ResultadoRegistro
    data class Duplicado(val existente: ColaEntity) : ResultadoRegistro
}

class PorteriaRepository(
    private val dao: PorteriaDao,
    private val api: KidCheckApi,
    private val prefs: SecurePrefs
) {

    private val zona = ZoneId.of("America/Lima")

    suspend fun roster(): List<RosterEntity> = dao.roster()

    suspend fun buscarRoster(texto: String): List<RosterEntity> =
        if (texto.isBlank()) dao.roster() else dao.buscar(texto.trim())

    suspend fun sincronizarRoster(): Int {
        val respuesta = api.roster()
        val entidades = respuesta.estudiantes.map { it.aEntidad() }
        dao.reemplazarRoster(entidades)
        prefs.ultimaSyncRoster = System.currentTimeMillis()
        return entidades.size
    }

    suspend fun sincronizarCola(): ResultadoSync {
        val pendientes = dao.pendientes(LOTE)
        if (pendientes.isEmpty()) {
            prefs.ultimaSyncCola = System.currentTimeMillis()
            return ResultadoSync(0, 0, 0)
        }

        val items = pendientes.map {
            SyncItem(
                clientId = it.clientId,
                idEstudiante = it.idEstudiante,
                fechaHoraUtc = Instant.ofEpochMilli(it.fechaHoraUtc).toString(),
                canal = it.canal,
                observacion = it.motivo
            )
        }

        val respuesta = api.sincronizarIngresos(items)
        val porCliente = respuesta.resultados.associateBy { it.clientId }
        var enviados = 0
        var duplicados = 0
        var errores = 0

        for (pendiente in pendientes) {
            val resultado = porCliente[pendiente.clientId]
            when (resultado?.status) {
                "CREADO" -> {
                    dao.cambiarEstado(pendiente.clientId, ESTADO_ENVIADO, null)
                    enviados++
                }
                "DUPLICADO" -> {
                    dao.cambiarEstado(pendiente.clientId, ESTADO_ENVIADO, null)
                    duplicados++
                }
                "ERROR" -> {
                    dao.cambiarEstado(
                        pendiente.clientId,
                        ESTADO_ERROR,
                        resultado.mensaje ?: resultado.codigo ?: "Error al sincronizar"
                    )
                    errores++
                }
                else -> {
                    // Sin respuesta para este clientId: queda pendiente para el próximo intento.
                }
            }
        }

        prefs.ultimaSyncCola = System.currentTimeMillis()
        return ResultadoSync(enviados, duplicados, errores)
    }

    suspend fun registrarEscaneo(estudiante: RosterEntity): ResultadoRegistro =
        registrar(estudiante, CANAL_QR, null)

    suspend fun registrarManual(estudiante: RosterEntity, motivo: String): ResultadoRegistro =
        registrar(estudiante, CANAL_MANUAL, motivo)

    private suspend fun registrar(
        estudiante: RosterEntity,
        canal: String,
        motivo: String?
    ): ResultadoRegistro {
        val (desde, hasta) = rangoDelDia()
        val existente = dao.ingresoDelDia(estudiante.idEstudiante, desde, hasta)
        if (existente != null) return ResultadoRegistro.Duplicado(existente)

        val ahora = System.currentTimeMillis()
        val item = ColaEntity(
            clientId = UUID.randomUUID().toString(),
            idEstudiante = estudiante.idEstudiante,
            nombre = estudiante.nombreCompleto,
            seccion = estudiante.seccion,
            fechaHoraUtc = ahora,
            canal = canal,
            motivo = motivo,
            estado = ESTADO_PENDIENTE,
            creadoEn = ahora,
            error = null
        )
        dao.encolar(item)
        return ResultadoRegistro.Registrado(item)
    }

    fun observarDelDia(): Flow<List<ColaEntity>> {
        val (desde, hasta) = rangoDelDia()
        return dao.observarDelDia(desde, hasta)
    }

    fun observarPendientes(): Flow<Int> = dao.observarPendientes()

    fun rangoDelDia(): Pair<Long, Long> {
        val hoy = LocalDate.now(zona)
        val inicio = hoy.atStartOfDay(zona).toInstant().toEpochMilli()
        val fin = hoy.plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli()
        return inicio to fin
    }

    suspend fun estudiantePorQr(contenido: String): RosterEntity? =
        dao.porHash(hashQr(contenido))

    suspend fun estado(clave: String): String? = dao.estado(clave)

    suspend fun guardarEstado(clave: String, valor: String) {
        dao.guardarEstado(SyncStateEntity(clave, valor))
    }

    private fun EstudianteRoster.aEntidad() = RosterEntity(
        idEstudiante = idEstudiante,
        codigoMatricula = codigoMatricula,
        nombres = nombres,
        apellidos = apellidos,
        tokenHash = tokenHash.lowercase(),
        seccion = seccion,
        horaLimite = horaLimite
    )

    companion object {
        const val ESTADO_PENDIENTE = "PENDIENTE"
        const val ESTADO_ENVIADO = "ENVIADO"
        const val ESTADO_ERROR = "ERROR"
        const val CANAL_QR = "qr"
        const val CANAL_MANUAL = "manual"
        private const val LOTE = 50

        fun hashQr(contenido: String): String {
            val resumen = MessageDigest.getInstance("SHA-256")
                .digest(contenido.trim().toByteArray(Charsets.UTF_8))
            return resumen.joinToString("") { byte -> "%02x".format(byte) }
        }
    }
}
