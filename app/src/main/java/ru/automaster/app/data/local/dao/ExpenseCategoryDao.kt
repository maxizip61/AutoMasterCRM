package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.ExpenseCategoryEntity

@Dao
interface ExpenseCategoryDao {
    @Query("SELECT * FROM expense_categories ORDER BY name ASC")
    fun getAll(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT id FROM expense_categories LIMIT 1")
    suspend fun getFirstCategoryId(): Long?

    /** Нужен для идемпотентного seed: проверяем, пустая ли таблица. */
    @Query("SELECT COUNT(*) FROM expense_categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<ExpenseCategoryEntity>)
}