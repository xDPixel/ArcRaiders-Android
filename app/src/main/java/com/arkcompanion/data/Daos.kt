package com.arkcompanion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)
}

@Dao
interface ArcDao {
    @Query("SELECT * FROM arcs")
    fun getAllArcs(): Flow<List<ArcEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArcs(arcs: List<ArcEntity>)
}

@Dao
interface HideoutDao {
    @Query("SELECT * FROM hideout_tables")
    fun getAllTables(): Flow<List<HideoutTableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTables(tables: List<HideoutTableEntity>)

    @Query("UPDATE hideout_tables SET currentLevel = :newLevel WHERE id = :id")
    suspend fun updateTableLevel(id: String, newLevel: Int)
}
