package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AgendaItemDao {
    @Query("SELECT * FROM agenda_items ORDER BY dateTime ASC")
    fun getAllItems(): Flow<List<AgendaItem>>

    @Query("SELECT * FROM agenda_items WHERE id = :id")
    suspend fun getItemById(id: Int): AgendaItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: AgendaItem): Long

    @Update
    suspend fun updateItem(item: AgendaItem)

    @Delete
    suspend fun deleteItem(item: AgendaItem)

    @Query("DELETE FROM agenda_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Query("UPDATE agenda_items SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Int, completed: Boolean)
}
