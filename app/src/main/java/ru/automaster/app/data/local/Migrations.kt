package ru.automaster.app.data.local

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val TAG = "Migrations"

/** Есть ли колонка в таблице. */
private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean {
    query("PRAGMA table_info($table)").use { cursor ->
        val nameIdx = cursor.getColumnIndex("name")
        if (nameIdx < 0) return false
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIdx) == column) return true
        }
    }
    return false
}

/** Есть ли таблица. */
private fun SupportSQLiteDatabase.hasTable(table: String): Boolean {
    query(
        "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
        arrayOf(table)
    ).use { cursor -> return cursor.moveToFirst() }
}

/**
 * Добавляет колонку ТОЛЬКО если её ещё нет.
 * Защита от "duplicate column name" при обновлении поверх
 * частично мигрированной базы.
 */
fun SupportSQLiteDatabase.addColumnIfMissing(
    table: String,
    column: String,
    definition: String
) {
    if (!hasTable(table)) {
        Log.w(TAG, "Таблицы $table нет — пропуск $column")
        return
    }
    if (hasColumn(table, column)) {
        Log.i(TAG, "$table.$column уже существует — пропуск")
        return
    }
    execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
    Log.i(TAG, "Добавлена колонка $table.$column")
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.i(TAG, "Миграция 1 → 2")
        db.addColumnIfMissing("posts", "displayName", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("posts", "isActive", "INTEGER NOT NULL DEFAULT 1")
        db.addColumnIfMissing("posts", "sortOrder", "INTEGER NOT NULL DEFAULT 0")

        if (db.hasTable("posts") && db.hasColumn("posts", "displayName")) {
            db.execSQL("UPDATE posts SET displayName = name WHERE displayName = ''")
        }

        db.addColumnIfMissing("file_deletion_tasks", "attempts", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("file_deletion_tasks", "lastError", "TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.i(TAG, "Миграция 2 → 3")
        db.addColumnIfMissing(
            "order_employee_assignments", "role", "TEXT NOT NULL DEFAULT 'GENERAL'"
        )
        db.addColumnIfMissing(
            "order_employee_accruals", "role", "TEXT NOT NULL DEFAULT 'GENERAL'"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.i(TAG, "Миграция 3 → 4")
        db.execSQL("DROP INDEX IF EXISTS index_order_employee_assignments_orderId")
        db.execSQL("DROP INDEX IF EXISTS index_order_employee_assignments_employeeId")

        // Убираем дубли ДО создания уникального индекса,
        // иначе CREATE UNIQUE INDEX упадёт на старых данных.
        if (db.hasTable("order_employee_assignments")) {
            db.execSQL(
                """
                DELETE FROM order_employee_assignments
                WHERE id NOT IN (
                    SELECT MIN(id) FROM order_employee_assignments
                    GROUP BY orderId, employeeId, role
                )
                """.trimIndent()
            )
            Log.i(TAG, "Дубли назначений удалены")
        }

        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
            index_order_employee_assignments_orderId_employeeId_role
            ON order_employee_assignments(orderId, employeeId, role)
            """.trimIndent()
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.i(TAG, "Миграция 4 → 5")
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
            "CREATE INDEX IF NOT EXISTS `index_order_works_orderId` ON `order_works` (`orderId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_order_works_assignedEmployeeId` " +
                    "ON `order_works` (`assignedEmployeeId`)"
        )
    }
}
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.i(TAG, "Миграция 5 → 6: таблица ocr_corrections")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ocr_corrections` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `field` TEXT NOT NULL,
                `rawToken` TEXT NOT NULL,
                `correctedToken` TEXT NOT NULL,
                `occurrences` INTEGER NOT NULL DEFAULT 1,
                `confirmations` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL,
                `lastUsedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_ocr_corrections_field_rawToken` " +
                    "ON `ocr_corrections` (`field`, `rawToken`)"
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.i(TAG, "Миграция 6 → 7: оценка по фото")
        db.addColumnIfMissing("order_works", "panel", "TEXT")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `damage_assessments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `carMake` TEXT NOT NULL, `carModel` TEXT NOT NULL, `carClass` TEXT NOT NULL,
                `source` TEXT NOT NULL, `status` TEXT NOT NULL, `orderId` INTEGER,
                `totalMinCents` INTEGER NOT NULL, `totalMaxCents` INTEGER NOT NULL,
                `photoPaths` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`orderId`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_damage_assessments_orderId` ON `damage_assessments` (`orderId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `damage_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `assessmentId` INTEGER NOT NULL, `panel` TEXT NOT NULL, `damageType` TEXT NOT NULL,
                `laborMinCents` INTEGER NOT NULL, `laborMaxCents` INTEGER NOT NULL,
                `materialsCents` INTEGER NOT NULL, `totalMinCents` INTEGER NOT NULL,
                `totalMaxCents` INTEGER NOT NULL, `actualCents` INTEGER, `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`assessmentId`) REFERENCES `damage_assessments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_damage_items_assessmentId` ON `damage_items` (`assessmentId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `panel_price_book` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `panel` TEXT NOT NULL, `damageType` TEXT NOT NULL, `workType` TEXT NOT NULL,
                `baseLaborCents` INTEGER NOT NULL, `samplesCount` INTEGER NOT NULL,
                `source` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_panel_price_book_panel_damageType_workType` " +
                    "ON `panel_price_book` (`panel`, `damageType`, `workType`)"
        )
    }
}