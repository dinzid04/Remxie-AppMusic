package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PillActiveIndicator
import com.example.ui.theme.PillNavBackground
import com.example.ui.theme.PillNavBorder
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey

enum class BitChordTab {
    HOME,
    LIBRARY,
    SEARCH
}

@Composable
fun FloatingPillBottomBar(
    selectedTab: BitChordTab,
    onSelectTab: (BitChordTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = 260.dp, max = 356.dp)
                .fillMaxWidth()
                .height(66.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clip(RoundedCornerShape(50))
                .background(PillNavBackground.copy(alpha = 0.96f))
                .border(
                    width = 1.dp,
                    color = PillNavBorder.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(50)
                )
                .padding(5.dp)
                .testTag("floating_bottom_nav"),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillNavItem(
                label = stringResource(R.string.nav_home),
                selected = selectedTab == BitChordTab.HOME,
                activeIcon = Icons.Filled.Home,
                inactiveIcon = Icons.Outlined.Home,
                testTag = "nav_home_button",
                onClick = { onSelectTab(BitChordTab.HOME) },
                modifier = Modifier.weight(1f)
            )

            PillNavItem(
                label = stringResource(R.string.nav_library),
                selected = selectedTab == BitChordTab.LIBRARY,
                activeIcon = Icons.Filled.LibraryMusic,
                inactiveIcon = Icons.Outlined.LibraryMusic,
                testTag = "nav_library_button",
                onClick = { onSelectTab(BitChordTab.LIBRARY) },
                modifier = Modifier.weight(1f)
            )

            PillNavItem(
                label = stringResource(R.string.nav_search),
                selected = selectedTab == BitChordTab.SEARCH,
                activeIcon = Icons.Filled.Search,
                inactiveIcon = Icons.Outlined.Search,
                testTag = "nav_search_button",
                onClick = { onSelectTab(BitChordTab.SEARCH) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PillNavItem(
    label: String,
    selected: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) PillActiveIndicator else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pill_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) TextPrimaryWhite else TextSecondaryGrey,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pill_content"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) activeIcon else inactiveIcon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(23.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontFamily = PlusJakartaSans,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = 11.5.sp,
                color = contentColor
            )
        }
    }
}

