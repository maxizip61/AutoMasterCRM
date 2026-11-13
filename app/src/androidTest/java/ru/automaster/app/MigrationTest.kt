package ru.automaster.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.automaster.app.data.local.AppDatabase
import ru.automaster.app.data.local.MIGRATION_1_2
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java, // <-- ИСПРАВЛЕНО
        emptyList(),
        androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO posts (name, type) VALUES ('PREP_1', 'PREP')")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).apply {
            val cursor = query("SELECT displayName, isActive, sortOrder FROM posts")
            cursor.moveToFirst()
            assert(cursor.getString(0) == "PREP_1")
            assert(cursor.getInt(1) == 1)
            assert(cursor.getInt(2) == 0)
            close()
        }
    }
}