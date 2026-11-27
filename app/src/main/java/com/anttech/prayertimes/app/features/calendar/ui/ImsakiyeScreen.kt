package com.anttech.prayertimes.features.calendar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anttech.prayertimes.core.ui.components.AppCard
import com.anttech.prayertimes.core.ui.components.SectionTitle
import com.anttech.prayertimes.core.ui.theme.BrandHeroGradient
import com.anttech.prayertimes.features.calendar.domain.FastTrackingDay
import com.anttech.prayertimes.features.calendar.domain.ImsakiyeDay
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HERO_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM EEEE", Locale.forLanguageTag("tr-TR"))
private val TRACKING_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr-TR"))

/** Solid white panels on the brand hero gradient, with deep burnt-orange type. */
private val TimeTileBackground = Color.White
private val TimeTileContent = Color(0xFFC2410C)
private val TimeTileLabel = Color(0xFF9A3412)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImsakiyeScreen(
    state: ImsakiyeUiState,
    onIntent: (ImsakiyeIntent) -> Unit,
    modifier: Modifier = Modifier,
    wrapInScaffold: Boolean = true
) {
    val scale = state.fontSizeMultiplier

    val content: @Composable (PaddingValues) -> Unit = { innerPadding ->
        when {
            state.isLoading -> LoadingContent(Modifier.padding(innerPadding))
            state.todayImsakiye != null -> ImsakiyeContent(state = state, scale = scale, onIntent = onIntent, modifier = Modifier.padding(innerPadding))
            else -> ErrorContent(
                message = "İmsakiye verisi yüklenemedi",
                scale = scale,
                onRetry = { onIntent(ImsakiyeIntent.LoadImsakiye(state.city, state.yearMonth)) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    if (wrapInScaffold) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.city,
                            fontSize = (20 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { onIntent(ImsakiyeIntent.ShareTodayImsakiye) },
                            modifier = Modifier.testTag("share_imsakiye_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Bugünün imsakiyesini paylaş",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { onIntent(ImsakiyeIntent.LoadImsakiye(state.city, state.yearMonth)) },
                            modifier = Modifier.testTag("refresh_imsakiye_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Yenile",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            content(innerPadding)
        }
    } else {
        content(PaddingValues(0.dp))
    }
}

@Composable
private fun ImsakiyeContent(
    state: ImsakiyeUiState,
    scale: Float,
    onIntent: (ImsakiyeIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        state.todayImsakiye?.let { today ->
            item {
                TodayImsakiyeCard(
                    today = today,
                    scale = scale,
                    onToggleFasted = {
                        onIntent(ImsakiyeIntent.ToggleFastedDay(
                            date = today.date,
                            hijriDay = today.hijriDay,
                            hijriMonth = today.hijriMonth,
                            hijriMonthName = today.hijriMonthName,
                            hijriYear = today.hijriYear
                        ))
                    }
                )
            }
        }

        item {
            IftarPrayerCard(scale = scale)
        }

        // Fasting Tracking Card
        item {
            FastTrackingCard(
                scale = scale,
                nonFastedRamadanDays = state.nonFastedRamadanDays,
                fastedNonRamadanDays = state.fastedNonRamadanDays,
                onToggleDay = { day ->
                    onIntent(ImsakiyeIntent.ToggleFastedDay(
                        date = day.date,
                        hijriDay = day.hijriDay,
                        hijriMonth = day.hijriMonth,
                        hijriMonthName = day.hijriMonthName,
                        hijriYear = day.hijriYear
                    ))
                }
            )
        }
    }
}

@Composable
private fun TodayImsakiyeCard(
    today: ImsakiyeDay,
    scale: Float,
    onToggleFasted: () -> Unit
) {
    // Compact hijri date display - both dates same size
    val hijriText = if (today.hijriMonthName.isNotBlank()) {
        "${today.hijriDay} ${today.hijriMonthName} ${today.hijriYear}"
    } else ""

    // Forbidden day check
    val isForbidden = today.isForbidden
    val forbiddenReason = today.forbiddenReason

    val contentColor = if (isForbidden) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface

    // Hero card with brand gradient (matching TimesScreen)
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isForbidden) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
            contentColor = if (isForbidden) MaterialTheme.colorScheme.onErrorContainer else Color.White
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .then(if (isForbidden) Modifier else Modifier.background(Brush.linearGradient(BrandHeroGradient)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
            // Date row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = today.date.format(HERO_DATE_FORMATTER)
                            .replaceFirstChar { it.titlecase(Locale.forLanguageTag("tr-TR")) },
                        fontSize = (14 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isForbidden) contentColor else Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (hijriText.isNotBlank()) {
                        Text(
                            text = hijriText,
                            fontSize = (14 * scale).sp,
                            color = if (isForbidden) contentColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                if (!isForbidden) {
                    // Toggle chip - translucent pill on gradient
                    Row(
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = if (today.isFasted) 0.95f else 0.18f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = if (today.isFasted) 0f else 0.35f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { onToggleFasted() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (today.isFasted) Icons.Default.CheckCircle else Icons.Default.NightsStay,
                            contentDescription = when {
                                today.hijriMonth == 9 && today.isFasted -> "Ramazan orucu tutulmadı olarak işaretli"
                                today.hijriMonth == 9 -> "Ramazan orucu tutulmadı olarak işaretle"
                                today.isFasted -> "Nafile oruç işaretli"
                                else -> "Oruç işaretle"
                            },
                            tint = if (today.isFasted) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                today.hijriMonth == 9 && today.isFasted -> "Tutulmadı"
                                today.hijriMonth == 9 -> "Tutulmadı işaretle"
                                today.isFasted -> "Nafile oruç"
                                else -> "Oruç işaretle"
                            },
                            fontSize = (12 * scale).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (today.isFasted) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }
            }

            // Forbidden day indicator
            if (isForbidden) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = forbiddenReason.ifBlank { "Bugün oruç tutulmaz" },
                        fontSize = (13 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Imsak and Iftar times on translucent tiles
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TimeTile(
                        modifier = Modifier.weight(1f),
                        label = "Sahur (İmsak)",
                        time = today.imsakFormatted,
                        scale = scale
                    )
                    TimeTile(
                        modifier = Modifier.weight(1f),
                        label = "İftar (Akşam)",
                        time = today.aksamFormatted,
                        scale = scale
                    )
                }
            }
        }
    }
}
}

@Composable
private fun TimeTile(
    label: String,
    time: String,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TimeTileBackground)
            .padding(horizontal = 10.dp, vertical = 14.dp)
    ) {
        Text(
            text = label.uppercase(Locale.forLanguageTag("tr-TR")),
            fontSize = (12 * scale).sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            color = TimeTileLabel.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = time,
            fontSize = (32 * scale).sp,
            fontWeight = FontWeight.Bold,
            color = TimeTileContent
        )
    }
}

@Composable
private fun IftarPrayerCard(scale: Float) {
    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Dua text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "İftar Duası",
                    fontSize = (16 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.WbTwilight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "اَللّٰهُمَّ اِنِّي لَكَ صُمْتُ وَبِكَ اٰمَنْتُ وَعَلَيْكَ تَوَكَّلْتُ وَعَلٰى رِزْقِكَ اَفْطَرْتُ",
                fontSize = (16 * scale).sp,
                lineHeight = (30 * scale).sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
            Text(
                text = "\"Allah'ım, senin için oruç tuttum, sana iman ettim, sana güvendim ve senin rızığınla iftar ettim.\"",
                fontSize = (12 * scale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun FastTrackingCard(
    scale: Float,
    nonFastedRamadanDays: List<FastTrackingDay>,
    fastedNonRamadanDays: List<FastTrackingDay>,
    onToggleDay: (FastTrackingDay) -> Unit
) {
    val allMarkedDays = nonFastedRamadanDays + fastedNonRamadanDays

    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Oruç Takip",
                    fontSize = (16 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tutulmayan Ramazan günü sayacı - yalnızca kayıt varsa
                    if (nonFastedRamadanDays.isNotEmpty()) {
                        TrackingCountChip(
                            text = "Tutulmadı ${nonFastedRamadanDays.size}",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            scale = scale
                        )
                    }
                    // Nafile count chip - only when there are nafile days
                    if (fastedNonRamadanDays.isNotEmpty()) {
                        TrackingCountChip(
                            text = "Nafile oruç ${fastedNonRamadanDays.size}",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            scale = scale
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bölüm: Ramazan orucu tutulmayan günler (kırmızı)
            if (nonFastedRamadanDays.isNotEmpty()) {
                SectionTitle(text = "${nonFastedRamadanDays.size} tutulmadı", modifier = Modifier.padding(start = 4.dp, bottom = 4.dp).fillMaxWidth())
                nonFastedRamadanDays.forEach { day ->
                    TrackingRow(
                        day = day,
                        scale = scale,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        badgeText = "Tutulmadı",
                        badgeColor = MaterialTheme.colorScheme.error,
                        onClick = { onToggleDay(day) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Section: Nafl days (teal)
            if (fastedNonRamadanDays.isNotEmpty()) {
                SectionTitle(text = "${fastedNonRamadanDays.size} nafile oruç", modifier = Modifier.padding(start = 4.dp, bottom = 4.dp).fillMaxWidth())
                fastedNonRamadanDays.forEach { day ->
                    TrackingRow(
                        day = day,
                        scale = scale,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        badgeText = "Nafile oruç",
                        badgeColor = MaterialTheme.colorScheme.tertiary,
                        onClick = { onToggleDay(day) }
                    )
                }
            }

            // Empty state
            if (allMarkedDays.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Henüz işaretli gün yok",
                        fontSize = (14 * scale).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ramazanda oruç tutmadığınız günleri,\nDiğer aylarda tuttuğunuz nafile oruçları işaretleyin.",
                        fontSize = (12 * scale).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackingCountChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    scale: Float
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            fontSize = (11 * scale).sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun TrackingRow(
    day: FastTrackingDay,
    scale: Float,
    containerColor: Color,
    contentColor: Color,
    badgeText: String,
    badgeColor: Color,
    onClick: (() -> Unit)?,
    isReadOnly: Boolean = false
) {
    val actualContainerColor = if (isReadOnly) containerColor.copy(alpha = 0.5f) else containerColor
    val actualContentColor = if (isReadOnly) MaterialTheme.colorScheme.onSurfaceVariant else contentColor
    val clickAction = onClick?.takeIf { !isReadOnly }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .then(if (clickAction != null) Modifier.clickable { clickAction() } else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = actualContainerColor,
            contentColor = actualContentColor
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (!isReadOnly) BorderStroke(1.dp, containerColor.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = day.date.format(TRACKING_DATE_FORMATTER),
                    fontSize = (13 * scale).sp,
                    fontWeight = FontWeight.Medium,
                    color = actualContentColor
                )
                Text(
                    text = "${day.hijriDay} ${day.hijriMonthName} ${day.hijriYear}",
                    fontSize = (11 * scale).sp,
                    color = actualContentColor.copy(alpha = 0.75f)
                )
            }
            Surface(
                color = if (isReadOnly) badgeColor.copy(alpha = 0.5f) else badgeColor,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = badgeText,
                    fontSize = (11 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isReadOnly) badgeColor.copy(alpha = 0.7f) else Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "İmsakiye yükleniyor...",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    scale: Float,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                fontSize = (14 * scale).sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            androidx.compose.material3.Button(onClick = onRetry) {
                Text(text = "Tekrar Dene", fontSize = (14 * scale).sp)
            }
        }
    }
}