package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.IslemKaydi
import com.eray.muhasebeapp.csvRaporuOlustur
import com.eray.muhasebeapp.rememberDosyaPaylasici
import com.eray.muhasebeapp.formatTarih
import com.eray.muhasebeapp.excelXlsxOlustur

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaporlamaScreen(
    database: AppDatabase,
    onNavigateBack: () -> Unit
) {
    val satislar = remember { database.appDatabaseQueries.selectAllSatis().executeAsList() }
    val alislar = remember { database.appDatabaseQueries.selectAllAlis().executeAsList() }
    val masraflar = remember { database.appDatabaseQueries.selectAllMasraf().executeAsList() }
    val stokHareketleri = remember { database.appDatabaseQueries.selectAllStokHareketi().executeAsList() }

    val dosyaPaylasici = rememberDosyaPaylasici()

    val tumIslemler = remember(satislar, alislar, masraflar, stokHareketleri) {
        (satislar.map { satis ->
            val kalemler = database.appDatabaseQueries.selectKalemlerBySatisId(satis.id).executeAsList()
            IslemKaydi.SatisIslemi(satis, kalemler)
        } +
                alislar.map { alis ->
                    val kalemler = database.appDatabaseQueries.selectKalemlerByAlisId(alis.id).executeAsList()
                    IslemKaydi.AlisIslemi(alis, kalemler)
                } +
                masraflar.map { IslemKaydi.MasrafIslemi(it) } +
                stokHareketleri.map { IslemKaydi.StokIslemi(it) })
            .sortedByDescending { it.tarih.toLongOrNull() ?: 0L }
            .take(50)
    }
    val toplamSatis = satislar.sumOf { it.toplamTutar }
    val toplamAlis = alislar.sumOf { it.toplamTutar }
    val toplamMasraf = masraflar.sumOf { it.tutar }

    // Ödenen ve Bekleyen masrafları filtreleyerek ayırıyoruz
    val odenenMasraf = masraflar.filter { it.odendiMi == 1L }.sumOf { it.tutar }
    val bekleyenMasraf = masraflar.filter { it.odendiMi != 1L }.sumOf { it.tutar }

// Net Kâr hesaplanırken sadece cebinden çıkan (Ödenen) masrafları düşüyoruz
    val netKar = toplamSatis - toplamAlis - odenenMasraf

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            TopAppBar(
                title = { Text("Raporlama", fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val csv = csvRaporuOlustur(satislar, alislar, masraflar, stokHareketleri)
                        dosyaPaylasici.paylas("muhasebe_raporu.csv", csv)
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Rapor Çıkar", tint = Color(0xFF007AFF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ÖZET KARTLARI
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OzetKart("Satış", "₺$toplamSatis", Color(0xFF34C759), Modifier.weight(1f))
                OzetKart("Alış", "₺$toplamAlis", Color(0xFFFF9500), Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 0.dp, 16.dp, 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OzetKart("Ödenen Masraf", "₺$odenenMasraf", Color(0xFFFF3B30), Modifier.weight(1f))
                OzetKart("Bekleyen Masraf", "₺$bekleyenMasraf", Color(0xFF8E8E93), Modifier.weight(1f)) // Gri renkli bekleyen kartı
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 0.dp, 16.dp, 8.dp)
            ) {
                OzetKart(
                    baslik = "Net Kâr / Zarar (Nakit)",
                    deger = "₺$netKar",
                    renk = if (netKar >= 0) Color(0xFF007AFF) else Color(0xFFFF3B30),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // RAPOR ÇIKAR BUTONU
            Button(
                onClick = {
                    val excel = excelXlsxOlustur(satislar, alislar, masraflar, stokHareketleri)
                    dosyaPaylasici.paylasBytes("muhasebe_raporu.xlsx", excel)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rapor Çıkar (Excel/CSV)", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "TÜM İŞLEMLER",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (tumIslemler.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Henüz işlem yok", color = Color(0xFF8E8E93), fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tumIslemler) { islem ->
                        IslemKart(islem)
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) } // 💡 FAB veya alt alanlar için ekstra kaydırma payı
                }
            }
        }
    }
}

@Composable
fun OzetKart(baslik: String, deger: String, renk: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(baslik, fontSize = 12.sp, color = Color(0xFF8E8E93))
            Spacer(modifier = Modifier.height(4.dp))
            Text(deger, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = renk)
        }
    }
}

@Composable
fun IslemKart(islem: IslemKaydi) {
    val (baslik, altBaslik, tutar, renk, ikon) = when (islem) {
        is IslemKaydi.MasrafIslemi -> {
            val durumMetni = if (islem.masraf.odendiMi == 1L) " (Ödendi)" else " (Ödenecek)"
            IslemGoruntu(
                "Masraf",
                "${islem.masraf.kategori}$durumMetni",
                islem.masraf.tutar,
                if (islem.masraf.odendiMi == 1L) Color(0xFFFF3B30) else Color(0xFFFF9500), // Ödenmediyse turuncu ikon
                Icons.Default.Receipt
            )
        }
        is IslemKaydi.StokIslemi -> IslemGoruntu(
            "Stok Hareketi", "${islem.stokHareketi.urunAdi} • ${islem.stokHareketi.hareketTuru} (${islem.stokHareketi.miktar})",
            islem.tutar, Color(0xFF5856D6), Icons.Default.Inventory2
        )
        is IslemKaydi.SatisIslemi -> {
            val urunListesi = islem.kalemler.joinToString(", ") { "${it.urunAdi} x${it.adet}" }
            IslemGoruntu(
                "Satış",
                if (urunListesi.isNotEmpty()) "$urunListesi\n${islem.satis.musteriAdi} • ${islem.satis.odemeTuru}" else "${islem.satis.musteriAdi} • ${islem.satis.odemeTuru}",
                islem.satis.toplamTutar, Color(0xFF34C759), Icons.Default.TrendingUp
            )
        }
        is IslemKaydi.AlisIslemi -> {
            val urunListesi = islem.kalemler.joinToString(", ") { "${it.urunAdi} x${it.adet}" }
            IslemGoruntu(
                "Alış",
                if (urunListesi.isNotEmpty()) "$urunListesi\n${islem.alis.tedarikciAdi} • ${islem.alis.odemeTuru}" else "${islem.alis.tedarikciAdi} • ${islem.alis.odemeTuru}",
                islem.alis.toplamTutar, Color(0xFFFF9500), Icons.Default.ShoppingBag
            )
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 🎯 DÜZELTME: İç içe Row karmaşası giderildi, Alignment.Top çok satırlı metinlerde daha simetrik durur
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 1. İKON ALANI
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(renk.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(ikon, contentDescription = null, tint = renk)
            }

            // 2. METİN ALANI (Esnek ve Sınırlandırılmış)
            Column(
                modifier = Modifier.weight(1f) // 🎯 Tüm boş alanı buraya veriyoruz, sağ paneli sıkıştıramaz
            ) {
                Text(
                    text = baslik,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = altBaslik,
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93),
                    lineHeight = 16.sp
                )
            }

            // 3. TUTAR VE TARİH ALANI (Sabit Kalır)
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = "₺$tutar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, // SemiBold yerine Bold fiyatta daha okunaklı durur
                    color = renk,
                    maxLines = 1, //  Fiyatın asla ikiye bölünmesini istemeyiz
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTarih(islem.tarih),
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E93),
                    maxLines = 1
                )
            }
        }
    }
}

private data class IslemGoruntu(
    val baslik: String,
    val altBaslik: String,
    val tutar: Double,
    val renk: Color,
    val ikon: androidx.compose.ui.graphics.vector.ImageVector
)

private operator fun IslemGoruntu.component1() = baslik
private operator fun IslemGoruntu.component2() = altBaslik
private operator fun IslemGoruntu.component3() = tutar
private operator fun IslemGoruntu.component4() = renk
private operator fun IslemGoruntu.component5() = ikon