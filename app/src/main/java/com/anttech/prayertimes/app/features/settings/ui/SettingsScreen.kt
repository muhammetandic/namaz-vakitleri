package com.anttech.prayertimes.features.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    isSimpleMode: Boolean,
    fontScale: Float = 1.0f,
    onSimpleModeToggle: () -> Unit,
    onFontScaleChange: (Float) -> Unit = {},
    onBackClick: () -> Unit,
    onSoundSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onCitySelectionClick: () -> Unit,
    onAboutClick: () -> Unit,
    onPermissionsClick: () -> Unit = {}
) {
    val scale = fontScale

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                title = {
                    Text(
                        text = "Ayarlar",
                        fontSize = (20 * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SettingsSection(title = "GÖRÜNÜM") {
                    SettingsSwitchRow(
                        icon = Icons.Default.Visibility,
                        title = "Sade Mod",
                        description = "Vakit ekranını sadeleştir",
                        checked = isSimpleMode,
                        scale = scale,
                        modifier = Modifier.testTag("settings_simple_mode"),
                        onCheckedChange = { onSimpleModeToggle() }
                    )
                    SettingsDivider()
                    SettingsFontScaleRow(
                        scale = scale,
                        value = fontScale,
                        onValueChange = onFontScaleChange,
                        modifier = Modifier.testTag("settings_font_scale")
                    )
                }
            }

            item {
                SettingsSection(title = "AYARLAR") {
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "Bildirimler",
                        description = "Vakit bildirimleri, titreşim ve test",
                        scale = scale,
                        modifier = Modifier.testTag("settings_notifications"),
                        onClick = onNotificationsClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Place,
                        title = "Şehir Seçimi",
                        description = "Yaşadığınız şehri seçin",
                        scale = scale,
                        modifier = Modifier.testTag("settings_city"),
                        onClick = onCitySelectionClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Headphones,
                        title = "Ezan ve Sesler",
                        description = "Vakitlere özel sesleri seçin",
                        scale = scale,
                        modifier = Modifier.testTag("settings_sounds"),
                        onClick = onSoundSettingsClick
                    )
                }
            }

            item {
                SettingsSection(title = "İZİNLER") {
                    SettingsItem(
                        icon = Icons.Default.VerifiedUser,
                        title = "İzinler",
                        description = "Bildirim, konum ve alarm izinleri",
                        scale = scale,
                        modifier = Modifier.testTag("settings_permissions"),
                        onClick = onPermissionsClick
                    )
                }
            }

            item {
                SettingsSection(title = "UYGULAMA") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Hakkında",
                        description = "Uygulama sürümü ve bilgiler",
                        scale = scale,
                        modifier = Modifier.testTag("settings_about"),
                        onClick = onAboutClick
                    )
                }
            }
        }
    }
}
