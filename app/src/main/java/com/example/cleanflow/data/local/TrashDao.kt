package com.example.cleanflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    
    @Query("SELECT * FROM trashed_files ORDER BY trashedAt DESC")
    fun getAllTrashed(): Flow<List<TrashedFileEntity>>
    
    @Query("SELECT mediaId FROM trashed_files")
    fun getTrashedIds(): Flow<List<Long>>
    
    @Query("SELECT COUNT(*) FROM trashed_files")
    fun getTrashCount(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToTrash(file: TrashedFileEntity)
    
    @Query("DELETE FROM trashed_files WHERE mediaId = :id")
    suspend fun removeFromTrash(id: Long)
    
    @Query("DELETE FROM trashed_files")
    suspend fun clearTrash()
    
    @Query("SELECT * FROM trashed_files WHERE mediaId = :id")
    suspend fun getTrashedFile(id: Long): TrashedFileEntity?
}
