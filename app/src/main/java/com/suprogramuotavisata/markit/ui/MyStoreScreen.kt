package com.suprogramuotavisata.markit.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.suprogramuotavisata.markit.data.DatabaseHelper
import com.suprogramuotavisata.markit.data.LocalAppStrings
import com.suprogramuotavisata.markit.data.ProductGroup
import com.suprogramuotavisata.markit.data.ProductItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MyStoreScreen(
    initialGroupFilter: String? = null
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()
    val s = LocalAppStrings.current

    // Query inputs
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val selectedGroupIds = remember { mutableStateListOf<Long>() }
    var sortBy by remember { mutableStateOf("date") } // "date" or "code"
    var sortDesc by remember { mutableStateOf(true) }

    var reloadTrigger by remember { mutableStateOf(0) }
    var showCamera by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }

    // Database states
    var allGroups by remember { mutableStateOf<List<ProductGroup>>(emptyList()) }
    val itemsLiveData = remember { androidx.lifecycle.MutableLiveData<List<ProductItem>>() }
    val itemsList by itemsLiveData.observeAsState(emptyList())
    var isLoading by remember { mutableStateOf(true) }

    // Dialog state
    var selectedViewItem by remember { mutableStateOf<ProductItem?>(null) }

    // Load groups and set initial filter
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            allGroups = dbHelper.getAllGroups()
        }

        if (initialGroupFilter != null) {
            val matchingGroup = allGroups.find { it.name.equals(initialGroupFilter, ignoreCase = true) }
            if (matchingGroup != null) {
                selectedGroupIds.add(matchingGroup.id)
            }
        }
    }

    // Load matching items
    LaunchedEffect(searchQuery.text, selectedGroupIds.size, sortBy, sortDesc, allGroups, reloadTrigger) {
        isLoading = true
        coroutineScope.launch {
            val list = withContext(Dispatchers.IO) {
                dbHelper.getAllItems(
                    groupIds = if (selectedGroupIds.isEmpty()) null else selectedGroupIds.toList(),
                    searchQuery = searchQuery.text.takeIf { it.isNotBlank() },
                    sortBy = sortBy,
                    sortDesc = sortDesc
                )
            }
            itemsLiveData.postValue(list)
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = s.myStore,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Search Toggle Button
                Button(
                    onClick = { showCamera = false },
                    shape = RoundedCornerShape(8.dp),
                    colors = if (!showCamera) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp).border(
                        width = if (showCamera) 1.dp else 0.dp,
                        color = if (showCamera) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp), tint = if (!showCamera) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ieškoti", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!showCamera) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                    }
                }

                // Scan Toggle Button
                Button(
                    onClick = { showCamera = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = if (showCamera) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp).border(
                        width = if (!showCamera) 1.dp else 0.dp,
                        color = if (!showCamera) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp), tint = if (showCamera) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Skenuoti", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (showCamera) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (showCamera) {
            // Use reusable Scanner Widget
            LiveScanner(
                onResult = { text ->
                    searchQuery = TextFieldValue(text = text, selection = TextRange(0, text.length))
                },
                isScanning = isScanning,
                modifier = Modifier.padding(vertical = 4.dp),
                height = 120
            )
        } else {
            // Search text box
            val interactionSource = remember { MutableInteractionSource() }
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            searchQuery = searchQuery.copy(selection = TextRange(0, searchQuery.text.length))
                        }
                    },
                interactionSource = interactionSource,
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    TextFieldDefaults.DecorationBox(
                        value = searchQuery.text,
                        innerTextField = innerTextField,
                        enabled = true,
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                        interactionSource = interactionSource,
                        placeholder = { Text(text = s.searchPlaceholder, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = if (searchQuery.text.isNotEmpty()) {
                            { IconButton(onClick = { searchQuery = TextFieldValue("") }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp)) } }
                        } else null,
                        container = {
                            OutlinedTextFieldDefaults.Container(
                                enabled = true, isError = false, interactionSource = interactionSource,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF2F2F2), unfocusedContainerColor = Color(0xFFF9F9F9),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    )
                }
            )
        }

        // Group filters row (Horizontal scrollable)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
            FilterChip(selected = selectedGroupIds.isEmpty(), onClick = { selectedGroupIds.clear() }, label = { Text(s.all, fontSize = 12.sp) })
            allGroups.forEach { group ->
                val isSelected = selectedGroupIds.contains(group.id)
                FilterChip(selected = isSelected, onClick = { if (isSelected) selectedGroupIds.remove(group.id) else selectedGroupIds.add(group.id) }, label = { Text(group.name, fontSize = 12.sp) })
            }
        }

        // Sorting Bar
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "${s.foundCount}: ${itemsList.size}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { sortBy = if (sortBy == "date") "code" else "date" }) {
                    Icon(Icons.Default.SortByAlpha, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (sortBy == "date") s.sortByDate else s.sortByCode, fontSize = 11.sp)
                }
                Text(text = if (sortDesc) s.sortDesc else s.sortAsc, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { sortDesc = !sortDesc })
            }
        }

        // Items List
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (itemsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { Text(text = s.noItemsFound, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, fontSize = 14.sp) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().weight(1f), contentPadding = PaddingValues(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(itemsList, key = { it.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    dbHelper.deleteItem(item.id)
                                    item.localPhotoPath?.let { path -> try { File(path).takeIf { it.exists() }?.delete() } catch (e: Exception) { Log.e("MyStoreScreen", "Delete error", e) } }
                                    withContext(Dispatchers.Main) { reloadTrigger++; Toast.makeText(context, "Ištrinta!", Toast.LENGTH_SHORT).show() }
                                }
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(state = dismissState, backgroundContent = {
                        val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(color).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) { Icon(Icons.Default.Delete, "Trinti", tint = Color.White) }
                    }, enableDismissFromStartToEnd = false, content = { ItemCard(item = item, onViewImageClick = { selectedViewItem = item }) })
                }
            }
        }
    }

    // Photo Viewer Dialog
    if (selectedViewItem != null) {
        val item = selectedViewItem!!
        val file = item.localPhotoPath?.let { File(it) }
        val hasLocalFile = file != null && file.exists()

        Dialog(onDismissRequest = { selectedViewItem = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = item.code, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${s.categoryLabel}: ${item.groupName}", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    IconButton(onClick = { selectedViewItem = null }, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))) { Icon(Icons.Default.Close, s.cancel, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }

                if (hasLocalFile) {
                    AsyncImage(model = file, contentDescription = "Foto", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).align(Alignment.Center))
                } else {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(s.photoNotFound, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                    }
                }

                if (!item.comment.isNullOrBlank() || item.date.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)), modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (!item.comment.isNullOrBlank()) { Text(text = "${s.commentLabel}: ${item.comment}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) ; Spacer(modifier = Modifier.height(8.dp)) }
                            Text(text = "${s.dateLabel}: ${item.date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemCard(item: ProductItem, onViewImageClick: () -> Unit) {
    val s = LocalAppStrings.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.groupName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(text = item.date.split(" ").firstOrNull() ?: "", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "${s.barcodeLabel}: ${item.code}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (!item.barcode.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "${s.groupFieldBarcode}: ${item.barcode}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${s.commentLabel}: ${item.comment ?: "-"}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = onViewImageClick,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(50))
            ) {
                Icon(imageVector = Icons.Default.Visibility, contentDescription = s.viewPhoto, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
