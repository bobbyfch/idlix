package com.idlix.tv.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.tv.material3.Icon
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Border
import androidx.tv.material3.Text
import com.idlix.tv.R
import com.idlix.tv.ui.theme.NetflixBlack
import com.idlix.tv.ui.theme.NetflixLightGray
import com.idlix.tv.ui.theme.NetflixWhite
import com.idlix.tv.ui.theme.NetflixRed

enum class NavItem(val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.home, Icons.Default.Home),
    MOVIES(R.string.movies, Icons.Default.Movie),
    SERIES(R.string.series, Icons.Default.Tv),
    SHORTS(R.string.filter_shorts, Icons.Default.PlayCircle),
    EXPLORE(R.string.explore, Icons.Default.Explore),
    SEARCH(R.string.search, Icons.Default.Search),
    CONTINUE_WATCHING(R.string.continue_watching, Icons.Default.History),
    SETTINGS(R.string.settings, Icons.Default.Settings)
}

@Composable
fun TvNavDrawer(
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val homeFocusRequester = remember { FocusRequester() }
    val drawerWidth by animateDpAsState(
        targetValue = if (isExpanded) 252.dp else 76.dp,
        label = "drawerWidth"
    )

    LaunchedEffect(Unit) {
        if (selectedItem == NavItem.HOME) {
            try {
                homeFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(visible) {
        if (!visible) isExpanded = false
    }

    Box(modifier = modifier.fillMaxSize().background(NetflixBlack)) {
        // 1. Content stays put while the rail expands as an overlay.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (visible) 82.dp else 0.dp)
        ) {
            content()
        }

        // 2. Scrim overlay when drawer is expanded
        if (visible && isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.22f))
            )
        }

        // 3. Material-style continuous rail. No per-item cards or pill outlines.
        if (visible) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .background(com.idlix.tv.ui.theme.NetflixDarkCard.copy(alpha = 0.96f))
                .padding(vertical = 19.dp, horizontal = 10.dp)
                .onFocusChanged { isExpanded = it.hasFocus },
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Official IDLIX Logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(bottom = 10.dp),
                contentAlignment = if (isExpanded) Alignment.CenterStart else Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        R.drawable.idlix_logo
                    ),
                    contentDescription = "IDLIX",
                    contentScale = ContentScale.Fit,
                    modifier = if (isExpanded) {
                        Modifier.height(28.dp).padding(start = 10.dp)
                    } else {
                        Modifier.width(52.dp).height(21.dp)
                    }
                )
            }

            // Compose Foundation lazy list supports native remote focus positioning.
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(NavItem.values().toList(), key = { it.name }) { item ->
                val isSelected = item == selectedItem
                var itemFocused by remember { mutableStateOf(false) }

                val itemTint by animateColorAsState(
                    targetValue = when {
                        itemFocused -> NetflixWhite
                        isSelected -> NetflixRed
                        else -> NetflixLightGray
                    },
                    label = "navigationItemTint"
                )
                val iconSize by animateDpAsState(
                    targetValue = if (itemFocused) 25.dp else 23.dp,
                    label = "navigationIconSize"
                )
                Card(
                    onClick = { onItemSelected(item) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .then(if (item == NavItem.HOME) Modifier.focusRequester(homeFocusRequester) else Modifier)
                        .onFocusChanged {
                            itemFocused = it.isFocused
                            if (it.isFocused) isExpanded = true
                        },
                    shape = CardDefaults.shape(shape = RoundedCornerShape(0.dp)),
                    colors = CardDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    ),
                    scale = CardDefaults.scale(focusedScale = 1.0f),
                    border = CardDefaults.border(border = Border.None, focusedBorder = Border.None)
                ) {
                    Box(Modifier.fillMaxSize()) {
                    if (isSelected || itemFocused) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(3.dp)
                                .height(if (itemFocused) 26.dp else 22.dp)
                                .background(
                                    if (itemFocused && !isSelected) NetflixWhite.copy(alpha = 0.76f)
                                    else NetflixRed,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = if (isExpanded) 21.dp else 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = stringResource(item.labelRes),
                            tint = itemTint,
                            modifier = Modifier.size(iconSize)
                        )
                        if (isExpanded) {
                            androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
                            Text(
                                text = stringResource(item.labelRes),
                                color = itemTint,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected || itemFocused) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                    }
                }
                }
            }
        }
        }
    }
}
