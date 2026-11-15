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
        // Добавляем role в таблицу назначений
        db.execSQL("ALTER TABLE order_employee_assignments ADD COLUMN role TEXT NOT NULL DEFAULT 'GENERAL'")
        // Добавляем role в таблицу начислений
        db.execSQL("ALTER TABLE order_employee_accruals ADD COLUMN role TEXT NOT NULL DEFAULT 'GENERAL'")
    }
}