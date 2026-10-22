package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.lyrics.LyricsSettingsState
import com.example.data.lyrics.LyricsSource
import com.example.data.lyrics.translationLanguageName
import com.example.data.model.ConnectedAccountState
import com.example.data.model.IntegrationPlatform
import com.example.data.model.SpotifyImportProgressState
import com.example.ui.components.LyricsOffsetSheet
import com.example.ui.components.LyricsSourcesDialog
import com.example.ui.components.TranslationLanguageDialog

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAccountIntegrations: () -> Unit,
    spotifyConnected: Boolean,
    ytMusicConnected: Boolean,
    downloadStorageLocation: com.example.player.DownloadStorageLocation = com.example.player.DownloadStorageLocation.APP_INTERNAL,
    onSelectDownloadStorageLocation: (com.example.player.DownloadStorageLocation) -> Unit = {},
    downloadedTracksCount: Int = 0,
    lyricsSettings: LyricsSettingsState = LyricsSettingsState(),
    onToggleLyricsSourceEnabled: (LyricsSource) -> Unit = {},
    onMoveLyricsSourceOrder: (Int, Int) -> Unit = { _, _ -> },
    onTogglePrioritizeSyllable: (Boolean) -> Unit = {},
    onSavePaxSenixApiKey: (String) -> Unit = {},
    onResetLyricsSourcesDefault: () -> Unit = {},
    onChangeLyricsOffsetMs: (Int) -> Unit = {},
    onSelectTranslationLanguage: (String) -> Unit = {},
    onSetSyncedLyricsEnabled: (Boolean) -> Unit = {},
    onSetLyricsBlurEnabled: (Boolean) -> Unit = {},
    onSetReduceLyricsAnimation: (Boolean) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var soonMessage by remember { mutableStateOf<String?>(null) }
    var showLyricsSourcesDialog by remember { mutableStateOf(false) }
    var showLyricsOffsetSheet by remember { mutableStateOf(false) }
    var showTranslationLanguageDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    LaunchedEffect(soonMessage) {
        soonMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            soonMessage = null
        }
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0E),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1F222A),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0E))
                .padding(innerPadding)
                .testTag("settings_screen"),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16161D))
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Settings",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Storage & Download Location Section (2 Pilihan: Aplikasi Internal vs Penyimpanan HP .mp3)
            item {
                SettingsSectionHeader("Lokasi Penyimpanan Download Musik")
                SettingsGroupCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(Color(0xFFFFB74D).copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SdStorage,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pilih Lokasi Simpan Lagu Offline",
                                    color = Color.White,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (downloadedTracksCount > 0) {
                                        "$downloadedTracksCount lagu terunduh • Otomatis dipindahkan saat lokasi diubah"
                                    } else {
                                        "Termasuk unduhan thumbnail offline & sinkronisasi hapus otomatis"
                                    },
                                    color = Color(0xFF9EA3B0),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        com.example.player.DownloadStorageLocation.entries.forEach { option ->
                            val isSelected = downloadStorageLocation == option
                            val optionTag = if (option == com.example.player.DownloadStorageLocation.APP_INTERNAL) {
                                "storage_option_app_internal"
                            } else {
                                "storage_option_device_storage"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) Color(0xFFFF4D6D).copy(alpha = 0.15f)
                                        else Color(0xFF1A1B23)
                                    )
                                    .border(
                                        width = if (isSelected) 1.4.dp else 1.dp,
                                        color = if (isSelected) Color(0xFFFF4D6D) else Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        if (downloadStorageLocation != option) {
                                            onSelectDownloadStorageLocation(option)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .testTag(optionTag),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (option == com.example.player.DownloadStorageLocation.APP_INTERNAL) {
                                        Icons.Outlined.PhoneAndroid
                                    } else {
                                        Icons.Outlined.Folder
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFFFF4D6D) else Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.title,
                                        color = Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.subtitle,
                                        color = Color(0xFFA6AAB8),
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFFFF4D6D) else Color(0xFF6B7080),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // BitChord Lyrics Engine & Multi-Provider Section
            item {
                SettingsSectionHeader("BitChord Lyrics Engine & Providers")
                SettingsGroupCard {
                    SettingsRowItem(
                        icon = Icons.Outlined.Tune,
                        iconTint = Color(0xFFFF4D6D),
                        title = "Sumber & Prioritas Lirik (15+ Providers)",
                        subtitle = "${lyricsSettings.enabledSources.size} sumber aktif • Urutan: ${lyricsSettings.sourceOrder.take(3).joinToString(", ") { it.displayName }}...",
                        isSoon = false,
                        activeBadgeText = "${lyricsSettings.enabledSources.size} Aktif",
                        onClick = { showLyricsSourcesDialog = true },
                        testTag = "settings_lyrics_sources_row"
                    )
                    SettingsDivider()
                    val offsetSec = lyricsSettings.globalOffsetMs / 1000f
                    SettingsRowItem(
                        icon = Icons.Outlined.Schedule,
                        iconTint = Color(0xFF4DD0E1),
                        title = "Offset Waktu Lirik (Timing Sync)",
                        subtitle = "Sesuaikan jeda waktu lirik (-5.0s s/d +5.0s)",
                        isSoon = false,
                        activeBadgeText = if (lyricsSettings.globalOffsetMs == 0) "0.0s" else "%+.1fs".format(offsetSec),
                        onClick = { showLyricsOffsetSheet = true },
                        testTag = "settings_lyrics_offset_row"
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Outlined.Translate,
                        iconTint = Color(0xFFBA68C8),
                        title = "Bahasa Terjemahan & Romanisasi",
                        subtitle = "Target bahasa terjemahan otomatis: ${translationLanguageName(lyricsSettings.translationLanguage)}",
                        isSoon = false,
                        activeBadgeText = lyricsSettings.translationLanguage.uppercase(),
                        onClick = { showTranslationLanguageDialog = true },
                        testTag = "settings_lyrics_translation_lang_row"
                    )
                    SettingsDivider()
                    SettingsSwitchRowItem(
                        icon = Icons.Outlined.Lyrics,
                        iconTint = Color(0xFF38EF7D),
                        title = "Lirik Tersinkronisasi (Synced & Word-Sync)",
                        subtitle = "Jalankan lirik otomatis mengikuti ketukan & suku kata lagu",
                        checked = lyricsSettings.syncedLyrics,
                        onCheckedChange = onSetSyncedLyricsEnabled,
                        testTag = "settings_synced_lyrics_switch"
                    )
                    SettingsDivider()
                    SettingsSwitchRowItem(
                        icon = Icons.Outlined.AutoAwesome,
                        iconTint = Color(0xFFFFB74D),
                        title = "Utamakan Lirik Per Suku Kata (Syllable TTML)",
                        subtitle = "Prioritaskan provider Apple Music TTML, Musixmatch RichSync & KuGou HRC",
                        checked = lyricsSettings.prioritizeSyllableSync,
                        onCheckedChange = onTogglePrioritizeSyllable,
                        testTag = "settings_prioritize_syllable_switch"
                    )
                    SettingsDivider()
                    SettingsSwitchRowItem(
                        icon = Icons.Outlined.BlurOn,
                        iconTint = Color(0xFF64B5F6),
                        title = "Efek Blur Baris Lirik Non-Aktif",
                        subtitle = "Berikan efek kedalaman blur halus pada baris lirik yang jauh",
                        checked = lyricsSettings.lyricsBlur,
                        onCheckedChange = onSetLyricsBlurEnabled,
                        testTag = "settings_lyrics_blur_switch"
                    )
                }
            }

            // Interface Section
            item {
                SettingsSectionHeader("Interface")
                SettingsGroupCard {
                    SettingsRowItem(
                        icon = Icons.Outlined.Palette,
                        iconTint = Color(0xFFE8A838),
                        title = "Appearance",
                        subtitle = "Customize theme, colors, and visual layout",
                        isSoon = true,
                        onClick = { soonMessage = "Appearance customization coming soon!" }
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Outlined.FormatSize,
                        iconTint = Color(0xFF64B5F6),
                        title = "Player & Lyrics UI",
                        subtitle = "Atur sumber lirik, efek blur, dan sinkronisasi waktu",
                        isSoon = false,
                        activeBadgeText = "Active",
                        onClick = { showLyricsSourcesDialog = true }
                    )
                }
            }

            // Content & Playback Section
            item {
                SettingsSectionHeader("Content & Playback")
                SettingsGroupCard {
                    SettingsRowItem(
                        icon = Icons.Outlined.GraphicEq,
                        iconTint = Color(0xFF4DD0E1),
                        title = "Player & Audio",
                        subtitle = "Audio quality, gapless playback, crossfade & equalizer",
                        isSoon = true,
                        onClick = { soonMessage = "Player & Audio settings coming soon!" }
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Outlined.Public,
                        iconTint = Color(0xFF81C784),
                        title = "Content & Filters",
                        subtitle = "Language preferences, country charts & explicit filter",
                        isSoon = true,
                        onClick = { soonMessage = "Content & Filters coming soon!" }
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Outlined.AutoAwesome,
                        iconTint = Color(0xFFBA68C8),
                        title = "AI & Translations",
                        subtitle = "Aktif: ${translationLanguageName(lyricsSettings.translationLanguage)} • Terjemahan & Romanisasi",
                        isSoon = false,
                        activeBadgeText = lyricsSettings.translationLanguage.uppercase(),
                        onClick = { showTranslationLanguageDialog = true }
                    )
                }
            }

            // Storage & Data Section (Account & Integrations is ACTIVE!)
            item {
                SettingsSectionHeader("Storage & Data")
                SettingsGroupCard {
                    val activeCount = (if (spotifyConnected) 1 else 0) + (if (ytMusicConnected) 1 else 0)
                    SettingsRowItem(
                        icon = Icons.Outlined.ManageAccounts,
                        iconTint = Color(0xFF1ED760),
                        title = "Account & Integrations",
                        subtitle = if (activeCount > 0) {
                            "$activeCount layanan terhubung • Impor & sinkronisasi playlist aktif"
                        } else {
                            "Login YouTube Music (Google) & Spotify untuk impor playlist"
                        },
                        isSoon = false,
                        activeBadgeText = if (activeCount > 0) "$activeCount Active" else "Available",
                        onClick = onOpenAccountIntegrations,
                        testTag = "settings_account_integrations_row"
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Outlined.SdStorage,
                        iconTint = Color(0xFFFFB74D),
                        title = "Storage & Offline Cache",
                        subtitle = "Aktif: ${downloadStorageLocation.title}",
                        isSoon = false,
                        activeBadgeText = if (downloadStorageLocation == com.example.player.DownloadStorageLocation.APP_INTERNAL) "Internal APK" else "Storage .mp3",
                        onClick = {
                            val next = if (downloadStorageLocation == com.example.player.DownloadStorageLocation.APP_INTERNAL) {
                                com.example.player.DownloadStorageLocation.DEVICE_STORAGE
                            } else {
                                com.example.player.DownloadStorageLocation.APP_INTERNAL
                            }
                            onSelectDownloadStorageLocation(next)
                        }
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Outlined.Backup,
                        iconTint = Color(0xFF7986CB),
                        title = "Backup & Restore",
                        subtitle = "Export local favorites, playlists, and settings",
                        isSoon = true,
                        onClick = { soonMessage = "Backup & Restore coming soon!" }
                    )
                }
            }

            // System & Info Section
            item {
                SettingsSectionHeader("System & Info")
                SettingsGroupCard {
                    SettingsRowItem(
                        icon = Icons.Outlined.Shield,
                        iconTint = Color(0xFF4DB6AC),
                        title = "Privacy & Security",
                        subtitle = "Listening history, telemetry, and permissions",
                        isSoon = true,
                        onClick = { soonMessage = "Privacy & Security settings coming soon!" }
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Outlined.SystemUpdate,
                        iconTint = Color(0xFF90CAF9),
                        title = "Updater",
                        subtitle = "Check for BitChord releases and changelog",
                        isSoon = true,
                        onClick = { soonMessage = "Updater coming soon!" }
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Outlined.Info,
                        iconTint = Color(0xFFB0BEC5),
                        title = "About BitChord",
                        subtitle = "Version 2.4.0 • Full-Stack Audio & LRC Engine",
                        isSoon = true,
                        onClick = { soonMessage = "BitChord Music v2.4.0" }
                    )
                }
            }
        }
    }

    if (showLyricsSourcesDialog) {
        LyricsSourcesDialog(
            settings = lyricsSettings,
            onToggleSource = onToggleLyricsSourceEnabled,
            onMoveSource = onMoveLyricsSourceOrder,
            onTogglePrioritizeSyllable = onTogglePrioritizeSyllable,
            onSavePaxSenixApiKey = onSavePaxSenixApiKey,
            onResetDefault = onResetLyricsSourcesDefault,
            onDismiss = { showLyricsSourcesDialog = false }
        )
    }

    if (showLyricsOffsetSheet) {
        LyricsOffsetSheet(
            currentOffsetMs = lyricsSettings.globalOffsetMs,
            onChangeOffsetMs = onChangeLyricsOffsetMs,
            onDismiss = { showLyricsOffsetSheet = false }
        )
    }

    if (showTranslationLanguageDialog) {
        TranslationLanguageDialog(
            selectedCode = lyricsSettings.translationLanguage,
            onSelectLanguage = { code ->
                onSelectTranslationLanguage(code)
                showTranslationLanguageDialog = false
            },
            onDismiss = { showTranslationLanguageDialog = false }
        )
    }
}

@Composable
private fun SettingsSwitchRowItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF9EA3B0),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF4D6D)
            )
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = Color(0xFF8E929E),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF14151C))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp)),
        content = content
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp, end = 16.dp),
        color = Color.White.copy(alpha = 0.05f),
        thickness = 0.8.dp
    )
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isSoon: Boolean,
    activeBadgeText: String? = null,
    onClick: () -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .let { if (testTag != null) it.testTag(testTag) else it },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = if (isSoon) Color.White.copy(alpha = 0.85f) else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (isSoon) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Soon",
                            color = Color(0xFFB0B4C0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (activeBadgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF1ED760).copy(alpha = 0.16f))
                            .border(1.dp, Color(0xFF1ED760).copy(alpha = 0.4f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = activeBadgeText,
                            color = Color(0xFF1ED760),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = Color(0xFF8E929E),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (isSoon) Color(0xFF555863) else Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun AccountIntegrationsScreen(
    spotifyAccount: ConnectedAccountState,
    ytMusicAccount: ConnectedAccountState,
    isSyncing: Boolean,
    spotifyImportProgress: SpotifyImportProgressState? = null,
    statusMessage: String?,
    onBack: () -> Unit,
    onConnectSpotify: (String, String) -> Unit,
    onConnectYtMusic: (String, String) -> Unit,
    onDisconnectSpotify: () -> Unit,
    onDisconnectYtMusic: () -> Unit,
    onClearStatusMessage: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    var activeModalPlatform by remember { mutableStateOf<IntegrationPlatform?>(null) }
    var useGuestAlbumArtwork by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(onBack = onBack)

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            onClearStatusMessage()
        }
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0E),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1D222B),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0E))
                .padding(innerPadding)
                .testTag("account_integrations_screen"),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16161D))
                            .testTag("integrations_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Account & Integrations",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Connect accounts & import your playlists",
                            color = Color(0xFF8E929E),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Hero Account Card (matching the screenshot layout: Google = YT Music, Spotify = Spotify)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF191C26), Color(0xFF12141B))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF1ED760), Color(0xFFFF0033))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val connectedNames = listOfNotNull(
                                    ytMusicAccount.takeIf { it.isConnected }?.accountName,
                                    spotifyAccount.takeIf { it.isConnected }?.accountName
                                )
                                Text(
                                    text = if (connectedNames.isNotEmpty()) connectedNames.joinToString(" • ") else "BitChord Listener",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (ytMusicAccount.isConnected || spotifyAccount.isConnected) {
                                        "Sinkronisasi playlist aktif ke Home & Koleksi"
                                    } else {
                                        "Hubungkan Google (YouTube Music) atau Spotify untuk impor playlist"
                                    },
                                    color = Color(0xFFA0A4B0),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // "Login with Google" -> YouTube Music
                        Button(
                            onClick = { activeModalPlatform = IntegrationPlatform.YOUTUBE_MUSIC },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (ytMusicAccount.isConnected) Color(0xFF262936) else Color.White,
                                contentColor = if (ytMusicAccount.isConnected) Color.White else Color(0xFF111111)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_with_google_ytm_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = null,
                                tint = Color(0xFFFF0033),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (ytMusicAccount.isConnected) {
                                    "YouTube Music Connected (${ytMusicAccount.importedPlaylistsCount} Playlists) • Import More"
                                } else {
                                    "Continue with Google (YouTube Music)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // "Login with Spotify" -> Spotify API / Playlist Sync
                        Button(
                            onClick = { activeModalPlatform = IntegrationPlatform.SPOTIFY },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (spotifyAccount.isConnected) Color(0xFF1A3826) else Color(0xFF1ED760),
                                contentColor = if (spotifyAccount.isConnected) Color(0xFF1ED760) else Color(0xFF0A0A0E)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_with_spotify_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LibraryMusic,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (spotifyAccount.isConnected) {
                                    "Spotify Connected (${spotifyAccount.importedPlaylistsCount} Playlists) • Import More"
                                } else {
                                    "Connect with Spotify (API & Playlist Sync)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (ytMusicAccount.isConnected || spotifyAccount.isConnected) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = onNavigateToLibrary) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderSpecial,
                                        contentDescription = null,
                                        tint = Color(0xFF1ED760),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Lihat Playlist Diimpor di Koleksi",
                                        color = Color(0xFF1ED760),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                                Row {
                                    if (ytMusicAccount.isConnected) {
                                        TextButton(onClick = onDisconnectYtMusic) {
                                            Text("Putuskan YTM", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                                        }
                                    }
                                    if (spotifyAccount.isConnected) {
                                        TextButton(onClick = onDisconnectSpotify) {
                                            Text("Putuskan Spotify", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Loading & Real-Time Progress Bar when syncing/scanning Spotify & YT Music playlists
            item {
                AnimatedVisibility(visible = isSyncing || spotifyImportProgress != null) {
                    val progressState = spotifyImportProgress
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF17212D))
                            .border(1.dp, Color(0xFF1ED760).copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .padding(18.dp)
                            .testTag("spotify_import_progress_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (progressState?.isActive == true || isSyncing) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF1ED760),
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF1ED760),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Column {
                                    Text(
                                        text = progressState?.playlistTitle?.ifBlank { "Memindai Playlist Spotify..." }
                                            ?: "Menyinkronkan Playlist ke Library...",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (progressState != null && progressState.totalTracks > 0) {
                                        Text(
                                            text = "Memindai semua lagu (${progressState.processedTracks}/${progressState.totalTracks} lagu) + gambar asli per musik",
                                            color = Color(0xFFA6B0C2),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            if (progressState != null) {
                                Text(
                                    text = "${progressState.progressPercent}%",
                                    color = Color(0xFF1ED760),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        if (progressState != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { (progressState.progressPercent / 100f).coerceIn(0f, 1f) },
                                color = Color(0xFF1ED760),
                                trackColor = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xFF1ED760).copy(alpha = 0.16f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "✓ Berhasil: ${progressState.succeededTracks}",
                                            color = Color(0xFF1ED760),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xFFFF6B6B).copy(alpha = 0.16f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "✕ Gagal: ${progressState.failedTracks}",
                                            color = Color(0xFFFF6B6B),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "Total: ${progressState.totalTracks}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (progressState.currentTrackTitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = progressState.currentTrackTitle,
                                    color = Color(0xFFD5DCE8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Services Section (YouTube Music & Spotify Active, Discord & Last.fm Soon)
            item {
                SettingsSectionHeader("Connected Services")
                SettingsGroupCard {
                    SettingsRowItem(
                        icon = Icons.Outlined.PlayCircle,
                        iconTint = Color(0xFFFF0033),
                        title = "YouTube Music (Google)",
                        subtitle = if (ytMusicAccount.isConnected) {
                            "Terhubung sebagai ${ytMusicAccount.accountName} • ${ytMusicAccount.importedPlaylistsCount} playlist diimpor"
                        } else {
                            "Impor playlist pribadi/publik & koleksi lagu dari YouTube Music"
                        },
                        isSoon = false,
                        activeBadgeText = if (ytMusicAccount.isConnected) "Connected" else "Import",
                        onClick = { activeModalPlatform = IntegrationPlatform.YOUTUBE_MUSIC }
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Outlined.LibraryMusic,
                        iconTint = Color(0xFF1ED760),
                        title = "Spotify Web API & Playlists",
                        subtitle = if (spotifyAccount.isConnected) {
                            "Terhubung sebagai ${spotifyAccount.accountName} • ${spotifyAccount.importedPlaylistsCount} playlist diimpor"
                        } else {
                            "Login OAuth / Bearer Token / Link Playlist untuk impor ke pemutar"
                        },
                        isSoon = false,
                        activeBadgeText = if (spotifyAccount.isConnected) "Connected" else "Import",
                        onClick = { activeModalPlatform = IntegrationPlatform.SPOTIFY }
                    )
                }
            }

            // Social & Scrobbling Section ("Soon" as requested in user screenshot)
            item {
                SettingsSectionHeader("Rich Presence & Scrobbling")
                SettingsGroupCard {
                    SettingsRowItem(
                        icon = Icons.Outlined.Forum,
                        iconTint = Color(0xFF5865F2),
                        title = "Discord Rich Presence",
                        subtitle = "Display currently playing song, artist & timestamp on Discord",
                        isSoon = true,
                        onClick = {}
                    )
                    SettingsDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Use Guest Album Artwork",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Soon",
                                        color = Color(0xFFB0B4C0),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Show dynamic cover art in Discord status card",
                                color = Color(0xFF7D818D),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = useGuestAlbumArtwork,
                            onCheckedChange = { useGuestAlbumArtwork = it },
                            enabled = false
                        )
                    }
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Outlined.TrackChanges,
                        iconTint = Color(0xFFE53935),
                        title = "Last.fm & ListenBrainz",
                        subtitle = "Automatically scrobble tracks played in BitChord",
                        isSoon = true,
                        onClick = {}
                    )
                }
            }
        }
    }

    activeModalPlatform?.let { platform ->
        PlaylistImportDialog(
            platform = platform,
            onDismiss = { activeModalPlatform = null },
            onConfirmImport = { accountName, tokenOrPlaylistUrl ->
                activeModalPlatform = null
                if (platform == IntegrationPlatform.SPOTIFY) {
                    onConnectSpotify(accountName, tokenOrPlaylistUrl)
                } else {
                    onConnectYtMusic(accountName, tokenOrPlaylistUrl)
                }
            }
        )
    }
}

@Composable
private fun PlaylistImportDialog(
    platform: IntegrationPlatform,
    onDismiss: () -> Unit,
    onConfirmImport: (String, String) -> Unit
) {
    val isSpotify = platform == IntegrationPlatform.SPOTIFY
    val accentColor = if (isSpotify) Color(0xFF1ED760) else Color(0xFFFF0033)
    var accountName by remember {
        mutableStateOf(if (isSpotify) "Spotify Listener" else "Google YTM User")
    }
    var tokenOrUrl by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161821))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSpotify) Icons.Filled.LibraryMusic else Icons.Filled.PlayCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isSpotify) "Connect Spotify & Import" else "Connect Google (YouTube Music)",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isSpotify) "Spotify Web API / Playlist Link" else "YouTube Music OAuth / Playlist ID",
                        color = Color(0xFF9EA2B0),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text(if (isSpotify) "Nama Akun Spotify" else "Nama Akun Google / YTM") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = accentColor,
                    unfocusedLabelColor = Color(0xFF9EA2B0)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = tokenOrUrl,
                onValueChange = { tokenOrUrl = it },
                label = {
                    Text(
                        if (isSpotify) {
                            "OAuth Bearer Token atau Link Playlist Spotify (Opsional)"
                        } else {
                            "Link / ID Playlist YouTube Music (misal PL... / VL... Opsional)"
                        }
                    )
                },
                placeholder = {
                    Text(
                        if (isSpotify) {
                            "Kosongkan untuk sinkronisasi otomatis playlist populer Spotify, atau tempel link open.spotify.com/playlist/..."
                        } else {
                            "Kosongkan untuk sinkronisasi otomatis koleksi YouTube Music, atau tempel URL/ID playlist..."
                        },
                        fontSize = 12.sp
                    )
                },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = accentColor,
                    unfocusedLabelColor = Color(0xFF9EA2B0)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isSpotify) {
                    "Mendukung token `GET https://api.spotify.com/v1/me/playlists`, tautan playlist Spotify publik, atau klik tombol di bawah untuk langsung menyinkronkan playlist Anda."
                } else {
                    "Mendukung ID/Link Playlist YouTube Music (InnerTube Browse API) atau klik tombol di bawah untuk langsung mengimpor playlist akun Anda."
                },
                color = Color(0xFF8E929E),
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Batal", color = Color(0xFF9EA2B0))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onConfirmImport(accountName, tokenOrUrl) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (isSpotify) Color(0xFF0A0A0E) else Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("confirm_import_playlist_button")
                ) {
                    Text(
                        text = "Login & Impor Playlist",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
