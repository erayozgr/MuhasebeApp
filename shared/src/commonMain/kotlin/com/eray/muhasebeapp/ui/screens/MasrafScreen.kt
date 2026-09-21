package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import com.eray.muhasebeapp.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.data.model.Masraf
import com.eray.muhasebeapp.data.network.ApiService
import com.eray.muhasebeapp.formatTarih
import kotlinx.coroutines.launch

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
    val gosterim = formatTarih(tarihMillisStr).substringBefore(" ")
    val parcalar = gosterim.split("/")
    if (parcalar.size != 3) return "Bilinmeyen Tarih"
    val ay = parcalar[1].toIntOrNull() ?: return "Bilinmeyen Tarih"
    val yil = parcalar[2]
    val ayAdi = turkceAylar.getOrElse(ay - 1) { "Bilinmeyen" }
    return "$ayAdi $yil Masrafları"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasrafScreen(
    apiService: ApiService,
    onNavigateBack: () -> Unit
) {
    var yukleniyor by remember { mutableStateOf(true) }
    var hataMesaji by remember { mutableStateOf<String?>(null) }
    var tumMasraflar by remember { mutableStateOf<List<Masraf>>(emptyList()) }
    var seciliFiltre by remember { mutableStateOf("Tümü") }
    var mevcutLimit by remember { mutableStateOf(30) }
    var yenilemeTetikleyici by remember { mutableStateOf(0) }
    var aramaAcik by remember { mutableStateOf(false) }
    var aramaMetni by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(yenilemeTetikleyici) {
        yukleniyor = true
        try {
            tumMasraflar = apiService.getMasraflar()
        } catch (e: Throwable) {
            println("Masraf Yükleme Hatası: ${e.message}")
            hataMesaji = "İşlem başarısız. Tekrar deneyin."
        } finally {
            yukleniyor = false
        }
    }

    val masraflarFiltreli = remember(tumMasraflar, seciliFiltre, aramaMetni) {
        val aranan = aramaMetni.trim()
        tumMasraflar.filter { masraf ->
            val kategoriUyuyor = seciliFiltre == "Tümü" || masraf.kategori == seciliFiltre
            val aramaUyuyor = aranan.isBlank() ||
                    masraf.aciklama.contains(aranan, ignoreCase = true) ||
                    masraf.kategori.contains(aranan, ignoreCase = true)
            kategoriUyuyor && aramaUyuyor
        }.sortedByDescending { it.tarih.toLongOrNull() ?: 0L }
    }

    val toplamMasraf = remember(masraflarFiltreli) {
        masraflarFiltreli.sumOf { it.tutar }
    }

    val islemSayisi = masraflarFiltreli.size

    val gruplanmisMasraflar = remember(masraflarFiltreli, mevcutLimit) {
        masraflarFiltreli
            .take(mevcutLimit)
            .groupBy { ayBasligiUret(it.tarih) }
    }

    if (hataMesaji != null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    hataMesaji ?: "",
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        hataMesaji = null
                        yenilemeTetikleyici++
                    }
                ) {
                    Text("Tekrar Dene")
                }
            }
        }
        return
    }

    var dialogAcikMi by remember { mutableStateOf(false) }
    var silinecekMasraf by remember { mutableStateOf<Masraf?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = BrandColors.Background,
        modifier = Modifier.swipeToBack(onBack = onNavigateBack),
        topBar = {
            BrandTopBar("Masraflar", "Giderlerinizi kontrol altında tutun", onNavigateBack) {
                IconButton(
                    onClick = {
                        aramaAcik = !aramaAcik
                        if (!aramaAcik) {
                            aramaMetni = ""
                            mevcutLimit = 30
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (aramaAcik) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (aramaAcik) "Aramayı Kapat" else "Masraf Ara",
                        tint = Color.White
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialogAcikMi = true },
                containerColor = BrandColors.Navy,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Yeni masraf", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (yukleniyor && tumMasraflar.isEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandColors.Danger
                )
            }

            if (aramaAcik) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp)
                ) {
                    OutlinedTextField(
                        value = aramaMetni,
                        onValueChange = {
                            aramaMetni = it
                            mevcutLimit = 30
                        },
                        placeholder = {
                            Text(
                                "Açıklama veya kategori ara...",
                                fontSize = 14.sp,
                                color = BrandColors.Muted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = BrandColors.Muted
                            )
                        },
                        trailingIcon = {
                            if (aramaMetni.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        aramaMetni = ""
                                        mevcutLimit = 30
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Cancel,
                                        contentDescription = "Temizle",
                                        tint = BrandColors.Muted
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BrandColors.Background,
                            unfocusedContainerColor = BrandColors.Background,
                            focusedBorderColor = BrandColors.Danger,
                            unfocusedBorderColor = BrandColors.Border,
                            focusedTextColor = BrandColors.Ink,
                            unfocusedTextColor = BrandColors.Ink,
                            cursorColor = BrandColors.Danger
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    )
                    if (aramaMetni.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$islemSayisi masraf bulundu",
                            fontSize = 12.sp,
                            color = BrandColors.Muted,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MasrafOzetKart(
                    baslik = if (aramaMetni.isBlank()) "Toplam" else "Arama Toplamı",
                    deger = "₺${formatKisaPara(toplamMasraf)}",
                    renk = BrandColors.Danger,
                    modifier = Modifier.weight(1f)
                )
                MasrafOzetKart(
                    baslik = "İşlem Sayısı",
                    deger = "$islemSayisi",
                    renk = BrandColors.Muted,
                    modifier = Modifier.weight(1f)
                )
            }

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

            when {
                gruplanmisMasraflar.isEmpty() && !yukleniyor && aramaMetni.isNotBlank() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = BrandColors.Muted,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Masraf bulunamadı",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandColors.Ink
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "\"$aramaMetni\" ile eşleşen masraf yok",
                                fontSize = 13.sp,
                                color = BrandColors.Muted
                            )
                        }
                    }
                }

                gruplanmisMasraflar.isEmpty() && !yukleniyor -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = BrandColors.Muted,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Bu dönemde masraf yok",
                                color = BrandColors.Muted,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        gruplanmisMasraflar.forEach { (ayBasligi, masraflarListesi) ->
                            item {
                                Text(
                                    text = ayBasligi,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandColors.Danger,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                )
                            }
                            items(
                                items = masraflarListesi,
                                key = { it.id ?: it.hashCode().toLong() }
                            ) { masraf ->
                                MasrafKart(
                                    masraf = masraf,
                                    onSil = { silinecekMasraf = masraf }
                                )
                            }
                        }

                        if (islemSayisi > mevcutLimit) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TextButton(
                                        onClick = { mevcutLimit += 30 },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = BrandColors.Danger
                                        )
                                    ) {
                                        Text(
                                            "Daha Fazla Masraf Yükle (+30)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (dialogAcikMi) {
        MasrafEkleDialog(
            gecmis = tumMasraflar,
            onDismiss = { dialogAcikMi = false },
            onKaydet = { kategori, aciklama, tutar ->
                scope.launch {
                    try {
                        apiService.createMasraf(
                            Masraf(
                                kategori = kategori,
                                aciklama = aciklama,
                                tutar = tutar,
                                tarih = com.eray.muhasebeapp.getEpochMillis().toString()
                            )
                        )
                        yenilemeTetikleyici++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                dialogAcikMi = false
            }
        )
    }

    silinecekMasraf?.let { masraf ->
        AlertDialog(
            onDismissRequest = { silinecekMasraf = null },
            containerColor = Color.White,
            title = {
                Text("Masrafı Sil", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Bu masraf kaydını silmek istediğinize emin misiniz? Bu işlem geri alınamaz.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                apiService.deleteMasraf(masraf.id ?: 0L)
                                yenilemeTetikleyici++
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        silinecekMasraf = null
                    }
                ) {
                    Text("Sil", color = BrandColors.Danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { silinecekMasraf = null }) {
                    Text("İptal", color = BrandColors.Muted)
                }
            }
        )
    }
}

@Composable
private fun KategoriFiltreCip(baslik: String, seciliMi: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (seciliMi) BrandColors.Danger else Color.White,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            baslik,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (seciliMi) Color.White else BrandColors.Ink
        )
    }
}

@Composable
private fun MasrafKart(
    masraf: Masraf,
    onSil: () -> Unit
) {
    BrandRecordCard(
        title = masraf.kategori,
        detail = masraf.aciklama,
        value = "₺${formatKisaPara(masraf.tutar)}",
        caption = formatTarih(masraf.tarih).substringBefore(" "),
        icon = kategoriIkonu(masraf.kategori),
        accent = BrandColors.Danger
    ) {
        IconButton(onClick = onSil, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Masrafı sil",
                tint = BrandColors.Danger,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun MasrafEkleDialog(
    gecmis: List<Masraf>,
    onDismiss: () -> Unit,
    onKaydet: (kategori: String, aciklama: String, tutar: Double) -> Unit
) {
    val son = gecmis.sortedByDescending { it.id }.map { it.kategori }.distinct().take(5)
    val sik = gecmis.groupingBy { it.kategori }.eachCount().entries.sortedByDescending { it.value }.take(5).map { it.key }
    var seciliKategori by remember { mutableStateOf(masrafKategorileri.first().first) }
    var aciklama by remember { mutableStateOf("") }
    var tutarText by remember { mutableStateOf("") }
    var dropdownAcikMi by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Yeni Masraf", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("En sık kullanılanlar")
                sik.forEach { kategori -> TextButton(onClick = { seciliKategori = kategori }) { Text(kategori) } }
                Text("Son kullanılanlar")
                son.forEach { kategori -> TextButton(onClick = { seciliKategori = kategori }) { Text(kategori) } }
                Box {
                    OutlinedTextField(
                        value = seciliKategori,
                        onValueChange = { seciliKategori = it },
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
                        (sik + son).distinct().forEach { kategori ->
                            DropdownMenuItem(text = { Text(kategori) }, onClick = { seciliKategori = kategori; dropdownAcikMi = false })
                        }
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
                    onValueChange = { tutarText = ondalikGirdi(it) },
                    label = { Text("Tutar (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val temizTutarText = tutarText.replace(',', '.')
                val tutar = ondalikSayi(temizTutarText) ?: 0.0
                if (tutar <= 0 || seciliKategori.isBlank()) return@TextButton
                onKaydet(seciliKategori, aciklama.ifBlank { seciliKategori }, tutar)
            }) { Text("Kaydet", color = BrandColors.Danger, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = BrandColors.Muted) }
        }
    )
}

@Composable
private fun MasrafOzetKart(baslik: String, deger: String, renk: Color, modifier: Modifier = Modifier) {
    BrandMetric(baslik, deger, renk, modifier)
}

// =============================================================
// PARA FORMATLAMA (BÜYÜK SAYI & KISALTMA DESTEĞİ)
// =============================================================

/**
 * Tam tutar: binlik ayraçlı, iki ondalıklı. 70000000.0 -> "70.000.000,00"
 * Long üzerinden hesaplanır, Double yuvarlama hatası oluşmaz.
 */
private fun formatMasrafIkiBasamak(deger: Double): String {
    val negatifMi = deger < 0
    val mutlakDeger = if (negatifMi) -deger else deger

    val kurus = ((mutlakDeger * 100.0) + 0.5).toLong()
    val tamKisim = kurus / 100
    val kesirKisim = kurus % 100

    val tamStr = tamKisim.toString()
    val sb = StringBuilder()
    for (i in tamStr.indices) {
        if (i > 0 && (tamStr.length - i) % 3 == 0) sb.append('.')
        sb.append(tamStr[i])
    }

    val isaret = if (negatifMi) "-" else ""
    return "$isaret$sb,${kesirKisim.toString().padStart(2, '0')}"
}

/**
 * Dar alanlar (özet metrikleri, masraf kartları) için kısaltılmış tutar.
 * 70000000.0 -> "70,0 Mn"   1250.5 -> "1.250,50"   450000.0 -> "450,0 B"
 */
private fun formatKisaPara(deger: Double): String {
    val negatifMi = deger < 0
    val mutlak = if (negatifMi) -deger else deger
    val isaret = if (negatifMi) "-" else ""

    return when {
        mutlak >= 1_000_000_000 -> "$isaret${birOndalik(mutlak / 1_000_000_000)} Mr"
        mutlak >= 1_000_000 -> "$isaret${birOndalik(mutlak / 1_000_000)} Mn"
        mutlak >= 100_000 -> "$isaret${birOndalik(mutlak / 1_000)} B"
        else -> formatMasrafIkiBasamak(deger)
    }
}

private fun birOndalik(deger: Double): String {
    val x = ((deger * 10.0) + 0.5).toLong()
    return "${x / 10},${x % 10}"
}
