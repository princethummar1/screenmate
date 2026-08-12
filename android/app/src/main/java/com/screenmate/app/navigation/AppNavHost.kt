package com.screenmate.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
import com.screenmate.app.usage.UsageCollector
import com.screenmate.app.ScreenMateApplication
import com.screenmate.app.core.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val context = LocalContext.current
    val preferences = ScreenMateApplication.instance.preferences
    val startDestination = when {
        !preferences.isLoggedIn -> Screen.Login.route
        !UsageCollector.hasUsageAccess(context) -> Screen.UsageAccess.route
        else -> Screen.Dashboard.route
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val gesturesEnabled = currentRoute != Screen.Login.route && currentRoute != Screen.UsageAccess.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkSurface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "ScreenMate",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AccentPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                
                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                
                DrawerSection("MY APP")
                DrawerItem("Dashboard", Icons.Default.Home, Screen.Dashboard.route, currentRoute) {
                    navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } }
                    scope.launch { drawerState.close() }
                }

                DrawerSection("DIGITAL LIFE")
                DrawerItem("Screen Time", Icons.Default.PhoneAndroid, Screen.ScreenTimeDaily.route, currentRoute) {
                    navController.navigate(Screen.ScreenTimeDaily.route)
                    scope.launch { drawerState.close() }
                }
                DrawerItem("Weekly Report", Icons.Default.DateRange, Screen.ScreenTimeWeekly.route, currentRoute) {
                    navController.navigate(Screen.ScreenTimeWeekly.route)
                    scope.launch { drawerState.close() }
                }
                DrawerItem("Monthly Report", Icons.Default.BarChart, Screen.ScreenTimeMonthly.route, currentRoute) {
                    navController.navigate(Screen.ScreenTimeMonthly.route)
                    scope.launch { drawerState.close() }
                }

                DrawerSection("COLLECTIONS")
                DrawerItem("Watchlist", Icons.Default.PlayArrow, Screen.Watchlist.route, currentRoute) {
                    navController.navigate(Screen.Watchlist.route)
                    scope.launch { drawerState.close() }
                }
                DrawerItem("Watch Log", Icons.Default.CheckCircle, Screen.WatchLog.route, currentRoute) {
                    navController.navigate(Screen.WatchLog.route)
                    scope.launch { drawerState.close() }
                }
                DrawerItem("Reading", Icons.Default.Book, Screen.Reading.route, currentRoute) {
                    navController.navigate(Screen.Reading.route)
                    scope.launch { drawerState.close() }
                }
                DrawerItem("Playlists", Icons.Default.MusicNote, Screen.Playlists.route, currentRoute) {
                    navController.navigate(Screen.Playlists.route)
                    scope.launch { drawerState.close() }
                }
                DrawerItem("Wishlist", Icons.Default.ShoppingCart, Screen.Wishlist.route, currentRoute) {
                    navController.navigate(Screen.Wishlist.route)
                    scope.launch { drawerState.close() }
                }
                DrawerItem("Custom Lists", Icons.Default.List, Screen.CustomLists.route, currentRoute) {
                    navController.navigate(Screen.CustomLists.route)
                    scope.launch { drawerState.close() }
                }

                DrawerSection("NOTES")
                DrawerItem("Scratchpad", Icons.Default.Edit, Screen.Scratchpad.route, currentRoute) {
                    navController.navigate(Screen.Scratchpad.route)
                    scope.launch { drawerState.close() }
                }
                DrawerItem("Journal", Icons.Default.Book, Screen.Journal.route, currentRoute) {
                    navController.navigate(Screen.Journal.route)
                    scope.launch { drawerState.close() }
                }

                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                
                DrawerItem("Bookmarks", Icons.Default.Star, Screen.Bookmarks.route, currentRoute) {
                    navController.navigate(Screen.Bookmarks.route)
                    scope.launch { drawerState.close() }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(horizontal = 16.dp))
                DrawerItem("Settings", Icons.Default.Settings, Screen.Settings.route, currentRoute) {
                    navController.navigate(Screen.Settings.route)
                    scope.launch { drawerState.close() }
                }
                Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize().background(DarkBackground)
        ) {
            composable(Screen.Login.route) {
                com.screenmate.app.features.auth.LoginScreen(
                    onNavigateToDashboard = {
                        val nextRoute = if (UsageCollector.hasUsageAccess(context)) Screen.Dashboard.route else Screen.UsageAccess.route
                        navController.navigate(nextRoute) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.UsageAccess.route) {
                com.screenmate.app.features.onboarding.UsageAccessScreen(
                    onAccessGranted = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.UsageAccess.route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.UsageAccess.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Dashboard.route) {
                com.screenmate.app.features.dashboard.DashboardScreen(
                    onNavigateToWatchlist = { navController.navigate(Screen.Watchlist.route) },
                    onNavigateToJournal = { navController.navigate(Screen.Journal.route) },
                    onNavigateToDailyTime = { navController.navigate(Screen.ScreenTimeDaily.route) },
                    onNavigateToScratchpad = { navController.navigate(Screen.Scratchpad.route) },
                    onNavigateToBookmarks = { navController.navigate(Screen.Bookmarks.route) }
                )
            }
            composable(Screen.ScreenTimeDaily.route) { com.screenmate.app.features.screentime.DailyScreenTimeScreen() }
            composable(Screen.ScreenTimeWeekly.route) { com.screenmate.app.features.screentime.WeeklyReportScreen() }
            composable(Screen.ScreenTimeMonthly.route) { com.screenmate.app.features.screentime.MonthlyReportScreen() }
            composable(Screen.Watchlist.route) { 
                com.screenmate.app.features.watchlist.WatchlistScreen(
                    onNavigateToSearch = { navController.navigate(Screen.WatchlistSearch.route) }
                ) 
            }
            composable(Screen.WatchLog.route) { com.screenmate.app.features.watchlist.WatchLogScreen() }
            composable(Screen.WatchlistSearch.route) {
                com.screenmate.app.features.watchlist.WatchlistSearchScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Reading.route) {
                com.screenmate.app.features.reading.ReadingListScreen(
                    onNavigateToSearch = { navController.navigate(Screen.BookSearch.route) }
                )
            }
            composable(Screen.BookSearch.route) {
                com.screenmate.app.features.reading.BookSearchScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Playlists.route) { com.screenmate.app.features.playlists.PlaylistsScreen() }
            composable(
                route = Screen.PlaylistDetail.route + "/{id}",
                arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                com.screenmate.app.features.playlists.PlaylistDetailScreen(playlistId = id)
            }
            composable(Screen.Wishlist.route) { com.screenmate.app.features.wishlist.WishlistScreen() }
            composable(Screen.CustomLists.route) { com.screenmate.app.features.customlists.CustomListsScreen(
                onNavigateToDetail = { id -> navController.navigate(Screen.CustomListDetail.createRoute(id)) }
            ) }
            composable(
                route = Screen.CustomListDetail.route + "/{id}",
                arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                com.screenmate.app.features.customlists.CustomListDetailScreen(
                    listId = id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Scratchpad.route) { 
                com.screenmate.app.features.scratchpad.ScratchpadScreen(
                    onNavigateToNewNote = { navController.navigate(Screen.NoteEdit.createRoute(java.util.UUID.randomUUID().toString())) }
                ) 
            }
            composable(
                route = Screen.NoteEdit.route + "/{id}",
                arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                com.screenmate.app.features.scratchpad.NoteEditScreen(noteId = id)
            }
            composable(Screen.Journal.route) { 
                com.screenmate.app.features.journal.JournalScreen(
                    onNavigateToToday = { navController.navigate(Screen.JournalEntry.createRoute(com.screenmate.app.core.util.DateUtils.todayDate())) }
                ) 
            }
            composable(
                route = Screen.JournalEntry.route + "/{date}",
                arguments = listOf(androidx.navigation.navArgument("date") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date") ?: ""
                com.screenmate.app.features.journal.JournalEntryScreen(date = date)
            }
            composable(Screen.Bookmarks.route) { com.screenmate.app.features.bookmarks.BookmarksScreen() }
            composable(Screen.Settings.route) {
                com.screenmate.app.features.settings.SettingsScreen(
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DrawerSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = TextTertiary,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun DrawerItem(label: String, icon: ImageVector, route: String, currentRoute: String?, onClick: () -> Unit) {
    val selected = currentRoute == route
    NavigationDrawerItem(
        label = { Text(label, color = if (selected) AccentPrimary else TextPrimary) },
        icon = { Icon(icon, contentDescription = null, tint = if (selected) AccentPrimary else TextSecondary) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = AccentPrimary.copy(alpha = 0.15f),
            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

