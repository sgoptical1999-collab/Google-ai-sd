package com.example.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Core repository abstraction providing standardized async execution
 * and CRUD contract across all local SQLite repositories.
 */
interface BaseRepository<T, ID> {
    fun observeAll(): Flow<List<T>>
    suspend fun save(item: T): Long
    suspend fun update(item: T)
    suspend fun delete(item: T)
}

abstract class CoroutineRepository(
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    protected suspend fun <R> executeIo(block: suspend () -> R): R =
        withContext(ioDispatcher) { block() }
}
