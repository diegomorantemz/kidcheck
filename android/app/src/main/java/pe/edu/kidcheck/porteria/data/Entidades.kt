package pe.edu.kidcheck.porteria.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "roster",
    indices = [Index(value = ["tokenHash"], unique = true)]
)
data class RosterEntity(
    @PrimaryKey val idEstudiante: Long,
    val codigoMatricula: String,
    val nombres: String,
    val apellidos: String,
    val tokenHash: String,
    val seccion: String,
    val horaLimite: String?
) {
    val nombreCompleto: String
        get() = "$nombres $apellidos"
}

@Entity(
    tableName = "cola_ingreso",
    indices = [Index(value = ["idEstudiante", "fechaHoraUtc"]), Index(value = ["estado"])]
)
data class ColaEntity(
    @PrimaryKey val clientId: String,
    val idEstudiante: Long,
    val nombre: String,
    val seccion: String,
    val fechaHoraUtc: Long,
    val canal: String,
    val motivo: String?,
    val estado: String,
    val creadoEn: Long,
    val error: String?
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val clave: String,
    val valor: String
)
