package com.idlix.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.idlix.tv.data.local.TvPreferencesStore
import com.idlix.tv.data.repository.IdlixRepository
import com.idlix.tv.ui.screens.browse.BrowseChoice
import com.idlix.tv.ui.screens.browse.choicesFor
import com.idlix.tv.ui.screens.browse.catalogSections
import com.idlix.tv.ui.theme.NetflixWhite

private val tvGatewaySections = catalogSections.map { it.value to it.label }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCatalogGateway(
    contentType: String,
    onBrowse: (section: String, value: String, type: String) -> Unit
) {
    val repository = remember { IdlixRepository() }
    val preferences by TvPreferencesStore.preferences.collectAsState()
    var selectedSection by remember { mutableStateOf("genre") }
    var liveGenres by remember { mutableStateOf<List<BrowseChoice>>(emptyList()) }

    LaunchedEffect(repository) {
        liveGenres = repository.getAvailableGenres()
            .filter { it.slug.isNotBlank() && it.name.isNotBlank() }
            .map { BrowseChoice(it.name, it.slug) }
    }
    val choices = remember(selectedSection, liveGenres) {
        if (selectedSection == "genre") {
            (liveGenres + choicesFor("genre")).distinctBy { it.value.lowercase() }
                .sortedBy { it.label }
        } else choicesFor(selectedSection)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Text(
            text = "Temukan berdasarkan",
            color = NetflixWhite,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(preferences.glassIntensity, RoundedCornerShape(22.dp))
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tvGatewaySections, key = { it.first }) { (key, label) ->
                    val selected = key == selectedSection
                    Button(
                        onClick = { selectedSection = key },
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(13.dp)),
                        colors = ButtonDefaults.colors(
                            containerColor = if (selected) Color.White.copy(alpha = 0.18f)
                            else Color.Transparent,
                            contentColor = NetflixWhite,
                            focusedContainerColor = Color.White,
                            focusedContentColor = Color.Black
                        )
                    ) { Text(label, fontSize = 14.sp) }
                }
            }
            if (choices.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(choices, key = { it.value }) { choice ->
                        Button(
                            onClick = { onBrowse(selectedSection, choice.value, contentType) },
                            shape = ButtonDefaults.shape(shape = RoundedCornerShape(13.dp)),
                            colors = ButtonDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.09f),
                                contentColor = NetflixWhite,
                                focusedContainerColor = Color.White,
                                focusedContentColor = Color.Black
                            )
                        ) {
                            Text(choice.label, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                Button(
                    onClick = { onBrowse(selectedSection, "", contentType) },
                    modifier = Modifier.padding(horizontal = 18.dp)
                ) { Text("Lihat ${selectedSection.replaceFirstChar { it.uppercase() }}") }
            }
        }
    }
}
