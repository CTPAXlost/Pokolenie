package ru.pokolenie.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.pokolenie.app.data.db.DefaultSources
import ru.pokolenie.app.data.db.ServerDao
import ru.pokolenie.app.data.db.ServerEntity
import ru.pokolenie.app.data.db.SourceDao
import ru.pokolenie.app.data.db.SourceEntity
import ru.pokolenie.app.data.model.ParsedProxy
import ru.pokolenie.app.data.model.RefreshSummary
import ru.pokolenie.app.data.parser.ProxyLinkParser
import ru.pokolenie.app.data.remote.HttpClient

class SubscriptionRepository(
    private val sourceDao: SourceDao,
    private val serverDao: ServerDao
) {
    val sources: Flow<List<SourceEntity>> = sourceDao.observeAll()
    val servers: Flow<List<ServerEntity>> = serverDao.observeAll()

    suspend fun ensureDefaults() = withContext(Dispatchers.IO) {
        if (sourceDao.getAll().isEmpty()) {
            DefaultSources.all.forEach { sourceDao.insert(it) }
        }
    }

    suspend fun addSource(name: String, url: String) = withContext(Dispatchers.IO) {
        sourceDao.insert(SourceEntity(name = name.trim(), url = url.trim()))
    }

    suspend fun setSourceEnabled(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        sourceDao.setEnabled(id, enabled)
    }

    suspend fun deleteSource(id: Long) = withContext(Dispatchers.IO) {
        serverDao.deleteBySource(id)
        sourceDao.delete(id)
    }

    suspend fun selectServer(id: Long) = withContext(Dispatchers.IO) {
        serverDao.clearSelection()
        serverDao.select(id)
    }

    suspend fun deleteServer(id: Long) = withContext(Dispatchers.IO) {
        serverDao.delete(id)
    }

    suspend fun deleteServers(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) serverDao.deleteIds(ids)
    }

    suspend fun getSelectedServer(): ServerEntity? = withContext(Dispatchers.IO) {
        serverDao.getSelected() ?: serverDao.getAll().firstOrNull()
    }

    suspend fun getServer(id: Long): ServerEntity? = withContext(Dispatchers.IO) {
        serverDao.getById(id)
    }

    suspend fun refreshAll(): RefreshSummary = withContext(Dispatchers.IO) {
        val enabled = sourceDao.getEnabled()
        var added = 0
        var updated = 0
        var ok = 0
        var failed = 0
        val seenKeys = mutableSetOf<String>()

        enabled.forEach { source ->
            try {
                val body = HttpClient.getText(source.url)
                val parsed = ProxyLinkParser.parseSubscriptionBody(body)
                parsed.forEach { proxy ->
                    seenKeys += proxy.dedupeKey
                    val existing = serverDao.findByDedupe(proxy.dedupeKey)
                    if (existing == null) {
                        serverDao.upsert(proxy.toEntity(source.id))
                        added++
                    } else {
                        serverDao.upsert(
                            existing.copy(
                                sourceId = source.id,
                                name = proxy.name,
                                rawLink = proxy.rawLink,
                                sni = proxy.sni,
                                fingerprint = proxy.fingerprint,
                                flow = proxy.flow,
                                security = proxy.security,
                                transport = proxy.transport,
                                publicKey = proxy.publicKey,
                                shortId = proxy.shortId,
                                path = proxy.path,
                                alpn = proxy.alpn
                            )
                        )
                        updated++
                    }
                }
                sourceDao.markUpdated(source.id, System.currentTimeMillis(), null)
                ok++
            } catch (e: Exception) {
                sourceDao.markUpdated(source.id, System.currentTimeMillis(), e.message)
                failed++
            }
        }

        RefreshSummary(
            added = added,
            updated = updated,
            removedDead = 0,
            sourcesOk = ok,
            sourcesFailed = failed
        )
    }

    private fun ParsedProxy.toEntity(sourceId: Long) = ServerEntity(
        sourceId = sourceId,
        name = name,
        protocol = protocol,
        host = host,
        port = port,
        uuidOrPassword = uuidOrPassword,
        rawLink = rawLink,
        sni = sni,
        fingerprint = fingerprint,
        flow = flow,
        security = security,
        transport = transport,
        publicKey = publicKey,
        shortId = shortId,
        path = path,
        alpn = alpn,
        dedupeKey = dedupeKey
    )
}
