package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
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
import com.eray.muhasebeapp.data.model.Urun
import com.eray.muhasebeapp.data.network.ApiService
import kotlinx.coroutines.launch

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
    var horizontalDragAccumulator by remember { mutableStateOf(0f) }

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
            e.printStackTrace()
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragAccumulator = 0f },
                    onDragEnd = {
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
            }
    ) {

        // --- Üst Bar ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Geri",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = if (kritikStokFiltresiAcik) "Kritik Stok Listesi" else "Ürün Listesi",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (kritikStokFiltresiAcik) Color(0xFFFF3B30) else Color.Black
            )

            IconButton(
                onClick = { urunEklemeDialogGoster = true },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Ekle", tint = Color(0xFF007AFF))
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
                renk = Color(0xFF007AFF),
                modifier = Modifier.weight(1f)
            )
            UrunOzetKart(
                baslik = "Stok Değeri",
                deger = "₺${toplamStokDeger.toUrunParaFormat()}",
                renk = Color(0xFF34C759),
                modifier = Modifier.weight(1f)
            )
            UrunOzetKart(
                baslik = if (kritikStokFiltresiAcik) "Filtreyi Kaldır" else "Kritik Stok",
                deger = "$kritikStokSayisi",
                renk = if (kritikStokFiltresiAcik) Color.White else (if (kritikStokSayisi > 0) Color(0xFFFF3B30) else Color(0xFF8E8E93)),
                containerColor = if (kritikStokFiltresiAcik) Color(0xFFFF3B30) else Color.White,
                modifier = Modifier
                    .weight(1f)
                    .clickable { kritikStokFiltresiAcik = !kritikStokFiltresiAcik }
            )
        }

        // --- Arama Çubuğu ---
        TextField(
            value = aramaMetni,
            onValueChange = { aramaMetni = it },
            placeholder = { Text("Ürün adı veya barkod ara...", color = Color(0xFF8E8E93), fontSize = 15.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara", tint = Color(0xFF8E8E93)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFE3E3E8),
                unfocusedContainerColor = Color(0xFFE3E3E8),
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
                    color = Color(0xFF8E8E93),
                    fontSize = 15.sp
                )
            }
        } else {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)
            ) {
                LazyColumn {
                    itemsIndexed(urunListesi) { index, urun ->
                        UrunSatiri(
                            urun = urun,
                            onDuzenle = { duzenlenecekUrun = urun },
                            onSil = { silinecekUrun = urun }
                        )
                        if (index < urunListesi.lastIndex) {
                            HorizontalDivider(
                                color = Color(0xFFC6C6C8),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
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
                            errorMessage = result.exceptionOrNull()?.message ?: "Bilinmeyen hata"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Bağlantı hatası: ${e.message}"
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) { Text("Sil", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { silinecekUrun = null }) {
                    Text("Vazgeç", color = Color(0xFF007AFF))
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
    onKaydet: (barkod: String, ad: String, alis: Double, satis: Double, stok: Long, birim: String, kdv: Long) -> Unit,
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

    val birimSecenekleri = listOf("Adet", "Kg", "Lt", "Mt", "Kutu", "Paket")
    val kdvSecenekleri = listOf("0", "1", "10", "20")
    var birimMenuAcik by remember { mutableStateOf(false) }
    var kdvMenuAcik by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onIptal,
        title = { Text(baslik, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (errorMessage != null) {
                    Text(errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = ad,
                    onValueChange = { ad = it; adHata = false },
                    label = { Text("Ürün Adı *") },
                    isError = adHata,
                    supportingText = if (adHata) {{ Text("Ürün adı zorunludur") }} else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = barkod,
                    onValueChange = { barkod = it },
                    label = { Text("Barkod No") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = alis,
                        onValueChange = { alis = it },
                        label = { Text("Alış Fiyatı (₺)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        enabled = !isSaving
                    )
                    OutlinedTextField(
                        value = satis,
                        onValueChange = { satis = it },
                        label = { Text("Satış Fiyatı (₺)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        enabled = !isSaving
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stok,
                        onValueChange = { stok = it },
                        label = { Text("Stok Adedi") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !isSaving
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = "%$kdv",
                            onValueChange = {},
                            label = { Text("KDV Oranı") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSaving
                        )
                        if (!isSaving) Box(modifier = Modifier.matchParentSize().clickable { kdvMenuAcik = true })

                        DropdownMenu(
                            expanded = kdvMenuAcik,
                            onDismissRequest = { kdvMenuAcik = false }
                        ) {
                            kdvSecenekleri.forEach { oran ->
                                DropdownMenuItem(
                                    text = { Text("%$oran") },
                                    onClick = { kdv = oran; kdvMenuAcik = false }
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
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    )
                    if (!isSaving) Box(modifier = Modifier.matchParentSize().clickable { birimMenuAcik = true })

                    DropdownMenu(
                        expanded = birimMenuAcik,
                        onDismissRequest = { birimMenuAcik = false }
                    ) {
                        birimSecenekleri.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b) },
                                onClick = { birim = b; birimMenuAcik = false }
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
                onClick = {
                    if (ad.isEmpty()) {
                        adHata = true
                        return@Button
                    }

                    val temizAlisText = alis.replace(',', '.')
                    val temizSatisText = satis.replace(',', '.')

                    onKaydet(
                        barkod,
                        ad,
                        temizAlisText.toDoubleOrNull() ?: 0.0,
                        temizSatisText.toDoubleOrNull() ?: 0.0,
                        stok.toLongOrNull() ?: 0L,
                        birim,
                        kdv.toLongOrNull() ?: 20L
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
            ) { Text(if (isSaving) "Kaydediliyor..." else "Kaydet", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onIptal, enabled = !isSaving) { Text("Vazgeç", color = Color(0xFFFF3B30)) }
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
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = deger, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = renk)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = baslik,
                fontSize = 11.sp,
                color = if (containerColor == Color.White) Color(0xFF8E8E93) else Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun UrunSatiri(
    urun: Urun,
    onDuzenle: () -> Unit,
    onSil: () -> Unit
) {
    val stokUyarisi = urun.stokAdedi <= 5L
    val stokRengi = if (stokUyarisi) Color(0xFFFF3B30) else Color(0xFF8E8E93)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = urun.ad, fontSize = 17.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "KDV: %${urun.kdvOrani}", fontSize = 12.sp, color = Color(0xFF8E8E93))
                Text(text = "•", fontSize = 12.sp, color = Color(0xFFC6C6C8))
                Text(text = "Barkod: ${urun.barkod.ifEmpty { "-" }}", fontSize = 12.sp, color = Color(0xFF8E8E93))
            }
        }

        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 4.dp)) {
            Text(
                text = "₺${urun.satisFiyati.toUrunParaFormat()}",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Stok: ${urun.stokAdedi} ${urun.birim}",
                fontSize = 13.sp,
                color = stokRengi
            )
        }

        Row {
            IconButton(onClick = onDuzenle) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Düzenle",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onSil) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun Double.toUrunParaFormat(): String {
    val negatifMi = this < 0
    val mutlakDeger = if (negatifMi) -this else this
    val yuvarlanmis = ((mutlakDeger * 100.0) + 0.5).toLong() / 100.0
    val tamKisim = yuvarlanmis.toLong()
    val kesirKisim = (((yuvarlanmis - tamKisim) * 100.0) + 0.5).toLong()
    val kesirStr = kesirKisim.toString().padStart(2, '0')
    return "${if (negatifMi) "-" else ""}$tamKisim,$kesirStr"
}
