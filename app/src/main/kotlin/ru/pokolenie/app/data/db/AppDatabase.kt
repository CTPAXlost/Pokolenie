package ru.pokolenie.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import ru.pokolenie.app.data.model.ProtocolType

class Converters {
    @TypeConverter
    fun toProtocol(value: String): ProtocolType = runCatching { ProtocolType.valueOf(value) }
        .getOrDefault(ProtocolType.UNKNOWN)

    @TypeConverter
    fun fromProtocol(value: ProtocolType): String = value.name
}

@Database(
    entities = [SourceEntity::class, ServerEntity::class, WarpProfileEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun serverDao(): ServerDao
    abstract fun warpDao(): WarpDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pokolenie.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}

object DefaultSources {
    private const val V2_KEY = "49941B8EAED51CC"

    val all = listOf(
        SourceEntity(
            name = "Whitelist VLESS (rjsxrd)",
            url = "https://raw.githubusercontent.com/whoahaow/rjsxrd/refs/heads/main/githubmirror/split-by-protocols/vless.txt"
        ),
        SourceEntity(
            name = "Whitelist Trojan (rjsxrd)",
            url = "https://raw.githubusercontent.com/whoahaow/rjsxrd/refs/heads/main/githubmirror/split-by-protocols/trojan.txt"
        ),
        SourceEntity(
            name = "deqwl VLESS",
            url = "https://raw.githubusercontent.com/dequar/deqwl/refs/heads/main/deray.txt"
        ),
        SourceEntity(
            name = "sbornik VLESS",
            url = "https://raw.githubusercontent.com/kort0881/sbornik-vless/main/configs/final/vless.txt"
        ),
        SourceEntity(
            name = "sbornik Trojan",
            url = "https://raw.githubusercontent.com/kort0881/sbornik-vless/main/configs/final/trojan.txt"
        ),
        SourceEntity(name = "V2Nodes DE", url = "https://www.v2nodes.com/subscriptions/country/de/?key=$V2_KEY"),
        SourceEntity(name = "V2Nodes NL", url = "https://www.v2nodes.com/subscriptions/country/nl/?key=$V2_KEY"),
        SourceEntity(name = "V2Nodes FI", url = "https://www.v2nodes.com/subscriptions/country/fi/?key=$V2_KEY"),
        SourceEntity(name = "V2Nodes CZ", url = "https://www.v2nodes.com/subscriptions/country/cz/?key=$V2_KEY"),
        SourceEntity(name = "V2Nodes AM", url = "https://www.v2nodes.com/subscriptions/country/am/?key=$V2_KEY"),
        SourceEntity(name = "V2Nodes SE", url = "https://www.v2nodes.com/subscriptions/country/se/?key=$V2_KEY"),
        SourceEntity(name = "V2Nodes LT", url = "https://www.v2nodes.com/subscriptions/country/lt/?key=$V2_KEY"),
        SourceEntity(name = "V2Nodes US", url = "https://www.v2nodes.com/subscriptions/country/us/?key=$V2_KEY"),
        SourceEntity(name = "V2Nodes KZ", url = "https://www.v2nodes.com/subscriptions/country/kz/?key=$V2_KEY")
    )
}
