package com.anttech.prayertimes.features.tracker.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anttech.prayertimes.core.ui.theme.BrandHeroGradient
import com.anttech.prayertimes.features.tracker.domain.DhikrPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    state: TrackerUiState,
    onIntent: (TrackerIntent) -> Unit,
    modifier: Modifier = Modifier,
    wrapInScaffold: Boolean = true
) {
    val context = LocalContext.current
    val scale = state.fontSizeMultiplier

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    Scaffold(
        topBar = {
            if (wrapInScaffold) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Zikirmatik",
                            fontSize = (20 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (wrapInScaffold) { ScaffoldDefaults.contentWindowInsets } else { WindowInsets(0, 0, 0, 0) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        DhikrTabContent(
            state = state,
            scale = scale,
            onIntent = onIntent,
            onVibrate = { duration ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration)
                }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * Gözü kapatıp ekrana dokunarak zikir çekmeyi sağlayan tam ekran dokunma alanı.
 *
 * Üstte seçili zikrin hero kartı (tur sayacı + hedef ilerlemesi), ortada hedefe
 * doğru dolan turuncu halkalı dokunma dairesi, altta sıfırlama butonu.
 */
@Composable
private fun DhikrTabContent(
    state: TrackerUiState,
    scale: Float,
    onIntent: (TrackerIntent) -> Unit,
    onVibrate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val target = state.selectedPreset.target
    val progress = if (target > 0) {
        (state.dhikrCount.toFloat() / target).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DhikrHeroCard(state = state, scale = scale)

        Spacer(modifier = Modifier.height(10.dp))

        ZikirPresetRow(
            presets = state.availablePresets,
            selectedId = state.selectedPreset.id,
            scale = scale,
            onSelect = { onIntent(TrackerIntent.SelectPreset(it)) }
        )

        Spacer(modifier = Modifier.weight(1f))

        DhikrDial(
            count = state.dhikrCount,
            target = target,
            progress = progress,
            scale = scale,
            onClick = {
                onIntent(TrackerIntent.IncrementDhikr)
                onVibrate(40)
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = { onIntent(TrackerIntent.ResetDhikr) },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .heightIn(min = 56.dp)
                .testTag("dhikr_reset_button"),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sıfırla",
                fontSize = (16 * scale).sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun DhikrHeroCard(
    state: TrackerUiState,
    scale: Float
) {
    val preset = state.selectedPreset
    val target = preset.target
    val progress = if (target > 0) {
        (state.dhikrCount.toFloat() / target).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(BrandHeroGradient))
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("dhikr_hero")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SEÇİLİ ZİKİR",
                fontSize = (10.5f * scale).sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.82f),
                letterSpacing = 1.4.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Tur ${state.dhikrLaps}",
                    fontSize = (12 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 11.dp, vertical = 4.dp)
                        .testTag("dhikr_laps_text")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = preset.arabic,
            fontSize = (26 * scale).sp,
            lineHeight = (34 * scale).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dhikr_arabic_text")
        )
        Text(
            text = preset.turkishTitle,
            fontSize = (15 * scale).sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.28f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (target > 0) "${state.dhikrCount} / $target" else "∞",
                fontSize = (12.5f * scale).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.testTag("dhikr_progress_text")
            )
        }
    }
}

@Composable
private fun ZikirPresetRow(
    presets: List<DhikrPreset>,
    selectedId: String,
    scale: Float,
    onSelect: (DhikrPreset) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dhikr_preset_row")
    ) {
        items(presets) { preset ->
            val isSelected = selectedId == preset.id
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(preset) },
                shape = RoundedCornerShape(50),
                label = {
                    Text(
                        text = preset.turkishTitle,
                        fontSize = (13 * scale).sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun DhikrDial(
    count: Int,
    target: Int,
    progress: Float,
    scale: Float,
    onClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 180),
        label = "dhikrProgress"
    )

    Box(
        modifier = Modifier.size(232.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = Color.White.copy(alpha = 0.06f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(BrandHeroGradient),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(194.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onClick()
                }
                .testTag("dhikr_tap_area"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = count.toString(),
                    fontSize = (62 * scale).sp,
                    lineHeight = (68 * scale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("dhikr_count_text")
                )
                Text(
                    text = if (target > 0) "Hedef: $target" else "Sınırsız",
                    fontSize = (14 * scale).sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
