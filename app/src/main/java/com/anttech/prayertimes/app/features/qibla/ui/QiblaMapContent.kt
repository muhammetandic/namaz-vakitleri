package com.anttech.prayertimes.features.qibla.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anttech.prayertimes.core.ui.theme.BrandOrange
import com.anttech.prayertimes.features.qibla.domain.CalculateQiblaAngleUseCase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.roundToInt

/** AndroidView geri çağrıları arasında paylaşılan harita ve katman referansları. */
private class QiblaMapHolder {
    var mapView: MapView? = null
    var userMarker: Marker? = null
    var kaabaMarker: Marker? = null
    var qiblaLine: Polyline? = null
}

@Composable
fun QiblaMapContent(
    userLat: Double,
    userLng: Double,
    cityName: String,
    qiblaBearing: Float,
    fontScale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val holder = remember { QiblaMapHolder() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val userPoint = remember(userLat, userLng) { GeoPoint(userLat, userLng) }
    val kaabaPoint = remember {
        GeoPoint(CalculateQiblaAngleUseCase.KAABA_LATITUDE, CalculateQiblaAngleUseCase.KAABA_LONGITUDE)
    }

    // Harita yaşam döngüsü ekranın yaşam döngüsüne bağlanır: yeniden çizim
    // sırasında onResume() çağrılmaz, ekran durunca onPause() çalışır.
    DisposableEffect(lifecycleOwner, holder) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> holder.mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> holder.mapView?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            holder.mapView?.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            holder.mapView?.onPause()
            holder.mapView?.onDetach()
            holder.mapView = null
            holder.userMarker = null
            holder.kaabaMarker = null
            holder.qiblaLine = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("qibla_map_container")
    ) {
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true

                    // Kullanıcı Konum İşaretçisi
                    val userMarker = Marker(this).apply {
                        position = userPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = if (cityName.isNotBlank()) "Konumunuz: $cityName" else "Konumunuz"
                    }
                    overlays.add(userMarker)

                    // Kâbe İşaretçisi
                    val kaabaMarker = Marker(this).apply {
                        position = kaabaPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Kâbe-i Muazzama (Mekke)"
                    }
                    overlays.add(kaabaMarker)

                    // Kıble Doğrusu (Kullanıcı -> Kâbe)
                    val qiblaLine = Polyline().apply {
                        addPoint(userPoint)
                        addPoint(kaabaPoint)
                        outlinePaint.color = BrandOrange.toArgb()
                        outlinePaint.strokeWidth = 10f
                        isGeodesic = true
                    }
                    overlays.add(qiblaLine)

                    controller.setZoom(17.5)
                    controller.setCenter(userPoint)

                    holder.mapView = this
                    holder.userMarker = userMarker
                    holder.kaabaMarker = kaabaMarker
                    holder.qiblaLine = qiblaLine

                    // Harita oluşturulduğunda ekran zaten görünürse hemen başlat.
                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        onResume()
                    }
                }
            },
            update = { mapView ->
                // Konum değişince işaretçiler ve çizgi güncellenir.
                holder.userMarker?.apply {
                    position = userPoint
                    title = if (cityName.isNotBlank()) "Konumunuz: $cityName" else "Konumunuz"
                }
                holder.qiblaLine?.apply {
                    setPoints(listOf(userPoint, kaabaPoint))
                }
                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Üst Kılavuz Kartı
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Kıble Açısı: ${qiblaBearing.roundToInt()}°",
                    fontSize = (20 * fontScale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Turuncu çizgi bulunduğunuz noktadan Kâbe'ye uzanır. Sokak ve binanızın yönünü bu çizgiye göre hizalayabilirsiniz.",
                    fontSize = (12.5f * fontScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Hızlı Odaklanma Butonları
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Konumuma Odaklan (Yakın)
            SmallFloatingActionButton(
                onClick = {
                    holder.mapView?.controller?.apply {
                        setZoom(17.5)
                        animateTo(userPoint)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("map_recenter_user_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Konumuma Odaklan"
                )
            }

            // Kâbe Rotasını Göster (Geniş Bakış)
            SmallFloatingActionButton(
                onClick = {
                    holder.mapView?.controller?.apply {
                        setZoom(5.5)
                        animateTo(userPoint)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("map_overview_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CropFree,
                    contentDescription = "Kâbe Rotasını Göster"
                )
            }
        }
    }
}
