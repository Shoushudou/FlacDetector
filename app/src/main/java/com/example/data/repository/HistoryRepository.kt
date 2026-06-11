package com.example.data.repository

import com.example.data.db.ScanDao
import com.example.data.db.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val scanDao: ScanDao) {

    val allHistory: Flow<List<ScanHistoryEntity>> = scanDao.getAllHistory()

    suspend fun insertScan(scan: ScanHistoryEntity): Long {
        return scanDao.insertScan(scan)
    }

    suspend fun deleteScanById(id: Long) {
        scanDao.deleteScanById(id)
    }

    suspend fun clearHistory() {
        scanDao.clearHistory()
    }
}
