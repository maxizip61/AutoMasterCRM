package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.ExpenseCategoryEntity

@Dao
interface ExpenseCategoryDao {

    @Query("SELECT * FROM expense_categories ORDER BY name ASC")
    fun getAll(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_categories WHERE id = :id")
    suspend fun getById(id: Long): ExpenseCategoryEntity?

    @Query("SELECT COUNT(*) FROM expense_categories")
    suspend fun count(): Int

    @Query("SELECT id FROM expense_categories LIMIT 1")
    suspend fun getFirstCategoryId(): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<ExpenseCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: ExpenseCategoryEntity): Long

    @Update
    suspend fun update(category: ExpenseCategoryEntity)

    @Delete
    suspend fun delete(category: ExpenseCategoryEntity)
}