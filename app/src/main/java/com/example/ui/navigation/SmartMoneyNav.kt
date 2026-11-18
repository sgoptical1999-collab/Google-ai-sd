package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.TransactionEntity
import com.example.di.SmartMoneyViewModelFactory
import com.example.ui.screens.ContactDetailScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PendingAmber
import com.example.ui.viewmodel.ContactsViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.ReportsViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.TransactionsViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions?pendingOnly={pendingOnly}"
    const val CONTACTS = "contacts"
    const val CONTACT_DETAIL = "contact_detail/{contactId}"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"

    fun transactions(pendingOnly: Boolean = false) = "transactions?pendingOnly=$pendingOnly"
    fun contactDetail(contactId: Long) = "contact_detail/$contactId"
}

private data class TopNavItem(
    val routePattern: String,
    val targetRoute: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TOP_NAV_ITEMS = listOf(
    TopNavItem(Routes.DASHBOARD, Routes.DASHBOARD, "Home", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
    TopNavItem(Routes.TRANSACTIONS, Routes.transactions(false), "Ledger", Icons.AutoMirrored.Rounded.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, "nav_transactions"),
    TopNavItem(Routes.CONTACTS, Routes.CONTACTS, "Contacts", Icons.Rounded.People, Icons.Outlined.People, "nav_contacts"),
    TopNavItem(Routes.REPORTS, Routes.REPORTS, "Reports", Icons.Rounded.Assessment, Icons.Outlined.Assessment, "nav_reports"),
    TopNavItem(Routes.SETTINGS, Routes.SETTINGS, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun SmartMoneyAppShell(
    factory: SmartMoneyViewModelFactory
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val dashboardVm: DashboardViewModel = viewModel(factory = factory)
    val transactionsVm: TransactionsViewModel = viewModel(factory = factory)
    val contactsVm: ContactsViewModel = viewModel(factory = factory)
    val reportsVm: ReportsViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)

    val dashboardState by dashboardVm.uiState.collectAsStateWithLifecycle()
    val pendingCount = dashboardState.pendingCount

    var externalEditTxn by remember { mutableStateOf<TransactionEntity?>(null) }

    val showTopLevelNav = currentRoute != Routes.CONTACT_DETAIL

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        val navigateTopLevel: (String) -> Unit = { target ->
            navController.navigate(target) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showTopLevelNav && !useRail) {
                    NavigationBar(
                        containerColor = NavyPrimary,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        TOP_NAV_ITEMS.forEach { item ->
                            val selected = currentRoute == item.routePattern ||
                                (item.routePattern == Routes.TRANSACTIONS && currentRoute?.startsWith("transactions") == true)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.targetRoute) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    NavIconWithBadge(
                                        item = item,
                                        selected = selected,
                                        pendingCount = pendingCount
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = EmeraldContainer,
                                    unselectedIconColor = MutedSlate,
                                    unselectedTextColor = MutedSlate
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useRail) {
                    NavigationRail(containerColor = NavyPrimary) {
                        TOP_NAV_ITEMS.forEach { item ->
                            val selected = currentRoute == item.routePattern ||
                                (item.routePattern == Routes.TRANSACTIONS && currentRoute?.startsWith("transactions") == true)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.targetRoute) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    NavIconWithBadge(
                                        item = item,
                                        selected = selected,
                                        pendingCount = pendingCount
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = EmeraldContainer,
                                    unselectedIconColor = MutedSlate,
                                    unselectedTextColor = MutedSlate
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.DASHBOARD
                    ) {
                        composable(Routes.DASHBOARD) {
                            DashboardScreen(
                                viewModel = dashboardVm,
                                onOpenTransactions = { pendingOnly ->
                                    navController.navigate(Routes.transactions(pendingOnly)) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                    }
                                },
                                onOpenSettings = {
                                    navigateTopLevel(Routes.SETTINGS)
                                },
                                onEditTransaction = { txn ->
                                    externalEditTxn = txn
                                    navController.navigate(Routes.transactions(false)) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(
                            route = Routes.TRANSACTIONS,
                            arguments = listOf(
                                navArgument("pendingOnly") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { entry ->
                            val pendingOnly = entry.arguments?.getBoolean("pendingOnly") ?: false
                            TransactionsScreen(
                                viewModel = transactionsVm,
                                initialPendingOnly = pendingOnly,
                                externalEditTransaction = externalEditTxn,
                                onConsumeExternalEdit = { externalEditTxn = null }
                            )
                        }

                        composable(Routes.CONTACTS) {
                            ContactsScreen(
                                viewModel = contactsVm,
                                onOpenContactDetail = { id ->
                                    navController.navigate(Routes.contactDetail(id))
                                }
                            )
                        }

                        composable(
                            route = Routes.CONTACT_DETAIL,
                            arguments = listOf(
                                navArgument("contactId") { type = NavType.LongType }
                            )
                        ) { entry ->
                            val contactId = entry.arguments?.getLong("contactId") ?: 0L
                            ContactDetailScreen(
                                contactId = contactId,
                                viewModel = contactsVm,
                                onBack = { navController.popBackStack() },
                                onSelectTransaction = { txn ->
                                    externalEditTxn = txn
                                    navController.navigate(Routes.transactions(false))
                                }
                            )
                        }

                        composable(Routes.REPORTS) {
                            ReportsScreen(viewModel = reportsVm)
                        }

                        composable(Routes.SETTINGS) {
                            SettingsScreen(viewModel = settingsVm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavIconWithBadge(
    item: TopNavItem,
    selected: Boolean,
    pendingCount: Int
) {
    val icon = if (selected) item.selectedIcon else item.unselectedIcon
    if (item.routePattern == Routes.TRANSACTIONS && pendingCount > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = PendingAmber, contentColor = NavyDeep) {
                    Text(pendingCount.toString())
                }
            }
        ) {
            Icon(icon, contentDescription = item.label)
        }
    } else {
        Icon(icon, contentDescription = item.label)
    }
}
