package com.anttech.prayertimes.features.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectionScreen(
    currentCity: String,
    onCitySelected: (String) -> Unit,
    onUseLocation: () -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedCityData = remember(currentCity) {
        if (currentCity.contains(",")) {
            val parts = currentCity.split(",")
            val city = parts[0].trim()
            val district = if (parts.size > 1) parts[1].trim() else null
            city to district
        } else {
            currentCity to null
        }
    }

    var query by remember { mutableStateOf("") }
    val cities = remember(query) { filterCities(query) }
    var selectedCity by remember { mutableStateOf<String?>(selectedCityData.first) }
    var showDistricts by remember { mutableStateOf(false) }
    var selectedDistrict by remember { mutableStateOf<String?>(selectedCityData.second) }

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
                        text = "Şehir Seçimi",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("city_search"),
                placeholder = { Text("Şehir ara", fontSize = 16.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Ara")
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable(onClick = onUseLocation)
                    .testTag("use_location_button"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Konumumu Kullan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bulunduğun şehri otomatik algıla",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (showDistricts) {
                // District selection screen
                CityDetailRow(
                    city = selectedCity ?: "",
                    district = selectedDistrict,
                    onBackClick = { showDistricts = false }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable {
                            // Görünen adıyla aynı biçimde kaydet; ilçe seçildiyse dahil et
                            val city = selectedCity ?: ""
                            val displayName =
                                if (!selectedDistrict.isNullOrBlank() && selectedDistrict != city) {
                                    "$city, $selectedDistrict"
                                } else {
                                    city
                                }
                            onCitySelected(displayName)
                        }
                        .testTag("save_selection"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Seçili Yeri Kullan",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val displayText = if (!selectedCity.isNullOrBlank()) {
                                if (!selectedDistrict.isNullOrBlank() && selectedDistrict != selectedCity) {
                                    "$selectedCity, $selectedDistrict"
                                } else {
                                    selectedCity
                                }
                            } else {
                                selectedCity ?: ""
                            }
                            Text(
                                text = "Seçili Yeri Kullan: $displayText",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (selectedCity != null) {
                    val districts = TURKISH_CITIES_WITH_DISTRICTS
                        .find { it.city == selectedCity }
                        ?.districts ?: emptyList()
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(districts, key = { it }) { district ->
                            DistrictRow(
                                district = district,
                                isSelected = district == selectedDistrict,
                                onClick = { selectedDistrict = district }
                            )
                        }
                    }
                }
            } else {
                // City selection screen
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(cities, key = { it }) { city ->
                        CityRow(
                            city = city,
                            isSelected = city == selectedCityData.first,
                            onClick = {
                                // Farklı bir şehre geçerken eski şehrin ilçesi taşınmasın
                                selectedCity = city
                                selectedDistrict = null
                                showDistricts = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CityDetailRow(
    city: String,
    district: String?,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Geri",
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = city,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = district?.let { "İlçe: $it" } ?: "İlçe Seçin",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CityRow(
    city: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .testTag("city_item_$city"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = city,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DistrictRow(
    district: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .testTag("district_item_$district"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = district,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seçili ilçe",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
