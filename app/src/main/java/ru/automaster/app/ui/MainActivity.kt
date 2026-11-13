package ru.automaster.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.automaster.app.ui.navigation.AppNavHost
import ru.automaster.app.ui.navigation.Screen
import ru.automaster.app.ui.security.AppLockGate
import ru.automaster.app.ui.security.AppLockViewModel
import ru.automaster.app.ui.theme.AutoMasterTheme
import ru.automaster.app.ui.theme.IndigoPrimary

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoMasterTheme {
                MainScreen()
            }
        }
    }
}

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    lockViewModel: AppLockViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isLocked by lockViewModel.isLocked.collectAsState()
    val useBiometric by lockViewModel.useBiometric.collectAsState()

    // Слушатель жизненного цикла для автоблокировки
    val lifecycleOwner = ProcessLifecycleOwner.get()
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                lockViewModel.onAppBackgrounded()
            }
            override fun onStart(owner: LifecycleOwner) {
                lockViewModel.onAppForegrounded(60) // 60 секунд таймаут
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AppLockGate(
        isLocked = isLocked,
        useBiometric = useBiometric,
        onUnlock = { lockViewModel.unlock() },
        onVerifyPin = { pin -> lockViewModel.verifyPin(pin) }
    ) {
        val showBottomBar = currentRoute != Screen.AddOrder.route &&
                currentRoute != Screen.OrderDetails.route &&
                currentRoute != Screen.Backup.route

        val bottomNavItems = listOf(
            BottomNavItem(Screen.Dashboard, Icons.Default.Home, "Главная"),
            BottomNavItem(Screen.Orders, Icons.AutoMirrored.Filled.List, "Заказы"),
            BottomNavItem(Screen.Finance, Icons.Default.ShoppingCart, "Финансы"),
            BottomNavItem(Screen.Payroll, Icons.Default.Person, "Зарплата")
        )

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.95f),
                        contentColor = Color.White
                    ) {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                selected = currentRoute == item.screen.route,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = IndigoPrimary,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                ),
                                onClick = {
                                    if (currentRoute != item.screen.route) {
                                        navController.navigate(item.screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}