package com.arkcompanion.repository

import android.util.Log
import com.arkcompanion.network.ArcDto
import com.arkcompanion.network.HideoutDto
import com.arkcompanion.network.ItemDto
import com.arkcompanion.network.metaForgeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// No persistent storage. Data is stored entirely in memory.
// Cache is cleared on termination by OS.
object DataRepository {
    private const val TAG = "DataRepository"
    private const val PAGE_SIZE = 100

    private val _itemsCache = MutableStateFlow<List<ItemDto>>(emptyList())
    val itemsCache: StateFlow<List<ItemDto>> = _itemsCache.asStateFlow()
    private val _arcsCache = MutableStateFlow<List<ArcDto>>(emptyList())
    val arcsCache: StateFlow<List<ArcDto>> = _arcsCache.asStateFlow()
    private val _hideoutCache = MutableStateFlow<List<HideoutDto>>(emptyList())

    private var itemsLastFetchTime = 0L
    private var arcsLastFetchTime = 0L
    private var hideoutLastFetchTime = 0L
    private const val CACHE_TTL = 5 * 60 * 1000L // 5 mins in-memory TTL
    private val mutex = Mutex()

    suspend fun getItems(forceRefresh: Boolean = false): Result<List<ItemDto>> {
        val now = System.currentTimeMillis()
        
        mutex.withLock {
            if (!forceRefresh && _itemsCache.value.isNotEmpty() && (now - itemsLastFetchTime) < CACHE_TTL) {
                return Result.success(_itemsCache.value)
            }
        }

        return try {
            val freshData = fetchAllItems()
            mutex.withLock {
                _itemsCache.value = freshData
                itemsLastFetchTime = System.currentTimeMillis()
            }
            Result.success(freshData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch items from live API", e)
            if (_itemsCache.value.isNotEmpty()) {
                Log.w(TAG, "Using cached live items after API failure")
                Result.success(_itemsCache.value)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getArcs(forceRefresh: Boolean = false): Result<List<ArcDto>> {
        val now = System.currentTimeMillis()

        mutex.withLock {
            if (!forceRefresh && _arcsCache.value.isNotEmpty() && (now - arcsLastFetchTime) < CACHE_TTL) {
                return Result.success(_arcsCache.value)
            }
        }

        try {
            val freshData = fetchAllArcs()
            mutex.withLock {
                _arcsCache.value = freshData
                arcsLastFetchTime = System.currentTimeMillis()
            }
            return Result.success(freshData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch ARCs from live API", e)
            if (_arcsCache.value.isNotEmpty()) {
                Log.w(TAG, "Using cached live ARCs after API failure")
                return Result.success(_arcsCache.value)
            }
            return Result.failure(e)
        }
    }

    suspend fun getHideout(forceRefresh: Boolean = false): Result<List<HideoutDto>> {
        val now = System.currentTimeMillis()

        mutex.withLock {
            if (!forceRefresh && _hideoutCache.value.isNotEmpty() && (now - hideoutLastFetchTime) < CACHE_TTL) {
                return Result.success(_hideoutCache.value)
            }
        }

        try {
            val freshData = metaForgeService.getHideout()
            mutex.withLock {
                _hideoutCache.value = freshData
                hideoutLastFetchTime = System.currentTimeMillis()
            }
            return Result.success(freshData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Hideout from live API", e)
            if (_hideoutCache.value.isNotEmpty()) {
                Log.w(TAG, "Using cached live Hideout after API failure")
                return Result.success(_hideoutCache.value)
            }
            return Result.failure(e)
        }
    }

    private suspend fun fetchAllItems(): List<ItemDto> {
        val aggregated = mutableListOf<ItemDto>()
        var page = 1

        while (true) {
            val response = metaForgeService.getItems(page = page, limit = PAGE_SIZE)
            aggregated += response.data
            if (response.pagination?.hasNextPage != true) break
            page++
        }

        return aggregated
    }

    private suspend fun fetchAllArcs(): List<ArcDto> {
        val aggregated = mutableListOf<ArcDto>()
        var page = 1

        while (true) {
            val response = metaForgeService.getArcs(page = page, limit = PAGE_SIZE)
            aggregated += response.data
            if (response.pagination?.hasNextPage != true) break
            page++
        }

        return aggregated
    }
}
