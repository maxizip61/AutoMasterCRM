package ru.automaster.app.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Orders : Screen("orders")
    object Finance : Screen("finance")
    object Taxes : Screen("taxes") // <-- ДОБАВЛЕНО
    object Payroll : Screen("payroll")
    object Settings : Screen("settings")
    object AddOrder : Screen("add_order")
    object Backup : Screen("backup")

    object OrderDetails : Screen("order_details/{orderId}") {
        fun createRoute(orderId: Long) = "order_details/$orderId"
    }
}