package com.anttech.prayertimes.features.settings.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PermissionGrantedAccent = Color(0xFF7FD6D8)
private val PermissionNeededAccent = Color(0xFFFFC46B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    notificationsGranted: Boolean,
    locationGranted: Boolean,
    exactAlarmsGranted: Boolean,
    onRequestNotifications: () -> Unit,
    onRequestLocation: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSizeMultiplier: Float = 1f
) {
    val scale = fontSizeMultiplier

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
                        text = "İzinler",
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
                Text(
                    text = "Vakit uyarıları ve konum özellikleri için gerekli izinler. " +
                        "İzinler cihaz ayarlarından da yönetilebilir.",
                    fontSize = (12.5f * scale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                SettingsSection(title = "İZİNLER") {
                    PermissionRow(
                        icon = Icons.Default.Notifications,
                        title = "Bildirimler",
                        description = "Vakit uyarılarını göstermek için gerekli",
                        granted = notificationsGranted,
                        actionLabel = if (notificationsGranted) "Açık" else "İzin Ver",
                        testTag = "permission_notifications_row",
                        statusTestTag = "permission_notifications_status",
                        scale = scale,
                        onAction = onRequestNotifications
                    )
                    SettingsDivider()
                    PermissionRow(
                        icon = Icons.Default.MyLocation,
                        title = "Konum",
                        description = "Şehri ve vakitleri otomatik bulmak için gerekli",
                        granted = locationGranted,
                        actionLabel = if (locationGranted) "Açık" else "İzin Ver",
                        testTag = "permission_location_row",
                        statusTestTag = "permission_location_status",
                        scale = scale,
                        onAction = onRequestLocation
                    )
                    SettingsDivider()
                    PermissionRow(
                        icon = Icons.Default.Alarm,
                        title = "Kesin alarm",
                        description = "Vakit uyarılarının dakikası dakikasına çalması için",
                        granted = exactAlarmsGranted,
                        actionLabel = "Ayarları Aç",
                        testTag = "permission_exact_alarm_row",
                        statusTestTag = "permission_exact_alarm_status",
                        scale = scale,
                        onAction = onOpenExactAlarmSettings
                    )
                }
            }

            item {
                SettingsSection(title = "UYGULAMA AYARLARI") {
                    SettingsItem(
                        icon = Icons.Default.ManageAccounts,
                        title = "Tüm izinleri yönet",
                        description = "Cihazın uygulama ayarlarını açar",
                        scale = scale,
                        modifier = Modifier.testTag("permission_open_app_settings"),
                        onClick = onOpenAppSettings
                    )
                }
            }

            item {
                Text(
                    text = "Bildirim izni yoksa vakit uyarıları gösterilmez. " +
                        "Kesin alarm izni kapalıysa uyarılar birkaç dakika gecikebilir.",
                    fontSize = (12.5f * scale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    testTag: String,
    statusTestTag: String,
    scale: Float,
    onAction: () -> Unit
) {
    val accent = if (granted) PermissionGrantedAccent else PermissionNeededAccent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accent
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = (16 * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = (12.5f * scale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            PermissionStatusPill(
                granted = granted,
                scale = scale,
                statusTestTag = statusTestTag
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onAction,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .testTag("${testTag}_action"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontSize = (13.5f * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (granted) PermissionGrantedAccent else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusPill(
    granted: Boolean,
    scale: Float,
    statusTestTag: String
) {
    val accent = if (granted) PermissionGrantedAccent else PermissionNeededAccent
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = accent
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (granted) "Açık" else "Gerekli",
            fontSize = (11.5f * scale).sp,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            modifier = Modifier.testTag(statusTestTag)
        )
    }
}

/**
 * İzin durumlarını cihazdan okuyup ilgili sistem diyaloglarını açan sarmalayıcı.
 */
@Composable
fun PermissionsRoute(
    onBackClick: () -> Unit,
    fontSizeMultiplier: Float = 1f
) {
    val context = LocalContext.current
    val notificationsGranted: MutableState<Boolean> =
        rememberPermissionState(::hasNotificationPermission)
    val locationGranted: MutableState<Boolean> = rememberPermissionState(::hasLocationPermission)
    val exactAlarmsGranted: MutableState<Boolean> = rememberPermissionState(::canScheduleExactAlarms)

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationsGranted.value = hasNotificationPermission(context)
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        locationGranted.value = hasLocationPermission(context)
    }

    PermissionsScreen(
        notificationsGranted = notificationsGranted.value,
        locationGranted = locationGranted.value,
        exactAlarmsGranted = exactAlarmsGranted.value,
        onRequestNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings(context)
            }
        },
        onRequestLocation = {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        onOpenExactAlarmSettings = { openExactAlarmSettings(context) },
        onOpenAppSettings = { openAppSettings(context) },
        onBackClick = onBackClick,
        fontSizeMultiplier = fontSizeMultiplier
    )
}

/** Bildirim ayarları ekranı için izin sarmalayıcı yardımcısı. */
@Composable
fun rememberNotificationPermission(): Pair<Boolean, () -> Unit> {
    val context: Context = LocalContext.current
    val state = rememberPermissionState(::hasNotificationPermission)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        state.value = hasNotificationPermission(context)
    }
    val request: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotificationSettings(context)
        }
    }
    return state.value to request
}
