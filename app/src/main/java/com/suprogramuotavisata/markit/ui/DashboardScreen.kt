package com.suprogramuotavisata.markit.ui

import android.widget.Toast
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import com.suprogramuotavisata.markit.data.BarcodeGenerator
import com.suprogramuotavisata.markit.theme.MarkItTheme
import com.suprogramuotavisata.markit.data.EnStrings
import com.suprogramuotavisata.markit.data.DatabaseHelper
import com.suprogramuotavisata.markit.data.GoogleDriveService
import com.suprogramuotavisata.markit.data.LocalAppStrings
import com.suprogramuotavisata.markit.data.ProductGroup
import com.suprogramuotavisata.markit.data.PrintManager
import com.suprogramuotavisata.markit.utils.BarcodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BeeLogo(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Wings
        drawOval(
            color = Color(0xAA80DEEA),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.4f)
        )
        drawOval(
            color = Color(0xAA80DEEA),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.4f)
        )
        
        // Body
        drawOval(
            color = Color(0xFFFFD54F),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.35f),
            size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.45f)
        )
        
        // Stripes
        drawRect(
            color = Color(0xFF212121),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.37f, h * 0.35f),
            size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.45f)
        )
        drawRect(
            color = Color(0xFF212121),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.55f, h * 0.35f),
            size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.45f)
        )
        
        // Head
        drawCircle(
            color = Color(0xFF212121),
            radius = w * 0.12f,
            center = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.58f)
        )
        
        // Eye
        drawCircle(
            color = Color.White,
            radius = w * 0.03f,
            center = androidx.compose.ui.geometry.Offset(w * 0.21f, h * 0.55f)
        )
        
        // Stinger
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.75f, h * 0.5f)
            lineTo(w * 0.88f, h * 0.58f)
            lineTo(w * 0.75f, h * 0.65f)
            close()
        }
        drawPath(path = path, color = Color(0xFF212121))
    }
}

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
    var newGroupCode by remember { mutableStateOf(TextFieldValue("")) }
    var newGroupBarcode by remember { mutableStateOf(TextFieldValue("")) }
    var newGroupThereIs by remember { mutableStateOf(TextFieldValue("")) }
    var newGroupDescription by remember { mutableStateOf(TextFieldValue("")) }
    
    var isCreatingGroup by remember { mutableStateOf(false) }
    var focusedField by remember { mutableStateOf("name") }
    var showScannerInDialog by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }

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

    DashboardScreenContent(
        groups = groups,
        isLoading = isLoading,
        onNavigateToStore = onNavigateToStore,
        onAddGroupClick = {
            val suggestedNum = groups.size + 1
            val text = "Grupe $suggestedNum"
            newGroupName = TextFieldValue(text = text, selection = TextRange(0, text.length))
            val uniqueCode = java.util.UUID.randomUUID().toString().take(8).uppercase()
            newGroupCode = TextFieldValue(text = uniqueCode, selection = TextRange(0, uniqueCode.length))
            newGroupBarcode = TextFieldValue("")
            newGroupThereIs = TextFieldValue("")
            newGroupDescription = TextFieldValue("")
            focusedField = "name"
            showScannerInDialog = false
            showAddDialog = true
        },
        showAddDialog = showAddDialog,
        isCreatingGroup = isCreatingGroup,
        newGroupName = newGroupName,
        newGroupCode = newGroupCode,
        newGroupBarcode = newGroupBarcode,
        newGroupThereIs = newGroupThereIs,
        newGroupDescription = newGroupDescription,
        focusedField = focusedField,
        showScannerInDialog = showScannerInDialog,
        isScanning = isScanning,
        onShowScannerToggle = { showScannerInDialog = it },
        onFocusedFieldChange = { focusedField = it },
        onGroupNameChange = { newGroupName = it },
        onGroupCodeChange = { newGroupCode = it },
        onGroupBarcodeChange = { newGroupBarcode = it },
        onGroupThereIsChange = { newGroupThereIs = it },
        onGroupDescriptionChange = { newGroupDescription = it },
        onDismissAddDialog = { showAddDialog = false },
        onConfirmAddGroup = {
            isCreatingGroup = true
            coroutineScope.launch {
                val name = newGroupName.text.trim()
                val code = newGroupCode.text.trim()
                val barcode = newGroupBarcode.text.trim().takeIf { it.isNotBlank() }
                val thereIs = newGroupThereIs.text.trim().takeIf { it.isNotBlank() }
                val description = newGroupDescription.text.trim().takeIf { it.isNotBlank() }

                val existing = withContext(Dispatchers.IO) { dbHelper.getGroupByName(name) }
                if (existing != null) {
                    Toast.makeText(context, s.groupAlreadyExists, Toast.LENGTH_SHORT).show()
                    isCreatingGroup = false
                    return@launch
                }

                val sharedPrefs = context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE)
                val activeStorageMode = sharedPrefs.getString("active_storage_mode", "local") ?: "local"

                val driveFolderId = if (activeStorageMode == "google_drive") {
                    withContext(Dispatchers.IO) {
                        GoogleDriveService.findOrCreateCategoryFolder(context, name)
                    }
                } else {
                    null
                }

                val success = withContext(Dispatchers.IO) {
                    dbHelper.createGroup(name, driveFolderId, code, barcode, thereIs, description)
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
    )
}

@Composable
fun DashboardScreenContent(
    groups: List<ProductGroup>,
    isLoading: Boolean,
    onNavigateToStore: (String) -> Unit,
    onAddGroupClick: () -> Unit,
    showAddDialog: Boolean,
    isCreatingGroup: Boolean,
    newGroupName: TextFieldValue,
    newGroupCode: TextFieldValue,
    newGroupBarcode: TextFieldValue,
    newGroupThereIs: TextFieldValue,
    newGroupDescription: TextFieldValue,
    focusedField: String,
    showScannerInDialog: Boolean,
    isScanning: Boolean,
    onShowScannerToggle: (Boolean) -> Unit,
    onFocusedFieldChange: (String) -> Unit,
    onGroupNameChange: (TextFieldValue) -> Unit,
    onGroupCodeChange: (TextFieldValue) -> Unit,
    onGroupBarcodeChange: (TextFieldValue) -> Unit,
    onGroupThereIsChange: (TextFieldValue) -> Unit,
    onGroupDescriptionChange: (TextFieldValue) -> Unit,
    onDismissAddDialog: () -> Unit,
    onConfirmAddGroup: () -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddGroupClick,
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = s.noGroupsDesc,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
            val nameFocus = remember { FocusRequester() }
            val codeFocus = remember { FocusRequester() }
            val barcodeFocus = remember { FocusRequester() }
            val thereIsFocus = remember { FocusRequester() }
            val descFocus = remember { FocusRequester() }
            val focusManager = LocalFocusManager.current
            val dialogScrollState = rememberScrollState()

            LaunchedEffect(Unit) {
                nameFocus.requestFocus()
            }
            AlertDialog(
                onDismissRequest = { if (!isCreatingGroup) onDismissAddDialog() },
                confirmButton = {
                    Button(
                        enabled = newGroupName.text.isNotBlank() && !isCreatingGroup,
                        onClick = onConfirmAddGroup
                    ) {
                        Text(s.create)
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isCreatingGroup,
                        onClick = onDismissAddDialog
                    ) {
                        Text(s.cancel)
                    }
                },
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(s.createGroup, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { onShowScannerToggle(!showScannerInDialog) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (showScannerInDialog) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Skenuoti",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(dialogScrollState)
                    ) {
                        // Reusable Scanner Widget
                        if (showScannerInDialog) {
                            LiveScanner(
                                onResult = { text ->
                                    val newVal = TextFieldValue(text = text, selection = TextRange(0, text.length))
                                    when(focusedField) {
                                        "name" -> onGroupNameChange(newVal)
                                        "code" -> onGroupCodeChange(newVal)
                                        "barcode" -> onGroupBarcodeChange(newVal)
                                        "thereIs" -> onGroupThereIsChange(newVal)
                                        "description" -> onGroupDescriptionChange(newVal)
                                    }
                                },
                                isScanning = isScanning,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Text(
                            s.enterGroupName,
                            modifier = Modifier.padding(bottom = 8.dp),
                            fontSize = 14.sp
                        )

                        fun fieldModifier(myFocus: FocusRequester, nextFocus: FocusRequester?, fieldName: String): Modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(myFocus)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    onFocusedFieldChange(fieldName)
                                    when(fieldName) {
                                        "name" -> onGroupNameChange(newGroupName.copy(selection = TextRange(0, newGroupName.text.length)))
                                        "code" -> onGroupCodeChange(newGroupCode.copy(selection = TextRange(0, newGroupCode.text.length)))
                                        "barcode" -> onGroupBarcodeChange(newGroupBarcode.copy(selection = TextRange(0, newGroupBarcode.text.length)))
                                        "thereIs" -> onGroupThereIsChange(newGroupThereIs.copy(selection = TextRange(0, newGroupThereIs.text.length)))
                                        "description" -> onGroupDescriptionChange(newGroupDescription.copy(selection = TextRange(0, newGroupDescription.text.length)))
                                    }
                                }
                            }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.key == Key.Tab || keyEvent.key == Key.Enter) {
                                    if (nextFocus != null) nextFocus.requestFocus() else focusManager.clearFocus()
                                    true
                                } else false
                            }

                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = onGroupNameChange,
                            label = { Text(s.groupName) },
                            singleLine = true,
                            enabled = !isCreatingGroup,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { codeFocus.requestFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F2), unfocusedContainerColor = Color(0xFFF9F9F9)),
                            modifier = fieldModifier(nameFocus, codeFocus, "name")
                        )
                        OutlinedTextField(
                            value = newGroupCode,
                            onValueChange = onGroupCodeChange,
                            label = { Text(s.groupFieldCode) },
                            singleLine = true,
                            enabled = !isCreatingGroup,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { barcodeFocus.requestFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F2), unfocusedContainerColor = Color(0xFFF9F9F9)),
                            modifier = fieldModifier(codeFocus, barcodeFocus, "code")
                        )
                        OutlinedTextField(
                            value = newGroupBarcode,
                            onValueChange = onGroupBarcodeChange,
                            label = { Text(s.groupFieldBarcode) },
                            singleLine = true,
                            enabled = !isCreatingGroup,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { thereIsFocus.requestFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F2), unfocusedContainerColor = Color(0xFFF9F9F9)),
                            modifier = fieldModifier(barcodeFocus, thereIsFocus, "barcode")
                        )
                        OutlinedTextField(
                            value = newGroupThereIs,
                            onValueChange = onGroupThereIsChange,
                            label = { Text(s.groupFieldThereIsIt) },
                            singleLine = true,
                            enabled = !isCreatingGroup,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { descFocus.requestFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F2), unfocusedContainerColor = Color(0xFFF9F9F9)),
                            modifier = fieldModifier(thereIsFocus, descFocus, "thereIs")
                        )
                        OutlinedTextField(
                            value = newGroupDescription,
                            onValueChange = onGroupDescriptionChange,
                            label = { Text(s.groupFieldDescription) },
                            singleLine = true,
                            enabled = !isCreatingGroup,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFF2F2F2), unfocusedContainerColor = Color(0xFFF9F9F9)),
                            modifier = fieldModifier(descFocus, null, "description")
                        )

                        if (isCreatingGroup) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text(s.creatingGroupFolder, fontSize = 12.sp)
                            }
                        }
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
    val context = LocalContext.current
    val s = LocalAppStrings.current
    val sharedPrefs = remember { context.getSharedPreferences("MarkItSettings", Context.MODE_PRIVATE) }
    
    val printerType = sharedPrefs.getString("printer_type", "system") ?: "system"
    val printerIp = sharedPrefs.getString("printer_ip", "") ?: ""
    val printerBt = sharedPrefs.getString("printer_bt", "") ?: ""
    
    val isPrinterConnected = when (printerType) {
        "system" -> true
        "network" -> printerIp.isNotBlank()
        "bluetooth" -> printerBt.isNotBlank()
        else -> false
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(text = group.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val hasDrive = !group.driveFolderId.isNullOrBlank()
                    Icon(
                        imageVector = if (hasDrive) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = if (hasDrive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                    
                    IconButton(
                        onClick = { PrintManager.printGroupLabel(context, group) },
                        enabled = isPrinterConnected,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print Label",
                            tint = if (isPrinterConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            if (group.code.isNotBlank()) {
                Text(text = "${s.groupFieldCode}: ${group.code}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1, modifier = Modifier.padding(top = 1.dp))
            }
            if (!group.barcode.isNullOrBlank()) {
                Text(text = "${s.groupFieldBarcode}: ${group.barcode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 2, modifier = Modifier.padding(top = 1.dp))
                val br : Bitmap? = BarcodeGenerator.generateBarcode(group.barcode, width = 500, height = 80)
                if(br != null) {
                    BarcodeUtils.BarcodeBitmap(br, group.barcode, modifier = Modifier.fillMaxWidth().height(40.dp).padding(top = 1.dp))
                }
            }
            if (!group.padetaTen.isNullOrBlank()) {
                Text(text = "${s.groupFieldThereIsIt}: ${group.padetaTen}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 2, modifier = Modifier.padding(top = 1.dp))
            }
            if (!group.description.isNullOrBlank()) {
                Text(text = group.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 2, modifier = Modifier.padding(top = 1.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    MarkItTheme {
        CompositionLocalProvider(LocalAppStrings provides EnStrings) {
            DashboardScreenContent(
                groups = listOf(ProductGroup(id = 1, name = "Electronics", code = "ELEC", barcode="123456", description = "Gadgets")),
                isLoading = false,
                onNavigateToStore = {},
                onAddGroupClick = {},
                showAddDialog = false,
                isCreatingGroup = false,
                newGroupName = TextFieldValue(""),
                newGroupCode = TextFieldValue(""),
                newGroupBarcode = TextFieldValue(""),
                newGroupThereIs = TextFieldValue(""),
                newGroupDescription = TextFieldValue(""),
                focusedField = "name",
                showScannerInDialog = false,
                isScanning = false,
                onShowScannerToggle = {},
                onFocusedFieldChange = {},
                onGroupNameChange = {},
                onGroupCodeChange = {},
                onGroupBarcodeChange = {},
                onGroupThereIsChange = {},
                onGroupDescriptionChange = {},
                onDismissAddDialog = {},
                onConfirmAddGroup = {}
            )
        }
    }
}
