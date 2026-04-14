package com.arkcompanion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    suspend fun getAllItems(): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Query("DELETE FROM items")
    suspend fun deleteAll()
}

@Dao
interface ArcDao {
    @Query("SELECT * FROM arcs")
    suspend fun getAllArcs(): List<ArcEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(arcs: List<ArcEntity>)

    @Query("DELETE FROM arcs")
    suspend fun deleteAll()
}
