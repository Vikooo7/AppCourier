package com.example.rutalogcourier.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.rutalogcourier.data.local.entities.OperacionPendienteEntity
import com.example.rutalogcourier.data.local.entities.ParadaEntity
import com.example.rutalogcourier.data.local.entities.ResumenRutaEntity
import com.example.rutalogcourier.data.local.entities.SyncMetadataEntity
import com.example.rutalogcourier.data.local.entities.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(usuario: UsuarioEntity)

    /** Perfil guardado en el último inicio de sesión. */
    @Query("SELECT * FROM usuarios LIMIT 1")
    suspend fun actual(): UsuarioEntity?

    @Query("DELETE FROM usuarios")
    suspend fun borrarTodo()
}

@Dao
interface ParadaDao {

    /** RF-C01: primero las paradas en su orden; al final lo que va en camino y lo completado. */
    @Query("SELECT * FROM paradas ORDER BY orden, idRemoto")
    fun observarTodas(): Flow<List<ParadaEntity>>

    @Query("SELECT * FROM paradas WHERE idRemoto = :idRemoto")
    fun observar(idRemoto: Int): Flow<ParadaEntity?>

    @Query("SELECT * FROM paradas WHERE idRemoto = :idRemoto")
    suspend fun porId(idRemoto: Int): ParadaEntity?

    @Query("SELECT * FROM paradas")
    suspend fun todas(): List<ParadaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(parada: ParadaEntity)

    @Update
    suspend fun actualizar(parada: ParadaEntity)

    @Delete
    suspend fun eliminar(parada: ParadaEntity)

    @Query("UPDATE paradas SET estadoSync = 'PENDIENTE' WHERE estadoSync = 'ENVIANDO'")
    suspend fun recuperarInterrumpidas()

    @Query("DELETE FROM paradas")
    suspend fun borrarTodo()

    // ---- resumen_ruta ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarResumen(resumen: ResumenRutaEntity)

    @Query("SELECT * FROM resumen_ruta WHERE id = 1")
    fun observarResumen(): Flow<ResumenRutaEntity?>

    @Query("DELETE FROM resumen_ruta")
    suspend fun borrarResumen()
}

/** Operación pendiente junto con la guía del envío al que pertenece (para mostrarla). */
data class OperacionConGuia(
    @Embedded val operacion: OperacionPendienteEntity,
    val numeroGuia: String?
)

/** Cola local de operaciones y datos de la última sincronización. */
@Dao
interface OperacionPendienteDao {

    @Insert
    suspend fun insertar(operacion: OperacionPendienteEntity): Long

    @Update
    suspend fun actualizar(operacion: OperacionPendienteEntity)

    @Delete
    suspend fun eliminar(operacion: OperacionPendienteEntity)

    @Query(
        "SELECT o.*, p.numeroGuia AS numeroGuia FROM operaciones_pendientes o " +
            "LEFT JOIN paradas p ON p.idRemoto = o.idRemoto ORDER BY o.id"
    )
    fun observarConGuia(): Flow<List<OperacionConGuia>>

    @Query("SELECT * FROM operaciones_pendientes ORDER BY id")
    suspend fun todas(): List<OperacionPendienteEntity>

    /** En el orden en que ocurrieron. Las rechazadas (ERROR) esperan a que el courier reintente o descarte. */
    @Query("SELECT * FROM operaciones_pendientes WHERE estado != 'ERROR' ORDER BY id")
    suspend fun porEnviar(): List<OperacionPendienteEntity>

    @Query("SELECT * FROM operaciones_pendientes WHERE idRemoto = :idRemoto ORDER BY id")
    suspend fun deEnvio(idRemoto: Int): List<OperacionPendienteEntity>

    @Query("SELECT * FROM operaciones_pendientes WHERE id = :id")
    suspend fun porId(id: Int): OperacionPendienteEntity?

    @Query("UPDATE operaciones_pendientes SET estado = 'PENDIENTE' WHERE estado = 'ENVIANDO'")
    suspend fun recuperarInterrumpidas()

    @Query("DELETE FROM operaciones_pendientes")
    suspend fun borrarTodo()

    // ---- sync_metadata ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarMetadata(metadata: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE recurso = :recurso")
    fun observarMetadata(recurso: String): Flow<SyncMetadataEntity?>

    @Query("DELETE FROM sync_metadata")
    suspend fun borrarMetadata()
}
