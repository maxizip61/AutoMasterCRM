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

    /** Нужен для идемпотентного seed: проверяем, пустая ли таблица. */
    @Query("SELECT COUNT(*) FROM posts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(posts: List<PostEntity>)

    // TODO: Спринт 1 — реализовать после миграции 1→2 (добавить displayName и isActive в PostEntity)
    @Query("UPDATE posts SET name = :newName WHERE id = :id")
    suspend fun rename(id: Long, newName: PostName)

    // TODO: Спринт 1 — реализовать после миграции 1→2
    @Query("UPDATE posts SET type = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)
}