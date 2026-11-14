package ru.automaster.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ru.automaster.app.ui.screens.dashboard.DashboardScreen
import ru.automaster.app.ui.screens.finance.FinanceScreen
import ru.automaster.app.ui.screens.order_detail.OrderDetailsScreen
import ru.automaster.app.ui.screens.orders.AddOrderScreen
import ru.automaster.app.ui.screens.orders.OrdersScreen
import ru.automaster.app.ui.screens.payroll.PayrollScreen
import ru.automaster.app.ui.screens.settings.BackupScreen
import ru.automaster.app.ui.screens.settings.SettingsScreen
import ru.automaster.app.ui.screens.taxes.TaxScreen
import ru.automaster.app.ui.theme.Motion

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it / 4 }, animationSpec = tween(Motion.Medium)) + fadeIn(animationSpec = tween(Motion.Medium))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(Motion.Medium)) + fadeOut(animationSpec = tween(Motion.Medium))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(Motion.Medium)) + fadeIn(animationSpec = tween(Motion.Medium))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it / 4 }, animationSpec = tween(Motion.Medium)) + fadeOut(animationSpec = tween(Motion.Medium))
        }
    ) {
        // 1. Главный экран
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAddOrder = { navController.navigate(Screen.AddOrder.route) },
                onNavigateToOrderDetails = { orderId ->
                    navController.navigate(Screen.OrderDetails.createRoute(orderId))
                }
            )
        }

        // 2. Экран всех заказов
        composable(Screen.Orders.route) {
            OrdersScreen(
                onNavigateToOrderDetails = { orderId ->
                    navController.navigate(Screen.OrderDetails.createRoute(orderId))
                },
                onNavigateToAddOrder = { navController.navigate(Screen.AddOrder.route) }
            )
        }

        // 3. Финансы
        composable(Screen.Finance.route) {
            FinanceScreen()
        }

        // 4. Налоги
        composable(Screen.Taxes.route) {
            TaxScreen()
        }

        // 5. Зарплата
        composable(Screen.Payroll.route) {
            PayrollScreen()
        }

        // 6. Настройки
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToBackup = { navController.navigate(Screen.Backup.route) }
            )
        }

        // 7. Бэкапы
        composable(Screen.Backup.route) {
            BackupScreen(onNavigateBack = { navController.popBackStack() })
        }

        // 8. Создание заказа
        composable(Screen.AddOrder.route) {
            AddOrderScreen(onNavigateBack = { navController.popBackStack() })
        }

        // 9. Детали заказа
        composable(
            route = Screen.OrderDetails.route,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) {
            OrderDetailsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}