package com.idlix.tv.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.idlix.tv.ui.theme.NetflixWhite
import com.idlix.tv.ui.theme.NetflixLightGray
import com.idlix.tv.ui.theme.NetflixDarkCard
import com.idlix.tv.ui.theme.NetflixDarkGray
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val orderOptions = listOf(
    "newest" to "Terbaru", "popular" to "Populer",
    "rating" to "Rating", "az" to "A–Z", "oldest" to "Terlama"
)
private val typeOptions = listOf(
    "all" to "Semua", "movie" to "Film", "series" to "Serial"
)

/**
 * One context row replaces three pill rails. Full section, choice, sort and
 * content-type filters remain in an accessible scrollable bottom sheet.
 * Sorting/type changes remain local and do not trigger network reload.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileExploreControls(
    section: String,
    value: String,
    sort: String,
    filterType: String,
    sections: List<Pair<String, String>>,
    choices: List<Pair<String, String>>,
    onSectionSelected: (String) -> Unit,
    onChoiceSelected: (String) -> Unit,
    onSortSelected: (String) -> Unit,
    onTypeSelected: (String) -> Unit
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(section) { query = "" }

    val sectionName = sections.firstOrNull { it.first == section }?.second ?: "Genre"
    val choiceName = choices.firstOrNull { it.first == value }?.second
    val currentSort = orderOptions.firstOrNull { it.first == sort }?.second ?: "Terbaru"
    val typeSuffix = typeOptions.firstOrNull { it.first == filterType }?.second
        ?.takeUnless { filterType == "all" }
    val summary = listOfNotNull(sectionName, choiceName, typeSuffix).joinToString(" · ")

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            when (section) {
                "network" -> choiceName?.plus(" Originals") ?: "Network"
                "shorts" -> "Shorts"
                else -> "Jelajah"
            }, color = NetflixWhite, fontSize = 25.sp, fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(49.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(NetflixDarkGray)
                    .clickable { showFilters = true }
                    .padding(horizontal = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Tune, null, tint = NetflixWhite, modifier = Modifier.size(19.dp))
                Text(
                    summary,
                    modifier = Modifier.weight(1f),
                    color = NetflixWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
            }
            Box {
                Row(
                    modifier = Modifier
                        .height(49.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(NetflixDarkGray)
                        .clickable { showSortMenu = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(currentSort, color = NetflixWhite, fontSize = 12.sp, maxLines = 1)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.LightGray, modifier = Modifier.size(17.dp))
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(NetflixDarkCard)
                ) {
                    orderOptions.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = NetflixWhite) },
                            onClick = {
                                showSortMenu = false
                                onSortSelected(id)
                            },
                            trailingIcon = if (id == sort) {
                                { Icon(Icons.Default.Check, null, tint = NetflixWhite) }
                            } else null
                        )
                    }
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            containerColor = NetflixDarkCard,
            contentColor = NetflixWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 610.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Filter Jelajah", color = NetflixWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showFilters = false }) {
                        Text("Terapkan", color = NetflixWhite)
                    }
                }
                Text("Jelajah berdasarkan", color = NetflixLightGray, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(sections, key = { it.first }) { (id, label) ->
                        MobileFilterChip(
                            label = label,
                            selected = section == id,
                            onClick = { onSectionSelected(id) }
                        )
                    }
                }
                if (choices.isNotEmpty()) {
                    Text("Pilih ${sectionName.lowercase()}", color = NetflixLightGray, fontSize = 13.sp)
                    if (choices.size > 12) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Cari ${sectionName.lowercase()}") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp)
                        )
                    }
                    val matched = choices.filter {
                        query.isBlank() || it.second.contains(query, ignoreCase = true)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 224.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(matched, key = { it.first }) { (id, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(13.dp))
                                    .clickable { onChoiceSelected(id) }
                                    .padding(horizontal = 15.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, color = NetflixWhite, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                if (id == value) {
                                    Icon(Icons.Default.Check, null, tint = NetflixWhite, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
                Text("Jenis tayangan", color = NetflixLightGray, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(typeOptions, key = { it.first }) { (id, label) ->
                        MobileFilterChip(label, selected = filterType == id, onClick = { onTypeSelected(id) })
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
