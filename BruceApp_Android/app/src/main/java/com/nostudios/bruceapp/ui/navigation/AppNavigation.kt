package com.nostudios.bruceapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nostudios.bruceapp.store.StoreViewModel
import com.nostudios.bruceapp.ui.screens.*
import com.nostudios.bruceapp.ui.theme.Background
import com.nostudios.bruceapp.ui.theme.White
import com.nostudios.bruceapp.viewmodel.BruceViewModel
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard)
    data object Files : Screen("files", "Files", Icons.Filled.Folder)
    data object AppStore : Screen("appstore", "App Store", Icons.Filled.Apps)
    data object DeviceDetail : Screen("device_detail/{deviceId}", "Device Detail", Icons.Filled.Dashboard)
    data object AddDevice : Screen("add_device", "Add Device", Icons.Filled.Dashboard)
    data object Terminal : Screen("terminal", "Terminal", Icons.Filled.Dashboard)
    data object RemoteControl : Screen("remote_control", "Remote Control", Icons.Filled.Dashboard)
    data object QuickActions : Screen("quick_actions", "Quick Actions", Icons.Filled.Dashboard)
}

private val bottomNavItems = listOf(Screen.Dashboard, Screen.Files, Screen.AppStore)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    viewModel: BruceViewModel,
    storeViewModel: StoreViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Background,
                    contentColor = White
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = {
                                Text(
                                    screen.title,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = White,
                                selectedTextColor = White,
                                unselectedIconColor = White.copy(alpha = 0.5f),
                                unselectedTextColor = White.copy(alpha = 0.5f),
                                indicatorColor = White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                MyDevicesScreen(
                    viewModel = viewModel,
                    onDeviceClick = { device ->
                        navController.navigate("device_detail/${device.id}")
                    },
                    onAddDeviceClick = {
                        navController.navigate(Screen.AddDevice.route)
                    }
                )
            }

            composable(Screen.Files.route) {
                val devices by viewModel.savedDevices.collectAsState()
                FilesScreen(
                    viewModel = viewModel,
                    savedDevices = devices
                )
            }

            composable(Screen.AppStore.route) {
                AppStoreScreen(
                    viewModel = viewModel,
                    storeViewModel = storeViewModel
                )
            }

            composable(
                route = "device_detail/{deviceId}",
                arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
            ) { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
                val devices by viewModel.savedDevices.collectAsState()
                val device = devices.find { it.id == deviceId }
                if (device != null) {
                    DeviceDetailScreen(
                        device = device,
                        viewModel = viewModel,
                        onNavigateToTerminal = { navController.navigate(Screen.Terminal.route) },
                        onNavigateToRemoteControl = { navController.navigate(Screen.RemoteControl.route) },
                        onNavigateToQuickActions = { navController.navigate(Screen.QuickActions.route) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.AddDevice.route) {
                AddDeviceScreen(
                    viewModel = viewModel,
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Screen.Terminal.route) {
                TerminalScreen(viewModel = viewModel)
            }

            composable(Screen.RemoteControl.route) {
                RemoteControlScreen(viewModel = viewModel)
            }

            composable(Screen.QuickActions.route) {
                QuickActionsScreen(viewModel = viewModel)
            }
        }
    }
}
