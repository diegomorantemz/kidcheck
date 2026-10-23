package pe.edu.kidcheck.porteria.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PorteriaDao {

    @Query("SELECT * FROM roster ORDER BY apellidos COLLATE NOCASE, nombres COLLATE NOCASE")
    suspend fun roster(): List<RosterEntity>

    @Query("SELECT * FROM roster WHERE tokenHash = :hash LIMIT 1")
    suspend fun porHash(hash: String): RosterEntity?

    @Query(
        """
        SELECT * FROM roster
         WHERE nombres LIKE '%' || :texto || '%' COLLATE NOCASE
            OR apellidos LIKE '%' || :texto || '%' COLLATE NOCASE
            OR codigoMatricula LIKE '%' || :texto || '%' COLLATE NOCASE
         ORDER BY apellidos COLLATE NOCASE, nombres COLLATE NOCASE
         LIMIT 80
        """
    )
    suspend fun buscar(texto: String): List<RosterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarRoster(items: List<RosterEntity>)

    @Query("DELETE FROM roster")
    suspend fun borrarRoster()

    @Transaction
    suspend fun reemplazarRoster(items: List<RosterEntity>) {
        borrarRoster()
        insertarRoster(items)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun encolar(item: ColaEntity)

    @Query("SELECT * FROM cola_ingreso WHERE estado = 'PENDIENTE' ORDER BY fechaHoraUtc LIMIT :limite")
    suspend fun pendientes(limite: Int): List<ColaEntity>

    @Query(
        """
        SELECT * FROM cola_ingreso
         WHERE fechaHoraUtc >= :desde AND fechaHoraUtc < :hasta
         ORDER BY fechaHoraUtc DESC
        """
    )
    fun observarDelDia(desde: Long, hasta: Long): Flow<List<ColaEntity>>

    @Query("SELECT COUNT(*) FROM cola_ingreso WHERE estado = 'PENDIENTE'")
    fun observarPendientes(): Flow<Int>

    @Query(
        """
        SELECT * FROM cola_ingreso
         WHERE idEstudiante = :idEstudiante
           AND fechaHoraUtc >= :desde AND fechaHoraUtc < :hasta
           AND estado != 'ERROR'
         LIMIT 1
        """
    )
    suspend fun ingresoDelDia(idEstudiante: Long, desde: Long, hasta: Long): ColaEntity?

    @Query("UPDATE cola_ingreso SET estado = :estado, error = :error WHERE clientId = :clientId")
    suspend fun cambiarEstado(clientId: String, estado: String, error: String?)

    @Query("SELECT valor FROM sync_state WHERE clave = :clave")
    suspend fun estado(clave: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarEstado(estado: SyncStateEntity)
}
