package ru.automaster.app.ui.util

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.util.Log

/**
 * Переводит исключение БД в понятное пользователю сообщение.
 * Оригинал логируется для отладки.
 *
 * Без этого любое исключение из DAO (особенно FK при ABORT) улетает
 * в viewModelScope необработанным и убивает процесс.
 */
fun Throwable.toUserMessage(tag: String = "DbError"): String {
    Log.e(tag, "Ошибка операции с БД", this)
    return when (this) {
        is SQLiteConstraintException -> when {
            message?.contains("FOREIGN KEY", ignoreCase = true) == true ->
                "Не удалось сохранить: не выбрана категория или заказ. Проверьте поля."
            message?.contains("UNIQUE", ignoreCase = true) == true ->
                "Такая запись уже существует."
            else -> "Данные нарушают правила целостности. Проверьте форму."
        }
        is SQLiteException -> "Ошибка базы данных. Попробуйте ещё раз."
        else -> "Не удалось сохранить. Попробуйте ещё раз."
    }
}