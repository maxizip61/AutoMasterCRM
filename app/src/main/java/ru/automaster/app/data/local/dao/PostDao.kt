package ru.automaster.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.PostEntity
import ru.automaster.app.domain.model.PostName

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY id ASC")
    fun getAll(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE name = :name LIMIT 1")
    suspend fun getByName(name: PostName): PostEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(posts: List<PostEntity>)
}