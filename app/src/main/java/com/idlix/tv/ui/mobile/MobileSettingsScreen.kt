package com.idlix.tv.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idlix.tv.BuildConfig
import com.idlix.tv.data.api.ApiClient
import com.idlix.tv.data.local.SubtitleAppearance
import com.idlix.tv.data.local.SubtitlePosition
import com.idlix.tv.data.local.SubtitleSize
import com.idlix.tv.data.local.TvPreferencesStore
import com.idlix.tv.data.local.AppColorMode
import com.idlix.tv.data.local.AppLayoutMode
import com.idlix.tv.ui.components.UpdateChecker
import com.idlix.tv.data.local.VideoResizeMode
import com.idlix.tv.data.repository.SubdlRepository
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private val FieldShape = RoundedCornerShape(16.dp)

/** Standard materials for content, never overlapping glass cards. */
@Composable
private fun SettingsSection(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
        }
    }
}

@Composable
private fun SettingChoices(
    label: String,
    options: List<Pair<String, String>>,
    current: String,
    onChoose: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == current }?.second ?: current
    Box(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(label, style = MaterialTheme.typography.titleMedium)
            },
            supportingContent = {
                Text(
                    currentLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            trailingContent = {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Pilih $label",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(role = Role.Button) { expanded = true },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        expanded = false
                        onChoose(id)
                    },
                    trailingIcon = if (current == id) {
                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(label, style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = {
            Text(description, style = MaterialTheme.typography.bodyMedium)
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = null)
        },
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SettingSegmentedChoices(
    label: String,
    options: List<Pair<String, String>>,
    current: String,
    enabled: Boolean = true,
    onChoose: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (id, name) ->
                SegmentedButton(
                    selected = current == id,
                    enabled = enabled,
                    onClick = { onChoose(id) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun MobileSettingsScreen() {
    val context = LocalContext.current
    val preferences by TvPreferencesStore.preferences.collectAsState()
    var url by remember { mutableStateOf(ApiClient.getBaseUrl()) }
    var key by remember { mutableStateOf(SubdlRepository.getApiKey()) }
    var saveMessage by remember { mutableStateOf("") }
    var saveError by remember { mutableStateOf(false) }
    var showApiKey by remember { mutableStateOf(false) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
    )

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 880.dp)
            .statusBarsPadding()
            .padding(top = 62.dp),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 20.dp, bottom = 112.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "Pengaturan",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "Atur tampilan, pemutaran, dan sumber katalog.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            SettingsSection(
                title = "Tampilan aplikasi",
                subtitle = "TV untuk remote, Mobile untuk sentuh, Tablet adaptif portrait/landscape."
            ) {
                SettingChoices(
                    label = "Layout Aplikasi",
                    options = listOf(
                        AppLayoutMode.AUTO.name to "Otomatis",
                        AppLayoutMode.TV.name to "TV",
                        AppLayoutMode.MOBILE.name to "Mobile",
                        AppLayoutMode.TABLET.name to "Tablet"
                    ),
                    current = preferences.layoutMode.name,
                    onChoose = { selection ->
                        AppLayoutMode.values().firstOrNull { it.name == selection }?.let {
                            TvPreferencesStore.setLayoutMode(context, it)
                        }
                    }
                )
                Text(
                    "Otomatis menyesuaikan perangkat. Pilihan manual langsung mengubah " +
                        "tampilan dan mengembalikan kamu ke beranda.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp
                )
                SettingSegmentedChoices(
                    label = "Tema",
                    options = listOf(
                        AppColorMode.LIGHT.name to "Terang",
                        AppColorMode.DARK.name to "Gelap",
                        AppColorMode.OLED.name to "OLED"
                    ),
                    current = preferences.colorMode.name,
                    enabled = !preferences.materialYouEnabled,
                    onChoose = { selection ->
                        AppColorMode.values().firstOrNull { it.name == selection }?.let {
                            TvPreferencesStore.setColorMode(context, it)
                        }
                    }
                )
                SettingSwitchRow(
                    label = "Material You",
                    description = "Ikuti warna wallpaper dan mode sistem (Android 12+).",
                    checked = preferences.materialYouEnabled,
                    onCheckedChange = { TvPreferencesStore.setMaterialYouEnabled(context, it) }
                )
                SettingSwitchRow(
                    label = "Efek blur navigasi",
                    description = "Efek transparan dapat menambah penggunaan daya.",
                    checked = preferences.mobileGlassEnabled,
                    onCheckedChange = { TvPreferencesStore.setMobileGlassEnabled(context, it) }
                )
                SettingSwitchRow(
                    label = "Status bar HP",
                    description = "Tampilkan ikon waktu, jaringan, dan baterai di layar.",
                    checked = preferences.mobileStatusBarEnabled,
                    onCheckedChange = { TvPreferencesStore.setMobileStatusBarEnabled(context, it) }
                )
                Text(
                    "Pilihan tema manual aktif saat Material You dimatikan. OLED memakai hitam murni dengan aksen merah IDLIX.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            SettingsSection(
                title = "Tampilan video",
                subtitle = "Terapkan rasio video saat menonton tanpa mengubah file sumber."
            ) {
                SettingChoices(
                    label = "Penyesuaian layar",
                    options = listOf(
                        VideoResizeMode.FIT.name to "Fit",
                        VideoResizeMode.ZOOM.name to "Fill / Zoom",
                        VideoResizeMode.FILL.name to "Stretch",
                        VideoResizeMode.FIXED_WIDTH.name to "Lebar",
                        VideoResizeMode.FIXED_HEIGHT.name to "Tinggi"
                    ),
                    current = preferences.videoResizeMode.name,
                    onChoose = { id ->
                        VideoResizeMode.values().firstOrNull { it.name == id }?.let {
                            TvPreferencesStore.setVideoResizeMode(context, it)
                        }
                    }
                )
            }
        }
        item {
            SettingsSection(
                title = "Subtitle",
                subtitle = "Ukuran, gaya, dan posisi disimpan otomatis."
            ) {
                SettingChoices(
                    label = "Tampilan",
                    options = listOf(
                        SubtitleAppearance.SYSTEM.name to "Sistem",
                        SubtitleAppearance.OUTLINE.name to "Garis Tepi",
                        SubtitleAppearance.SHADOW.name to "Bayangan",
                        SubtitleAppearance.SOFT_BOX.name to "Kotak Lembut"
                    ),
                    current = preferences.subtitleAppearance.name,
                    onChoose = { id ->
                        SubtitleAppearance.values().firstOrNull { it.name == id }?.let {
                            TvPreferencesStore.setSubtitleAppearance(context, it)
                        }
                    }
                )
                SettingChoices(
                    label = "Ukuran",
                    options = listOf(
                        SubtitleSize.SMALL.name to "Kecil",
                        SubtitleSize.STANDARD.name to "Normal",
                        SubtitleSize.LARGE.name to "Besar"
                    ),
                    current = preferences.subtitleSize.name,
                    onChoose = { id ->
                        SubtitleSize.values().firstOrNull { it.name == id }?.let {
                            TvPreferencesStore.setSubtitleSize(context, it)
                        }
                    }
                )
                SettingChoices(
                    label = "Posisi",
                    options = listOf(
                        SubtitlePosition.LOW.name to "Bawah",
                        SubtitlePosition.STANDARD.name to "Normal",
                        SubtitlePosition.HIGH.name to "Atas"
                    ),
                    current = preferences.subtitlePosition.name,
                    onChoose = { id ->
                        SubtitlePosition.values().firstOrNull { it.name == id }?.let {
                            TvPreferencesStore.setSubtitlePosition(context, it)
                        }
                    }
                )
            }
        }
        item {
            SettingsSection(
                title = "Sumber dan koneksi",
                subtitle = "Jangan bagikan API key milikmu kepada orang lain."
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; saveMessage = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Alamat katalog") },
                    singleLine = true,
                    shape = FieldShape,
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it; saveMessage = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("SubDL API key") },
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "Sembunyikan API key" else "Tampilkan API key"
                            )
                        }
                    },
                    shape = FieldShape,
                    colors = fieldColors
                )
                Button(
                    onClick = {
                        val parsed = url.trim().toHttpUrlOrNull()
                        if (parsed == null) {
                            saveError = true
                            saveMessage = "Alamat backend harus berupa URL HTTP atau HTTPS yang valid."
                        } else {
                            val changed = ApiClient.getBaseUrl() != parsed.toString()
                            ApiClient.setBaseUrl(parsed.toString())
                            SubdlRepository.setApiKey(key)
                            url = ApiClient.getBaseUrl()
                            saveError = false
                            saveMessage = if (changed) {
                                "Tersimpan. Buka ulang aplikasi untuk memperbarui katalog."
                            } else "Pengaturan disimpan."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = FieldShape,
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Text(
                        "  Simpan Pengaturan",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(
                    onClick = {
                        url = BuildConfig.BACKEND_API_URL
                        key = SubdlRepository.DEFAULT_API_KEY
                        saveError = false
                        saveMessage = "Nilai bawaan dipilih. Tekan Simpan untuk menerapkan."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pulihkan nilai bawaan", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                if (saveMessage.isNotBlank()) {
                    Text(
                        saveMessage,
                        color = if (saveError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }
            }
        }
        item {
            SettingsSection(
                title = "Tentang IDLIX",
                subtitle = "Satu aplikasi native untuk Android HP dan Android TV."
            ) {
                Text(
                    "Versi ${BuildConfig.VERSION_NAME} · Build ${BuildConfig.VERSION_CODE}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                UpdateChecker(tv = false)
                Text(
                    "Dikembangkan oleh Bobby Fajar Christian",
                    color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Preferensi dan riwayat tersimpan di perangkat ini.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    "Artwork dan informasi film dapat bersumber dari TMDB dan Cinemeta. " +
                        "This product uses the TMDB API but is not endorsed or certified by TMDB.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
    }
}
