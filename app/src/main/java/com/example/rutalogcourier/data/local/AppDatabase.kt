package com.example.rutalogcourier.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun paradaDao(): ParadaDao
    abstract fun operacionPendienteDao(): OperacionPendienteDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        /** Fase de pagos: cobro contraentrega de cada parada. Solo agrega columnas: la cola no se pierde. */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("cobroId", "cobroEstado", "cobroQr", "cobroNro").forEach {
                    db.execSQL("ALTER TABLE `paradas` ADD COLUMN `$it` TEXT")
                }
            }
        }

        /** Fase BI y ML: riesgo de falla de cada entrega (ML-01). Solo agrega columnas: la cola no se pierde. */
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `paradas` ADD COLUMN `riesgoProbabilidad` REAL")
                db.execSQL("ALTER TABLE `paradas` ADD COLUMN `riesgoAlto` INTEGER")
                db.execSQL("ALTER TABLE `paradas` ADD COLUMN `riesgoFactores` TEXT")
            }
        }

        fun getDB(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rutalog_courier.db"
                )
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                    .build()
                    .also { instancia = it }
            }
    }
}
