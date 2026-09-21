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
import com.eray.muhasebeapp.data.model.*
import com.eray.muhasebeapp.data.network.ApiService
import com.eray.muhasebeapp.formatTarih
import kotlinx.coroutines.launch

data class SepetKalemi(
    val urun: Urun,
    val adet: Double,
    val satisFiyati: Double
) {
    val toplam: Double get() = satisFiyati * adet
}

data class GecmisSatisKaydi(
    val satis: Satis,
    val kalemler: List<SatisKalemi>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatisScreen(
    apiService: ApiService,
    onNavigateBack: () -> Unit
) {
    var urunler by remember { mutableStateOf<List<Urun>>(emptyList()) }
    var musteriler by remember { mutableStateOf<List<Musteri>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var duzenlenen by remember { mutableStateOf<GecmisSatisKaydi?>(null) }
    var yenileme by remember { mutableStateOf(0) }

    var sepet by remember { mutableStateOf(listOf<SepetKalemi>()) }
    var seciliMusteri by remember { mutableStateOf<Musteri?>(null) }

    var urunDialogAcikMi by remember { mutableStateOf(false) }
    var musteriDropdownAcikMi by remember { mutableStateOf(false) }
    var basariliMesajGoster by remember { mutableStateOf(false) }
    var hataMesaji by remember { mutableStateOf<String?>(null) }

    val toplamTutar = sepet.sumOf { it.toplam }

    var gecmisSatislar by remember { mutableStateOf(listOf<GecmisSatisKaydi>()) }
    var gecmisYukleniyor by remember { mutableStateOf(false) }
    var loadingInitialData by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loadingInitialData = true
        try {
            urunler = apiService.getUrunler()
            musteriler = apiService.getMusteriler()
        } catch (e: Exception) {
            println("Satış Ekranı Başlangıç Veri Hatası: ${e.message}")
            e.printStackTrace()
        } finally {
            loadingInitialData = false
        }
    }

    LaunchedEffect(sepet.isEmpty(), yenileme) {
        if (sepet.isEmpty()) {
            gecmisYukleniyor = true
            try {
                val list = apiService.getSatislar().take(20)
                val mapped = list.map { s ->
                    val kalemler = try {
                        apiService.getSatisKalemler(s.id ?: 0L)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    GecmisSatisKaydi(s, kalemler)
                }
                gecmisSatislar = mapped
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                gecmisYukleniyor = false
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = BrandColors.Background,
        modifier = Modifier.swipeToBack(onBack = onNavigateBack),
        topBar = {
            BrandTopBar("Satışlar", "Yeni satış ve son işlemler", onNavigateBack) {}
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { urunDialogAcikMi = true },
                containerColor = BrandColors.Navy,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.padding(bottom = 110.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Ürün ekle", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (loadingInitialData) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BrandColors.Border),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 8.dp)
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { musteriDropdownAcikMi = true }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = BrandColors.Navy)
                            Text(
                                text = seciliMusteri?.ad ?: "Müşteri Seçin (İsteğe Bağlı)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (seciliMusteri == null) BrandColors.Muted else BrandColors.Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandColors.Muted)
                    }
                    DropdownMenu(
                        expanded = musteriDropdownAcikMi,
                        onDismissRequest = { musteriDropdownAcikMi = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Genel Müşteri") },
                            onClick = {
                                seciliMusteri = null
                                musteriDropdownAcikMi = false
                            }
                        )
                        musteriler.forEach { musteri ->
                            DropdownMenuItem(
                                text = { Text(musteri.ad) },
                                onClick = {
                                    seciliMusteri = musteri
                                    musteriDropdownAcikMi = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (sepet.isEmpty()) {
                Text(
                    text = "SON SATIŞLAR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandColors.Muted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                if (gecmisSatislar.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (gecmisYukleniyor) "Yükleniyor..." else "Sepet boş, ürün eklemek için + tuşuna bas",
                            color = BrandColors.Muted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = gecmisSatislar,
                            key = { it.satis.id ?: it.hashCode() }
                        ) { kayit ->
                            GecmisSatisKart(kayit) { duzenlenen = kayit }
                        }
                        item { Spacer(modifier = Modifier.height(88.dp)) }
                    }
                }
            } else {
                Text(
                    text = "SEPET",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandColors.Muted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = sepet,
                        key = { "${it.urun.id}_${it.satisFiyati}" }
                    ) { kalem ->
                        SepetKalemKart(
                            kalem = kalem,
                            onSil = { sepet = sepet - kalem }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }

            Card(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                border = BorderStroke(1.dp, BrandColors.Border),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Genel Toplam", fontSize = 15.sp, color = BrandColors.Ink)
                        Text(
                            text = "₺${formatSatisFiyatiIkiBasamak(toplamTutar)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandColors.Success,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End,
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                    }
                    sepet.groupBy { it.urun.id }.values.forEach { rows ->
                        val kalan = miktarYuvarla(rows.first().urun.stokAdedi - rows.sumOf { it.adet })
                        if (kalan < 0) Text("${rows.first().urun.ad}: satış sonrası stok ${miktarMetni(kalan)}. Satışa devam edebilirsiniz.", color = BrandColors.Danger)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (sepet.isNotEmpty()) {
                                println("UI_DEBUG: Satışı Tamamla tıklandı. Sepet boyutu: ${sepet.size}, Müşteri: ${seciliMusteri?.ad ?: "Genel"}")
                                scope.launch {
                                    try {
                                        val request = SatisKayitRequest(
                                            musteriId = seciliMusteri?.id,
                                            kalemler = sepet.map { SatisKalemiRequest(it.urun.id ?: 0L, it.adet, it.satisFiyati) }
                                        )
                                        println("UI_DEBUG: API isteği gönderiliyor -> $request")

                                        val result = apiService.createSatis(request)

                                        if (result.isSuccess) {
                                            println("UI_DEBUG: Satış başarılı. UI temizleniyor.")
                                            urunler = apiService.getUrunler()
                                            sepet = listOf()
                                            seciliMusteri = null
                                            basariliMesajGoster = true
                                        } else {
                                            val error = result.exceptionOrNull()?.message
                                            println("UI_DEBUG: Satış BAŞARISIZ! Hata (Sunucu): $error")
                                            hataMesaji = "İşlem başarısız. Tekrar deneyin."
                                        }
                                    } catch (e: Exception) {
                                        println("UI_DEBUG: Satış işlemi KRİTİK HATA: ${e.message}")
                                        hataMesaji = "İşlem başarısız. Tekrar deneyin."
                                        e.printStackTrace()
                                    }
                                }
                            }
                        },
                        enabled = sepet.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandColors.Navy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text("Satışı Tamamla", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    duzenlenen?.let { kayit ->
        IslemDuzenleDialog(
            baslik = "İşlemi düzenle",
            tarih = kayit.satis.tarih,
            ilkKalemler = kayit.kalemler.map { DuzenlenenKalem(it.urunId, it.urunAdi, it.birim, it.adet.toString(), it.birimFiyat.toString()) },
            urunler = urunler,
            satisMi = true,
            onDismiss = { duzenlenen = null },
            onKaydet = { kalemler, tarih ->
                apiService.updateSatis(kayit.satis.id ?: error("Kayıt kimliği bulunamadı"), SatisKayitRequest(
                    musteriId = kayit.satis.musteriId,
                    kalemler = kalemler.map { SatisKalemiRequest(it.urunId, ondalikSayi(it.adet)!!, ondalikSayi(it.fiyat)!!) },
                    tarih = tarih
                )).getOrThrow()
                urunler = apiService.getUrunler()
                yenileme++
            }
        )
    }
    if (urunDialogAcikMi) {
        UrunSecDialog(
            urunler = urunler,
            sepet = sepet,
            onDismiss = { urunDialogAcikMi = false },
            onEkle = { urun, adet, girilenFiyat ->
                val mevcutIndex = sepet.indexOfFirst { it.urun.id == urun.id && it.satisFiyati == girilenFiyat }
                sepet = if (mevcutIndex >= 0) {
                    sepet.toMutableList().apply {
                        this[mevcutIndex] = this[mevcutIndex].copy(adet = miktarYuvarla(this[mevcutIndex].adet + adet))
                    }
                } else {
                    sepet + SepetKalemi(urun, adet, girilenFiyat)
                }
                urunDialogAcikMi = false
            }
        )
    }

    if (basariliMesajGoster) {
        AlertDialog(
            onDismissRequest = { basariliMesajGoster = false },
            containerColor = Color.White,
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandColors.Success) },
            title = { Text("Satış Tamamlandı") },
            text = {
                Text("Satış kaydı başarıyla oluşturuldu ve stoklar güncellendi.")
            },
            confirmButton = {
                TextButton(onClick = { basariliMesajGoster = false }) {
                    Text("Tamam", color = BrandColors.Navy, fontWeight = FontWeight.SemiBold)
                }
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
private fun SepetKalemKart(kalem: SepetKalemi, onSil: () -> Unit) {
    BrandRecordCard(
        title = kalem.urun.ad,
        detail = "${kalem.adet} ${kalem.urun.birim} × ₺${formatSatisFiyatiIkiBasamak(kalem.satisFiyati)}",
        value = "₺${formatKisaPara(kalem.toplam)}",
        caption = "Kalem toplamı",
        icon = Icons.Default.ShoppingBag,
        accent = BrandColors.Teal
    ) {
        IconButton(onClick = onSil, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Sepetten çıkar",
                tint = BrandColors.Danger,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun GecmisSatisKart(kayit: GecmisSatisKaydi, onDuzenle: () -> Unit) {
    val formatliTarih = formatTarih(kayit.satis.tarih)
    val tarih = formatliTarih.substringBefore(" ")
    val saat = formatliTarih.substringAfter(" ", "")

    BrandRecordCard(
        title = kayit.satis.musteriAdi ?: "Genel Müşteri",
        detail = kayit.kalemler.joinToString(", ") { "${it.urunAdi} ×${it.adet}" },
        value = "₺${formatKisaPara(kayit.satis.toplamTutar)}",
        caption = if (saat.isNotEmpty()) "$tarih • $saat" else tarih,
        icon = Icons.Default.ShoppingCart,
        accent = BrandColors.Success
    ) { IconButton(onClick = onDuzenle, enabled = kayit.kalemler.isNotEmpty(), modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Edit, contentDescription = "Düzenle", modifier = Modifier.size(19.dp)) } }
}

@Composable
private fun UrunSecDialog(
    urunler: List<Urun>,
    sepet: List<SepetKalemi>,
    onDismiss: () -> Unit,
    onEkle: (Urun, Double, Double) -> Unit
) {
    var seciliUrun by remember { mutableStateOf<Urun?>(null) }
    var adetText by remember { mutableStateOf("") }
    var fiyatText by remember { mutableStateOf("") }
    var dropdownAcikMi by remember { mutableStateOf(false) }

    val sepettekiAdet = remember(seciliUrun, sepet) {
        sepet.filter { it.urun.id == seciliUrun?.id }.sumOf { it.adet }
    }
    val kalanStok = (seciliUrun?.stokAdedi ?: 0.0) - sepettekiAdet

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Ürün Ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedTextField(
                        value = seciliUrun?.ad ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ürün Seç") },
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
                        urunler.forEach { urun ->
                            val urunSepetAdet = sepet.filter { it.urun.id == urun.id }.sumOf { it.adet }
                            val urunKalanStok = urun.stokAdedi - urunSepetAdet

                            DropdownMenuItem(
                                text = { Text("${urun.ad} (Kalan Stok: $urunKalanStok)") },
                                onClick = {
                                    seciliUrun = urun
                                    fiyatText = ""
                                    adetText = ""
                                    dropdownAcikMi = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = fiyatText,
                    onValueChange = { fiyatText = ondalikGirdi(it) },
                    label = { Text("Satış Fiyatı (₺)") },
                    placeholder = {
                        Text(text = seciliUrun?.let { "Öneri: ₺${formatSatisFiyatiIkiBasamak(it.satisFiyati)}" } ?: "0,00")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = adetText,
                    onValueChange = { adetText = ondalikGirdi(it) },
                    label = { Text("Miktar (${seciliUrun?.birim ?: "Adet"})") },
                    isError = adetText.isNotBlank() && (ondalikSayi(adetText) ?: 0.0) <= 0,
                    placeholder = { Text(text = "Öneri: 1") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if ((ondalikSayi(adetText) ?: 1.0) > kalanStok && seciliUrun != null) {
                    Text("Stok yetersiz. İşlem sonrası stok: ${miktarMetni(kalanStok - (ondalikSayi(adetText) ?: 1.0))}. Satışa devam edebilirsiniz.", color = BrandColors.Danger)
                }
                seciliUrun?.let {
                    Text(
                        text = "Toplam Stok: ${it.stokAdedi} | Sepette: $sepettekiAdet | Kalan Açıktaki Stok: $kalanStok",
                        fontSize = 12.sp,
                        color = if (kalanStok <= 0) BrandColors.Danger else BrandColors.Muted,
                        fontWeight = if (kalanStok <= 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val urun = seciliUrun
                val adet = if (adetText.isBlank()) 1.0 else (ondalikSayi(adetText) ?: 0.0)
                val temizFiyatText = fiyatText.replace(',', '.')
                val girilenFiyat = if (temizFiyatText.isBlank()) (urun?.satisFiyati ?: 0.0) else (ondalikSayi(temizFiyatText) ?: 0.0)

                if (urun != null && adet > 0 && girilenFiyat > 0.0) {
                    onEkle(urun, adet, girilenFiyat)
                }
            }) { Text("Sepete Ekle", color = BrandColors.Navy, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = BrandColors.Muted) }
        }
    )
}

// =============================================================
// PARA FORMATLAMA (BÜYÜK SAYI & KISALTMA DESTEĞİ)
// =============================================================

/**
 * Tam tutar: binlik ayraçlı, iki ondalıklı. 70000000.0 -> "70.000.000,00"
 * Long üzerinden hesaplanır, Double yuvarlama hatası oluşmaz.
 */
private fun formatSatisFiyatiIkiBasamak(deger: Double?): String {
    val d = deger ?: 0.0
    val negatifMi = d < 0
    val mutlakDeger = if (negatifMi) -d else d

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
 * Dar alanlar (kartlar, sepet kalemleri) için kısaltılmış tutar.
 * 70000000.0 -> "70,0 Mn"   1250.5 -> "1.250,50"   450000.0 -> "450,0 B"
 */
private fun formatKisaPara(deger: Double?): String {
    val d = deger ?: 0.0
    val negatifMi = d < 0
    val mutlak = if (negatifMi) -d else d
    val isaret = if (negatifMi) "-" else ""

    return when {
        mutlak >= 1_000_000_000 -> "$isaret${birOndalik(mutlak / 1_000_000_000)} Mr"
        mutlak >= 1_000_000 -> "$isaret${birOndalik(mutlak / 1_000_000)} Mn"
        mutlak >= 100_000 -> "$isaret${birOndalik(mutlak / 1_000)} B"
        else -> formatSatisFiyatiIkiBasamak(d)
    }
}

private fun birOndalik(deger: Double): String {
    val x = ((deger * 10.0) + 0.5).toLong()
    return "${x / 10},${x % 10}"
}
