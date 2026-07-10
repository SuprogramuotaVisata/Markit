package com.suprogramuotavisata.markit.data

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage {
    EN,
    LT
}

interface AppStrings {
    val appName: String
    val dashboard: String
    val scanIt: String
    val myStore: String
    val myThings: String
    val sloganText: String
    val groupFieldCode: String
    val groupFieldBarcode: String
    val groupFieldThereIsIt: String
    val groupFieldDescription: String
    val groupFieldBarcodePlaceholder: String
    val groupFieldThereIsItPlaceholder: String
    val groupFieldDescriptionPlaceholder: String
    val settings: String
    val createGroup: String
    val groupName: String
    val enterGroupName: String
    val create: String
    val cancel: String
    val groupAlreadyExists: String
    val groupCreated: String
    val dbSaveError: String
    val noGroupsTitle: String
    val noGroupsDesc: String
    val selectGroup: String
    val barcodeLabel: String
    val commentLabel: String
    val scanBarcode: String
    val addCodeToPic: String
    val addCommentToPic: String
    val addDateToPic: String
    val printBarcodeOpt: String
    val captureBtn: String
    val scannedCode: String
    val scanFailed: String
    val selectGroupFirst: String
    val createGroupFirstPrompt: String
    val saveSyncSuccess: String
    val saveLocalOnly: String
    val printBarcodeError: String
    val printError: String
    val searchPlaceholder: String
    val filterGroups: String
    val all: String
    val foundCount: String
    val sortByDate: String
    val sortByCode: String
    val sortAsc: String
    val sortDesc: String
    val noItemsFound: String
    val viewPhoto: String
    val categoryLabel: String
    val photoNotFound: String
    val uploadedToDrive: String
    val dateLabel: String
    val driveSettings: String
    val driveSettingsDesc: String
    val signInGoogle: String
    val signInMock: String
    val signOut: String
    val testConnection: String
    val connectionOk: String
    val connectionFail: String
    val defaultStates: String
    val defaultStatesDesc: String
    val printerSettings: String
    val printerSettingsDesc: String
    val printTestBarcode: String
    val testBarcodeText: String
    val connectionStatus: String
    val connectionUser: String
    val connectionMode: String
    val connectionLoggedIn: String
    val connectionLocalMode: String
    val errorLoadingData: String
    val dataLoading: String
    val mockModeActive: String
    val logOutSuccess: String
    val creatingGroupFolder: String
    val groupLabel: String
    
    // Printer settings translation keys
    val printerTypeLabel: String
    val printerTypeSystem: String
    val printerTypeNetwork: String
    val printerTypeBluetooth: String
    val printerIpLabel: String
    val printerPortLabel: String
    val printerBtLabel: String
    
    // Action buttons for printing/pairing
    val openSystemPrintSettings: String
    val openBluetoothSettings: String
    
    // NFC integration keys
    val printerNfcLabel: String
    val printerNfcDesc: String
    val printerNfcStatus: String
    val printerNfcSupported: String
    val printerNfcDisabled: String
    val printerNfcNotSupported: String
    val printerNfcPairBtn: String
    val printerNfcPairingPrompt: String

    // Storage Location & Migration keys
    val storageLocationLabel: String
    val storageLocationDesc: String
    val storageLocal: String
    val storageDrive: String
    val migratingTitle: String
    val migratingDesc: String
    val migrationSuccess: String
    val migrationError: String
}

object EnStrings : AppStrings {
    override val appName = "Mark It"
    override val dashboard = "Groups"
    override val scanIt = "Capture"
    override val myStore = "My Things"
    override val myThings = "My Things"
    override val sloganText = "Mark it and know where it is (Mark it)"
    override val groupFieldCode = "Code"
    override val groupFieldBarcode = "Barcode"
    override val groupFieldThereIsIt = "There is it"
    override val groupFieldDescription = "Description"
    override val groupFieldBarcodePlaceholder = "Additional barcode"
    override val groupFieldThereIsItPlaceholder = "Where it is placed"
    override val groupFieldDescriptionPlaceholder = "Group description"
    override val settings = "Settings"
    override val createGroup = "New Group"
    override val groupName = "Group Name"
    override val enterGroupName = "Enter group details. It will be mapped to Google Drive."
    override val create = "Create"
    override val cancel = "Cancel"
    override val groupAlreadyExists = "Group with this name already exists!"
    override val groupCreated = "Group created successfully!"
    override val dbSaveError = "Error saving to database"
    override val noGroupsTitle = "No groups created"
    override val noGroupsDesc = "Create a new group by clicking the + button below to catalog products."
    override val selectGroup = "Select group"
    override val barcodeLabel = "Code / Barcode"
    override val commentLabel = "Comment / Description"
    override val scanBarcode = "Scan Barcode"
    override val addCodeToPic = "Add code to photo"
    override val addCommentToPic = "Add comment to photo"
    override val addDateToPic = "Add current date"
    override val printBarcodeOpt = "Automatically print barcode"
    override val captureBtn = "Capture"
    override val scannedCode = "Scanned code"
    override val scanFailed = "Scan failed"
    override val selectGroupFirst = "Please select a group first!"
    override val createGroupFirstPrompt = "Please create at least one group in the main screen before capturing!"
    override val saveSyncSuccess = "Saved and synced with Google Drive!"
    override val saveLocalOnly = "Saved locally (Google Drive pending/offline)"
    override val printBarcodeError = "Failed to generate barcode for printing!"
    override val printError = "Print error"
    override val searchPlaceholder = "Search by code, description or date..."
    override val filterGroups = "Filter by groups:"
    override val all = "All"
    override val foundCount = "Found"
    override val sortByDate = "By date"
    override val sortByCode = "By code"
    override val sortAsc = "Ascending"
    override val sortDesc = "Descending"
    override val noItemsFound = "No items found. Try changing filters or search terms."
    override val viewPhoto = "View Photo"
    override val categoryLabel = "Category"
    override val photoNotFound = "Photo not found on local device."
    override val uploadedToDrive = "Photo uploaded to Google Drive"
    override val dateLabel = "Created"
    override val driveSettings = "Google Drive Backups"
    override val driveSettingsDesc = "Photos and info can be automatically saved to your Google Drive in a dedicated folder."
    override val signInGoogle = "Google Sign-In"
    override val signInMock = "Local Mock"
    override val signOut = "Sign Out"
    override val testConnection = "Test Connection"
    override val connectionOk = "Connection working! Folder found/created."
    override val connectionFail = "Failed to access Google Drive."
    override val defaultStates = "Camera Default Configuration"
    override val defaultStatesDesc = "Choose which info is included by default when opening the Capture screen."
    override val printerSettings = "Printer Settings"
    override val printerSettingsDesc = "If your device is connected to a printer, you can print a test barcode."
    override val printTestBarcode = "Print Test Barcode"
    override val testBarcodeText = "TEST"
    override val connectionStatus = "Status"
    override val connectionUser = "User"
    override val connectionMode = "Mode"
    override val connectionLoggedIn = "Connected"
    override val connectionLocalMode = "Local (Mock)"
    override val errorLoadingData = "Error loading data"
    override val dataLoading = "Loading..."
    override val mockModeActive = "Local mock mode activated!"
    override val logOutSuccess = "Logged out successfully!"
    override val creatingGroupFolder = "Creating group and Google Drive folder..."
    override val groupLabel = "Group"
    
    // Printer translations
    override val printerTypeLabel = "Printer Connection Type"
    override val printerTypeSystem = "System Print Dialog"
    override val printerTypeNetwork = "Wi-Fi / Network Printer"
    override val printerTypeBluetooth = "Bluetooth Printer"
    override val printerIpLabel = "Printer IP Address"
    override val printerPortLabel = "Printer Port (e.g. 9100)"
    override val printerBtLabel = "Bluetooth Device MAC / Name"
    
    // Action button translations
    override val openSystemPrintSettings = "Open System Print Settings"
    override val openBluetoothSettings = "Open Bluetooth Settings (Pair)"
    
    // NFC translations
    override val printerNfcLabel = "NFC Quick Connect"
    override val printerNfcDesc = "Touch the printer's NFC tag to automatically pair and configure Wi-Fi settings."
    override val printerNfcStatus = "NFC Status"
    override val printerNfcSupported = "Supported & Active"
    override val printerNfcDisabled = "NFC is disabled in system settings"
    override val printerNfcNotSupported = "Not supported on this device"
    override val printerNfcPairBtn = "Start NFC Pairing"
    override val printerNfcPairingPrompt = "Bring the phone close to the printer's NFC tag..."

    // Storage Location & Migration translations
    override val storageLocationLabel = "Active Storage Location"
    override val storageLocationDesc = "Choose where to save groups and items: locally on this device or synchronized to Google Drive."
    override val storageLocal = "Local Storage (Device/SD)"
    override val storageDrive = "Google Drive Cloud"
    override val migratingTitle = "Migrating Storage"
    override val migratingDesc = "Please wait while we migrate your data between local storage and Google Drive..."
    override val migrationSuccess = "Migration completed successfully!"
    override val migrationError = "Migration failed: "
}

// Proper Lithuanian Strings (Grammatically correct with standard accented characters)
object LtStrings : AppStrings {
    override val appName = "Atžymėk tai"
    override val dashboard = "Grupės"
    override val scanIt = "Fotografuoti"
    override val myStore = "Mano daiktai"
    override val myThings = "Mano daiktai"
    override val sloganText = "Pažymėk ir žinok, kur jis yra (Pažymėk)"
    override val groupFieldCode = "Grupės kodas"
    override val groupFieldBarcode = "Barkodas"
    override val groupFieldThereIsIt = "Padėta ten"
    override val groupFieldDescription = "Aprašymas"
    override val groupFieldBarcodePlaceholder = "Įveskite papildomą barkodą"
    override val groupFieldThereIsItPlaceholder = "Kur daiktas yra padėtas"
    override val groupFieldDescriptionPlaceholder = "Grupės aprašymas"
    override val settings = "Nustatymai"
    override val createGroup = "Nauja grupė"
    override val groupName = "Grupės pavadinimas"
    override val enterGroupName = "Įveskite naujos grupės duomenis. Ji bus susieta su Google Drive."
    override val create = "Sukurti"
    override val cancel = "Atšaukti"
    override val groupAlreadyExists = "Grupė tokiu pavadinimu jau egzistuoja!"
    override val groupCreated = "Grupė sėkmingai sukurta!"
    override val dbSaveError = "Klaida išsaugant DB"
    override val noGroupsTitle = "Nėra sukurtų grupių"
    override val noGroupsDesc = "Sukurkite naują grupę paspausdami + mygtuką apačioje, kad galėtumėte kataloguoti produktus."
    override val selectGroup = "Pasirinkti grupę"
    override val barcodeLabel = "Kodas / Barkodas"
    override val commentLabel = "Komentaras / Aprašymas"
    override val scanBarcode = "Skenuoti barkodą"
    override val addCodeToPic = "Pridėti kodą"
    override val addCommentToPic = "Pridėti komentarą / aprašymą"
    override val addDateToPic = "Pridėti dabartinę datą"
    override val printBarcodeOpt = "Automatiškai spausdinti barkodą"
    override val captureBtn = "Užfiksuoti"
    override val scannedCode = "Nuskaitytas kodas"
    override val scanFailed = "Nepavyko nuskaityti"
    override val selectGroupFirst = "Pirmiausia pasirinkite grupę!"
    override val createGroupFirstPrompt = "Sukurkite bent vieną grupę pagrindiniame lange prieš fotografuodami!"
    override val saveSyncSuccess = "Išsaugota ir sinchronizuota su Google Drive!"
    override val saveLocalOnly = "Išsaugota lokaliai (Google Drive laukia eilėje/neprijungtas)"
    override val printBarcodeError = "Nepavyko sugeneruoti barkodo spausdinimui!"
    override val printError = "Spausdinimo klaida"
    override val searchPlaceholder = "Paieška pagal kodą, aprašymą ar datą..."
    override val filterGroups = "Filtruoti pagal grupes:"
    override val all = "Visi"
    override val foundCount = "Rasta"
    override val sortByDate = "Pagal datą"
    override val sortByCode = "Pagal kodą"
    override val sortAsc = "Didėjančia"
    override val sortDesc = "Mažėjančia"
    override val noItemsFound = "Prekių nerasta. Pabandykite pakeisti paieškos frazę ar grupių filtrus."
    override val viewPhoto = "Peržiūrėti nuotrauką"
    override val categoryLabel = "Kategorija"
    override val photoNotFound = "Nuotrauka nerasta lokaliame įrenginyje."
    override val uploadedToDrive = "Nuotrauka įkelta į Google Drive"
    override val dateLabel = "Sukurta"
    override val driveSettings = "Google Drive atsarginės kopijos"
    override val driveSettingsDesc = "Nuotraukos ir informacija gali būti automatiškai išsaugomos jūsų Google Drive paskyroje specialiai sukurtame kataloge."
    override val signInGoogle = "Google prisijungimas"
    override val signInMock = "Vietinis bandomasis"
    override val signOut = "Atsijungti"
    override val testConnection = "Tikrinti ryšį"
    override val connectionOk = "Ryšys veikia! Programos katalogas rastas/sukurtas."
    override val connectionFail = "Nepavyko pasiekti Google Drive."
    override val defaultStates = "Fotografavimo numatytosios būsenos"
    override val defaultStatesDesc = "Pasirinkite, kuri informacija pagal nutylėjimą bus įtraukiama į nuotrauką atidarius langą 'Fotografuoti'."
    override val printerSettings = "Spausdintuvo nustatymai"
    override val printerSettingsDesc = "Jei įrenginys prijungtas prie spausdintuvo, galite atspausdinti bandomąjį barkodą."
    override val printTestBarcode = "Spausdinti bandomąjį barkodą"
    override val testBarcodeText = "TESTAS"
    override val connectionStatus = "Būsena"
    override val connectionUser = "Vartotojas"
    override val connectionMode = "Režimas"
    override val connectionLoggedIn = "Prisijungta"
    override val connectionLocalMode = "Vietinis (Mock)"
    override val errorLoadingData = "Klaida įkraunant duomenis"
    override val dataLoading = "Įkraunama..."
    override val mockModeActive = "Aktyvuotas vietinis bandomasis režimas!"
    override val logOutSuccess = "Sėkmingai atsijungta!"
    override val creatingGroupFolder = "Kuriama grupė ir Google Drive katalogas..."
    override val groupLabel = "Grupė"
    
    // Printer translations
    override val printerTypeLabel = "Spausdintuvo ryšio tipas"
    override val printerTypeSystem = "Sisteminis spausdinimo langas"
    override val printerTypeNetwork = "Tinklo / Wi-Fi spausdintuvas"
    override val printerTypeBluetooth = "Bluetooth spausdintuvas"
    override val printerIpLabel = "Spausdintuvo IP adresas"
    override val printerPortLabel = "Spausdintuvo prievadas (Port pvz. 9100)"
    override val printerBtLabel = "Bluetooth įrenginio MAC / pavadinimas"
    
    // Action buttons
    override val openSystemPrintSettings = "Atidaryti spausdinimo nustatymus"
    override val openBluetoothSettings = "Atidaryti Bluetooth nustatymus (Susieti)"
    
    // NFC translations
    override val printerNfcLabel = "Greitas prisijungimas per NFC"
    override val printerNfcDesc = "Prilieskite telefoną prie spausdintuvo NFC žymos, kad automatiškai sukonfigūruotumėte Wi-Fi nustatymus."
    override val printerNfcStatus = "NFC Būsena"
    override val printerNfcSupported = "Palaikoma ir aktyvi"
    override val printerNfcDisabled = "NFC išjungtas sistemos nustatymuose"
    override val printerNfcNotSupported = "Nepalaikoma šiame įrenginyje"
    override val printerNfcPairBtn = "Pradėti NFC susiejimą"
    override val printerNfcPairingPrompt = "Priartinkite telefoną prie spausdintuvo NFC žymos..."

    // Storage Location & Migration translations
    override val storageLocationLabel = "Aktyvi saugojimo vieta"
    override val storageLocationDesc = "Pasirinkite, kur saugoti grupes ir prekes: lokaliai šiame įrenginyje ar sinchronizuoti su Google Drive."
    override val storageLocal = "Vietinė saugykla (Telefonas/SD)"
    override val storageDrive = "Google Drive debesis"
    override val migratingTitle = "Perkeliamas turinys"
    override val migratingDesc = "Prašome palaukti, perkeliami duomenys tarp vietinės saugyklos ir Google Drive..."
    override val migrationSuccess = "Turinys sėkmingai perkeltas!"
    override val migrationError = "Turinio perkėlimo klaida: "
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { EnStrings }

object TranslationManager {
    private const val PREFS_NAME = "MarkItLangPrefs"
    private const val KEY_LANG = "selected_lang"

    fun getLanguage(context: Context): AppLanguage {
        val langStr = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG, AppLanguage.EN.name) ?: AppLanguage.EN.name
        return try {
            AppLanguage.valueOf(langStr)
        } catch (e: Exception) {
            AppLanguage.EN
        }
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, language.name)
            .apply()
    }

    /**
     * Strips Lithuanian accented letters for hardware printer compatibility.
     */
    fun stripAccents(str: String): String {
        return str
            .replace("ą", "a").replace("Ą", "A")
            .replace("č", "c").replace("Č", "C")
            .replace("ę", "e").replace("Ę", "E")
            .replace("ė", "e").replace("Ė", "E")
            .replace("į", "i").replace("Į", "I")
            .replace("š", "s").replace("Š", "S")
            .replace("ų", "u").replace("Ų", "U")
            .replace("ū", "u").replace("Ū", "U")
            .replace("ž", "z").replace("Ž", "Z")
    }
}
