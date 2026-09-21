package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import com.eray.muhasebeapp.ui.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.data.model.*
import com.eray.muhasebeapp.data.network.ApiService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StokScreen(
    apiService: ApiService,
    onNavigateBack: () -> Unit
) {
    var yenilemeTetikleyici by remember { mutableStateOf(0) }
    var urunler by remember { mutableStateOf<List<Urun>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var hataMesaji by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(yenilemeTetikleyici) {
        loading = true
        try {
            urunler = apiService.getUrunler()
        } catch (e: Exception) {
            println("Stok Listesi Yükleme Hatası: ${e.message}")
            e.printStackTrace()
        } finally {
            loading = false
        }
    }

    var aramaMetni by remember { mutableStateOf("") }
    var seciliUrun by remember { mutableStateOf<Urun?>(null) }

    val filtreliUrunler = remember(urunler, aramaMetni) {
        val aranacak = aramaMetni.trim()
        if (aranacak.isBlank()) urunler
        else urunler.filter {
            it.ad.contains(aranacak, ignoreCase = true) || it.barkod.contains(aranacak, ignoreCase = true)
        }
    }

    val kritikStoklar = remember(urunler) { urunler.filter { it.stokAdedi <= 5L } }
    val toplamStokAdedi = remember(urunler) { urunler.sumOf { it.stokAdedi } }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = BrandColors.Background,
        modifier = Modifier.swipeToBack(onBack = onNavigateBack),
        topBar = {
            BrandTopBar("Stoklar", "Ürün miktarları ve stok hareketleri", onNavigateBack) {}
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (loading && urunler.isEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // ÜST ÖZET KARTLARI
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StokOzetKart("Toplam Ürün", formatKisaStok(urunler.size.toDouble()), BrandColors.Teal, Modifier.weight(1f))
                StokOzetKart("Toplam Adet", formatKisaStok(toplamStokAdedi), BrandColors.Navy, Modifier.weight(1f))
                StokOzetKart(
                    "Kritik Stok",
                    formatKisaStok(kritikStoklar.size.toDouble()),
                    if (kritikStoklar.isNotEmpty()) BrandColors.Danger else BrandColors.Success,
                    Modifier.weight(1f)
                )
            }

            // ARAMA ÇUBUĞU
            OutlinedTextField(
                value = aramaMetni,
                onValueChange = { aramaMetni = it },
                label = { Text("Ürün ara (isim veya barkod)") },
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandColors.Muted) },
                trailingIcon = {
                    if (aramaMetni.isNotEmpty()) {
                        IconButton(onClick = { aramaMetni = "" }) {
                            Icon(Icons.Default.Cancel, contentDescription = "Aramayı Temizle", tint = BrandColors.Muted)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Text(
                text = "STOK LİSTESİ",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandColors.Muted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (filtreliUrunler.isEmpty() && !loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (aramaMetni.isNotBlank()) "\"$aramaMetni\" ile eşleşen ürün bulunamadı" else "Henüz ürün eklenmedi",
                        color = BrandColors.Muted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filtreliUrunler,
                        key = { it.id ?: it.barkod.ifEmpty { it.ad } }
                    ) { urun ->
                        StokKart(
                            urun = urun,
                            onDuzenle = { seciliUrun = urun }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    seciliUrun?.let { urun ->
        StokDuzenleDialog(
            urun = urun,
            onDismiss = { seciliUrun = null },
            onKaydet = { hareketTuru, miktar, aciklama ->
                scope.launch {
                    try {
                        apiService.createStokHareketi(
                            StokHareketi(
                                urunId = urun.id ?: 0L,
                                urunAdi = urun.ad,
                                hareketTuru = hareketTuru,
                                miktar = miktar,
                                birimFiyat = 0.0,
                                aciklama = aciklama.ifBlank { "Manuel düzeltme" },
                                tarih = com.eray.muhasebeapp.getEpochMillis().toString()
                            )
                        )
                        yenilemeTetikleyici++
                    } catch (e: Exception) {
                        println("Stok Düzeltme Hatası: ${e.message}")
                        hataMesaji = "İşlem başarısız. Tekrar deneyin."
                        e.printStackTrace()
                    }
                }
                seciliUrun = null
            }
        )
    }

    if (hataMesaji != null) {
        AlertDialog(
            onDismissRequest = { hataMesaji = null },
            containerColor = Color.White,
            icon = { Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red) },
            title = { Text("Hata Oluştu") },
            text = { Text(hataMesaji ?: "") },
            confirmButton = {
                TextButton(onClick = { hataMesaji = null }) {
                    Text("Tamam", color = BrandColors.Navy, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun StokKart(urun: Urun, onDuzenle: () -> Unit) {
    val critical = urun.stokAdedi <= 5L
    BrandRecordCard(
        title = urun.ad,
        detail = urun.barkod.ifEmpty { "—" },
        value = "${formatKisaStok(urun.stokAdedi)} ${urun.birim}",
        caption = if (critical) "Kritik stok · Stok hareketi eklemek için dokunun" else "Mevcut stok · Hareket eklemek için dokunun",
        icon = if (critical) Icons.Default.Warning else Icons.Default.Inventory2,
        accent = if (critical) BrandColors.Danger else BrandColors.Teal,
        onClick = onDuzenle
    )
}

@Composable
private fun StokDuzenleDialog(
    urun: Urun,
    onDismiss: () -> Unit,
    onKaydet: (hareketTuru: String, miktar: Double, aciklama: String) -> Unit
) {
    var hareketTuru by remember { mutableStateOf("Giriş") }
    var miktarText by remember { mutableStateOf("") }
    var aciklama by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val aciklamaFocusRequester = remember { FocusRequester() }

    fun kaydet() {
        val temizMiktarText = miktarText.replace(',', '.')
        val miktar = ondalikSayi(temizMiktarText) ?: 0.0

        if (miktar > 0) {
            keyboardController?.hide()
            focusManager.clearFocus()
            onKaydet(hareketTuru, miktar, aciklama)
        }
    }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        containerColor = Color.White,
        title = {
            Text(
                "${urun.ad} - Stok Düzelt",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Mevcut stok: ${formatStokAdedi(urun.stokAdedi)} ${urun.birim}",
                    fontSize = 13.sp,
                    color = BrandColors.Muted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HareketTuruButon(
                        baslik = "Giriş",
                        seciliMi = hareketTuru == "Giriş",
                        renk = BrandColors.Success,
                        modifier = Modifier.weight(1f)
                    ) { hareketTuru = "Giriş" }

                    HareketTuruButon(
                        baslik = "Çıkış",
                        seciliMi = hareketTuru == "Çıkış",
                        renk = BrandColors.Danger,
                        modifier = Modifier.weight(1f)
                    ) { hareketTuru = "Çıkış" }
                }

                OutlinedTextField(
                    value = miktarText,
                    onValueChange = { miktarText = ondalikGirdi(it) },
                    label = { Text("Miktar (${urun.birim})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        aciklamaFocusRequester.requestFocus()
                        keyboardController?.show()
                    }),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = aciklama,
                    onValueChange = { aciklama = it },
                    label = { Text("Açıklama (fire, sayım, iade vb.)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { kaydet() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(aciklamaFocusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { kaydet() }) {
                Text("Kaydet", color = BrandColors.Navy, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) {
                Text("İptal", color = BrandColors.Muted)
            }
        }
    )
}

@Composable
private fun HareketTuruButon(
    baslik: String,
    seciliMi: Boolean,
    renk: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(if (seciliMi) renk else BrandColors.Background, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            baslik,
            color = if (seciliMi) Color.White else BrandColors.Ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun StokOzetKart(baslik: String, deger: String, renk: Color, modifier: Modifier = Modifier) {
    BrandMetric(baslik, deger, renk, modifier)
}

// =============================================================
// STOK SAYISI FORMATLAMA (BÜYÜK SAYI & KISALTMA DESTEĞİ)
// =============================================================

/**
 * Tam stok adedi: binlik ayraçlı. 70000000 -> "70.000.000"
 */
private fun formatStokAdedi(deger: Double): String = miktarMetni(deger)

/**
 * Dar alanlar (özet metrikleri, stok kartları) için kısaltılmış stok adedi.
 * 70000000 -> "70,0 Mn"   450000 -> "450,0 B"   1250 -> "1.250"
 */
private fun formatKisaStok(deger: Double): String {
    val negatifMi = deger < 0
    val mutlak = if (negatifMi) -deger else deger
    val isaret = if (negatifMi) "-" else ""

    return when {
        mutlak >= 1_000_000_000L -> "$isaret${birOndalik(mutlak.toDouble() / 1_000_000_000.0)} Mr"
        mutlak >= 1_000_000L -> "$isaret${birOndalik(mutlak.toDouble() / 1_000_000.0)} Mn"
        mutlak >= 100_000L -> "$isaret${birOndalik(mutlak.toDouble() / 1_000.0)} B"
        else -> formatStokAdedi(deger)
    }
}

private fun birOndalik(deger: Double): String {
    val x = ((deger * 10.0) + 0.5).toLong()
    return "${x / 10},${x % 10}"
}
