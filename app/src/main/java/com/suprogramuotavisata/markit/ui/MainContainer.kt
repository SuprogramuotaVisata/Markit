package com.suprogramuotavisata.markit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.suprogramuotavisata.markit.Dashboard
import com.suprogramuotavisata.markit.MyStore
import com.suprogramuotavisata.markit.ScanIt
import com.suprogramuotavisata.markit.Settings
import com.suprogramuotavisata.markit.data.AppLanguage
import com.suprogramuotavisata.markit.data.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(
    currentKey: NavKey,
    currentLanguage: AppLanguage,
    onLanguageToggle: (AppLanguage) -> Unit,
    onNavigate: (NavKey) -> Unit,
    content: @Composable () -> Unit
) {
    val s = LocalAppStrings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            BeeLogo(modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${s.appName} - ${s.sloganText}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Start
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "EN",
                                fontSize = 11.sp,
                                fontWeight = if (currentLanguage == AppLanguage.EN) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentLanguage == AppLanguage.EN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.clickable { onLanguageToggle(AppLanguage.EN) }
                            )
                            Switch(
                                checked = currentLanguage == AppLanguage.LT,
                                onCheckedChange = { isLt ->
                                    onLanguageToggle(if (isLt) AppLanguage.LT else AppLanguage.EN)
                                },
                                modifier = Modifier.scale(0.6f)
                            )
                            Text(
                                text = "LT",
                                fontSize = 11.sp,
                                fontWeight = if (currentLanguage == AppLanguage.LT) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentLanguage == AppLanguage.LT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.clickable { onLanguageToggle(AppLanguage.LT) }
                            )
                        }
                    }
                },
                actions = {}
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentKey is Dashboard,
                    onClick = { onNavigate(Dashboard) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = s.dashboard) },
                    label = { 
                        Text(
                            text = s.dashboard,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp
                        )
                    }
                )
                NavigationBarItem(
                    selected = currentKey is ScanIt,
                    onClick = { onNavigate(ScanIt) },
                    icon = { Icon(Icons.Default.PhotoCamera, contentDescription = s.scanIt) },
                    label = { 
                        Text(
                            text = s.scanIt,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp
                        )
                    }
                )
                NavigationBarItem(
                    selected = currentKey is MyStore,
                    onClick = { onNavigate(MyStore(null)) },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = s.myStore) },
                    label = { 
                        Text(
                            text = s.myStore,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp
                        )
                    }
                )
                NavigationBarItem(
                    selected = currentKey is Settings,
                    onClick = { onNavigate(Settings) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = s.settings) },
                    label = { 
                        Text(
                            text = s.settings,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
}
