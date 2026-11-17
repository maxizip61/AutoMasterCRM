package ru.automaster.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Добавляет колонку только если её ещё нет.
 * Room в некоторых сценариях (откат APK, повторная установка поверх
 * уже мигрированной БД) вызывает миграцию заново, что даёт
 * SQLiteException "duplicate column name". Хелпер предотвращает ошибку.
 */
private fun SupportSQLiteDatabase.addColumnIfMissing(
    table: String,
    column: String,
    definition: String
) {
    val cursor = query("PRAGMA table_info(`$table`)")
    var exists = false
    try {
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == column) {
                exists = true
                break
            }
        }
    } finally {
        cursor.close()
    }
    if (!exists) {
        execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("posts", "displayName", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("posts", "isActive", "INTEGER NOT NULL DEFAULT 1")
        db.addColumnIfMissing("posts", "sortOrder", "INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE posts SET displayName = name WHERE displayName = ''")

        db.addColumnIfMissing("file_deletion_tasks", "attempts", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("file_deletion_tasks", "lastError", "TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing(
            "order_employee_assignments",
            "role",
            "TEXT NOT NULL DEFAULT 'GENERAL'"
        )
        db.addColumnIfMissing(
            "order_employee_accruals",
            "role",
            "TEXT NOT NULL DEFAULT 'GENERAL'"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Удаляем дубли по (orderId, employeeId, role), оставляя MIN(id).
        //    Без этого CREATE UNIQUE INDEX упадёт на старых данных.
        db.execSQL(
            """
            DELETE FROM order_employee_assignments
            WHERE id NOT IN (
                SELECT MIN(id) FROM order_employee_assignments
                GROUP BY orderId, employeeId, role
            )
            """.trimIndent()
        )

        // 2. Безопасно дропаем старые индексы (могли быть в разных версиях схемы).
        db.execSQL("DROP INDEX IF EXISTS index_order_employee_assignments_orderId")
        db.execSQL("DROP INDEX IF EXISTS index_order_employee_assignments_employeeId")
        db.execSQL("DROP INDEX IF EXISTS index_order_employee_assignments_orderId_employeeId")

        // 3. Создаём новый уникальный индекс и восстанавливаем одиночный по employeeId
        //    (предыдущая версия миграции его удаляла, но не пересоздавала — Room падал
        //    на проверке схемы «expected index employeeId, found none»).
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "index_order_employee_assignments_orderId_employeeId_role " +
                    "ON order_employee_assignments(orderId, employeeId, role)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_order_employee_assignments_employeeId " +
                    "ON order_employee_assignments(employeeId)"
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `order_works` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `orderId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `costCents` INTEGER NOT NULL,
                `assignedEmployeeId` INTEGER,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`orderId`) REFERENCES `orders`(`id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`assignedEmployeeId`) REFERENCES `employees`(`id`) 
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_order_works_orderId` " +
                    "ON `order_works` (`orderId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_order_works_assignedEmployeeId` " +
                    "ON `order_works` (`assignedEmployeeId`)"
        )
    }
}