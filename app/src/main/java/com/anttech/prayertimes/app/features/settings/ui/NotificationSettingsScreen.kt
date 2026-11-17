package com.anttech.prayertimes.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    state: NotificationSettingsUiState,
    hasNotificationPermission: Boolean,
    onIntent: (NotificationSettingsIntent) -> Unit,
    onBackClick: () -> Unit,
    onRequestPermission: () -> Unit,
    onBatteryOptimizationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = state.fontSizeMultiplier

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
                        text = "Bildirimler",
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
            if (!hasNotificationPermission) {
                item {
                    PermissionBanner(
                        scale = scale,
                        onRequestPermission = onRequestPermission
                    )
                }
            }

            item {
                SettingsSection(title = "VAKİT BİLDİRİMLERİ") {
                    SettingsSwitchRow(
                        icon = Icons.Default.Notifications,
                        title = "Vakit bildirimleri",
                        description = "Vakit girdiğinde bildirim gönder",
                        checked = state.notificationsEnabled,
                        scale = scale,
                        modifier = Modifier.testTag("notification_enabled_row"),
                        switchTestTag = "notification_enabled_switch",
                        onCheckedChange = { onIntent(NotificationSettingsIntent.ToggleNotifications) }
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        icon = Icons.Default.Vibration,
                        title = "Titreşim",
                        description = "Bildirimle birlikte telefon titresin",
                        checked = state.vibrationEnabled,
                        scale = scale,
                        modifier = Modifier.testTag("notification_vibration_row"),
                        switchTestTag = "notification_vibration_switch",
                        onCheckedChange = { onIntent(NotificationSettingsIntent.ToggleVibration) }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.BatteryAlert,
                        title = "Pil optimizasyonu",
                        description = "Arka planda vakit ve widget güncellemesi kısıtlanmasın",
                        scale = scale,
                        modifier = Modifier.testTag("notification_battery_row"),
                        onClick = onBatteryOptimizationClick
                    )
                }
            }

            item {
                SettingsSection(title = "BİLDİRİM TESTİ") {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "Bildirimlerin çalıştığını denetleyin",
                            fontSize = (15 * scale).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kısa bir deneme bildirimi gönderilir; ses ve titreşim ayarlarınız uygulanır.",
                            fontSize = (12.5f * scale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = { onIntent(NotificationSettingsIntent.SendTestNotification) },
                            enabled = hasNotificationPermission,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .testTag("notification_test_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Test bildirimi gönder",
                                fontSize = (15 * scale).sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (state.notificationsEnabled) {
                        "Telefon sesliyken yalnızca seçtiğiniz ses çalınır. Telefon sessiz veya " +
                            "titreşimdeyken yalnızca metin bildirimi gelir. Ses ve ses seviyesi " +
                            "Ayarlar > Ezan ve Sesler ekranından yönetilir."
                    } else {
                        "Vakit bildirimleri kapalı. Açtığınızda cihazınıza vakit uyarıları yeniden kurulur."
                    },
                    fontSize = (12.5f * scale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionBanner(
    scale: Float,
    onRequestPermission: () -> Unit
) {
    val accent = Color(0xFFFFC46B)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("notification_permission_banner")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Bildirim izni kapalı",
                fontSize = (15 * scale).sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "İzin verilmediği için vakit uyarıları gösterilemez.",
            fontSize = (12.5f * scale).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        FilledTonalButton(
            onClick = onRequestPermission,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("notification_permission_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "İzin Ver",
                fontSize = (14 * scale).sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
