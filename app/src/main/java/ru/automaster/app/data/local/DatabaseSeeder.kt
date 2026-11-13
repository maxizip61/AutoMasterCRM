package ru.automaster.app.data.local

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.automaster.app.data.local.entity.ExpenseCategoryEntity
import ru.automaster.app.data.local.entity.PostEntity
import ru.automaster.app.domain.model.PostName
import ru.automaster.app.domain.model.PostType

/**
 * Идемпотентное заполнение справочников при каждом старте приложения.
 *
 * Room Callback.onCreate срабатывает ТОЛЬКО при первом создании файла БД.
 * Если база уже существовала (например, создана до появления seed-логики),
 * справочники остаются пустыми навсегда. Это вызывало:
 * - пустые посты на дашборде
 * - FK constraint failed при вставке расхода (categoryId ссылается на пустую таблицу)
 *
 * Этот seeder вызывается при каждом старте и досоздаёт только то, чего нет.
 * Существующие данные не трогает.
 */
class DatabaseSeeder(private val db: AppDatabase) {

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        runCatching {
            seedPosts()
            seedExpenseCategories()
        }.onFailure { e ->
            Log.e(TAG, "Ошибка заполнения справочников", e)
        }
    }

    private suspend fun seedPosts() {
        val existing = db.postDao().count()
        if (existing > 0) {
            Log.d(TAG, "Посты уже есть: $existing — пропускаем")
            return
        }
        db.postDao().insertAll(
            listOf(
                PostEntity(name = PostName.PREP_1, type = PostType.PREP),
                PostEntity(name = PostName.PREP_2, type = PostType.PREP),
                PostEntity(name = PostName.PAINT_1, type = PostType.PAINT)
            )
        )
        Log.i(TAG, "Создано 3 поста")
    }

    private suspend fun seedExpenseCategories() {
        val existing = db.expenseCategoryDao().count()
        if (existing > 0) {
            Log.d(TAG, "Категории уже есть: $existing — пропускаем")
            return
        }
        db.expenseCategoryDao().insertAll(
            listOf(
                ExpenseCategoryEntity(name = "Аренда"),
                ExpenseCategoryEntity(name = "Коммунальные"),
                ExpenseCategoryEntity(name = "Налоги"),
                ExpenseCategoryEntity(name = "Оборудование"),
                ExpenseCategoryEntity(name = "Инструменты"),
                ExpenseCategoryEntity(name = "Расходники"),
                ExpenseCategoryEntity(name = "Прочее")
            )
        )
        Log.i(TAG, "Создано 7 категорий расходов")
    }

    private companion object {
        const val TAG = "DatabaseSeeder"
    }
}