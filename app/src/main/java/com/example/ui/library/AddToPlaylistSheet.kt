package com.example.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.data.model.ActiveDownloadProgress
import com.example.data.model.MusicItem
import com.example.data.model.PlaylistPrivacy
import com.example.data.model.UserLibraryPlaylist
import com.example.ui.theme.PlusJakartaSans

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistDialog(
    initialTitle: String = "",
    onDismiss: () -> Unit,
    onCreate: (title: String, privacy: PlaylistPrivacy) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var privacy by remember { mutableStateOf(PlaylistPrivacy.PRIVATE) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0A0A0C),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8E8E95))
            )
        },
        modifier = Modifier.testTag("create_playlist_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 28.dp)
        ) {
            Text(
                text = "New playlist",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = Color.White,
                letterSpacing = (-0.4).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Saved to your YouTube Music account",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = Color(0xFF8E8E95)
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(
                        text = "Playlist name",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF8E8E95)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1C1C1E),
                    unfocusedContainerColor = Color(0xFF1C1C1E),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_playlist_name_input")
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "WHO CAN SEE IT",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = Color(0xFF8E8E95),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrivacyOptionChip(
                    label = "Private",
                    icon = Icons.Rounded.Lock,
                    selected = privacy == PlaylistPrivacy.PRIVATE,
                    onClick = { privacy = PlaylistPrivacy.PRIVATE }
                )
                PrivacyOptionChip(
                    label = "Unlisted",
                    icon = Icons.Rounded.Edit,
                    selected = privacy == PlaylistPrivacy.UNLISTED,
                    onClick = { privacy = PlaylistPrivacy.UNLISTED },
                    useLinkStyleIcon = true
                )
                PrivacyOptionChip(
                    label = "Public",
                    icon = Icons.Rounded.Public,
                    selected = privacy == PlaylistPrivacy.PUBLIC,
                    onClick = { privacy = PlaylistPrivacy.PUBLIC }
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            val hasText = title.isNotBlank()
            Button(
                onClick = {
                    val finalTitle = title.trim().ifBlank { "New Playlist" }
                    onCreate(finalTitle, privacy)
                    onDismiss()
                },
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasText) Color.White else Color(0xFF1C1C1E),
                    contentColor = if (hasText) Color.Black else Color(0xFF8E8E95)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("create_playlist_confirm_button")
            ) {
                Text(
                    text = "Create playlist",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun PrivacyOptionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useLinkStyleIcon: Boolean = false
) {
    val bgColor = if (selected) Color.White else Color(0xFF1C1C1E)
    val contentColor = if (selected) Color.Black else Color(0xFF8E8E95)

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (useLinkStyleIcon) {
            Text(
                text = "⊶",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = contentColor
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = contentColor
        )
    }
}

@Composable
fun RenamePlaylistDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var title by remember(currentTitle) { mutableStateOf(currentTitle) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1E1E26),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rename_playlist_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Ubah Nama Playlist",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = {
                        Text(
                            text = "Nama baru playlist",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFB4C5FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                        focusedLabelColor = Color(0xFFB4C5FF),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_playlist_input")
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Batal",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB4C5FF)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onRename(title.trim())
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3E4759),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("rename_playlist_confirm_button")
                    ) {
                        Text(
                            text = "Simpan",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistBottomSheet(
    track: MusicItem,
    userPlaylists: List<UserLibraryPlaylist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (UserLibraryPlaylist) -> Unit,
    onCreateNewPlaylistAndAdd: (title: String, privacy: PlaylistPrivacy) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCreateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, privacy ->
                onCreateNewPlaylistAndAdd(title, privacy)
                showCreateDialog = false
                onDismiss()
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF16161D),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Tambahkan ke Playlist",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${track.title} • ${track.cleanArtist}",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Buat Playlist Baru Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCreateDialog = true }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("sheet_create_new_playlist_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF262632)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Buat Playlist Baru",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Buat Playlist Baru",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Buat dan langsung simpan lagu ini",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (userPlaylists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada playlist di Library. Ketuk 'Buat Playlist Baru' di atas.",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                ) {
                    items(userPlaylists, key = { it.id }) { playlist ->
                        val coverUrl = playlist.thumbnailUrl.ifBlank {
                            playlist.tracks.firstOrNull()?.thumbnailUrl ?: ""
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectPlaylist(playlist)
                                    onDismiss()
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF23232E)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = remember(coverUrl) {
                                            ImageRequest.Builder(context)
                                                .data(coverUrl)
                                                .size(Size(144, 144))
                                                .crossfade(false)
                                                .build()
                                        },
                                        contentDescription = playlist.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.title,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${playlist.tracks.size} lagu",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalTrackMenuBottomSheet(
    track: MusicItem,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    activeDownloadProgress: ActiveDownloadProgress? = null,
    downloadProgressPercent: Int? = activeDownloadProgress?.progressPercent,
    isVideoModeActive: Boolean = false,
    onDismiss: () -> Unit,
    onPlayNow: (() -> Unit)? = null,
    onToggleVideoMode: (() -> Unit)? = null,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownloadTrack: () -> Unit,
    onRemoveDownload: () -> Unit = {},
    onOpenArtist: (() -> Unit)? = null,
    onOpenArtistProfile: (() -> Unit)? = onOpenArtist,
    onRemoveFromCurrentPlaylist: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF16161D),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            // Track Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = remember(track.thumbnailUrl) {
                        ImageRequest.Builder(context)
                            .data(track.highResThumbnail)
                            .size(Size(144, 144))
                            .crossfade(false)
                            .build()
                    },
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF252530))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.cleanArtist,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.padding(vertical = 10.dp)
            )

            // 0. Switch to Video / Switch to Cover Image (shown in Music Player 3-dot menu!)
            if (onToggleVideoMode != null) {
                SheetActionRow(
                    icon = Icons.Rounded.MusicNote,
                    iconTint = Color(0xFF38EF7D),
                    title = if (isVideoModeActive) "Kembali ke Gambar Sampul" else "Ubah ke Video Musik (Full)",
                    subtitle = if (isVideoModeActive) "Tampilkan sampul album standar" else "Putar video YouTube secara penuh di pemutar musik",
                    onClick = {
                        onToggleVideoMode()
                        onDismiss()
                    },
                    testTag = "menu_toggle_video_mode"
                )
            }

            // 1. Sukai / Batal Sukai
            SheetActionRow(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                iconTint = if (isFavorite) Color(0xFFFF4D6D) else Color.White,
                title = if (isFavorite) "Hapus dari Musik yang Disukai" else "Simpan ke Musik yang Disukai",
                subtitle = "Tersimpan otomatis di Library Anda",
                onClick = {
                    onToggleFavorite()
                    onDismiss()
                },
                testTag = "menu_toggle_favorite"
            )

            // 2. Tambahkan ke Playlist
            SheetActionRow(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                iconTint = Color(0xFFB4C5FF),
                title = "Tambahkan ke Playlist...",
                subtitle = "Masukkan lagu ini ke playlist pilihan Anda",
                onClick = {
                    onDismiss()
                    onAddToPlaylist()
                },
                testTag = "menu_add_to_playlist"
            )

            // 3. Download Lagu Offline
            if (downloadProgressPercent != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        progress = { downloadProgressPercent / 100f },
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF38EF7D),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mengunduh... $downloadProgressPercent%",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF38EF7D)
                        )
                        Text(
                            text = "Progres unduhan tampil di Notifikasi Status Bar HP",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            } else if (isDownloaded) {
                SheetActionRow(
                    icon = Icons.Rounded.DownloadDone,
                    iconTint = Color(0xFF38EF7D),
                    title = "Tersimpan Offline (Hapus Unduhan)",
                    subtitle = "Lagu ini sudah diunduh & siap diputar tanpa internet",
                    onClick = {
                        onRemoveDownload()
                        onDismiss()
                    },
                    testTag = "menu_remove_download"
                )
            } else {
                SheetActionRow(
                    icon = Icons.Rounded.Download,
                    iconTint = Color(0xFF38EF7D),
                    title = "Download Musik (Offline)",
                    subtitle = "Simpan ke penyimpanan & tampilkan bar % di Notifikasi HP",
                    onClick = {
                        onDownloadTrack()
                        onDismiss()
                    },
                    testTag = "menu_download_track"
                )
            }

            // 4. Hapus dari Playlist Ini (jika dibuka dari dalam playlist buatan user)
            if (onRemoveFromCurrentPlaylist != null) {
                SheetActionRow(
                    icon = Icons.Rounded.DeleteOutline,
                    iconTint = Color(0xFFFF6B6B),
                    title = "Hapus dari Playlist ini",
                    subtitle = "Keluarkan lagu dari daftar putar",
                    onClick = {
                        onRemoveFromCurrentPlaylist()
                        onDismiss()
                    },
                    testTag = "menu_remove_from_playlist"
                )
            }

            // 5. Lihat Artis
            val artistAction = onOpenArtistProfile ?: onOpenArtist
            if (artistAction != null) {
                SheetActionRow(
                    icon = Icons.Rounded.Person,
                    iconTint = Color.White,
                    title = "Lihat Profil Artis",
                    subtitle = track.cleanArtist,
                    onClick = {
                        onDismiss()
                        artistAction()
                    },
                    testTag = "menu_open_artist"
                )
            }
        }
    }
}

@Composable
fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = Color.White,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp)
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
