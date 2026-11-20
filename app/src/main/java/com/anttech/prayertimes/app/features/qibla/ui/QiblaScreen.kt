package com.anttech.prayertimes.features.qibla.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anttech.prayertimes.core.ui.theme.ActivePrayerGlow
import com.anttech.prayertimes.core.ui.theme.WarmGoldDark
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    state: QiblaUiState,
    onIntent: (QiblaIntent) -> Unit,
    modifier: Modifier = Modifier,
    fontScale: Float = 1.0f,
    wrapInScaffold: Boolean = true
) {
    val context = LocalContext.current

    // Titreşim efekti tetikleyicisi
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    LaunchedEffect(state.isAligned) {
        if (state.isAligned) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(120)
            }
        }
    }

    Scaffold(
        topBar = {
            if (wrapInScaffold) {
            TopAppBar(
                title = {
                    Text(
                        text = "Kıble Pusulası",
                        fontSize = (20 * fontScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
            }
        },
        contentWindowInsets = if (wrapInScaffold) { ScaffoldDefaults.contentWindowInsets } else { WindowInsets(0, 0, 0, 0) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Üst Bilgi Kartı (Açı ve Şehir)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (state.cityName.isNotEmpty()) state.cityName else "Konum Aranıyor...",
                        fontSize = (18 * fontScale).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Kıble Açısı: ${state.qiblaBearing.roundToInt()}°",
                        fontSize = (26 * fontScale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("qibla_bearing_text")
                    )
                }
            }

            // Kıble Durum Bildirimi (Yeşil Parlama)
            val ringColor by animateColorAsState(
                targetValue = if (state.isAligned) ActivePrayerGlow else MaterialTheme.colorScheme.outlineVariant,
                label = "RingColor"
            )

            if (!state.isSensorAvailable) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qibla_no_sensor_warning"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Manyetik Pusula Sensörü Yok",
                                fontSize = (15 * fontScale).sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cihazınızda pusula (manyetometre) sensörü bulunmadığı için canlı ibre dönemiyor. Kıble yönü olarak yukarıdaki ${state.qiblaBearing.roundToInt()}° açısını kullanabilirsiniz.",
                                fontSize = (12.5f * fontScale).sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            } else if (state.isAligned) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("qibla_aligned_indicator")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Kıble Bulundu",
                        tint = ActivePrayerGlow,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kıbleye Yöneldiniz",
                        fontSize = (22 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = ActivePrayerGlow
                    )
                }
            } else {
                Text(
                    text = "Pusulayı çevirerek yeşil göstergeyi hedefleyin",
                    fontSize = (15 * fontScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Ana Pusula Kadranı
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(width = if (state.isAligned) 6.dp else 3.dp, color = ringColor, shape = CircleShape)
                    .testTag("compass_dial"),
                contentAlignment = Alignment.Center
            ) {
                // Pusula Dereceleri & Harfler
                Text(
                    text = "K", // Kuzey
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                )

                // Dönen Kıble İbresi
                val animatedAngle by animateFloatAsState(
                    targetValue = state.needleAngle,
                    label = "NeedleAngle"
                )

                CompassNeedle(
                    rotationDegrees = animatedAngle,
                    isAligned = state.isAligned,
                    centerColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(240.dp)
                        .testTag("compass_needle")
                )
            }

            // Alt Bilgi & Kalibrasyon Notu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Pusula bilgisi",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isSensorAvailable) {
                        "Pusula sapması durumunda telefonunuzu havada 8 şeklinde hareket ettiriniz."
                    } else {
                        "Harita veya analog pusulada Kuzey (0°) yönünü bulunuz, telefonu ${state.qiblaBearing.roundToInt()}° açısına çeviriniz."
                    },
                    fontSize = (12 * fontScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompassNeedle(
    rotationDegrees: Float,
    isAligned: Boolean,
    centerColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.rotate(rotationDegrees)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val needleLength = size.height * 0.42f
        val needleWidth = 24f

        // Kabe Yönünü Gösteren İbre (Yeşil / Altın Sarısı)
        val northPath = Path().apply {
            moveTo(centerX, centerY - needleLength)
            lineTo(centerX + needleWidth / 2, centerY)
            lineTo(centerX - needleWidth / 2, centerY)
            close()
        }
        drawPath(
            path = northPath,
            color = if (isAligned) ActivePrayerGlow else WarmGoldDark
        )

        // Arka Kuyruk
        val southPath = Path().apply {
            moveTo(centerX, centerY + needleLength * 0.6f)
            lineTo(centerX + needleWidth / 2, centerY)
            lineTo(centerX - needleWidth / 2, centerY)
            close()
        }
        drawPath(
            path = southPath,
            color = Color.Gray.copy(alpha = 0.5f)
        )

        // Merkez Kabe Noktası
        drawCircle(
            color = if (isAligned) ActivePrayerGlow else centerColor,
            radius = 16f,
            center = Offset(centerX, centerY)
        )
    }
}
