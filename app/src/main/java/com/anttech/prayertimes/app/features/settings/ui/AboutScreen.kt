package com.anttech.prayertimes.features.settings.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anttech.prayertimes.R
import com.anttech.prayertimes.core.ui.theme.BrandHeroGradient
import kotlinx.coroutines.launch

private const val FALLBACK_VERSION = "1.0.0"
private const val FALLBACK_BUILD = 1L
private const val CONTACT_EMAIL = "muhammet.andic@gmail.com"

private data class AppInfo(
    val versionName: String,
    val versionCode: Long,
    val packageName: String
)

@Suppress("DEPRECATION")
private fun readAppInfo(context: Context): AppInfo {
    val info = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }.getOrNull()
    val versionCode = when {
        info == null -> FALLBACK_BUILD
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> info.longVersionCode
        else -> info.versionCode.toLong()
    }
    return AppInfo(
        versionName = info?.versionName ?: FALLBACK_VERSION,
        versionCode = versionCode,
        packageName = context.packageName
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val appInfo = remember(context) { readAppInfo(context) }
    val appName = stringResource(R.string.app_name)
    val emailIntent = remember(context) {
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$CONTACT_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, "Namazgâh geri bildirim")
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        text = "Sürüm ${appInfo.versionName}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.testTag("about_version")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Namaz vakitlerini, kıble yönünü ve imsakiyeyi tek yerde sunar. " +
                            "İnternet olmadan da çalışır; ses, bildirim ve yazı boyutu " +
                            "cihazınıza göre ayarlanır.",
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
                    AboutLine("Vakitler, kerahat zamanları ve sonraki vakit geri sayımı")
                    AboutLine("Pusula ve harita görünümüyle kıble yönü, hizalama titreşimi")
                    AboutLine("Aylık imsakiye, sahur-iftar sayacı ve oruç takip çetelesi")
                    AboutLine("Vakit ve sahur alarmları, ses/gecikme ayarı ve özel MP3 ezanlar")
                    AboutLine("Küçük ve büyük ana ekran widget'ları, canlı geri sayım")
                    AboutLine("Tam ekran zikirmatik, 33/99/100 hedefi ve titreşimli geri bildirim")
                    AboutLine("Sade Mod, dinamik yazı boyutu ve 56 dp dokunma alanları")
                }
            }

            item {
                AboutCard(
                    title = "Uygulama Bilgisi",
                    icon = Icons.Default.Info
                ) {
                    AboutLine(
                        text = "Derleme ${appInfo.versionCode}",
                        modifier = Modifier.testTag("about_build")
                    )
                    AboutLine(
                        text = "Paket ${appInfo.packageName}",
                        modifier = Modifier.testTag("about_package")
                    )
                    AboutLine("Minimum Android 8.0 (API 26)")
                }
            }

            item {
                AboutCard(
                    title = "Veri Kaynağı",
                    icon = Icons.Default.Cloud
                ) {
                    Text(
                        text = "Vakitler Aladhan API'sinden Diyanet yöntemiyle (method 13) alınır " +
                            "ve cihazdaki Room veritabanına kaydedilir. Ay sonuna yaklaşınca " +
                            "otomatik güncellenir; bağlantı yokken son kaydedilen veri gösterilir.",
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
                        text = "Konum yalnızca cihazda şehir seçimi için kullanılır. Aladhan " +
                            "isteğinde şehir adı ve ülke gönderilir; konum geçmişi, reklam, " +
                            "takip veya analiz yoktur.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.Button) {
                            runCatching { context.startActivity(emailIntent) }
                                .onFailure {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("E-posta uygulaması bulunamadı")
                                    }
                                }
                        }
                        .testTag("about_contact_email"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "E-posta gönder",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Geri bildirim ve öneriler",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = CONTACT_EMAIL,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
    icon: ImageVector,
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
private fun AboutLine(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("•", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
    }
}
