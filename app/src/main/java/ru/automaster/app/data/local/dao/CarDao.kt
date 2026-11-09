package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.CarEntity

@Dao
interface CarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(car: CarEntity): Long

    @Update
    suspend fun update(car: CarEntity)

    @Query("SELECT * FROM cars WHERE clientId = :clientId")
    fun getByClient(clientId: Long): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars WHERE id = :id")
    suspend fun getById(id: Long): CarEntity?

    @Query("SELECT * FROM cars WHERE vin = :vin LIMIT 1")
    suspend fun getByVin(vin: String): CarEntity?

    @Query("SELECT * FROM cars WHERE vin LIKE '%' || :query || '%' OR licensePlateSearchIndex LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<CarEntity>>
}