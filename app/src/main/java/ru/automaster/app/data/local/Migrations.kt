package ru.automaster.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Обновляем таблицу posts
        db.execSQL("ALTER TABLE posts ADD COLUMN displayName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE posts ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE posts ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")

        // Заполняем displayName из name для существующих постов
        db.execSQL("UPDATE posts SET displayName = name WHERE displayName = ''")

        // 2. Обновляем таблицу file_deletion_tasks
        db.execSQL("ALTER TABLE file_deletion_tasks ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE file_deletion_tasks ADD COLUMN lastError TEXT")
    }
}