package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.PhotoEntity

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(photo: PhotoEntity): Long

    @Delete
    suspend fun delete(photo: PhotoEntity)

    @Query("SELECT * FROM photos WHERE orderId = :orderId")
    fun getByOrder(orderId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT filePath FROM photos")
    suspend fun getAllFilePaths(): List<String>

    @Query("SELECT * FROM photos WHERE orderId = :orderId")
    suspend fun getPhotosSync(orderId: Long): List<PhotoEntity>

}