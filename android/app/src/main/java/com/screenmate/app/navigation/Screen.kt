package com.screenmate.app.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object UsageAccess : Screen("usage_access")
    data object Dashboard : Screen("dashboard")
    data object ScreenTimeDaily : Screen("screentime_daily")
    data object ScreenTimeWeekly : Screen("screentime_weekly")
    data object ScreenTimeMonthly : Screen("screentime_monthly")
    data object Watchlist : Screen("watchlist")
    data object WatchLog : Screen("watchlog")
    data object WatchlistSearch : Screen("watchlist_search")
    data object Reading : Screen("reading")
    data object BookSearch : Screen("book_search")
    data object Playlists : Screen("playlists")
    data object PlaylistDetail : Screen("playlist/{id}") {
        fun createRoute(id: String) = "playlist/$id"
    }
    data object Wishlist : Screen("wishlist")
    data object CustomLists : Screen("custom_lists")
    data object CustomListDetail : Screen("custom_list/{id}") {
        fun createRoute(id: String) = "custom_list/$id"
    }
    data object Scratchpad : Screen("scratchpad")
    data object NoteEdit : Screen("note/{id}") {
        fun createRoute(id: String) = "note/$id"
    }
    data object Journal : Screen("journal")
    data object JournalEntry : Screen("journal_entry/{date}") {
        fun createRoute(date: String) = "journal_entry/$date"
    }
    data object Bookmarks : Screen("bookmarks")
    data object Settings : Screen("settings")
}
