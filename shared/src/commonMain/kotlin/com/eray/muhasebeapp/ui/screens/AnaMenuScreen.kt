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

// Menü butonları için veri yapısı
data class MenuButonModel(
    val baslik: String,
    val ikon: ImageVector,
    val ikonRengi: Color,
    val tiklamaAksiyonu: () -> Unit
)

@Composable
fun AnaMenuScreen(
    database: AppDatabase,
    onNavigateToUrunler: () -> Unit
) {
    // DB'den canlı istatistikler
    val urunler = remember { database.appDatabaseQueries.selectAllUrun().executeAsList() }

    val toplamUrunSayisi = urunler.size
    val toplamStokDegeri = urunler.sumOf { it.alisFiyati * it.stokAdedi }
    val toplamSatisDegeri = urunler.sumOf { it.satisFiyati * it.stokAdedi }
    val kritikStoklar = urunler.filter { it.stokAdedi <= 5L }.sortedBy { it.stokAdedi }.take(5)
    val potansiyelKar = toplamSatisDegeri - toplamStokDegeri

    // Grid Menü Butonları Listesi
    val menuButonlari = listOf(
        MenuButonModel("Müşteri", Icons.Default.Person, Color(0xFF007AFF)) {},
        MenuButonModel("Tedarikçi", Icons.Default.LocalShipping, Color(0xFF5856D6)) {},
        MenuButonModel("Satış", Icons.Default.TrendingUp, Color(0xFF34C759)) {},
        MenuButonModel("Alış", Icons.Default.ShoppingBag, Color(0xFFFF9500)) {},
        MenuButonModel("Ürün", Icons.Default.Inventory, Color(0xFFFF2D55), onNavigateToUrunler),
        MenuButonModel("Masraf", Icons.Default.Receipt, Color(0xFFFF3B30)) {},
        MenuButonModel("Raporlama", Icons.Default.BarChart, Color(0xFFAF52DE)) {},
        MenuButonModel("Stok", Icons.Default.Storage, Color(0xFF5AC8FA)) {}
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .verticalScroll(rememberScrollState())
    ) {
        // Üst Bar & Karşılama Alanı
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

        Spacer(modifier = Modifier.height(16.dp))

        // STOK DURUMU
        Text(
            text = "STOK DURUMU",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BuyukStatKart(
                baslik = "Toplam Ürün",
                deger = "$toplamUrunSayisi",
                altBaslik = "kayıtlı ürün çeşidi",
                renk = Color(0xFF007AFF),
                ikon = Icons.Default.List,
                modifier = Modifier.weight(1f).clickable { onNavigateToUrunler() }
            )
            BuyukStatKart(
                baslik = "Kritik Stok",
                deger = "${kritikStoklar.size}",
                altBaslik = "ürün eşikte veya altında",
                renk = if (kritikStoklar.isNotEmpty()) Color(0xFFFF3B30) else Color(0xFF34C759),
                ikon = Icons.Default.Warning,
                modifier = Modifier.weight(1f).clickable { onNavigateToUrunler() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // MALİ ÖZET
        Text(
            text = "MALİ ÖZET",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column {
                MaliSatir(
                    etiket = "Stok Maliyet Değeri (Alış)",
                    deger = "₺${toplamStokDegeri}",
                    renk = Color(0xFFFF9500)
                )
                HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)
                MaliSatir(
                    etiket = "Stok Satış Değeri",
                    deger = "₺${toplamSatisDegeri}",
                    renk = Color(0xFF007AFF)
                )
                HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)
                MaliSatir(
                    etiket = "Potansiyel Kâr",
                    deger = "₺${potansiyelKar}",
                    renk = if (potansiyelKar >= 0) Color(0xFF34C759) else Color(0xFFFF3B30),
                    kalin = true
                )
            }
        }

        // KRİTİK STOK UYARILARI
        if (kritikStoklar.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
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
                    kritikStoklar.forEachIndexed { index, urun ->
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
                        if (index < kritikStoklar.lastIndex) {
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

        // HIZLI İŞLEMLER (2 Kolonlu Grid Menü)
        Spacer(modifier = Modifier.height(16.dp))
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

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// Alt Bileşen Fonksiyonları (Hataları Çözen Kısım)
// ─────────────────────────────────────────────────────────────

@Composable
fun MenuButonItem(model: MenuButonModel) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clickable { model.tiklamaAksiyonu() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = model.ikon,
                contentDescription = model.baslik,
                tint = model.ikonRengi,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = model.baslik,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun BuyukStatKart(
    baslik: String,
    deger: String,
    altBaslik: String,
    renk: Color,
    ikon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(imageVector = ikon, contentDescription = null, tint = renk, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = deger, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = renk)
            Text(text = baslik, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Text(text = altBaslik, fontSize = 11.sp, color = Color(0xFF8E8E93))
        }
    }
}

@Composable
fun MaliSatir(etiket: String, deger: String, renk: Color, kalin: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiket,
            fontSize = 15.sp,
            color = Color(0xFF3C3C43),
            fontWeight = if (kalin) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = deger,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = renk
        )
    }
}