package com.example.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MusicItem
import com.example.data.model.SearchCategorySection
import com.example.data.model.SearchFilter
import com.example.ui.home.SearchUiState
import com.example.ui.theme.CardPlaceholderDark
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey

@Composable
fun SearchScreen(
    searchState: SearchUiState,
    hasActiveMiniPlayer: Boolean,
    onQueryChange: (String) -> Unit,
    onFillQuery: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSubmitSearch: (String) -> Unit,
    onSelectFilter: (SearchFilter) -> Unit,
    onItemClick: (MusicItem, List<MusicItem>) -> Unit,
    onOpenTrackMenu: (MusicItem) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    BackHandler {
        if (searchState.isSubmitted) {
            onQueryChange(searchState.query)
        } else {
            onBackToHome()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("search_screen_root")
    ) {
        // Top Pill Search Bar matching Screenshot_20261018_210625_BitChord.jpg & Screenshot_20261018_210508_BitChord.jpg
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF1B1B20))
                .border(1.dp, Color(0xFF2B2B33), RoundedCornerShape(50))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (searchState.isSubmitted) {
                        onQueryChange(searchState.query)
                    } else {
                        onBackToHome()
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("search_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = TextPrimaryWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchState.query.isEmpty()) {
                    Text(
                        text = "Cari lagu, artis, album, playlist...",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.5.sp,
                        color = TextSecondaryGrey
                    )
                }

                BasicTextField(
                    value = searchState.query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    cursorBrush = SolidColor(Color.White),
                    textStyle = TextStyle(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = TextPrimaryWhite
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onSubmitSearch(searchState.query)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_field")
                )
            }

            if (searchState.query.isNotEmpty()) {
                IconButton(
                    onClick = onClearQuery,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("search_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hapus Pencarian",
                        tint = TextPrimaryWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Filter Chips Row when Search has been submitted (All, Songs, Videos, Albums, Artists, Playlists)
        if (searchState.isSubmitted) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_filter_chips_row")
            ) {
                items(
                    items = SearchFilter.entries,
                    key = { it.name }
                ) { filter ->
                    val isSelected = searchState.selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) Color.White else Color(0xFF19191E))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White else Color(0xFF2D2D36),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onSelectFilter(filter)
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                            .testTag("search_filter_chip_${filter.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Main Content Area
        Box(modifier = Modifier.fillMaxSize()) {
            val bottomPad = if (hasActiveMiniPlayer) 172.dp else 112.dp

            when {
                // 1. Empty Query -> Show Recent / Popular Searches
                searchState.query.isBlank() -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = bottomPad
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "recent_searches_header") {
                            Text(
                                text = "Pencarian Populer & Terakhir",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = TextSecondaryGrey,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(
                            items = searchState.recentSearches,
                            key = { "recent_$it" }
                        ) { recentQuery ->
                            SuggestionQueryRow(
                                text = recentQuery,
                                isHistory = true,
                                onClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onSubmitSearch(recentQuery)
                                },
                                onFillClick = { onFillQuery(recentQuery) }
                            )
                        }
                    }
                }

                // 2. Typing Mode (Before pressing Enter) -> Live Suggestions + Direct Playable Results
                !searchState.isSubmitted -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 4.dp,
                            bottom = bottomPad
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_suggestions_list")
                    ) {
                        // Text query suggestions at the top
                        items(
                            items = searchState.querySuggestions,
                            key = { "sug_q_$it" }
                        ) { suggestionText ->
                            SuggestionQueryRow(
                                text = suggestionText,
                                isHistory = false,
                                onClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onSubmitSearch(suggestionText)
                                },
                                onFillClick = { onFillQuery(suggestionText) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        if (searchState.querySuggestions.isNotEmpty() && searchState.directSuggestionItems.isNotEmpty()) {
                            item(key = "suggestions_divider") {
                                HorizontalDivider(
                                    color = Color(0xFF1F1F26),
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                                )
                            }
                        }

                        // Direct playable items (Songs, Videos, Artists, Albums, Playlists) right below
                        items(
                            items = searchState.directSuggestionItems,
                            key = { "sug_item_${it.id}_${it.type}" }
                        ) { item ->
                            SearchResultItemRow(
                                item = item,
                                onClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onItemClick(item, searchState.directSuggestionItems)
                                },
                                onMenuClick = { onOpenTrackMenu(item) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        if (searchState.isLoadingSuggestions && searchState.directSuggestionItems.isEmpty()) {
                            item(key = "suggestions_loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Submitted Search Mode -> Loading Indicator
                searchState.isLoadingResults -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // 4. Submitted Search Mode -> "All" Categorized Results
                searchState.selectedFilter == SearchFilter.ALL -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = bottomPad
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_all_results_list")
                    ) {
                        items(
                            items = searchState.allCategorySections,
                            key = { sec -> "cat_${sec.title}" }
                        ) { categorySection ->
                            SearchCategoryBlock(
                                section = categorySection,
                                onItemClick = { item ->
                                    onItemClick(item, categorySection.items)
                                },
                                onOpenTrackMenu = onOpenTrackMenu,
                                onShowAllCategory = { catTitle ->
                                    val targetFilter = when {
                                        catTitle.equals("Songs", ignoreCase = true) -> SearchFilter.SONGS
                                        catTitle.equals("Videos", ignoreCase = true) -> SearchFilter.VIDEOS
                                        catTitle.equals("Albums", ignoreCase = true) -> SearchFilter.ALBUMS
                                        catTitle.equals("Artists", ignoreCase = true) -> SearchFilter.ARTISTS
                                        catTitle.equals("Playlists", ignoreCase = true) -> SearchFilter.PLAYLISTS
                                        else -> null
                                    }
                                    if (targetFilter != null) {
                                        onSelectFilter(targetFilter)
                                    }
                                }
                            )
                        }
                    }
                }

                // 5. Submitted Search Mode -> Specific Filter Tab (Songs, Videos, Albums, Artists, Playlists)
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = bottomPad
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_filtered_results_list")
                    ) {
                        items(
                            items = searchState.filteredItems,
                            key = { item -> "filt_${item.id}_${item.type}" }
                        ) { item ->
                            SearchResultItemRow(
                                item = item,
                                onClick = {
                                    onItemClick(item, searchState.filteredItems)
                                },
                                onMenuClick = { onOpenTrackMenu(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionQueryRow(
    text: String,
    isHistory: Boolean,
    onClick: () -> Unit,
    onFillClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isHistory) Icons.Default.History else Icons.Outlined.Search,
            contentDescription = null,
            tint = TextSecondaryGrey,
            modifier = Modifier.size(21.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.5.sp,
            color = TextPrimaryWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onFillClick,
            modifier = Modifier.size(40.dp)
        ) {
            // Diagonal Up-Left Arrow (↖) matching Screenshot_20261018_210625_BitChord.jpg
            Canvas(modifier = Modifier.size(15.dp)) {
                val w = size.width
                val h = size.height
                val stroke = 2.dp.toPx()
                val color = Color(0xFFA0A0AB)
                drawLine(
                    color = color,
                    start = Offset(w * 0.82f, h * 0.82f),
                    end = Offset(w * 0.18f, h * 0.18f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.18f, h * 0.18f),
                    end = Offset(w * 0.68f, h * 0.18f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.18f, h * 0.18f),
                    end = Offset(w * 0.18f, h * 0.68f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun SearchCategoryBlock(
    section: SearchCategorySection,
    onItemClick: (MusicItem) -> Unit,
    onOpenTrackMenu: (MusicItem) -> Unit,
    onShowAllCategory: (String) -> Unit
) {
    val displayItems = remember(section.items) { section.items.take(4) }
    val canShowAll = section.title in listOf("Songs", "Videos", "Albums", "Artists", "Playlists")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = section.title,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            letterSpacing = (-0.4).sp,
            color = TextPrimaryWhite
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            displayItems.forEach { item ->
                SearchResultItemRow(
                    item = item,
                    onClick = { onItemClick(item) },
                    onMenuClick = { onOpenTrackMenu(item) }
                )
            }
        }

        if (canShowAll && section.items.size > 2) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Color(0xFF2E2E38), RoundedCornerShape(50))
                    .clickable { onShowAllCategory(section.title) }
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Show all",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = TextPrimaryWhite
                )
            }
        }
    }
}

@Composable
private fun SearchResultItemRow(
    item: MusicItem,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isArtist = item.type == "artist"
    val isNavigationCollection = item.type == "artist" || item.type == "album" || item.type == "playlist"

    val imgReq = remember(context, item.smallThumbnail) {
        ImageRequest.Builder(context)
            .data(item.smallThumbnail)
            .size(150, 150)
            .allowHardware(true)
            .crossfade(false)
            .build()
    }

    val formattedSubtitle = remember(item.type, item.subtitle, item.duration) {
        val raw = item.subtitle
        val typePrefix = when (item.type) {
            "song" -> "Song"
            "video" -> "Video"
            "artist" -> "Artist"
            "album" -> "Album"
            "playlist" -> "Playlist"
            else -> ""
        }
        if (isArtist) {
            "Artist"
        } else if (raw.startsWith(typePrefix, ignoreCase = true) || raw.startsWith("Lagu", ignoreCase = true)) {
            raw
        } else if (typePrefix.isNotBlank()) {
            "$typePrefix • $raw"
        } else {
            raw
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag("search_result_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imgReq,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(if (isArtist) CircleShape else RoundedCornerShape(8.dp))
                .background(CardPlaceholderDark)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.5.sp,
                color = TextPrimaryWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formattedSubtitle,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextSecondaryGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isNavigationCollection) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Buka ${item.title}",
                tint = TextSecondaryGrey,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(22.dp)
            )
        } else {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu ${item.title}",
                    tint = TextSecondaryGrey,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
fun SearchPlaceholderScreen(
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    SearchScreen(
        searchState = SearchUiState(),
        hasActiveMiniPlayer = false,
        onQueryChange = {},
        onFillQuery = {},
        onClearQuery = {},
        onSubmitSearch = {},
        onSelectFilter = {},
        onItemClick = { _, _ -> },
        onOpenTrackMenu = {},
        onBackToHome = onBackToHome,
        modifier = modifier
    )
}
