package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.database.shared.AppDatabase
import com.eray.muhasebeapp.database.Masraf
import com.eray.muhasebeapp.getEpochMillis
import com.eray.muhasebeapp.formatTarih
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Masraf kategorileri ve ikonları
val masrafKategorileri = listOf(
    "Kira" to Icons.Default.Home,
    "Fatura" to Icons.Default.Receipt,
    "Maaş" to Icons.Default.Groups,
    "Yakıt" to Icons.Default.LocalGasStation,
    "Malzeme" to Icons.Default.Inventory,
    "Diğer" to Icons.Default.MoreHoriz
)

private fun kategoriIkonu(kategori: String) =
    masrafKategorileri.firstOrNull { it.first == kategori }?.second ?: Icons.Default.MoreHoriz

private val turkceAylar = listOf(
    "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
    "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
)

private fun ayBasligiUret(tarihMillisStr: String): String {
    val gosterim = formatTarih(tarihMillisStr).substringBefore(" ") // "dd.MM.yyyy"
    val parcalar = gosterim.split(".")
    if (parcalar.size != 3) return "Bilinmeyen Tarih"
    val ay = parcalar[1].toIntOrNull() ?: return "Bilinmeyen Tarih"
    val yil = parcalar[2]
    val ayAdi = turkceAylar.getOrElse(ay - 1) { "Bilinmeyen" }
    return "$ayAdi $yil Masrafları"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasrafScreen(
    database: AppDatabase,
    simdiMillis: Long,
    onNavigateBack: () -> Unit
) {
    var yukleniyor by remember { mutableStateOf(true) }
    var hataMesaji by remember { mutableStateOf<String?>(null) }
    var gruplanmisMasraflar by remember { mutableStateOf<Map<String, List<Masraf>>>(emptyMap()) }

    var seciliFiltre by remember { mutableStateOf("Tümü") }
    var seciliDonem by remember { mutableStateOf(RaporDonemi.BU_AY) }
    var mevcutLimit by remember { mutableStateOf(30) }
    var yenilemeTetikleyici by remember { mutableStateOf(0) }

    var toplamMasraf by remember { mutableStateOf(0.0) }
    var islemSayisi by remember { mutableStateOf(0) }

    // Sağa kaydırarak geri dönme (Swipe Back) durumu için drag takibi
    var horizontalDragAccumulator by remember { mutableStateOf(0f) }

    // KMP Uyumlu Default Background Thread aracı
    LaunchedEffect(seciliDonem, seciliFiltre, mevcutLimit, yenilemeTetikleyici) {
        yukleniyor = true
        withContext(Dispatchers.Default) {
            try {
                val donemBaslangic = donemBaslangicMillis(seciliDonem, simdiMillis)
                val hamMasraflar = database.appDatabaseQueries.selectAllMasraf().executeAsList()

                val masraflarFiltreli = hamMasraflar.filter { m ->
                    val kategoriUyar = seciliFiltre == "Tümü" || m.kategori == seciliFiltre
                    val donemUyar = donemBaslangic == null || (m.tarih.toLongOrNull() ?: 0L) >= donemBaslangic
                    kategoriUyar && donemUyar
                }.sortedByDescending { it.tarih.toLongOrNull() ?: 0L }

                toplamMasraf = masraflarFiltreli.sumOf { it.tutar }
                islemSayisi = masraflarFiltreli.size

                val limitliMasraflar = masraflarFiltreli.take(mevcutLimit)
                gruplanmisMasraflar = limitliMasraflar.groupBy { ayBasligiUret(it.tarih) }

            } catch (e: Throwable) {
                hataMesaji = "MASRAF VERİSİ İŞLENİRKEN HATA OLUŞTU:\n${e::class.simpleName}: ${e.message}\n${e.stackTraceToString().take(1000)}"
            } finally {
                yukleniyor = false
            }
        }
    }

    if (hataMesaji != null) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopStart) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Bir hata oluştu, lütfen bu metni kopyala:", fontWeight = FontWeight.Bold, color = Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
                SelectionContainer { Text(hataMesaji ?: "", fontSize = 12.sp) }
            }
        }
        return
    }

    if (yukleniyor && gruplanmisMasraflar.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFF3B30))
        }
        return
    }

    var dialogAcikMi by remember { mutableStateOf(false) }
    var silinecekMasraf by remember { mutableStateOf<Masraf?>(null) }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        modifier = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { horizontalDragAccumulator = 0f },
                onDragEnd = {
                    // Sağa doğru yeterli kaydırma yapıldıysa ana menüye dön
                    if (horizontalDragAccumulator > 150f) {
                        onNavigateBack()
                    }
                },
                onDragCancel = { horizontalDragAccumulator = 0f },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    horizontalDragAccumulator += dragAmount
                }
            )
        },
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

            // DÖNEM SEÇİCİ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RaporDonemi.entries.forEach { donem ->
                    FilterChip(
                        selected = seciliDonem == donem,
                        onClick = {
                            seciliDonem = donem
                            mevcutLimit = 30
                        },
                        label = { Text(donem.etiket, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF3B30),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // ÜST ÖZET KARTLARI
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 🎯 DEĞİŞTİRİLDİ: Toplam masraf virgülden sonra iki basamak yapıldı
                MasrafOzetKart("Toplam", "₺${formatMasrafIkiBasamak(toplamMasraf)}", Color(0xFFFF3B30), Modifier.weight(1f))
                MasrafOzetKart("İşlem Sayısı", "$islemSayisi", Color(0xFF8E8E93), Modifier.weight(1f))
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
                ) {
                    seciliFiltre = "Tümü"
                    mevcutLimit = 30
                }

                masrafKategorileri.forEach { (kategori, _) ->
                    KategoriFiltreCip(
                        baslik = kategori,
                        seciliMi = seciliFiltre == kategori
                    ) {
                        seciliFiltre = kategori
                        mevcutLimit = 30
                    }
                }
            }

            // LİSTELEME
            if (gruplanmisMasraflar.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if(yukleniyor) "Yükleniyor..." else "Bu dönemde masraf yok", color = Color(0xFF8E8E93), fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    gruplanmisMasraflar.forEach { (ayBasligi, masraflarListesi) ->
                        item {
                            Text(
                                text = ayBasligi,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF3B30),
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }
                        items(masraflarListesi, key = { it.id }) { masraf ->
                            MasrafKart(
                                masraf = masraf,
                                onSil = { silinecekMasraf = masraf }
                            )
                        }
                    }

                    if (islemSayisi > mevcutLimit) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(
                                    onClick = { mevcutLimit += 30 },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF3B30))
                                ) {
                                    Text("Daha Fazla Masraf Yükle (+30)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
        }
    }

    if (dialogAcikMi) {
        MasrafEkleDialog(
            onDismiss = { dialogAcikMi = false },
            onKaydet = { kategori, aciklama, tutar ->
                database.appDatabaseQueries.insertMasraf(
                    kategori,
                    aciklama,
                    tutar,
                    getEpochMillis().toString()
                )
                yenilemeTetikleyici++
                dialogAcikMi = false
            }
        )
    }

    silinecekMasraf?.let { masraf ->
        AlertDialog(
            onDismissRequest = { silinecekMasraf = null },
            containerColor = Color.White,
            title = { Text("Masrafı Sil", fontWeight = FontWeight.Bold) },
            text = { Text("Bu masraf kaydını silmek istediğinize emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        database.appDatabaseQueries.deleteMasraf(masraf.id)
                        yenilemeTetikleyici++
                        silinecekMasraf = null
                    }
                ) {
                    Text("Sil", color = Color(0xFFFF3B30), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { silinecekMasraf = null }) {
                    Text("İptal", color = Color(0xFF8E8E93))
                }
            }
        )
    }
}

// 🎯 ÇAKIŞMALARI ENGELLEMEK İÇİN YARDIMCI BİLEŞENLERE PRIVATE EKLENDİ
@Composable
private fun KategoriFiltreCip(baslik: String, seciliMi: Boolean, onClick: () -> Unit) {
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
private fun MasrafKart(
    masraf: Masraf,
    onSil: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
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
                        .background(Color(0xFFFF3B30).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(kategoriIkonu(masraf.kategori), contentDescription = null, tint = Color(0xFFFF3B30))
                }
                Column {
                    Text(masraf.kategori, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text(masraf.aciklama, fontSize = 13.sp, color = Color(0xFF8E8E93))
                    Text(
                        formatTarih(masraf.tarih).substringBefore(" "),
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 🎯 DEĞİŞTİRİLDİ: Tekil masraf kartı tutarı iki basamak yapıldı
                Text("₺${formatMasrafIkiBasamak(masraf.tutar)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF3B30))
                IconButton(onClick = onSil, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFFF3B30))
                }
            }
        }
    }
}

@Composable
private fun MasrafEkleDialog(
    onDismiss: () -> Unit,
    onKaydet: (kategori: String, aciklama: String, tutar: Double) -> Unit
) {
    var seciliKategori by remember { mutableStateOf(masrafKategorileri.first().first) }
    var aciklama by remember { mutableStateOf("") }
    var tutarText by remember { mutableStateOf("") }
    var dropdownAcikMi by remember { mutableStateOf(false) }

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
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tutarText,
                    onValueChange = { tutarText = it },
                    label = { Text("Tutar (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val tutar = tutarText.toDoubleOrNull() ?: 0.0
                if (tutar <= 0) return@TextButton
                onKaydet(seciliKategori, aciklama.ifBlank { seciliKategori }, tutar)
            }) { Text("Kaydet", color = Color(0xFFFF3B30), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

@Composable
private fun MasrafOzetKart(baslik: String, deger: String, renk: Color, modifier: Modifier = Modifier) {
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

// 🎯 KMP UYUMLU VE SAPMASIZ PARASAL BİÇİMLENDİRİCİ
private fun formatMasrafIkiBasamak(deger: Double): String {
    val negatifMi = deger < 0
    val mutlakDeger = if (negatifMi) -deger else deger
    val yuvarlanmis = ((mutlakDeger * 100.0) + 0.5).toLong() / 100.0
    val tamKisim = yuvarlanmis.toLong()
    val kesirKisim = (((yuvarlanmis - tamKisim) * 100.0) + 0.5).toLong()
    val kesirStr = kesirKisim.toString().padStart(2, '0')
    return "${if (negatifMi) "-" else ""}$tamKisim.$kesirStr"
}