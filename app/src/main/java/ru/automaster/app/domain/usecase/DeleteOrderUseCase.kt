package ru.automaster.app.domain.usecase

import androidx.room.withTransaction
import ru.automaster.app.data.local.AppDatabase
import ru.automaster.app.data.local.entity.FileDeletionTaskEntity
import java.time.Instant
import javax.inject.Inject

class DeleteOrderUseCase @Inject constructor(
    private val db: AppDatabase
) {
    suspend operator fun invoke(orderId: Long) {
        db.withTransaction {
            // 1. Получаем пути всех фото этого заказа
            val photos = db.photoDao().getPhotosSync(orderId)

            // 2. Создаем задачи на удаление
            val tasks = photos.map {
                FileDeletionTaskEntity(
                    filePath = it.filePath,
                    createdAt = Instant.now()
                )
            }
            db.fileDeletionTaskDao().insertAll(tasks)

            // 3. Удаляем сам заказ из БД (каскадно удалятся этапы, платежи и записи о фото)
            db.orderDao().deleteOrderById(orderId)
        }
    }
}