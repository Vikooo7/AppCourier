package com.example.rutalogcourier.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.rutalogcourier.data.local.dao.OperacionPendienteDao
import com.example.rutalogcourier.data.local.dao.ParadaDao
import com.example.rutalogcourier.data.local.dao.UsuarioDao
import com.example.rutalogcourier.data.local.entities.OperacionPendienteEntity
import com.example.rutalogcourier.data.local.entities.ParadaEntity
import com.example.rutalogcourier.data.local.entities.ResumenRutaEntity
import com.example.rutalogcourier.data.local.entities.SyncMetadataEntity
import com.example.rutalogcourier.data.local.entities.UsuarioEntity

/**
 * Base de datos local (SQLite mediante Room).
 * Tablas: usuarios, paradas, resumen_ruta, operaciones_pendientes y sync_metadata.
 * La ruta del día se descarga de la API; lo que el courier registra sin señal espera en la cola (RF-C08).
 */
@Database(
    entities = [
        UsuarioEntity::class,
        ParadaEntity::class,
        ResumenRutaEntity::class,
        OperacionPendienteEntity::class,
        SyncMetadataEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun paradaDao(): ParadaDao
    abstract fun operacionPendienteDao(): OperacionPendienteDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun getDB(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rutalog_courier.db"
                )
                    .build()
                    .also { instancia = it }
            }
    }
}
