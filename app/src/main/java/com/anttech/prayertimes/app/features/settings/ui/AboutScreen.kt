package com.anttech.prayertimes.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anttech.prayertimes.R
import com.anttech.prayertimes.core.ui.theme.BrandHeroGradient

private const val FALLBACK_VERSION = "1.0.0"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    val versionName = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: FALLBACK_VERSION

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
                        text = "Hakkında",
                        fontSize = 20.sp,
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
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(Brush.linearGradient(BrandHeroGradient))
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Mosque,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = appName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Sürüm $versionName",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.testTag("about_version")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Vakitleri takip et, kıbleyi bul, imsakiyeyi incele ve zikir çek. " +
                            "Yaşlı dostu, sade ve çevrimdışı çalışan namaz uygulaması.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            item {
                AboutCard(
                    title = "Özellikler",
                    icon = Icons.Default.Mosque
                ) {
                    AboutLine("Günlük vakitler ve sonraki vakit geri sayımı")
                    AboutLine("Kıble pusulası ve hizalama titreşimi")
                    AboutLine("Aylık imsakiye, sahur/iftar ve oruç takibi")
                    AboutLine("Vakit ve sahur alarmları, özel ezan/MP3")
                    AboutLine("Ana ekran widget'ları")
                    AboutLine("Zikirmatik ve 33/99/100 tur sayaçları")
                    AboutLine("Sade Mod, büyük yazı ve erişilebilirlik")
                }
            }

            item {
                AboutCard(
                    title = "Veri Kaynağı",
                    icon = Icons.Default.Cloud
                ) {
                    Text(
                        text = "Vakitler, Diyanet İşleri Başkanlığı yöntemi (Aladhan, method 13) " +
                            "ile hesaplanır ve Room veritabanında cihazda saklanır. Ay sonuna " +
                            "yaklaşınca yeni ay otomatik indirilir; internet yokken son indirilen " +
                            "vakitler gösterilir.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                AboutCard(
                    title = "Gizlilik",
                    icon = Icons.Default.Lock
                ) {
                    Text(
                        text = "Şehir bilgisi yalnızca vakit hesabı için Aladhan API'sine gönderilir. " +
                            "Konum geçmişi saklanmaz; reklam, takip veya analiz yoktur.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Text(
                    text = "Sevgiyle geliştirildi.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun AboutCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun AboutLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("•", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
