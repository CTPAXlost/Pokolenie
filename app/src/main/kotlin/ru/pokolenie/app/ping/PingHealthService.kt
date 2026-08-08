package ru.pokolenie.app.ping

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import ru.pokolenie.app.data.db.ServerDao
import ru.pokolenie.app.data.db.ServerEntity
import ru.pokolenie.app.data.db.WarpDao
import ru.pokolenie.app.data.db.WarpProfileEntity
import ru.pokolenie.app.data.model.PingResult
import java.net.InetSocketAddress
import java.net.Socket

class PingHealthService(
    private val serverDao: ServerDao,
    private val warpDao: WarpDao? = null
) {
    suspend fun pingOne(server: ServerEntity, timeoutMs: Int): PingResult =
        withContext(Dispatchers.IO) {
            val start = System.nanoTime()
            try {
                Socket().use { socket ->
                    socket.tcpNoDelay = true
                    socket.connect(InetSocketAddress(server.host, server.port), timeoutMs)
                }
                val latency = (System.nanoTime() - start) / 1_000_000
                serverDao.updateLatency(server.id, latency, System.currentTimeMillis())
                PingResult(server.id, latency, true)
            } catch (e: Exception) {
                serverDao.delete(server.id)
                PingResult(server.id, null, false, e.message ?: "timeout")
            }
        }

    suspend fun pingAll(
        servers: List<ServerEntity>,
        timeoutMs: Int,
        parallelism: Int = 16
    ): Pair<List<PingResult>, Int> = withContext(Dispatchers.IO) {
        if (servers.isEmpty()) return@withContext emptyList<PingResult>() to 0
        coroutineScope {
            val results = servers.chunked(parallelism).flatMap { chunk ->
                chunk.map { server ->
                    async { pingOne(server, timeoutMs) }
                }.awaitAll()
            }
            val removed = results.count { !it.ok }
            results to removed
        }
    }

    /** TCP connect latency to Warp endpoint (не через туннель). */
    suspend fun pingWarp(profile: WarpProfileEntity, timeoutMs: Int): PingResult =
        withContext(Dispatchers.IO) {
            val dao = warpDao ?: return@withContext PingResult(profile.id, null, false, "no dao")
            val start = System.nanoTime()
            try {
                Socket().use { socket ->
                    socket.tcpNoDelay = true
                    socket.connect(
                        InetSocketAddress(profile.endpointHost, profile.endpointPort),
                        timeoutMs
                    )
                }
                val latency = (System.nanoTime() - start) / 1_000_000
                dao.updateLatency(profile.id, latency, System.currentTimeMillis())
                PingResult(profile.id, latency, true)
            } catch (e: Exception) {
                dao.updateLatency(profile.id, null, System.currentTimeMillis())
                PingResult(profile.id, null, false, e.message ?: "timeout")
            }
        }

    suspend fun pingAllWarp(
        profiles: List<WarpProfileEntity>,
        timeoutMs: Int,
        parallelism: Int = 8
    ): List<PingResult> = withContext(Dispatchers.IO) {
        if (profiles.isEmpty()) return@withContext emptyList()
        coroutineScope {
            profiles.chunked(parallelism).flatMap { chunk ->
                chunk.map { profile -> async { pingWarp(profile, timeoutMs) } }.awaitAll()
            }
        }
    }
}
