package com.arkcompanion.repository

import android.content.Context
import android.util.Log
import com.arkcompanion.data.AppDatabase
import com.arkcompanion.data.ArcEntity
import com.arkcompanion.data.ItemEntity
import com.arkcompanion.network.ArcDto
import com.arkcompanion.network.EventScheduleDto
import com.arkcompanion.network.HideoutDto
import com.arkcompanion.network.ItemDto
import com.arkcompanion.network.QuestDto
import com.arkcompanion.network.TraderResponseDto
import com.arkcompanion.network.metaForgeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// DataRepository handles both local DB caching and live API fetching.
object DataRepository {
    private const val TAG = "DataRepository"
    private const val PAGE_SIZE = 100

    private var db: AppDatabase? = null

    fun initialize(context: Context) {
        if (db == null) {
            db = AppDatabase.getDatabase(context)
        }
    }

    private val _hideoutCache = MutableStateFlow<List<HideoutDto>>(emptyList())
    private var hideoutLastFetchTime = 0L

    private val _tradersCache = MutableStateFlow<TraderResponseDto?>(null)
    private var tradersLastFetchTime = 0L

    private val _questsCache = MutableStateFlow<List<QuestDto>>(emptyList())
    private var questsLastFetchTime = 0L

    private val _eventsScheduleCache = MutableStateFlow<List<EventScheduleDto>>(emptyList())
    private var eventsScheduleLastFetchTime = 0L

    private const val CACHE_TTL = 5 * 60 * 1000L // 5 mins in-memory TTL for Hideout
    private val mutex = Mutex()

    suspend fun getItems(forceRefresh: Boolean = false): Result<List<ItemEntity>> {
        requireNotNull(db) { "DataRepository not initialized" }
        val dao = db!!.itemDao()

        if (!forceRefresh) {
            val localData = dao.getAllItems()
            if (localData.isNotEmpty()) {
                return Result.success(localData)
            }
        }

        return try {
            val freshData = fetchAllItems()
            val entities = freshData.map { dto ->
                ItemEntity(
                    id = dto.id,
                    name = dto.name,
                    imageUrl = dto.imageUrl,
                    price = dto.price,
                    rarity = dto.rarity.ifBlank { "Unknown" },
                    category = dto.category.ifBlank { "Unknown" }
                )
            }
            dao.deleteAll()
            dao.insertAll(entities)
            Result.success(entities)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch items from live API", e)
            val localData = dao.getAllItems()
            if (localData.isNotEmpty()) {
                Log.w(TAG, "Using cached DB items after API failure")
                Result.success(localData)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getArcs(forceRefresh: Boolean = false): Result<List<ArcEntity>> {
        requireNotNull(db) { "DataRepository not initialized" }
        val dao = db!!.arcDao()

        if (!forceRefresh) {
            val localData = dao.getAllArcs()
            if (localData.isNotEmpty()) {
                return Result.success(localData)
            }
        }

        return try {
            val freshData = fetchAllArcs()
            val entities = freshData.map { dto ->
                ArcEntity(
                    id = dto.id,
                    name = dto.name,
                    iconUrl = dto.icon,
                    imageUrl = dto.image,
                    description = dto.description
                )
            }
            dao.deleteAll()
            dao.insertAll(entities)
            Result.success(entities)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch ARCs from live API", e)
            val localData = dao.getAllArcs()
            if (localData.isNotEmpty()) {
                Log.w(TAG, "Using cached DB ARCs after API failure")
                Result.success(localData)
            } else {
                Result.failure(e)
            }
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

    suspend fun getTraders(forceRefresh: Boolean = false): Result<TraderResponseDto> {
        val now = System.currentTimeMillis()

        mutex.withLock {
            val cache = _tradersCache.value
            if (!forceRefresh && cache != null && (now - tradersLastFetchTime) < CACHE_TTL) {
                return Result.success(cache)
            }
        }

        return try {
            val freshData = metaForgeService.getTraders()
            mutex.withLock {
                _tradersCache.value = freshData
                tradersLastFetchTime = System.currentTimeMillis()
            }
            Result.success(freshData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Traders from live API", e)
            val cache = _tradersCache.value
            if (cache != null) {
                Log.w(TAG, "Using cached Traders after API failure")
                Result.success(cache)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getQuests(forceRefresh: Boolean = false): Result<List<QuestDto>> {
        val now = System.currentTimeMillis()

        mutex.withLock {
            if (!forceRefresh && _questsCache.value.isNotEmpty() && (now - questsLastFetchTime) < CACHE_TTL) {
                return Result.success(_questsCache.value)
            }
        }

        return try {
            val freshData = fetchAllQuests()
            mutex.withLock {
                _questsCache.value = freshData
                questsLastFetchTime = System.currentTimeMillis()
            }
            Result.success(freshData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Quests from live API", e)
            if (_questsCache.value.isNotEmpty()) {
                Log.w(TAG, "Using cached Quests after API failure")
                Result.success(_questsCache.value)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getEventsSchedule(forceRefresh: Boolean = false): Result<List<EventScheduleDto>> {
        val now = System.currentTimeMillis()

        mutex.withLock {
            if (!forceRefresh && _eventsScheduleCache.value.isNotEmpty() && (now - eventsScheduleLastFetchTime) < CACHE_TTL) {
                return Result.success(_eventsScheduleCache.value)
            }
        }

        return try {
            val freshData = metaForgeService.getEventsSchedule().data.sortedBy { it.startTime }
            mutex.withLock {
                _eventsScheduleCache.value = freshData
                eventsScheduleLastFetchTime = System.currentTimeMillis()
            }
            Result.success(freshData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Events Schedule from live API", e)
            if (_eventsScheduleCache.value.isNotEmpty()) {
                Log.w(TAG, "Using cached Events Schedule after API failure")
                Result.success(_eventsScheduleCache.value)
            } else {
                Result.failure(e)
            }
        }
    }

    private suspend fun fetchAllQuests(): List<QuestDto> {
        val aggregated = mutableListOf<QuestDto>()
        var page = 1

        while (true) {
            val response = metaForgeService.getQuests(page = page, limit = PAGE_SIZE)
            aggregated += response.data
            if (response.pagination?.hasNextPage != true) break
            page++
        }

        return aggregated
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
