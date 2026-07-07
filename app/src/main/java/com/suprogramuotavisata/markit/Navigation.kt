package com.suprogramuotavisata.markit

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.suprogramuotavisata.markit.data.AppLanguage
import com.suprogramuotavisata.markit.ui.MainContainer
import com.suprogramuotavisata.markit.ui.DashboardScreen
import com.suprogramuotavisata.markit.ui.ScanItScreen
import com.suprogramuotavisata.markit.ui.MyStoreScreen
import com.suprogramuotavisata.markit.ui.SettingsScreen

@Composable
fun MainNavigation(
    currentLanguage: AppLanguage,
    onLanguageToggle: (AppLanguage) -> Unit
) {
  val backStack = rememberNavBackStack(Dashboard)

  MainContainer(
    currentKey = backStack.lastOrNull() ?: Dashboard,
    currentLanguage = currentLanguage,
    onLanguageToggle = onLanguageToggle,
    onNavigate = { key ->
      // Pop all elements to clear backstack, then add the target tab
      while (backStack.isNotEmpty()) {
        backStack.removeLastOrNull()
      }
      backStack.add(key)
    }
  ) {
    androidx.navigation3.ui.NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      entryProvider =
        entryProvider {
          entry<Dashboard> {
            DashboardScreen(
              onNavigateToStore = { groupName ->
                while (backStack.isNotEmpty()) {
                  backStack.removeLastOrNull()
                }
                backStack.add(MyStore(groupName))
              }
            )
          }
          entry<ScanIt> {
            ScanItScreen()
          }
          entry<MyStore> { key ->
            MyStoreScreen(initialGroupFilter = key.initialGroupFilter)
          }
          entry<Settings> {
            SettingsScreen()
          }
        },
    )
  }
}
