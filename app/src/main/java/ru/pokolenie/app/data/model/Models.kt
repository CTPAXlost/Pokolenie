package ru.pokolenie.app.data.model

enum class ProtocolType {
    VLESS, TROJAN, WARP, UNKNOWN
}

data class ParsedProxy(
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
    val dedupeKey: String
)

data class PingResult(
    val serverId: Long,
    val latencyMs: Long?,
    val ok: Boolean,
    val message: String? = null
)

data class RefreshSummary(
    val added: Int,
    val updated: Int,
    val removedDead: Int,
    val sourcesOk: Int,
    val sourcesFailed: Int
)
