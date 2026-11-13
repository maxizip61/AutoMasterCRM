package ru.automaster.app.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Orders : Screen("orders")
    object Finance : Screen("finance")
    object Tax : Screen("tax")
    object Payroll : Screen("payroll")
    object Settings : Screen("settings")
    object Backup : Screen("backup") // <-- ДОБАВЛЕНО
    object AddOrder : Screen("add_order")

    object OrderDetails : Screen("order_details/{orderId}") {
        fun createRoute(orderId: Long) = "order_details/$orderId"
    }
}