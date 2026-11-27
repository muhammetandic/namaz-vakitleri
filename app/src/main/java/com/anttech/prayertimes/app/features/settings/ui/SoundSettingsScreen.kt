package com.anttech.prayertimes.features.settings.ui

import com.anttech.prayertimes.core.notifications.rawSoundResId

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anttech.prayertimes.core.ui.theme.ActivePrayerGlow
import com.anttech.prayertimes.features.settings.domain.SoundOption
import com.anttech.prayertimes.features.times.domain.PrayerType
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSettingsScreen(
    state: SoundSettingsUiState,
    onIntent: (SoundSettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scale = state.fontSizeMultiplier
    val currentOnIntent by rememberUpdatedState(onIntent)
    var isSelectingSound by remember { mutableStateOf(false) }
    var isSelectingSahurSound by remember { mutableStateOf(false) }
    var selectedTarget by remember { mutableStateOf<PrayerType?>(null) }
    val prayerTypes = PrayerType.values().toList()
    val loadedSounds = prayerTypes.mapNotNull { state.prayerSounds[it] }
    val allSound = loadedSounds.firstOrNull()?.takeIf { sound ->
        loadedSounds.size == prayerTypes.size && loadedSounds.all { it.id == sound.id }
    }

    // Ekrandan ayrılınca önizleme sesini durdur
    DisposableEffect(Unit) {
        onDispose { currentOnIntent(SoundSettingsIntent.StopPreview) }
    }

    // Cihazdan MP3/Ses dosyası seçici başlatıcı. Uri izni yeniden başlatmada da korunur.
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            var fileName = "Özel Ezan Sesi"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            onIntent(
                SoundSettingsIntent.CustomAudioPicked(
                    prayerType = selectedTarget,
                    uriString = uri.toString(),
                    displayName = fileName
                )
            )
            isSelectingSound = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSelectingSound) isSelectingSound = false else onBackClick()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                title = {
                    Text(
                        text = if (isSelectingSound) {
                            selectedTarget?.let { "${it.turkishName} İçin Ses Seç" } ?: "Tüm Vakitler İçin Ses Seç"
                        } else {
                            "Ezan ve Uyarı Sesleri"
                        },
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
        if (isSelectingSahurSound) {
            SoundPickerContent(
                state = state,
                prayerType = null,
                isSahurAlarm = true,
                onPickCustomAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                onSelectSound = { soundOption ->
                    onIntent(SoundSettingsIntent.SelectSahurAlarmSound(soundOption))
                    isSelectingSahurSound = false
                },
                onIntent = onIntent,
                modifier = Modifier.padding(innerPadding)
            )
        } else if (isSelectingSound) {
            SoundPickerContent(
                state = state,
                prayerType = selectedTarget,
                onPickCustomAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                onSelectSound = { soundOption ->
                    if (selectedTarget == null) {
                        onIntent(SoundSettingsIntent.ApplySoundToAll(soundOption))
                    } else {
                        onIntent(SoundSettingsIntent.SelectSound(selectedTarget!!, soundOption))
                    }
                    isSelectingSound = false
                },
                onIntent = onIntent,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            SoundOverviewContent(
                state = state,
                prayerTypes = prayerTypes,
                allSound = allSound,
                onSelect = { prayerType ->
                    onIntent(SoundSettingsIntent.StopPreview)
                    selectedTarget = prayerType
                    isSelectingSound = true
                },
                onSelectSahurSound = {
                    onIntent(SoundSettingsIntent.StopPreview)
                    isSelectingSahurSound = true
                },
                onIntent = onIntent,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun SoundOverviewContent(
    state: SoundSettingsUiState,
    prayerTypes: List<PrayerType>,
    allSound: SoundOption?,
    onSelect: (PrayerType?) -> Unit,
    onSelectSahurSound: () -> Unit,
    onIntent: (SoundSettingsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = state.fontSizeMultiplier
    val context = LocalContext.current
    val rows = listOf("Tüm Vakitler" to null) + prayerTypes.map { it.turkishName to it }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            AppVolumeCard(
                volume = state.globalVolume,
                scale = scale,
                onVolumeChange = { onIntent(SoundSettingsIntent.SetGlobalVolume(it)) }
            )
        }
        item {
            SahurAlarmCard(
                enabled = state.sahurAlarmEnabled,
                minutesBefore = state.sahurAlarmMinutesBefore,
                options = state.sahurAlarmMinuteOptions,
                scale = scale,
                sahurAlarmSound = state.sahurAlarmSound,
                onToggle = { onIntent(SoundSettingsIntent.ToggleSahurAlarm) },
                onSelectMinutes = { onIntent(SoundSettingsIntent.SetSahurAlarmMinutes(it)) },
                onSelectSound = onSelectSahurSound,
                state = state,
                onIntent = onIntent
            )
        }
        items(rows.size) { index ->
            if (index == 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            val (title, prayerType) = rows[index]
            val sound = prayerType?.let { state.prayerSounds[it] } ?: allSound
            val soundAvailable = sound?.isPreviewable(context) == true
            val delayMinutes = prayerType?.let { state.prayerDelays[it] }
                ?: state.prayerDelays.values.distinct().singleOrNull()
            val baseDescription = sound?.let {
                if (it is SoundOption.BuiltIn && !soundAvailable) "${it.title} · Ses dosyası bulunamadı" else it.title
            } ?: if (prayerType == null) {
                if (state.prayerSounds.size == prayerTypes.size) "Vakitlere göre değişiyor" else "Sesler yükleniyor"
            } else {
                "Ses seçilmedi"
            }
            val description = if ((delayMinutes ?: 0) > 0) {
                "$baseDescription · ${delayMinutes} dk gecikmeli"
            } else {
                baseDescription
            }
            SoundTargetRow(
                title = title,
                soundDescription = description,
                soundOption = sound,
                isPlaying = soundAvailable && state.currentlyPlayingTargetId == (prayerType?.name ?: "ALL"),
                isPreviewable = soundAvailable,
                scale = scale,
                testTag = "sound_target_${prayerType?.name ?: "ALL"}",
                onSelect = { onSelect(prayerType) },
                onPlay = {
                    sound?.takeIf { soundAvailable }?.let {
                        onIntent(SoundSettingsIntent.PlayPreview(it, prayerType?.name ?: "ALL"))
                    }
                },
                onStop = { onIntent(SoundSettingsIntent.StopPreview) }
            )
        }
    }
}

@Composable
private fun SoundTargetRow(
    title: String,
    soundDescription: String,
    soundOption: SoundOption?,
    isPlaying: Boolean,
    isPreviewable: Boolean,
    scale: Float,
    testTag: String,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = (16 * scale).sp, fontWeight = FontWeight.Bold)
                Text(
                    soundDescription,
                    fontSize = (13 * scale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            FilledIconButton(
                onClick = onSelect,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("select_sound_${testTag.removePrefix("sound_target_")}"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = "Ses seç")
            }
            Spacer(modifier = Modifier.width(8.dp))
            PreviewPlayButton(
                soundOption = soundOption,
                isPlaying = isPlaying,
                isPreviewable = isPreviewable,
                onPlay = onPlay,
                onStop = onStop
            )
        }
    }
}

@Composable
private fun SoundPickerContent(
    state: SoundSettingsUiState,
    prayerType: PrayerType?,
    isSahurAlarm: Boolean = false,
    onPickCustomAudio: () -> Unit,
    onSelectSound: (SoundOption) -> Unit,
    onIntent: (SoundSettingsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = state.fontSizeMultiplier
    val context = LocalContext.current
    val currentSound = if (isSahurAlarm) {
        state.sahurAlarmSound
    } else {
        prayerType?.let { state.prayerSounds[it] } ?: run {
            val sounds = PrayerType.values().mapNotNull { state.prayerSounds[it] }
            sounds.firstOrNull()?.takeIf { first -> sounds.size == PrayerType.values().size && sounds.all { it.id == first.id } }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = if (isSahurAlarm) {
                    "Sahur uyandırma alarmı için ses seçin."
                } else if (prayerType == null) {
                    "Seçilen ses tüm vakitlere uygulanır."
                } else {
                    "${prayerType.turkishName} vakti için ses seçin."
                },
                fontSize = (15 * scale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isSahurAlarm) {
            item {
                val currentDelay: Int? = prayerType?.let { state.prayerDelays[it] ?: 0 }
                    ?: state.prayerDelays.values.distinct().singleOrNull()
                DelaySelector(
                    currentDelayMinutes = currentDelay,
                    options = state.delayOptionsMinutes,
                    scale = scale,
                    appliesToAll = prayerType == null,
                    onSelect = { onIntent(SoundSettingsIntent.SelectDelay(prayerType, it)) }
                )
            }
        }

        item {
            Button(
                onClick = onPickCustomAudio,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .testTag("custom_audio_picker_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Özel ses dosyası ekle")
                Spacer(modifier = Modifier.width(10.dp))
                Text("Telefonumdan MP3 / Ses Seç", fontSize = (17 * scale).sp, fontWeight = FontWeight.Bold)
            }
        }

        // Seçili Vaktin Aktif Sesi Özeti
        if (currentSound is SoundOption.CustomFile) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ActivePrayerGlow.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Yüklü Özel Ses: ${currentSound.title}",
                        modifier = Modifier.padding(14.dp),
                        fontSize = (16 * scale).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Dahili Ezan & Melodi Listesi
        items(state.availableBuiltInSounds) { soundOption ->
            val isSelectable = soundOption.isSelectableInPicker(context)
            val isPreviewable = soundOption.isPreviewable(context)
            val isSelected = currentSound?.id == soundOption.id
            val isPlaying = isPreviewable && state.currentlyPlayingTargetId == "picker:${soundOption.id}"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isSelectable) 1f else 0.4f)
                    .clickable(enabled = isSelectable) { onSelectSound(soundOption) }
                    .testTag("sound_item_${soundOption.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceContainer
                ),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectSound(soundOption) },
                        enabled = isSelectable
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = soundOption.title,
                            fontSize = (16 * scale).sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (!isSelectable) {
                            Text(
                                text = "Ses dosyası bulunamadı",
                                fontSize = (12 * scale).sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    PreviewPlayButton(
                        soundOption = soundOption,
                        isPlaying = isPlaying,
                        isPreviewable = isPreviewable,
                        onPlay = {
                            onIntent(SoundSettingsIntent.PlayPreview(soundOption, "picker:${soundOption.id}"))
                        },
                        onStop = { onIntent(SoundSettingsIntent.StopPreview) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppVolumeCard(
    volume: Float,
    scale: Float,
    onVolumeChange: (Float) -> Unit
) {
    val percent = (volume * 100).roundToInt()
    val isMuted = volume == 0f
    val iconName = when {
        isMuted -> Icons.AutoMirrored.Filled.VolumeOff
        percent < 30 -> Icons.AutoMirrored.Filled.VolumeDown
        percent < 70 -> Icons.AutoMirrored.Filled.VolumeUp
        else -> Icons.AutoMirrored.Filled.VolumeUp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_volume_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header row with icon, label, percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconName,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Uygulama Ses Seviyesi",
                        fontSize = (16 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Sistem sesinden bağımsız; yalnızca vakit seslerini etkiler",
                        fontSize = (11 * scale).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "%$percent",
                    fontSize = (18 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Slider
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                steps = 100,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("global_volume_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    activeTickColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    inactiveTickColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )
            )
        }
    }
}

@Composable
private fun SahurAlarmCard(
    enabled: Boolean,
    minutesBefore: Int,
    options: List<Int>,
    scale: Float,
    sahurAlarmSound: SoundOption?,
    onToggle: () -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onSelectSound: () -> Unit,
    state: SoundSettingsUiState,
    onIntent: (SoundSettingsIntent) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sahur_alarm_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NightsStay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sahur Uyandırma",
                        fontSize = (16 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = if (enabled) {
                            "İmsaktan $minutesBefore dakika önce alarm çalar"
                        } else {
                            "Oruç için sahurda uyanmak isterseniz açın"
                        },
                        fontSize = (11.5f * scale).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("sahur_alarm_switch")
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "İmsaktan ne kadar önce?",
                    fontSize = (14 * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("sahur_alarm_minutes")
                ) {
                    items(options) { minutes ->
                        FilterChip(
                            selected = minutesBefore == minutes,
                            onClick = { onSelectMinutes(minutes) },
                            label = {
                                Text(text = "$minutes dk", fontSize = (14 * scale).sp)
                            },
                            modifier = Modifier.testTag("sahur_alarm_option_$minutes")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "İmsak vakti için seçtiğiniz ses çalınır.",
                    fontSize = (11 * scale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Sahur alarmı ses seçimi - always visible when enabled
            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                SahurAlarmSoundRow(
                    currentSound = sahurAlarmSound,
                    scale = scale,
                    onSelect = onSelectSound,
                    isPlaying = state.currentlyPlayingTargetId == "SAHUR",
                    onPlay = { sahurAlarmSound?.let { onIntent(SoundSettingsIntent.PlayPreview(it, "SAHUR")) } },
                    onStop = { onIntent(SoundSettingsIntent.StopPreview) }
                )
            }
        }
    }
}

@Composable
private fun SahurAlarmSoundRow(
    currentSound: SoundOption?,
    scale: Float,
    onSelect: () -> Unit,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    val soundAvailable = currentSound?.isPreviewable(context) == true
    val soundDescription = currentSound?.let {
        if (it is SoundOption.BuiltIn && !soundAvailable) "${it.title} · Ses dosyası bulunamadı" else it.title
    } ?: "Varsayılan (İmsak sesi)"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sahur_sound_row"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Alarm Sesi",
                    fontSize = (16 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = soundDescription,
                    fontSize = (13 * scale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            FilledIconButton(
                onClick = onSelect,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("sahur_sound_select"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = "Ses seç")
            }
            Spacer(modifier = Modifier.width(8.dp))
            PreviewPlayButton(
                soundOption = currentSound,
                isPlaying = soundAvailable && isPlaying,
                isPreviewable = soundAvailable,
                onPlay = onPlay,
                onStop = onStop
            )
        }
    }
}

@Composable
private fun DelaySelector(
    currentDelayMinutes: Int?,
    options: List<Int>,
    scale: Float,
    appliesToAll: Boolean,
    onSelect: (Int) -> Unit
) {
    Column {
        Text(
            text = if (appliesToAll) "Ses gecikmesi (tüm vakitler)" else "Ses gecikmesi",
            fontSize = (15 * scale).sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Vakit girdikten sonra ses bu kadar dakika sonra çalar.",
            fontSize = (12 * scale).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("delay_selector")
        ) {
            items(options) { minutes ->
                FilterChip(
                    selected = currentDelayMinutes == minutes,
                    onClick = { onSelect(minutes) },
                    label = {
                        Text(
                            text = if (minutes == 0) "Yok" else "$minutes dk",
                            fontSize = (14 * scale).sp
                        )
                    },
                    modifier = Modifier.testTag("delay_option_$minutes")
                )
            }
        }
    }
}

@Composable
private fun PreviewPlayButton(
    soundOption: SoundOption?,
    isPlaying: Boolean,
    isPreviewable: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
    FilledIconButton(
        onClick = { if (isPlaying) onStop() else onPlay() },
        enabled = soundOption != null && isPreviewable,
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
        )
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Sesi Durdur" else "Sesi Dinle"
        )
    }
}

private fun SoundOption.isPreviewable(context: android.content.Context): Boolean = when (this) {
    is SoundOption.BuiltIn -> rawSoundResId(resName) != 0
    is SoundOption.CustomFile -> true
    SoundOption.Silent, SoundOption.VibrateOnly -> false
}

private fun SoundOption.isSelectableInPicker(context: android.content.Context): Boolean = when (this) {
    is SoundOption.BuiltIn -> isPreviewable(context)
    is SoundOption.CustomFile, SoundOption.Silent, SoundOption.VibrateOnly -> true
}
