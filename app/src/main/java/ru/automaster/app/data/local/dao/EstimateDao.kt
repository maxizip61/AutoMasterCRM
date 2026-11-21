package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.DamageAssessmentEntity
import ru.automaster.app.data.local.entity.DamageItemEntity
import ru.automaster.app.data.local.entity.PanelPriceBookEntity

@Dao
interface EstimateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(a: DamageAssessmentEntity): Long

    @Update
    suspend fun updateAssessment(a: DamageAssessmentEntity)

    @Query("SELECT * FROM damage_assessments WHERE id = :id")
    suspend fun getAssessment(id: Long): DamageAssessmentEntity?

    @Query("SELECT * FROM damage_assessments ORDER BY createdAt DESC")
    fun getAllAssessments(): Flow<List<DamageAssessmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(i: DamageItemEntity): Long

    @Update
    suspend fun updateItem(i: DamageItemEntity)

    @Query("SELECT * FROM damage_items WHERE assessmentId = :id")
    fun getItems(id: Long): Flow<List<DamageItemEntity>>

    @Query("DELETE FROM damage_items WHERE assessmentId = :id")
    suspend fun clearItems(id: Long)

    @Query("SELECT * FROM panel_price_book WHERE panel = :p AND damageType = :d AND workType = :w LIMIT 1")
    suspend fun findPrice(p: String, d: String, w: String): PanelPriceBookEntity?

    @Query("SELECT * FROM panel_price_book WHERE panel = :p AND damageType = :d")
    suspend fun pricesFor(p: String, d: String): List<PanelPriceBookEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPrice(p: PanelPriceBookEntity): Long

    @Update
    suspend fun updatePrice(p: PanelPriceBookEntity)
}