package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.database.Masraf
import com.eray.muhasebeapp.getEpochMillis
import com.eray.muhasebeapp.getBugununTarihiString

// Masraf kategorileri ve ikonları
val masrafKategorileri = listOf(
    "Kira" to Icons.Default.Home,
    "Fatura" to Icons.Default.Receipt,
    "Maaş" to Icons.Default.Groups,
    "Yakıt" to Icons.Default.LocalGasStation,
    "Malzeme" to Icons.Default.Inventory,
    "Diğer" to Icons.Default.MoreHoriz
)

fun kategoriIkonu(kategori: String) =
    masrafKategorileri.firstOrNull { it.first == kategori }?.second ?: Icons.Default.MoreHoriz

// "yyyy-MM-dd" formatını ekranda göstermek için "gg.aa.yyyy" formatına çevirir
fun tarihGoruntule(tarihStr: String): String {
    if (tarihStr.isBlank()) return "-"
    val parcalar = tarihStr.split("-")
    if (parcalar.size != 3) return tarihStr
    return "${parcalar[2]}.${parcalar[1]}.${parcalar[0]}"
}

// "yyyy-MM-dd" formatındaki girdinin geçerli bir tarih oluşturup oluşturmadığını doğrular
fun tarihGecerliMi(gun: Int, ay: Int, yil: Int): Boolean {
    return try {
        if (ay in 1..12 && gun in 1..31) {
            val subatSınır = if ((yil % 4 == 0 && yil % 100 != 0) || (yil % 400 == 0)) 29 else 28
            val gunSınırları = intArrayOf(31, subatSınır, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
            gun <= gunSınırları[ay - 1]
        } else false
    } catch (e: Exception) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasrafScreen(
    database: AppDatabase,
    onNavigateBack: () -> Unit
) {
    var yenilemeTetikleyici by remember { mutableStateOf(0) }
    val tumMasraflar = remember(yenilemeTetikleyici) {
        database.appDatabaseQueries.selectAllMasraf().executeAsList()
    }

    var dialogAcikMi by remember { mutableStateOf(false) }
    var seciliFiltre by remember { mutableStateOf("Tümü") }

    val masraflar = remember(tumMasraflar, seciliFiltre) {
        if (seciliFiltre == "Tümü") tumMasraflar
        else tumMasraflar.filter { it.kategori == seciliFiltre }
    }

    val toplamMasraf = masraflar.sumOf { it.tutar }
    val odenmemisToplam = masraflar.filter { it.odendiMi != 1L }.sumOf { it.tutar }

    val bugun = getBugununTarihiString()

    val gecikenSayisi = masraflar.count {
        it.odendiMi != 1L && it.sonOdemeTarihi.isNotBlank() && it.sonOdemeTarihi < bugun
    }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            TopAppBar(
                title = { Text("Masraf", fontWeight = FontWeight.Bold, color = Color.Black) },
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
                containerColor = Color(0xFFFF3B30)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ekle", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ÜST ÖZET KARTLARI
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OzetKart("Toplam", "₺$toplamMasraf", Color(0xFFFF3B30), Modifier.weight(1f))
                OzetKart("Ödenmemiş", "₺$odenmemisToplam", Color(0xFFFF9500), Modifier.weight(1f))
                OzetKart(
                    "Geciken",
                    "$gecikenSayisi",
                    if (gecikenSayisi > 0) Color(0xFFFF3B30) else Color(0xFF34C759),
                    Modifier.weight(1f)
                )
            }

            // KATEGORİ FİLTRE ÇUBUĞU
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KategoriFiltreCip(
                    baslik = "Tümü",
                    seciliMi = seciliFiltre == "Tümü"
                ) { seciliFiltre = "Tümü" }

                masrafKategorileri.forEach { (kategori, _) ->
                    KategoriFiltreCip(
                        baslik = kategori,
                        seciliMi = seciliFiltre == kategori
                    ) { seciliFiltre = kategori }
                }
            }

            Text(
                text = "MASRAF GEÇMİŞİ",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (masraflar.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Bu kategoride masraf yok", color = Color(0xFF8E8E93), fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(masraflar) { masraf ->
                        MasrafKart(
                            masraf = masraf,
                            bugun = bugun,
                            onDurumDegistir = {
                                val yeniDurum = if (masraf.odendiMi == 1L) 0L else 1L
                                database.appDatabaseQueries.updateMasrafOdemeDurumu(yeniDurum, masraf.id)
                                yenilemeTetikleyici++
                            },
                            onSil = {
                                database.appDatabaseQueries.deleteMasraf(masraf.id)
                                yenilemeTetikleyici++
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
        }
    }

    if (dialogAcikMi) {
        MasrafEkleDialog(
            onDismiss = { dialogAcikMi = false },
            onKaydet = { kategori, aciklama, tutar, odendiMi, sonOdemeTarihi ->
                database.appDatabaseQueries.insertMasraf(
                    kategori,
                    aciklama,
                    tutar,
                    getEpochMillis().toString(),
                    if (odendiMi) 1L else 0L,
                    sonOdemeTarihi
                )
                yenilemeTetikleyici++
                dialogAcikMi = false
            }
        )
    }
}

@Composable
fun KategoriFiltreCip(baslik: String, seciliMi: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (seciliMi) Color(0xFFFF3B30) else Color.White,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            baslik,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (seciliMi) Color.White else Color(0xFF3C3C43)
        )
    }
}

@Composable
fun MasrafKart(
    masraf: Masraf,
    bugun: String,
    onDurumDegistir: () -> Unit,
    onSil: () -> Unit
) {
    val odendiMi = masraf.odendiMi == 1L
    val sonOdemeTarihStr = masraf.sonOdemeTarihi
    val gecikmisMi = !odendiMi && sonOdemeTarihStr.isNotBlank() && sonOdemeTarihStr < bugun

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFF3B30).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(kategoriIkonu(masraf.kategori), contentDescription = null, tint = Color(0xFFFF3B30))
                    }
                    Column {
                        Text(masraf.kategori, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        Text(masraf.aciklama, fontSize = 13.sp, color = Color(0xFF8E8E93))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("₺${masraf.tutar}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF3B30))
                    IconButton(onClick = onSil, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFFF3B30))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            (if (odendiMi) Color(0xFF34C759) else Color(0xFFFF9500)).copy(alpha = 0.12f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onDurumDegistir() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (odendiMi) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (odendiMi) Color(0xFF34C759) else Color(0xFFFF9500),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        if (odendiMi) "Ödendi" else "Ödenecek",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (odendiMi) Color(0xFF34C759) else Color(0xFFFF9500)
                    )
                }

                if (sonOdemeTarihStr.isNotBlank()) {
                    Text(
                        text = if (gecikmisMi) "Gecikti: ${tarihGoruntule(sonOdemeTarihStr)}"
                        else "Son ödeme: ${tarihGoruntule(sonOdemeTarihStr)}",
                        fontSize = 12.sp,
                        fontWeight = if (gecikmisMi) FontWeight.Bold else FontWeight.Medium,
                        color = if (gecikmisMi) Color(0xFFFF3B30) else Color(0xFF8E8E93)
                    )
                }
            }
        }
    }
}

@Composable
fun MasrafEkleDialog(
    onDismiss: () -> Unit,
    onKaydet: (kategori: String, aciklama: String, tutar: Double, odendiMi: Boolean, sonOdemeTarihi: String) -> Unit
) {
    var seciliKategori by remember { mutableStateOf(masrafKategorileri.first().first) }
    var aciklama by remember { mutableStateOf("") }
    var tutarText by remember { mutableStateOf("") }
    var dropdownAcikMi by remember { mutableStateOf(false) }
    var odendiMi by remember { mutableStateOf(true) }

    var gunText by remember { mutableStateOf("") }
    var ayText by remember { mutableStateOf("") }
    var yilText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Yeni Masraf", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedTextField(
                        value = seciliKategori,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        modifier = Modifier.fillMaxWidth().clickable { dropdownAcikMi = true },
                        trailingIcon = {
                            IconButton(onClick = { dropdownAcikMi = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = dropdownAcikMi,
                        onDismissRequest = { dropdownAcikMi = false }
                    ) {
                        masrafKategorileri.forEach { (kategori, ikon) ->
                            DropdownMenuItem(
                                text = { Text(kategori) },
                                leadingIcon = { Icon(ikon, contentDescription = null) },
                                onClick = {
                                    seciliKategori = kategori
                                    dropdownAcikMi = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = aciklama,
                    onValueChange = { aciklama = it },
                    label = { Text("Açıklama") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = tutarText,
                    onValueChange = { tutarText = it },
                    label = { Text("Tutar (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 🎯 DÜZELTME: Çakışmayı önlemek için yeni isimle (MasrafDurumButonu) çağırıldı
                    MasrafDurumButonu(
                        baslik = "Ödendi",
                        seciliMi = odendiMi,
                        renk = Color(0xFF34C759),
                        modifier = Modifier.weight(1f)
                    ) {
                        odendiMi = true
                    }

                    MasrafDurumButonu(
                        baslik = "Ödenecek",
                        seciliMi = !odendiMi,
                        renk = Color(0xFFFF9500),
                        modifier = Modifier.weight(1f)
                    ) {
                        odendiMi = false
                    }
                }

                if (!odendiMi) {
                    Text("Son Ödeme Tarihi", fontSize = 12.sp, color = Color(0xFF8E8E93))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = gunText,
                            onValueChange = { input ->
                                if (input.length <= 2 && input.all { it.isDigit() }) gunText = input
                            },
                            label = { Text("Gün") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ayText,
                            onValueChange = { input ->
                                if (input.length <= 2 && input.all { it.isDigit() }) ayText = input
                            },
                            label = { Text("Ay") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = yilText,
                            onValueChange = { input ->
                                if (input.length <= 4 && input.all { it.isDigit() }) yilText = input
                            },
                            label = { Text("Yıl") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val tutar = tutarText.toDoubleOrNull() ?: 0.0
                if (tutar <= 0) return@TextButton

                val sonOdemeTarihi = if (!odendiMi) {
                    val gun = gunText.toIntOrNull()
                    val ay = ayText.toIntOrNull()
                    val yil = yilText.toIntOrNull()
                    if (gun != null && ay != null && yil != null && tarihGecerliMi(gun, ay, yil)) {
                        val gunStr = gun.toString().padStart(2, '0')
                        val ayStr = ay.toString().padStart(2, '0')
                        "$yil-$ayStr-$gunStr"
                    } else ""
                } else ""

                onKaydet(seciliKategori, aciklama.ifBlank { seciliKategori }, tutar, odendiMi, sonOdemeTarihi)
            }) { Text("Kaydet", color = Color(0xFFFF3B30), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

// 🎯 DÜZELTME: SatisScreen'deki butonla çakışmaması için ismi 'MasrafDurumButonu' olarak değiştirildi
@Composable
private fun MasrafDurumButonu(
    baslik: String,
    seciliMi: Boolean,
    renk: Color,
    aktifMi: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                if (seciliMi) renk else Color.White,
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = aktifMi) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            baslik,
            color = if (seciliMi) Color.White else if (aktifMi) Color.Black else Color(0xFFC7C7CC),
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}