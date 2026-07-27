package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.PlatformDatabaseManager
import com.eray.muhasebeapp.rememberDosyaPaylasici

data class MenuButonModel(
    val baslik: String,
    val ikon: ImageVector,
    val ikonRengi: Color,
    val tiklamaAksiyonu: () -> Unit
)

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
    val urunler = remember { database.appDatabaseQueries.selectAllUrun().executeAsList() }
    val dosyaPaylasici = rememberDosyaPaylasici()

    val tumKritikUrunler = urunler.filter { it.stokAdedi <= 5L }
    val kritikStoklarGosterim = tumKritikUrunler.sortedBy { it.stokAdedi }.take(5)

    var uyarıMesaji by remember { mutableStateOf("") }
    var diyalogAcikMi by remember { mutableStateOf(false) }

    val menuButonlari = listOf(
        MenuButonModel("Müşteri", Icons.Default.Person, Color(0xFF007AFF), onNavigateToMusteriler),
        MenuButonModel("Tedarikçi", Icons.Default.LocalShipping, Color(0xFF5856D6), onNavigateToTedarikciler),
        MenuButonModel("Satış", Icons.Default.TrendingUp, Color(0xFF34C759), onNavigateToSatis),
        MenuButonModel("Alış", Icons.Default.ShoppingBag, Color(0xFFFF9500), onNavigateToAlis),
        MenuButonModel("Ürün", Icons.Default.Inventory, Color(0xFFFF2D55), onNavigateToUrunler),
        MenuButonModel("Masraf", Icons.Default.Receipt, Color(0xFFFF3B30), onNavigateToMasraf),
        MenuButonModel("Raporlama", Icons.Default.BarChart, Color(0xFFAF52DE), onNavigateToRaporlama),
        MenuButonModel("Stok", Icons.Default.Storage, Color(0xFF5AC8FA), onNavigateToStok)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                Text(
                    text = "Hoş geldiniz Talha Bey",
                    fontSize = 15.sp,
                    color = Color(0xFF8E8E93),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Muhasebe",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        TarihBari(tarih = guncelTarih)
        Spacer(modifier = Modifier.height(16.dp))

        // 1. BÖLÜM: HIZLI İŞLEMLER
        Text(
            text = "HIZLI İŞLEMLER",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 410.dp)
                .padding(horizontal = 16.dp)
        ) {
            items(menuButonlari) { buton ->
                MenuButonItem(model = buton)
            }
        }

        // 2. BÖLÜM: KRİTİK STOK UYARILARI
        if (tumKritikUrunler.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "KRİTİK STOK UYARILARI",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onNavigateToUrunler() }
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
                                    tint = Color(0xFFFF3B30),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(urun.ad, fontSize = 15.sp, color = Color.Black)
                            }
                            Text(
                                text = "${urun.stokAdedi} ${urun.birim}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF3B30)
                            )
                        }
                        if (index < kritikStoklarGosterim.lastIndex) {
                            HorizontalDivider(
                                color = Color(0xFFF2F2F7),
                                thickness = 1.dp,
                                modifier = Modifier.padding(start = 40.dp)
                            )
                        }
                    }
                }
            }
        }

        // 🎯 3. BÖLÜM: VERİ GÜVENLİĞİ VE YEDEKLEME (SAĞ / SOL ALTA GELECEK YAPI)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "VERİ GÜVENLİĞİ VE BULUT YEDEK",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // İNDİR (DIŞA AKTAR) BUTTON
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
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
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = "İndir", tint = Color(0xFF34C759), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("DB Yedek İndir", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                }
            }

            // YÜKLE (İÇE AKTAR) BUTTON
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .clickable { onYedekYukleIstegi() }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Yükle", tint = Color(0xFF007AFF), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("DB Yedek Yükle", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
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
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = tarih, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF8E8E93))
        }
    }
}

@Composable
fun MenuButonItem(model: MenuButonModel) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().height(90.dp).clickable { model.tiklamaAksiyonu() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(imageVector = model.ikon, contentDescription = model.baslik, tint = model.ikonRengi, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = model.baslik, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
        }
    }
}