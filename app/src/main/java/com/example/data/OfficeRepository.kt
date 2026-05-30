package com.example.data

import kotlinx.coroutines.flow.Flow

class OfficeRepository(
    private val officeDao: OfficeItemDao,
    private val agendaDao: AgendaItemDao
) {
    val allItems: Flow<List<OfficeItem>> = officeDao.getAllItems()

    suspend fun getItemById(id: Int): OfficeItem? {
        return officeDao.getItemById(id)
    }

    suspend fun insertItem(item: OfficeItem): Long {
        return officeDao.insertItem(item)
    }

    suspend fun updateItem(item: OfficeItem) {
        officeDao.updateItem(item)
    }

    suspend fun deleteItem(item: OfficeItem) {
        officeDao.deleteItem(item)
    }

    suspend fun deleteItemById(id: Int) {
        officeDao.deleteItemById(id)
    }

    // Agenda methods
    val allAgendaItems: Flow<List<AgendaItem>> = agendaDao.getAllItems()

    suspend fun insertAgendaItem(item: AgendaItem): Long {
        return agendaDao.insertItem(item)
    }

    suspend fun updateAgendaItem(item: AgendaItem) {
        agendaDao.updateItem(item)
    }

    suspend fun deleteAgendaItem(item: AgendaItem) {
        agendaDao.deleteItem(item)
    }

    suspend fun setAgendaItemCompleted(id: Int, completed: Boolean) {
        agendaDao.setCompleted(id, completed)
    }
}
