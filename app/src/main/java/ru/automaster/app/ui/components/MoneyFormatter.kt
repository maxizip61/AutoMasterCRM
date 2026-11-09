package ru.automaster.app.ui.components

import java.text.NumberFormat
import java.util.Locale

object MoneyFormatter {
    private val format = NumberFormat.getCurrencyInstance(Locale("ru", "RU"))

    fun format(cents: Long): String {
        return format.format(cents / 100.0)
    }
}