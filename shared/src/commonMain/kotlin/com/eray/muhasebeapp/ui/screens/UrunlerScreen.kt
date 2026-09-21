package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import com.eray.muhasebeapp.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.data.model.Urun
import com.eray.muhasebeapp.data.network.ApiService
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrunlerScreen(
    apiService: ApiService,
    onNavigateBack: () -> Unit
) {
    var aramaMetni by remember { mutableStateOf("") }
    var urunEklemeDialogGoster by remember { mutableStateOf(false) }
    var duzenlenecekUrun by remember { mutableStateOf<Urun?>(null) }
    var silinecekUrun by remember { mutableStateOf<Urun?>(null) }

    var refreshTrigger by remember { mutableStateOf(0) }
    var kritikStokFiltresiAcik by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var urunListesi by remember { mutableStateOf<List<Urun>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(aramaMetni, refreshTrigger, kritikStokFiltresiAcik) {
        loading = true
        try {
            val hamListe = if (aramaMetni.isEmpty()) {
                apiService.getUrunler()
            } else {
                apiService.searchUrunler(aramaMetni)
            }

            urunListesi = if (kritikStokFiltresiAcik) {
                hamListe.filter { it.stokAdedi <= 5L }
            } else {
                hamListe
            }
        } catch (e: Exception) {
            println("Ürün Yükleme Hatası: ${e.message}")
            e.printStackTrace()
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandColors.Background)
            .swipeToBack(onBack = onNavigateBack)
    ) {

        // --- Üst Bar ---
        BrandTopBar(
            title = if (kritikStokFiltresiAcik) "Kritik stok" else "Ürünler",
            subtitle = "Ürün kataloğu ve fiyat yönetimi",
            onBack = onNavigateBack
        ) {
            IconButton(onClick = { urunEklemeDialogGoster = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ürün Ekle", tint = Color.White)
            }
        }

        if (loading && urunListesi.isEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // --- Özet Kartları ---
        val toplamStokDeger = urunListesi.sumOf { it.alisFiyati * it.stokAdedi }
        val kritikStokSayisi = urunListesi.count { it.stokAdedi <= 5L }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            UrunOzetKart(
                baslik = "Toplam Ürün",
                deger = "${urunListesi.size}",
                renk = BrandColors.Navy,
                modifier = Modifier.weight(1f)
            )
            UrunOzetKart(
                baslik = "Stok Değeri",
                deger = "₺${formatKisaPara(toplamStokDeger)}",
                renk = BrandColors.Success,
                modifier = Modifier.weight(1f)
            )
            UrunOzetKart(
                baslik = if (kritikStokFiltresiAcik) "Filtreyi Kaldır" else "Kritik Stok",
                deger = "$kritikStokSayisi",
                renk = if (kritikStokFiltresiAcik) Color.White else (if (kritikStokSayisi > 0) BrandColors.Danger else BrandColors.Muted),
                containerColor = if (kritikStokFiltresiAcik) BrandColors.Danger else Color.White,
                modifier = Modifier
                    .weight(1f)
                    .clickable { kritikStokFiltresiAcik = !kritikStokFiltresiAcik }
            )
        }

        // --- Arama Çubuğu ---
        TextField(
            value = aramaMetni,
            onValueChange = { aramaMetni = it },
            placeholder = { Text("Ürün adı veya barkod ara...", color = BrandColors.Muted, fontSize = 15.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara", tint = BrandColors.Muted) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BrandColors.Soft,
                unfocusedContainerColor = BrandColors.Soft,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        // --- Ürün Listesi ---
        if (urunListesi.isEmpty() && !loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = when {
                        kritikStokFiltresiAcik -> "Kritik stokta ürün bulunmuyor."
                        aramaMetni.isEmpty() -> "Ürün bulunamadı. Eklemek için + butonuna basın."
                        else -> "\"$aramaMetni\" için sonuç yok."
                    },
                    color = BrandColors.Muted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = urunListesi,
                    key = { _, urun -> urun.id ?: urun.barkod.ifEmpty { urun.ad } }
                ) { _, urun ->
                    UrunSatiri(
                        urun = urun,
                        onDuzenle = { duzenlenecekUrun = urun },
                        onSil = { silinecekUrun = urun }
                    )
                }
            }
        }
    }

    // --- Yeni Ürün Ekleme Dialog ---
    if (urunEklemeDialogGoster) {
        var isSaving by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        UrunFormDialog(
            baslik = "Yeni Ürün Ekle",
            isSaving = isSaving,
            errorMessage = errorMessage,
            onKaydet = { barkod, ad, alis, satis, stok, birim, kdv ->
                scope.launch {
                    isSaving = true
                    errorMessage = null
                    try {
                        val result = apiService.createUrun(
                            Urun(
                                barkod = barkod,
                                ad = ad,
                                alisFiyati = alis,
                                satisFiyati = satis,
                                stokAdedi = stok,
                                birim = birim,
                                kdvOrani = kdv.toInt()
                            )
                        )
                        if (result.isSuccess) {
                            refreshTrigger++
                            urunEklemeDialogGoster = false
                        } else {
                            println("Ürün Kayıt Hatası (Sunucu): ${result.exceptionOrNull()?.message}")
                            errorMessage = "İşlem başarısız. Tekrar deneyin."
                        }
                    } catch (e: Exception) {
                        println("Ürün Kayıt Hatası (Kritik): ${e.message}")
                        errorMessage = "İşlem başarısız. Tekrar deneyin."
                        e.printStackTrace()
                    } finally {
                        isSaving = false
                    }
                }
            },
            onIptal = { if (!isSaving) urunEklemeDialogGoster = false }
        )
    }

    // --- Ürün Düzenleme Dialog ---
    duzenlenecekUrun?.let { urun ->
        UrunFormDialog(
            baslik = "Ürünü Düzenle",
            mevcutBarkod = urun.barkod,
            mevcutAd = urun.ad,
            mevcutAlis = urun.alisFiyati.toString(),
            mevcutSatis = urun.satisFiyati.toString(),
            mevcutStok = urun.stokAdedi.toString(),
            mevcutBirim = urun.birim,
            mevcutKdv = urun.kdvOrani.toString(),
            onKaydet = { barkod, ad, alis, satis, stok, birim, kdv ->
                scope.launch {
                    try {
                        apiService.updateUrun(
                            urun.id ?: 0L,
                            Urun(
                                id = urun.id,
                                barkod = barkod,
                                ad = ad,
                                alisFiyati = alis,
                                satisFiyati = satis,
                                stokAdedi = stok,
                                birim = birim,
                                kdvOrani = kdv.toInt()
                            )
                        )
                        refreshTrigger++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                duzenlenecekUrun = null
            },
            onIptal = { duzenlenecekUrun = null }
        )
    }

    // --- Silme Onay Dialog ---
    silinecekUrun?.let { urun ->
        AlertDialog(
            onDismissRequest = { silinecekUrun = null },
            title = { Text("Ürünü Sil", fontWeight = FontWeight.Bold) },
            text = { Text("\"${urun.ad}\" ürününü silmek istediğinize emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                apiService.deleteUrun(urun.id ?: 0L)
                                refreshTrigger++
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        silinecekUrun = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandColors.Danger)
                ) { Text("Sil", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { silinecekUrun = null }) {
                    Text("Vazgeç", color = BrandColors.Navy)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UrunFormDialog(
    baslik: String,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    mevcutBarkod: String = "",
    mevcutAd: String = "",
    mevcutAlis: String = "",
    mevcutSatis: String = "",
    mevcutStok: String = "",
    mevcutBirim: String = "Adet",
    mevcutKdv: String = "20",
    onKaydet: (
        barkod: String,
        ad: String,
        alis: Double,
        satis: Double,
        stok: Double,
        birim: String,
        kdv: Long
    ) -> Unit,
    onIptal: () -> Unit
) {
    var ad by remember { mutableStateOf(mevcutAd) }
    var barkod by remember { mutableStateOf(mevcutBarkod) }
    var alis by remember { mutableStateOf(mevcutAlis) }
    var satis by remember { mutableStateOf(mevcutSatis) }
    var stok by remember { mutableStateOf(mevcutStok) }
    var birim by remember { mutableStateOf(mevcutBirim) }
    var kdv by remember { mutableStateOf(mevcutKdv) }

    var adHata by remember { mutableStateOf(false) }
    var sayiHata by remember { mutableStateOf<String?>(null) }

    val birimSecenekleri = listOf("Adet", "Kg", "Lt", "Mt", "Kutu", "Paket")
    val kdvSecenekleri = listOf("0", "1", "10", "20")

    var birimMenuAcik by remember { mutableStateOf(false) }
    var kdvMenuAcik by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val barkodFocusRequester = remember { FocusRequester() }
    val alisFocusRequester = remember { FocusRequester() }
    val satisFocusRequester = remember { FocusRequester() }
    val stokFocusRequester = remember { FocusRequester() }

    fun sonrakiAlanaGit(focusRequester: FocusRequester) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun kaydet() {
        if (ad.isBlank()) {
            adHata = true
            return
        }

        keyboardController?.hide()
        focusManager.clearFocus()

        val temizAlisText = alis.ifBlank { "0" }.replace(',', '.')
        val temizSatisText = satis.ifBlank { "0" }.replace(',', '.')

        if (ondalikSayi(temizAlisText) == null || ondalikSayi(temizSatisText) == null || (stok.isNotBlank() && ondalikSayi(stok) == null)) {
            sayiHata = "Geçerli bir fiyat ve stok miktarı girin (örnek: 1,25)."; return
        }
        if (ondalikSayi(temizAlisText)!! < 0 || ondalikSayi(temizSatisText)!! < 0) {
            sayiHata = "Fiyat negatif olamaz."; return
        }
        onKaydet(
            barkod.trim(),
            ad.trim(),
            temizAlisText.toDoubleOrNull() ?: 0.0,
            temizSatisText.toDoubleOrNull() ?: 0.0,
            ondalikSayi(stok) ?: 0.0,
            birim,
            kdv.toLongOrNull() ?: 20L
        )
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                keyboardController?.hide()
                focusManager.clearFocus()
                onIptal()
            }
        },
        title = {
            Text(
                text = baslik,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BrandColors.Ink
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sayiHata?.let { Text(it, color = Color.Red) }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = ad,
                    onValueChange = {
                        ad = it
                        adHata = false
                    },
                    label = { Text("Ürün Adı *") },
                    isError = adHata,
                    supportingText = if (adHata) { { Text("Ürün adı zorunludur") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(barkodFocusRequester) }),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving
                )

                OutlinedTextField(
                    value = barkod,
                    onValueChange = { yeniDeger -> barkod = yeniDeger.filter { it.isDigit() } },
                    label = { Text("Barkod No") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(alisFocusRequester) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(barkodFocusRequester),
                    singleLine = true,
                    enabled = !isSaving
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = alis,
                        onValueChange = { yeniDeger ->
                            alis = ondalikGirdi(yeniDeger)
                        },
                        label = { Text("Alış Fiyatı (₺)") },
                        modifier = Modifier.weight(1f).focusRequester(alisFocusRequester),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(satisFocusRequester) }),
                        singleLine = true,
                        enabled = !isSaving
                    )

                    OutlinedTextField(
                        value = satis,
                        onValueChange = { yeniDeger ->
                            satis = ondalikGirdi(yeniDeger)
                        },
                        label = { Text("Satış Fiyatı (₺)") },
                        modifier = Modifier.weight(1f).focusRequester(satisFocusRequester),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(stokFocusRequester) }),
                        singleLine = true,
                        enabled = !isSaving
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stok,
                        onValueChange = { yeniDeger -> stok = ondalikGirdi(yeniDeger) },
                        label = { Text("Stok Adedi") },
                        modifier = Modifier.weight(1f).focusRequester(stokFocusRequester),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }),
                        singleLine = true,
                        enabled = !isSaving
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = "%$kdv",
                            onValueChange = {},
                            label = { Text("KDV Oranı") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "KDV seç") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSaving
                        )

                        if (!isSaving) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        kdvMenuAcik = true
                                    }
                            )
                        }

                        DropdownMenu(
                            expanded = kdvMenuAcik,
                            onDismissRequest = { kdvMenuAcik = false }
                        ) {
                            kdvSecenekleri.forEach { oran ->
                                DropdownMenuItem(
                                    text = { Text("%$oran") },
                                    onClick = {
                                        kdv = oran
                                        kdvMenuAcik = false
                                    }
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = birim,
                        onValueChange = {},
                        label = { Text("Birim") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Birim seç") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    )

                    if (!isSaving) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    birimMenuAcik = true
                                }
                        )
                    }

                    DropdownMenu(
                        expanded = birimMenuAcik,
                        onDismissRequest = { birimMenuAcik = false }
                    ) {
                        birimSecenekleri.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b) },
                                onClick = {
                                    birim = b
                                    birimMenuAcik = false
                                }
                            )
                        }
                    }
                }

                if (isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = { kaydet() },
                colors = ButtonDefaults.buttonColors(containerColor = BrandColors.Navy)
            ) {
                Text(
                    text = if (isSaving) "Kaydediliyor..." else "Kaydet",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onIptal()
                }
            ) {
                Text(text = "Vazgeç", color = BrandColors.Danger)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}

@Composable
private fun UrunOzetKart(
    baslik: String,
    deger: String,
    renk: Color,
    containerColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    BrandMetric(baslik, deger, renk, modifier, containerColor)
}

@Composable
private fun UrunSatiri(
    urun: Urun,
    onDuzenle: () -> Unit,
    onSil: () -> Unit
) {
    BrandRecordCard(
        title = urun.ad,
        detail = "Barkod: ${urun.barkod.ifEmpty { "—" }} · KDV %${urun.kdvOrani}",
        value = "₺${formatKisaPara(urun.satisFiyati)}",
        caption = "Satış fiyatı · Stok: ${urun.stokAdedi} ${urun.birim}",
        icon = Icons.Default.Inventory2,
        accent = if (urun.stokAdedi <= 5L) BrandColors.Danger else BrandColors.Teal
    ) {
        IconButton(onClick = onDuzenle, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.Edit, "Ürünü düzenle", tint = BrandColors.Navy, modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onSil, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.DeleteOutline, "Ürünü sil", tint = BrandColors.Danger, modifier = Modifier.size(19.dp))
        }
    }
}

// =============================================================
// PARA FORMATLAMA (BÜYÜK SAYI & KISALTMA DESTEĞİ)
// =============================================================

/**
 * Tam tutar: binlik ayraçlı, iki ondalıklı. 70000000.0 -> "70.000.000,00"
 * Long üzerinden hesaplanır, Double yuvarlama hatası oluşmaz.
 */
private fun formatUrunParaIkiBasamak(deger: Double): String {
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
 * Dar alanlar (özet kartı, ürün listesi) için kısaltılmış tutar.
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
        else -> formatUrunParaIkiBasamak(deger)
    }
}

private fun birOndalik(deger: Double): String {
    val x = ((deger * 10.0) + 0.5).toLong()
    return "${x / 10},${x % 10}"
}
