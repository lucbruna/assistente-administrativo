package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficeItemDao {
    @Query("SELECT * FROM office_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<OfficeItem>>

    @Query("SELECT * FROM office_items WHERE id = :id")
    suspend fun getItemById(id: Int): OfficeItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: OfficeItem): Long

    @Update
    suspend fun updateItem(item: OfficeItem)

    @Delete
    suspend fun deleteItem(item: OfficeItem)

    @Query("DELETE FROM office_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)
}
