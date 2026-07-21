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
import com.eray.muhasebeapp.database.Tedarikci
import com.eray.muhasebeapp.database.Alis
import com.eray.muhasebeapp.database.AlisKalemi
import com.eray.muhasebeapp.rememberUrlAcici
import com.eray.muhasebeapp.telefonLinkOlustur
import com.eray.muhasebeapp.whatsappLinkOlustur
import com.eray.muhasebeapp.formatTarih

/**
 * Tedarikçinin bakiye durumuna göre metin ve renk bilgisini dönen yardımcı fonksiyon.
 * Muhasebe mantığına göre: Bakiye > 0 ise tedarikçiye BORCUMUZ vardır (İşletme için borç riski).
 */
private fun tedarikciBakiyeMetniVeRengi(bakiye: Double): Pair<String, Color> {
    return when {
        bakiye > 0 -> "₺$bakiye (Borcumuz)" to Color(0xFFFF3B30)      // Kırmızı: Ödememiz gereken borç
        bakiye < 0 -> "₺${-bakiye} (Alacaklıyız)" to Color(0xFF007AFF) // Mavi: Tedarikçiden alacaklıyız
        else -> "₺0.0 (Dengede)" to Color(0xFF34C759)                 // Yeşil: Hesap kapalı, temiz
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TedarikcilerScreen(
    database: AppDatabase,
    onNavigateBack: () -> Unit
) {
    var yenilemeTetikleyici by remember { mutableStateOf(0) }
    val tedarikciler = remember(yenilemeTetikleyici) {
        database.appDatabaseQueries.selectAllTedarikci().executeAsList()
    }

    var dialogAcikMi by remember { mutableStateOf(false) }
    var detayGosterilenTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var duzenlenenTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var bakiyeDuzenlenenTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var silinecekTedarikci by remember { mutableStateOf<Tedarikci?>(null) }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            TopAppBar(
                title = { Text("Tedarikçiler", fontWeight = FontWeight.Bold, color = Color.Black) },
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
                containerColor = Color(0xFF5856D6)
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
                        Text("Toplam Tedarikçi", fontSize = 13.sp, color = Color(0xFF8E8E93))
                        Text("${tedarikciler.size}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5856D6))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Toplam Borcumuz", fontSize = 13.sp, color = Color(0xFF8E8E93))
                        val toplamBorc = tedarikciler.sumOf { it.bakiye }
                        Text(
                            "₺$toplamBorc",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (toplamBorc > 0) Color(0xFFFF9500) else Color(0xFF34C759)
                        )
                    }
                }
            }

            if (tedarikciler.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Henüz tedarikçi eklenmedi", color = Color(0xFF8E8E93), fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tedarikciler) { tedarikci ->
                        TedarikciKart(
                            tedarikci = tedarikci,
                            onTikla = { detayGosterilenTedarikci = tedarikci },
                            onSil = { silinecekTedarikci = tedarikci }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (dialogAcikMi) {
        TedarikciEkleDialog(
            onDismiss = { dialogAcikMi = false },
            onKaydet = { ad, telefon, adres, bakiye ->
                database.appDatabaseQueries.insertTedarikci(ad, telefon, adres, bakiye)
                yenilemeTetikleyici++
                dialogAcikMi = false
            }
        )
    }

    detayGosterilenTedarikci?.let { tedarikci ->
        TedarikciDetayDialog(
            database = database,
            tedarikci = tedarikci,
            onDismiss = { detayGosterilenTedarikci = null },
            onDuzenle = {
                duzenlenenTedarikci = tedarikci
                detayGosterilenTedarikci = null
            },
            onBakiyeDuzenle = {
                bakiyeDuzenlenenTedarikci = tedarikci
                detayGosterilenTedarikci = null
            }
        )
    }

    duzenlenenTedarikci?.let { tedarikci ->
        TedarikciDuzenleDialog(
            tedarikci = tedarikci,
            onDismiss = { duzenlenenTedarikci = null },
            onKaydet = { ad, telefon, adres ->
                database.appDatabaseQueries.updateTedarikci(ad, telefon, adres, tedarikci.id)
                yenilemeTetikleyici++
                duzenlenenTedarikci = null
            }
        )
    }

    bakiyeDuzenlenenTedarikci?.let { tedarikci ->
        TedarikciBakiyeDuzenleDialog(
            tedarikci = tedarikci,
            onDismiss = { bakiyeDuzenlenenTedarikci = null },
            onKaydet = { yeniBakiye ->
                database.appDatabaseQueries.updateTedarikciBakiye(yeniBakiye, tedarikci.id)
                yenilemeTetikleyici++
                bakiyeDuzenlenenTedarikci = null
            }
        )
    }

    // --- Silme Onay Dialog ---
    silinecekTedarikci?.let { tedarikci ->
        AlertDialog(
            onDismissRequest = { silinecekTedarikci = null },
            title = { Text("Tedarikçiyi Sil", fontWeight = FontWeight.Bold) },
            text = { Text("\"${tedarikci.ad}\" tedarikçisini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        database.appDatabaseQueries.deleteTedarikci(tedarikci.id)
                        yenilemeTetikleyici++
                        silinecekTedarikci = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) { Text("Sil", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { silinecekTedarikci = null }) {
                    Text("Vazgeç", color = Color(0xFF5856D6))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun TedarikciKart(tedarikci: Tedarikci, onTikla: () -> Unit, onSil: () -> Unit) {
    val (bakiyeFormatli, renk) = tedarikciBakiyeMetniVeRengi(tedarikci.bakiye)

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
                        .background(Color(0xFF5856D6).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF5856D6))
                }
                Column {
                    Text(tedarikci.ad, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text(tedarikci.telefon, fontSize = 13.sp, color = Color(0xFF8E8E93))
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
fun TedarikciDetayDialog(
    database: AppDatabase,
    tedarikci: Tedarikci,
    onDismiss: () -> Unit,
    onDuzenle: () -> Unit,
    onBakiyeDuzenle: () -> Unit
) {
    val urlAcici = rememberUrlAcici()
    val (bakiyeFormatli, renk) = tedarikciBakiyeMetniVeRengi(tedarikci.bakiye)

    val gecmisAlislar = remember(tedarikci.id) {
        database.appDatabaseQueries.selectAlisByTedarikciId(tedarikci.id).executeAsList().take(6)
    }

    val alisKalemleri = remember(gecmisAlislar) {
        gecmisAlislar.associate { alis ->
            alis.id to database.appDatabaseQueries.selectKalemlerByAlisId(alis.id).executeAsList()
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
                Text(tedarikci.ad, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDuzenle, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = Color(0xFF5856D6))
                }
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(tedarikci.adres, fontSize = 13.sp, color = Color(0xFF8E8E93))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mevcut Durum: $bakiyeFormatli",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = renk
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TedarikciIletisimButonu(
                        baslik = "Doğrudan Borç Düzenle",
                        ikon = Icons.Default.Edit,
                        renk = Color(0xFF5856D6),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onBakiyeDuzenle() }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        TedarikciIletisimButonu(
                            baslik = "Ara",
                            ikon = Icons.Default.Call,
                            renk = Color(0xFF5856D6),
                            modifier = Modifier.weight(1f),
                            onClick = { urlAcici.ac(telefonLinkOlustur(tedarikci.telefon)) }
                        )
                        TedarikciIletisimButonu(
                            baslik = "WhatsApp",
                            ikon = Icons.Default.Chat,
                            renk = Color(0xFF34C759),
                            modifier = Modifier.weight(1f),
                            onClick = { urlAcici.ac(whatsappLinkOlustur(tedarikci.telefon)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "GEÇMİŞ ALIŞLAR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (gecmisAlislar.isEmpty()) {
                    Text("Henüz vadeli/kayıtlı alış yapılmadı", fontSize = 13.sp, color = Color(0xFF8E8E93))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        gecmisAlislar.forEach { alis ->
                            GecmisAlisKarti(
                                alis = alis,
                                kalemler = alisKalemleri[alis.id] ?: emptyList()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun GecmisAlisKarti(alis: Alis, kalemler: List<AlisKalemi>) {
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
            Text(alis.odemeTuru, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3C3C43))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₺${alis.toplamTutar}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alis.odemeTuru == "Veresiye") Color(0xFFFF9500) else Color(0xFF34C759)
                )
                Text(formatTarih(alis.tarih), fontSize = 10.sp, color = Color(0xFF8E8E93))
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
                        "${kalem.urunAdi} — ${kalem.adet} × ₺${kalem.birimFiyat}",
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
fun TedarikciIletisimButonu(
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
fun TedarikciEkleDialog(
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
        title = { Text("Yeni Tedarikçi Kaydı", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = ad, onValueChange = { ad = it }, label = { Text("Firma / Ad Soyad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
                    label = { Text("Mevcut Başlangıç Borcumuz (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (ad.isNotBlank()) {
                    onKaydet(ad, telefon, adres, bakiye.toDoubleOrNull() ?: 0.0)
                }
            }) { Text("Kaydet", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

@Composable
fun TedarikciDuzenleDialog(
    tedarikci: Tedarikci,
    onDismiss: () -> Unit,
    onKaydet: (ad: String, telefon: String, adres: String) -> Unit
) {
    var ad by remember { mutableStateOf(tedarikci.ad) }
    var telefon by remember { mutableStateOf(tedarikci.telefon) }
    var adres by remember { mutableStateOf(tedarikci.adres) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Tedarikçi Bilgilerini Düzenle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = ad, onValueChange = { ad = it }, label = { Text("Firma / Ad Soyad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = telefon, onValueChange = { telefon = it }, label = { Text("Telefon") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = adres, onValueChange = { adres = it }, label = { Text("Adres") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(
                    "Not: Borç bilgisi bu ekrandan değiştirilemez, mal alışı veya ödeme menülerinden otomatik güncellenmesi önerilir.",
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
            }) { Text("Kaydet", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

@Composable
fun TedarikciBakiyeDuzenleDialog(
    tedarikci: Tedarikci,
    onDismiss: () -> Unit,
    onKaydet: (yeniBakiye: Double) -> Unit
) {
    var yeniBakiyeText by remember { mutableStateOf(tedarikci.bakiye.toString()) }
    val (bakiyeFormatli, _) = tedarikciBakiyeMetniVeRengi(tedarikci.bakiye)

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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Dikkat: Gireceğiniz tutar firmaya olan güncel toplam net borcumuz sayılacaktır. Firmaya borçluysak düz rakam (Örn: 1500), eğer firmadan alacaklıysak eksi değer (Örn: -300) giriniz.",
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