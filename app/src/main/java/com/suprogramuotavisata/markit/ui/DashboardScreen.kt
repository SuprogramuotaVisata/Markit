package com.suprogramuotavisata.markit.ui

import android.widget.Toast
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suprogramuotavisata.markit.data.DatabaseHelper
import com.suprogramuotavisata.markit.data.GoogleDriveService
import com.suprogramuotavisata.markit.data.LocalAppStrings
import com.suprogramuotavisata.markit.data.ProductGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen(
    onNavigateToStore: (String) -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()
    val s = LocalAppStrings.current

    var groups by remember { mutableStateOf<List<ProductGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf(TextFieldValue("")) }
    var isCreatingGroup by remember { mutableStateOf(false) }

    // Function to reload groups
    fun loadGroups() {
        coroutineScope.launch {
            isLoading = true
            groups = withContext(Dispatchers.IO) {
                dbHelper.getAllGroups()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadGroups()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val suggestedNum = groups.size + 1
                    val text = "Grupe $suggestedNum"
                    newGroupName = TextFieldValue(text = text, selection = TextRange(0, text.length))
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = s.createGroup)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = s.appName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = s.dashboard,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (groups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = s.noGroupsTitle,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = s.noGroupsTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = s.noGroupsDesc,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(groups) { group ->
                        GroupCard(
                            group = group,
                            onClick = { onNavigateToStore(group.name) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            AlertDialog(
                onDismissRequest = { if (!isCreatingGroup) showAddDialog = false },
                title = { Text(s.createGroup) },
                text = {
                    Column {
                        Text(
                            s.enterGroupName,
                            modifier = Modifier.padding(bottom = 8.dp),
                            fontSize = 14.sp
                        )
                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            label = { Text(s.groupName) },
                            singleLine = true,
                            enabled = !isCreatingGroup,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF2F2F2),
                                unfocusedContainerColor = Color(0xFFF9F9F9)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                        if (isCreatingGroup) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text(s.creatingGroupFolder, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = newGroupName.text.isNotBlank() && !isCreatingGroup,
                        onClick = {
                            isCreatingGroup = true
                            coroutineScope.launch {
                                val name = newGroupName.text.trim()
                                val existing = withContext(Dispatchers.IO) { dbHelper.getGroupByName(name) }
                                if (existing != null) {
                                    Toast.makeText(context, s.groupAlreadyExists, Toast.LENGTH_SHORT).show()
                                    isCreatingGroup = false
                                    return@launch
                                }

                                val sharedPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
                                val activeStorageMode = sharedPrefs.getString("active_storage_mode", "local") ?: "local"

                                // Create folder on Google Drive in background only if configured
                                val driveFolderId = if (activeStorageMode == "google_drive") {
                                    withContext(Dispatchers.IO) {
                                        GoogleDriveService.findOrCreateCategoryFolder(context, name)
                                    }
                                } else {
                                    null
                                }

                                // Save in local SQLite
                                val success = withContext(Dispatchers.IO) {
                                    dbHelper.createGroup(name, driveFolderId)
                                }

                                isCreatingGroup = false
                                showAddDialog = false
                                if (success > -1) {
                                    Toast.makeText(context, s.groupCreated, Toast.LENGTH_SHORT).show()
                                    loadGroups()
                                } else {
                                    Toast.makeText(context, s.dbSaveError, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text(s.create)
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isCreatingGroup,
                        onClick = { showAddDialog = false }
                    ) {
                        Text(s.cancel)
                    }
                }
            )
        }
    }
}

@Composable
fun GroupCard(
    group: ProductGroup,
    onClick: () -> Unit
) {
    val s = LocalAppStrings.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = group.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val hasDrive = !group.driveFolderId.isNullOrBlank()
                Icon(
                    imageVector = if (hasDrive) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = if (hasDrive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (hasDrive) s.connectionLoggedIn else s.connectionLocalMode,
                    fontSize = 11.sp,
                    color = if (hasDrive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
