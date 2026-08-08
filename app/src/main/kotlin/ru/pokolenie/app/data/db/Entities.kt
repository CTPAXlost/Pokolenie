package ru.pokolenie.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.pokolenie.app.data.model.ProtocolType

@Entity(
    tableName = "sources",
    indices = [Index(value = ["url"], unique = true)]
)
data class SourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val lastUpdatedAt: Long = 0,
    val lastError: String? = null
)

@Entity(
    tableName = "servers",
    indices = [
        Index(value = ["dedupeKey"], unique = true),
        Index(value = ["sourceId"])
    ]
)
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long?,
    val name: String,
    val protocol: ProtocolType,
    val host: String,
    val port: Int,
    val uuidOrPassword: String,
    val rawLink: String,
    val sni: String? = null,
    val fingerprint: String? = null,
    val flow: String? = null,
    val security: String? = null,
    val transport: String? = null,
    val publicKey: String? = null,
    val shortId: String? = null,
    val path: String? = null,
    val alpn: String? = null,
    val dedupeKey: String,
    val latencyMs: Long? = null,
    val lastPingAt: Long = 0,
    val isFavorite: Boolean = false,
    val isSelected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "warp_profiles")
data class WarpProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val privateKey: String,
    val publicKey: String,
    val addressV4: String,
    val addressV6: String?,
    val endpointHost: String,
    val endpointPort: Int,
    val clientId: String?,
    val confText: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isSelected: Boolean = false
)
