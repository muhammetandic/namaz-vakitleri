package com.anttech.prayertimes.features.times.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anttech.prayertimes.core.ui.components.PrayerTimeRow
import com.anttech.prayertimes.core.ui.theme.BrandHeroGradient
import com.anttech.prayertimes.features.times.domain.KerahaResult
import com.anttech.prayertimes.features.times.domain.KerahaType
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.times.domain.PrayerType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM EEEE", Locale.forLanguageTag("tr-TR"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimesScreen(
    state: TimesUiState,
    onIntent: (TimesIntent) -> Unit,
    modifier: Modifier = Modifier,
    wrapInScaffold: Boolean = true
) {
    val scale = state.fontSizeMultiplier

    val content: @Composable (PaddingValues) -> Unit = { innerPadding ->
        when {
            state.isLoading -> LoadingContent(Modifier.padding(innerPadding))
            state.prayerTimes != null -> {
                if (state.isSimpleMode) {
                    SimpleModeContent(state = state, scale = scale, modifier = Modifier.padding(innerPadding))
                } else {
                    StandardModeContent(state = state, scale = scale, modifier = Modifier.padding(innerPadding))
                }
            }
            else -> ErrorContent(
                message = state.errorMessage ?: "Vakit verisi yüklenemedi",
                scale = scale,
                onRetry = { onIntent(TimesIntent.RefreshLocation) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    if (wrapInScaffold) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        CityTitle(city = state.city, scale = scale)
                    },
                    actions = {
                        IconButton(
                            onClick = { onIntent(TimesIntent.RefreshLocation) },
                            modifier = Modifier.testTag("refresh_location_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Mevcut Konumu Bul",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        FilledTonalButton(
                            onClick = { onIntent(TimesIntent.ToggleSimpleMode) },
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .testTag("toggle_simple_mode_button"),
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Text(
                                text = if (state.isSimpleMode) "Standart Mod" else "Sade Mod",
                                fontSize = (13 * scale).sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            content(innerPadding)
        }
    } else {
        content(PaddingValues())
    }
}

@Composable
private fun CityTitle(city: String, scale: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = "Şehir konumu",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = city,
            fontSize = (20 * scale).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    message: String,
    scale: Float,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            fontSize = (18 * scale).sp,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, shape = MaterialTheme.shapes.extraLarge) {
            Text("Yeniden Dene")
        }
    }
}

@Composable
private fun StandardModeContent(
    state: TimesUiState,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val times = state.prayerTimes ?: return
    val countdown = state.countdown
    val rows = prayerRows(times)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        item {
            CountdownHero(
                nextPrayerName = countdown?.nextPrayer?.turkishName ?: "--",
                formattedCountdown = countdown?.formattedCountdown ?: "--:--:--",
                progress = countdown?.progress ?: 0f,
                dateText = DATE_FORMATTER.format(times.date)
                    .replaceFirstChar { it.titlecase(Locale.forLanguageTag("tr-TR")) },
                keraha = state.keraha,
                scale = scale,
                countdownTag = "standard_countdown_text",
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        item {
            PrayerTimesCard(
                rows = rows,
                activePrayer = countdown?.activePrayer,
                scale = scale,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun PrayerTimesCard(
    rows: List<PrayerRowItem>,
    activePrayer: PrayerType?,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    thickness = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                )
            }
            PrayerRowContent(
                name = row.prayerType.turkishName,
                time = row.time.format(TIME_FORMATTER),
                icon = row.prayerType.icon(),
                isActive = activePrayer == row.prayerType,
                scale = scale
            )
        }
    }
}

@Composable
private fun PrayerRowContent(
    name: String,
    time: String,
    icon: ImageVector,
    isActive: Boolean,
    scale: Float
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(
                if (isActive) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    Color.Transparent
                }
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            },
            shape = CircleShape,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .padding(7.dp)
                    .size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            fontSize = (17 * scale).sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (isActive) {
            Text(
                text = "ŞİMDİ",
                fontSize = (9 * scale).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = time,
            fontSize = (20 * scale).sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun CountdownHero(
    nextPrayerName: String,
    formattedCountdown: String,
    progress: Float,
    dateText: String,
    keraha: KerahaResult?,
    scale: Float,
    countdownTag: String,
    modifier: Modifier = Modifier
) {
    val isKeraha = keraha?.isKeraha == true

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(BrandHeroGradient))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dateText,
                fontSize = (13.5f * scale).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.95f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Kerahat chip yalnızca kerahat vaktinde görünür
            if (isKeraha) {
                Row(
                    modifier = Modifier
                        .testTag("kerahat_badge")
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFFFE29A))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFF5A3A00),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${keraha.type.shortTitle()} · ${keraha.minutesRemaining} dk",
                        fontSize = (10.5f * scale).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF5A3A00),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "SIRADAKİ VAKİT",
            fontSize = (10.5f * scale).sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.78f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = nextPrayerName,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        contentDescription = "Sıradaki vakit: $nextPrayerName"
                    }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formattedCountdown,
                fontSize = (28 * scale).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.testTag(countdownTag)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.25f)
        )
    }
}

private fun KerahaType.shortTitle(): String = when (this) {
    KerahaType.SUNRISE -> "Sabah kerahati"
    KerahaType.MIDDAY -> "Öğle kerahati"
    KerahaType.SUNSET -> "İkindi kerahati"
    KerahaType.NONE -> "Kerahat yok"
}

@Composable
private fun SimpleModeContent(
    state: TimesUiState,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val times = state.prayerTimes ?: return
    val countdown = state.countdown
    val rows = prayerRows(times)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(Brush.linearGradient(BrandHeroGradient))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${countdown?.nextPrayer?.turkishName ?: ""} Vaktine Kalan:",
                fontSize = (20 * scale).sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = countdown?.formattedCountdown ?: "--:--:--",
                fontSize = (40 * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.testTag("simple_countdown_text")
            )
            state.keraha?.takeIf { it.isKeraha }?.let { keraha ->
                Spacer(modifier = Modifier.height(8.dp))
                SimpleKerahaChip(keraha = keraha, scale = scale)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rows.size) { index ->
                val row = rows[index]
                PrayerTimeRow(
                    name = row.prayerType.turkishName,
                    time = row.time.format(TIME_FORMATTER),
                    icon = row.prayerType.icon(),
                    isActive = countdown?.activePrayer == row.prayerType,
                    scale = scale
                )
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SimpleKerahaChip(keraha: KerahaResult, scale: Float) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFFFE29A))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFF5A3A00),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${keraha.type.shortTitle()} · ${keraha.minutesRemaining} dk",
            fontSize = (11 * scale).sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF5A3A00)
        )
    }
}

private data class PrayerRowItem(
    val prayerType: PrayerType,
    val time: LocalTime
)

private fun prayerRows(times: PrayerTimes): List<PrayerRowItem> = listOf(
    PrayerRowItem(PrayerType.IMSAK, times.imsak),
    PrayerRowItem(PrayerType.GUNES, times.gunes),
    PrayerRowItem(PrayerType.OGLE, times.ogle),
    PrayerRowItem(PrayerType.IKINDI, times.ikindi),
    PrayerRowItem(PrayerType.AKSAM, times.aksam),
    PrayerRowItem(PrayerType.YATSI, times.yatsi)
)

private fun PrayerType.icon(): ImageVector = when (this) {
    PrayerType.IMSAK -> Icons.Default.NightsStay
    PrayerType.GUNES -> Icons.Default.WbSunny
    PrayerType.OGLE -> Icons.Default.LightMode
    PrayerType.IKINDI -> Icons.Default.WbTwilight
    PrayerType.AKSAM -> Icons.Default.Bedtime
    PrayerType.YATSI -> Icons.Default.DarkMode
}