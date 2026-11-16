package ru.automaster.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE posts ADD COLUMN displayName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE posts ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE posts ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE posts SET displayName = name WHERE displayName = ''")
        db.execSQL("ALTER TABLE file_deletion_tasks ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE file_deletion_tasks ADD COLUMN lastError TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE order_employee_assignments ADD COLUMN role TEXT NOT NULL DEFAULT 'GENERAL'")
        db.execSQL("ALTER TABLE order_employee_accruals ADD COLUMN role TEXT NOT NULL DEFAULT 'GENERAL'")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS index_order_employee_assignments_orderId")
        db.execSQL("DROP INDEX IF EXISTS index_order_employee_assignments_employeeId")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_order_employee_assignments_orderId_employeeId_role ON order_employee_assignments(orderId, employeeId, role)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `order_works` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `orderId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `costCents` INTEGER NOT NULL,
                `assignedEmployeeId` INTEGER,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`orderId`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`assignedEmployeeId`) REFERENCES `employees`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_works_orderId` ON `order_works` (`orderId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_works_assignedEmployeeId` ON `order_works` (`assignedEmployeeId`)")
    }
}