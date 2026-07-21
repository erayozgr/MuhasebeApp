package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.database.Musteri
import com.eray.muhasebeapp.database.Satis
import com.eray.muhasebeapp.database.SatisKalemi
import com.eray.muhasebeapp.rememberUrlAcici
import com.eray.muhasebeapp.telefonLinkOlustur
import com.eray.muhasebeapp.whatsappLinkOlustur
import com.eray.muhasebeapp.formatTarih

/**
 * Müşterinin bakiye durumuna göre metin ve renk bilgisini dönen yardımcı fonksiyon.
 * Muhasebe mantığına göre: Bakiye > 0 ise müşteri işletmeye BORÇLUdur (Bizim alacağımızdır).
 */
private fun bakiyeMetniVeRengi(bakiye: Double): Pair<String, Color> {
    return when {
        bakiye > 0 -> "₺$bakiye (Borçlu)" to Color(0xFFFF3B30)       // Kırmızı: Takip edilmesi gereken borç
        bakiye < 0 -> "₺${-bakiye} (Alacaklı)" to Color(0xFF007AFF)   // Mavi: Müşteri içeride artıda
        else -> "₺0.0 (Dengede)" to Color(0xFF34C759)                // Yeşil: Hesap kapalı, temiz
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusterilerScreen(
    database: AppDatabase,
    onNavigateBack: () -> Unit
) {
    var yenilemeTetikleyici by remember { mutableStateOf(0) }
    val musteriler = remember(yenilemeTetikleyici) {
        database.appDatabaseQueries.selectAllMusteri().executeAsList()
    }

    var dialogAcikMi by remember { mutableStateOf(false) }
    var detayGosterilenMusteri by remember { mutableStateOf<Musteri?>(null) }
    var duzenlenenMusteri by remember { mutableStateOf<Musteri?>(null) }
    var bakiyeDuzenlenenMusteri by remember { mutableStateOf<Musteri?>(null) }
    var silinecekMusteri by remember { mutableStateOf<Musteri?>(null) }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            TopAppBar(
                title = { Text("Müşteriler", fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { dialogAcikMi = true },
                containerColor = Color(0xFF007AFF)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ekle", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ÜST ÖZET KARTI
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Toplam Müşteri", fontSize = 13.sp, color = Color(0xFF8E8E93))
                        Text("${musteriler.size}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Toplam Müşteri Borcu", fontSize = 13.sp, color = Color(0xFF8E8E93))
                        val toplamBakiye = musteriler.sumOf { it.bakiye }
                        Text(
                            "₺$toplamBakiye",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (toplamBakiye > 0) Color(0xFFFF9500) else Color(0xFF34C759)
                        )
                    }
                }
            }

            if (musteriler.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Henüz müşteri eklenmedi", color = Color(0xFF8E8E93), fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(musteriler) { musteri ->
                        MusteriKart(
                            musteri = musteri,
                            onTikla = { detayGosterilenMusteri = musteri },
                            onSil = { silinecekMusteri = musteri }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (dialogAcikMi) {
        MusteriEkleDialog(
            onDismiss = { dialogAcikMi = false },
            onKaydet = { ad, telefon, adres, bakiye ->
                database.appDatabaseQueries.insertMusteri(ad, telefon, adres, bakiye)
                yenilemeTetikleyici++
                dialogAcikMi = false
            }
        )
    }

    detayGosterilenMusteri?.let { musteri ->
        MusteriDetayDialog(
            database = database,
            musteri = musteri,
            onDismiss = { detayGosterilenMusteri = null },
            onDuzenle = {
                duzenlenenMusteri = musteri
                detayGosterilenMusteri = null
            },
            onBakiyeDuzenle = {
                bakiyeDuzenlenenMusteri = musteri
                detayGosterilenMusteri = null
            }
        )
    }

    duzenlenenMusteri?.let { musteri ->
        MusteriDuzenleDialog(
            musteri = musteri,
            onDismiss = { duzenlenenMusteri = null },
            onKaydet = { ad, telefon, adres ->
                database.appDatabaseQueries.updateMusteri(ad, telefon, adres, musteri.id)
                yenilemeTetikleyici++
                duzenlenenMusteri = null
            }
        )
    }

    bakiyeDuzenlenenMusteri?.let { musteri ->
        BakiyeDuzenleDialog(
            musteri = musteri,
            onDismiss = { bakiyeDuzenlenenMusteri = null },
            onKaydet = { yeniBakiye ->
                database.appDatabaseQueries.updateMusteriBakiye(yeniBakiye, musteri.id)
                yenilemeTetikleyici++
                bakiyeDuzenlenenMusteri = null
            }
        )
    }

    // --- Silme Onay Dialog ---
    silinecekMusteri?.let { musteri ->
        AlertDialog(
            onDismissRequest = { silinecekMusteri = null },
            title = { Text("Müşteriyi Sil", fontWeight = FontWeight.Bold) },
            text = { Text("\"${musteri.ad}\" müşterisini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        database.appDatabaseQueries.deleteMusteri(musteri.id)
                        yenilemeTetikleyici++
                        silinecekMusteri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) { Text("Sil", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { silinecekMusteri = null }) {
                    Text("Vazgeç", color = Color(0xFF007AFF))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun MusteriKart(musteri: Musteri, onTikla: () -> Unit, onSil: () -> Unit) {
    val (bakiyeFormatli, renk) = bakiyeMetniVeRengi(musteri.bakiye)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().clickable { onTikla() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF007AFF).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF007AFF))
                }
                Column {
                    Text(musteri.ad, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text(musteri.telefon, fontSize = 13.sp, color = Color(0xFF8E8E93))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = bakiyeFormatli,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = renk
                )
                IconButton(onClick = onSil, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFFF3B30))
                }
            }
        }
    }
}

@Composable
fun MusteriDetayDialog(
    database: AppDatabase,
    musteri: Musteri,
    onDismiss: () -> Unit,
    onDuzenle: () -> Unit,
    onBakiyeDuzenle: () -> Unit
) {
    val urlAcici = rememberUrlAcici()
    val (bakiyeFormatli, renk) = bakiyeMetniVeRengi(musteri.bakiye)

    val gecmisSatislar = remember(musteri.id) {
        database.appDatabaseQueries.selectSatisByMusteriId(musteri.id).executeAsList().take(6)
    }

    val satisKalemleri = remember(gecmisSatislar) {
        gecmisSatislar.associate { satis ->
            satis.id to database.appDatabaseQueries.selectKalemlerBySatisId(satis.id).executeAsList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(musteri.ad, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDuzenle, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = Color(0xFF007AFF))
                }
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(musteri.adres, fontSize = 13.sp, color = Color(0xFF8E8E93))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mevcut Durum: $bakiyeFormatli",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = renk
                )

                Spacer(modifier = Modifier.height(16.dp))

                // İLETİŞİM VE BAKİYE DÜZENLEME AKSIYONLARI
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IletisimButonu(
                        baslik = "Doğrudan Bakiye/Borç Düzenle",
                        ikon = Icons.Default.Edit,
                        renk = Color(0xFF5856D6),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        onBakiyeDuzenle()
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        IletisimButonu(
                            baslik = "Ara",
                            ikon = Icons.Default.Call,
                            renk = Color(0xFF007AFF),
                            modifier = Modifier.weight(1f)
                        ) {
                            urlAcici.ac(telefonLinkOlustur(musteri.telefon))
                        }
                        IletisimButonu(
                            baslik = "WhatsApp",
                            ikon = Icons.Default.Chat,
                            renk = Color(0xFF34C759),
                            modifier = Modifier.weight(1f)
                        ) {
                            urlAcici.ac(whatsappLinkOlustur(musteri.telefon))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "GEÇMİŞ SATIŞLAR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (gecmisSatislar.isEmpty()) {
                    Text("Henüz veresiye/kayıtlı satış yapılmadı", fontSize = 13.sp, color = Color(0xFF8E8E93))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        gecmisSatislar.forEach { satis ->
                            GecmisSatisKarti(
                                satis = satis,
                                kalemler = satisKalemleri[satis.id] ?: emptyList()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun GecmisSatisKarti(satis: Satis, kalemler: List<SatisKalemi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F2F7), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(satis.odemeTuru, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3C3C43))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₺${satis.toplamTutar}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (satis.odemeTuru == "Veresiye") Color(0xFFFF9500) else Color(0xFF34C759)
                )
                Text(formatTarih(satis.tarih), fontSize = 10.sp, color = Color(0xFF8E8E93))
            }
        }

        if (kalemler.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))
            kalemler.forEach { kalem ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${kalem.urunAdi} — ${kalem.adet} ${kalem.birim} × ₺${kalem.birimFiyat}",
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "₺${kalem.toplam}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF3C3C43)
                    )
                }
            }
        }
    }
}

@Composable
fun IletisimButonu(
    baslik: String,
    ikon: ImageVector,
    renk: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .background(renk.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(ikon, contentDescription = null, tint = renk, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(baslik, color = renk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun MusteriEkleDialog(
    onDismiss: () -> Unit,
    onKaydet: (ad: String, telefon: String, adres: String, bakiye: Double) -> Unit
) {
    var ad by remember { mutableStateOf("") }
    var telefon by remember { mutableStateOf("") }
    var adres by remember { mutableStateOf("") }
    var bakiye by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Yeni Müşteri Kaydı", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = ad, onValueChange = { ad = it }, label = { Text("Ad Soyad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = telefon,
                    onValueChange = { telefon = it },
                    label = { Text("Telefon (05XX XXX XX XX)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = adres, onValueChange = { adres = it }, label = { Text("Adres") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = bakiye,
                    onValueChange = { bakiye = it },
                    label = { Text("Mevcut Başlangıç Borcu (₺)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (ad.isNotBlank()) {
                    onKaydet(ad, telefon, adres, bakiye.toDoubleOrNull() ?: 0.0)
                }
            }) { Text("Kaydet", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

@Composable
fun MusteriDuzenleDialog(
    musteri: Musteri,
    onDismiss: () -> Unit,
    onKaydet: (ad: String, telefon: String, adres: String) -> Unit
) {
    var ad by remember { mutableStateOf(musteri.ad) }
    var telefon by remember { mutableStateOf(musteri.telefon) }
    var adres by remember { mutableStateOf(musteri.adres) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Müşteri Bilgilerini Düzenle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = ad, onValueChange = { ad = it }, label = { Text("Ad Soyad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = telefon,
                    onValueChange = { telefon = it },
                    label = { Text("Telefon") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = adres, onValueChange = { adres = it }, label = { Text("Adres") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(
                    "Not: Borç/Bakiye bilgisi bu ekrandan değiştirilemez, satış veya tahsilat menülerinden otomatik güncellenmesi önerilir.",
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (ad.isNotBlank()) {
                    onKaydet(ad, telefon, adres)
                }
            }) { Text("Kaydet", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

@Composable
fun BakiyeDuzenleDialog(
    musteri: Musteri,
    onDismiss: () -> Unit,
    onKaydet: (yeniBakiye: Double) -> Unit
) {
    var yeniBakiyeText by remember { mutableStateOf(musteri.bakiye.toString()) }
    val (bakiyeFormatli, _) = bakiyeMetniVeRengi(musteri.bakiye)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Net Borç Ayarla", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Mevcut durum: $bakiyeFormatli",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
                OutlinedTextField(
                    value = yeniBakiyeText,
                    onValueChange = { yeniBakiyeText = it },
                    label = { Text("Yeni Net Borç Tutarı (₺)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Dikkat: Gireceğiniz tutar müşterinin güncel net borcu sayılacaktır. Müşteri size borçluysa düz rakam (Örn: 500), eğer siz müşteriye borçluysanız eksi değer (Örn: -200) giriniz.",
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val yeniBakiye = yeniBakiyeText.toDoubleOrNull()
                if (yeniBakiye != null) {
                    onKaydet(yeniBakiye)
                }
            }) { Text("Güncelle", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}