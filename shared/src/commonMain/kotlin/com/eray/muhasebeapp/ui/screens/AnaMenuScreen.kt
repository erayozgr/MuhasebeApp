package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.PlatformDatabaseManager
import com.eray.muhasebeapp.rememberDosyaPaylasici

data class MenuButonModel(
    val baslik: String,
    val ikon: ImageVector,
    val tiklamaAksiyonu: () -> Unit
)

// 🎨 Uygulama teması: BizimHesap tarzı gradyan (turkuaz -> lacivert)
private val ArkaPlanGradyan = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF13B0A5), // üstte turkuaz
        Color(0xFF0E7C8C),
        Color(0xFF14406B),
        Color(0xFF0B1F3A)  // altta koyu lacivert
    )
)

private val BeyazYariSeffaf = Color.White.copy(alpha = 0.14f)
private val BeyazCizgi = Color.White.copy(alpha = 0.22f)

@Composable
fun AnaMenuScreen(
    database: AppDatabase,
    platformDbManager: PlatformDatabaseManager, // 🎯 KMP platform yöneticisi
    onNavigateToUrunler: () -> Unit,
    onNavigateToMusteriler: () -> Unit,
    onNavigateToTedarikciler: () -> Unit,
    onNavigateToSatis: () -> Unit,
    onNavigateToAlis: () -> Unit,
    onNavigateToMasraf: () -> Unit,
    onNavigateToRaporlama: () -> Unit,
    onNavigateToStok: () -> Unit,
    onYedekYukleIstegi: () -> Unit, // 🎯 UI dışından (FilePicker tetiklemek için) lambda fonksiyonu
    guncelTarih: String
) {
    val urunler = remember(database) {
        database.appDatabaseQueries
            .selectAllUrun()
            .executeAsList()
    }
    val dosyaPaylasici = rememberDosyaPaylasici()

    val tumKritikUrunler = urunler.filter { it.stokAdedi <= 5L }
    val kritikStoklarGosterim = tumKritikUrunler.sortedBy { it.stokAdedi }.take(5)

    var uyarıMesaji by remember { mutableStateOf("") }
    var diyalogAcikMi by remember { mutableStateOf(false) }

    // 🎯 BizimHesap tarzı: tek renkli ikonlar, arka plan gradyan üstünde beyaz daireler
    val menuButonlari = listOf(
        MenuButonModel("Müşteriler", Icons.Default.Groups, onNavigateToMusteriler),
        MenuButonModel("Tedarikçiler", Icons.Default.LocalShipping, onNavigateToTedarikciler),
        MenuButonModel("Ürünler", Icons.Default.Sell, onNavigateToUrunler),
        MenuButonModel("Satışlar", Icons.Default.ShoppingCart, onNavigateToSatis),
        MenuButonModel("Alışlar", Icons.Default.Balance, onNavigateToAlis),
        MenuButonModel("Masraflar", Icons.Default.AttachMoney, onNavigateToMasraf),
        MenuButonModel("Stoklar", Icons.Default.Storage, onNavigateToStok),
        MenuButonModel("Raporlar", Icons.Default.BarChart, onNavigateToRaporlama)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArkaPlanGradyan)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ÜST BAŞLIK
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "Hoş geldiniz",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Muhasebe",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            TarihBari(tarih = guncelTarih)
            Spacer(modifier = Modifier.height(20.dp))

            // 1. BÖLÜM: HIZLI İŞLEMLER — daire ikonlu grid (BizimHesap tarzı)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .padding(horizontal = 12.dp)
            ) {
                items(menuButonlari) { buton ->
                    MenuButonItem(model = buton)
                }
            }

            // 2. BÖLÜM: KRİTİK STOK UYARILARI
            if (tumKritikUrunler.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "KRİTİK STOK UYARILARI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BeyazYariSeffaf),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onNavigateToUrunler() }
                ) {
                    Column {
                        kritikStoklarGosterim.forEachIndexed { index, urun ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB74D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(urun.ad, fontSize = 15.sp, color = Color.White)
                                }
                                Text(
                                    text = "${urun.stokAdedi} ${urun.birim}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFFB74D)
                                )
                            }
                            if (index < kritikStoklarGosterim.lastIndex) {
                                HorizontalDivider(color = BeyazCizgi, thickness = 1.dp, modifier = Modifier.padding(start = 40.dp))
                            }
                        }
                    }
                }
            }

            // 3. BÖLÜM: VERİ GÜVENLİĞİ VE YEDEKLEME
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "VERİ GÜVENLİĞİ VE BULUT YEDEK",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BeyazYariSeffaf),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .clickable {
                            val dbBytes = platformDbManager.getDatabaseBytes(database)
                            if (dbBytes != null) {
                                dosyaPaylasici.paylasBytes("muhasebe_yedek.db", dbBytes)
                            } else {
                                uyarıMesaji = "Yedek dosyası okunurken hata oluştu!"
                                diyalogAcikMi = true
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "İndir", tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("DB Yedek İndir", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BeyazYariSeffaf),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .clickable { onYedekYukleIstegi() }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Yükle", tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("DB Yedek Yükle", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Bildirim Penceresi
    if (diyalogAcikMi) {
        AlertDialog(
            onDismissRequest = { diyalogAcikMi = false },
            title = { Text("Sistem Mesajı", fontWeight = FontWeight.Bold) },
            text = { Text(uyarıMesaji) },
            confirmButton = {
                TextButton(onClick = { diyalogAcikMi = false }) { Text("Tamam") }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun TarihBari(tarih: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = tarih, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.75f))
    }
}

// 🎯 BizimHesap tarzı: dairesel ikon + altında etiket
@Composable
fun MenuButonItem(model: MenuButonModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { model.tiklamaAksiyonu() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(BeyazYariSeffaf),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = model.ikon,
                contentDescription = model.baslik,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = model.baslik,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}