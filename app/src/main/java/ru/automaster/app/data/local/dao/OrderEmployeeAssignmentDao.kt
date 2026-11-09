package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.OrderEmployeeAssignmentEntity

@Dao
interface OrderEmployeeAssignmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assignment: OrderEmployeeAssignmentEntity): Long

    @Delete
    suspend fun delete(assignment: OrderEmployeeAssignmentEntity)

    @Query("SELECT * FROM order_employee_assignments WHERE orderId = :orderId")
    fun getByOrder(orderId: Long): Flow<List<OrderEmployeeAssignmentEntity>>
}