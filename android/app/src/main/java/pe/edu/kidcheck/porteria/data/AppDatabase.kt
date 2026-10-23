package pe.edu.kidcheck.porteria.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [RosterEntity::class, ColaEntity::class, SyncStateEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao(): PorteriaDao

    companion object {
        fun build(context: Context, passphrase: ByteArray): AppDatabase {
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "kidcheck_porteria.db"
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
