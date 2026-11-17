package ru.automaster.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.automaster.app.data.local.entity.PostEntity
import ru.automaster.app.domain.model.PostName

@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY sortOrder ASC, id ASC")
    fun getAll(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE isActive = 1 ORDER BY sortOrder ASC, id ASC")
    fun observeActive(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE name = :name LIMIT 1")
    suspend fun getByName(name: PostName): PostEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Query("UPDATE posts SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)

    @Query("UPDATE posts SET displayName = :displayName WHERE id = :id")
    suspend fun rename(id: Long, displayName: String)

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun count(): Int
}