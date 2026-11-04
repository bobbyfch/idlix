package com.idlix.tv.ui.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idlix.tv.data.repository.IdlixRepository
import com.idlix.tv.ui.screens.browse.BrowseChoice
import com.idlix.tv.ui.screens.browse.choicesFor
import com.idlix.tv.ui.screens.browse.catalogSections

internal typealias OpenBrowse = (section: String, value: String, type: String) -> Unit

private val gatewaySections = catalogSections.map { it.value to it.label }

/**
 * One website-aligned discovery taxonomy for Home, Movie and Series.
 * Tabs keep all possible choices reachable without filling the whole page.
 * The underlying /api/genres response may be partial, so fallback web genres
 * remain accessible. Network/country/year use bounded local catalog filtering.
 */
@Composable
internal fun MobileCatalogGateway(
    contentType: String,
    onBrowse: OpenBrowse,
    label: String = "Telusuri berdasarkan"
) {
    var section by rememberSaveable { mutableStateOf("genre") }
    val repository = remember { IdlixRepository() }
    var liveGenres by remember { mutableStateOf<List<BrowseChoice>>(emptyList()) }
    LaunchedEffect(repository) {
        liveGenres = repository.getAvailableGenres()
            .filter { it.slug.isNotBlank() && it.name.isNotBlank() }
            .map { BrowseChoice(it.name, it.slug) }
    }
    val choices = remember(section, liveGenres) {
        if (section == "genre") {
            (liveGenres + choicesFor("genre")).distinctBy { it.value.lowercase() }
                .sortedBy { it.label }
        } else choicesFor(section)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 9.dp)
            )
            TextButton(
                onClick = {
                    onBrowse(section, choices.firstOrNull()?.value.orEmpty(), contentType)
                }
            ) {
                Text("Lihat semua", fontSize = 12.sp, color = Color(0xFFD6D6DA))
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = Color(0xFFD6D6DA))
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gatewaySections, key = { it.first }) { (key, title) ->
                MobileFilterChip(
                    label = title,
                    selected = key == section,
                    onClick = { section = key }
                )
            }
        }

        if (choices.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(choices, key = { it.value }) { choice ->
                    MobileFilterChip(
                        label = choice.label,
                        selected = false,
                        onClick = { onBrowse(section, choice.value, contentType) }
                    )
                }
            }
        } else {
            Text(
                "Lihat daftar pilihan di Jelajah",
                color = Color(0xFFB0B0B7),
                modifier = Modifier.padding(horizontal = 18.dp),
                fontSize = 12.sp
            )
        }
    }
}
