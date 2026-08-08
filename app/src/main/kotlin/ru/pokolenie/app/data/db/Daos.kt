package ru.pokolenie.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY id ASC")
    fun observeAll(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE enabled = 1 ORDER BY id ASC")
    suspend fun getEnabled(): List<SourceEntity>

    @Query("SELECT * FROM sources ORDER BY id ASC")
    suspend fun getAll(): List<SourceEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: SourceEntity): Long

    @Update
    suspend fun update(entity: SourceEntity)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE sources SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE sources SET lastUpdatedAt = :ts, lastError = :error WHERE id = :id")
    suspend fun markUpdated(id: Long, ts: Long, error: String?)
}

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY CASE WHEN latencyMs IS NULL THEN 1 ELSE 0 END, latencyMs ASC, name ASC")
    fun observeAll(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers ORDER BY CASE WHEN latencyMs IS NULL THEN 1 ELSE 0 END, latencyMs ASC")
    suspend fun getAll(): List<ServerEntity>

    @Query("SELECT * FROM servers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ServerEntity?

    @Query("SELECT * FROM servers WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelected(): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ServerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ServerEntity>)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM servers WHERE id IN (:ids)")
    suspend fun deleteIds(ids: List<Long>)

    @Query("DELETE FROM servers WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: Long)

    @Query("UPDATE servers SET isSelected = 0")
    suspend fun clearSelection()

    @Query("UPDATE servers SET isSelected = 1 WHERE id = :id")
    suspend fun select(id: Long)

    @Query("UPDATE servers SET latencyMs = :latency, lastPingAt = :ts WHERE id = :id")
    suspend fun updateLatency(id: Long, latency: Long?, ts: Long)

    @Query("SELECT * FROM servers WHERE dedupeKey = :key LIMIT 1")
    suspend fun findByDedupe(key: String): ServerEntity?
}

@Dao
interface WarpDao {
    @Query("SELECT * FROM warp_profiles ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WarpProfileEntity>>

    @Query("SELECT * FROM warp_profiles ORDER BY createdAt DESC")
    suspend fun getAll(): List<WarpProfileEntity>

    @Query("SELECT * FROM warp_profiles WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelected(): WarpProfileEntity?

    @Insert
    suspend fun insert(entity: WarpProfileEntity): Long

    @Query("DELETE FROM warp_profiles WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE warp_profiles SET isSelected = 0")
    suspend fun clearSelection()

    @Query("UPDATE warp_profiles SET isSelected = 1 WHERE id = :id")
    suspend fun select(id: Long)

    @Query("UPDATE warp_profiles SET latencyMs = :latency, lastPingAt = :ts WHERE id = :id")
    suspend fun updateLatency(id: Long, latency: Long?, ts: Long)
}
