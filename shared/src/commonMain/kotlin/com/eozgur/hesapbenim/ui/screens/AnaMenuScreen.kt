package com.eozgur.hesapbenim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eozgur.hesapbenim.data.model.*
import com.eozgur.hesapbenim.data.network.ApiService
import com.eozgur.hesapbenim.rememberUrlAcici
import com.eozgur.hesapbenim.util.AppConfig.ODEME_WEB_URL
import com.eozgur.hesapbenim.data.model.AppConfigDto
import com.eozgur.hesapbenim.util.GosterimParaBirimi

data class MenuButonModel(
    val baslik: String,
    val ikon: ImageVector,
    val tiklamaAksiyonu: () -> Unit
)

private val ArkaPlanGradyan = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF13B0A5),
        Color(0xFF0E7C8C),
        Color(0xFF14406B),
        Color(0xFF0B1F3A)
    )
)

private val BeyazYariSeffaf = Color.White.copy(alpha = 0.14f)
private val BeyazCizgi = Color.White.copy(alpha = 0.22f)

@Composable
fun AnaMenuScreen(
    apiService: ApiService,
    kullaniciAdi: String = "Kullanıcı",
    kullaniciEmail: String = "",
    onCikisYap: () -> Unit = {},
    onNavigateToUrunler: () -> Unit,
    onNavigateToMusteriler: () -> Unit,
    onNavigateToTedarikciler: () -> Unit,
    onNavigateToSatis: () -> Unit,
    onNavigateToAlis: () -> Unit,
    onNavigateToMasraf: () -> Unit,
    onNavigateToRaporlama: () -> Unit,
    onNavigateToStok: () -> Unit,
    onNavigateToBilgiler: () -> Unit,
    guncelTarih: String,
    ozelliklerAcik: Boolean = false,
    abonelikMesaji: String? = null,
    odemeGerekli: Boolean = false,
    kontrolEdiliyor: Boolean = false,
    onAbonelikYenile: () -> Unit = {},
    selectedCurrency: GosterimParaBirimi = GosterimParaBirimi.TRY,
    exchangeConfig: AppConfigDto? = null,
    currencySaving: Boolean = false,
    currencyMessage: String? = null,
    onCurrencyChange: (GosterimParaBirimi) -> Unit = {}
) {
    val urlAcici = rememberUrlAcici()
    fun kurYaz(deger: Double?, basamak: Int = 2): String = if (deger == null) "—" else {
        val factor = when (basamak) { 4 -> 10000.0; else -> 100.0 }
        val rounded = (deger * factor + 0.5).toLong()
        val whole = (rounded / factor.toLong()).toString()
        val grouped = whole.reversed().chunked(3).joinToString(".").reversed()
        val fraction = (rounded % factor.toLong()).toString().padStart(basamak, '0')
        "$grouped,$fraction"
    }
    var urunler by remember { mutableStateOf<List<Urun>>(emptyList()) }
    val seciliParaBirimi = GosterimParaBirimi.entries.firstOrNull { it == selectedCurrency } ?: GosterimParaBirimi.TRY
    
    LaunchedEffect(ozelliklerAcik) {
        urunler = emptyList()
        if (!ozelliklerAcik) return@LaunchedEffect
        try {
            urunler = apiService.getUrunler()
        } catch (e: Exception) { e.printStackTrace() }
    }

    val tumKritikUrunler = urunler.filter { it.stokAdedi <= 5L }
    val kritikStoklarGosterim = tumKritikUrunler.sortedBy { it.stokAdedi }.take(5)

    var uyarıMesaji by remember { mutableStateOf("") }
    var diyalogAcikMi by remember { mutableStateOf(false) }
    var paraBirimiSeciciAcik by remember { mutableStateOf(false) }

    // 🎯 9 Butonlu (3x3 tam simetrik) Izgara Menüsü
    val menuButonlari = listOf(
        MenuButonModel("Müşteriler", Icons.Default.Groups, onNavigateToMusteriler),
        MenuButonModel("Tedarikçiler", Icons.Default.LocalShipping, onNavigateToTedarikciler),
        MenuButonModel("Ürünler", Icons.Default.Sell, onNavigateToUrunler),
        MenuButonModel("Satışlar", Icons.Default.ShoppingCart, onNavigateToSatis),
        MenuButonModel("Alışlar", Icons.Default.Balance, onNavigateToAlis),
        MenuButonModel("Masraflar", Icons.Default.AttachMoney, onNavigateToMasraf),
        MenuButonModel("Stoklar", Icons.Default.Storage, onNavigateToStok),
        MenuButonModel("Raporlar", Icons.Default.BarChart, onNavigateToRaporlama),
        MenuButonModel("Bilgiler", Icons.Default.ManageAccounts, onNavigateToBilgiler)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArkaPlanGradyan)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ÜST BAŞLIK (İsim ve Hızlı Çıkış/Profil Alanı)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hoş geldiniz,",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = kullaniciAdi.ifBlank { "İşletme Hesabı" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        TextButton(onClick = { paraBirimiSeciciAcik = true }, enabled = !currencySaving, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                            Text("${selectedCurrency.simge} ${selectedCurrency.kod} ▾", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Text("USD ${kurYaz(exchangeConfig?.usdTryRate)} · EUR ${kurYaz(exchangeConfig?.eurTryRate)}", color = Color.White.copy(alpha = .8f), fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(46.dp).clip(CircleShape).background(BeyazYariSeffaf)
                                .clickable { onNavigateToBilgiler() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Bilgiler", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        TextButton(onClick = onNavigateToBilgiler, contentPadding = PaddingValues(0.dp)) { Text("Bilgiler", color = Color.White, fontSize = 10.sp) }
                    }
                }
            }

            TarihBari(tarih = guncelTarih)
            Spacer(modifier = Modifier.height(18.dp))

            if (abonelikMesaji != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(abonelikMesaji, color = Color(0xFF14406B))
                        if (odemeGerekli) {
                            Button(onClick = { urlAcici.ac(ODEME_WEB_URL) }) { Text("Web sitesine git") }
                        }
                        if (!kontrolEdiliyor) {
                            TextButton(onClick = onAbonelikYenile) { Text("Durumu yeniden kontrol et") }
                        }
                    }
                }
            }

            // 1. BÖLÜM: 3x3 HIZLI İŞLEMLER GRID
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .padding(horizontal = 16.dp)
            ) {
                items(menuButonlari) { buton ->
                    MenuButonItem(model = buton, enabled = ozelliklerAcik || buton.baslik == "Bilgiler")
                }
            }

            // 2. BÖLÜM: KRİTİK STOK UYARILARI
            if (ozelliklerAcik && tumKritikUrunler.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "KRİTİK STOK UYARILARI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BeyazYariSeffaf),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onNavigateToUrunler() }
                ) {
                    Column {
                        kritikStoklarGosterim.forEachIndexed { index, urun ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB74D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(urun.ad, fontSize = 15.sp, color = Color.White)
                                }
                                Text(
                                    text = "${urun.stokAdedi} ${urun.birim}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFFB74D)
                                )
                            }
                            if (index < kritikStoklarGosterim.lastIndex) {
                                HorizontalDivider(
                                    color = BeyazCizgi,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(start = 40.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    // Genel Sistem Uyarısı Penceresi
    if (paraBirimiSeciciAcik) {
        AlertDialog(
            onDismissRequest = { if (!currencySaving) paraBirimiSeciciAcik = false },
            title = { Text("Gösterim para birimi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Güncel kurlar • ${exchangeConfig?.exchangeRateDate ?: "Tarih bekleniyor"}")
                    Text("USD/TRY  ${kurYaz(exchangeConfig?.usdTryRate, 4)}")
                    Text("EUR/TRY  ${kurYaz(exchangeConfig?.eurTryRate, 4)}")
                    GosterimParaBirimi.entries.forEach { currency ->
                        TextButton(onClick = { onCurrencyChange(currency) }, enabled = !currencySaving, modifier = Modifier.fillMaxWidth()) {
                            Text("${currency.simge} ${currency.kod}${if (selectedCurrency == currency) " ✓" else ""}")
                        }
                    }
                    if (currencyMessage != null) Text(currencyMessage, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = { TextButton(onClick = { paraBirimiSeciciAcik = false }, enabled = !currencySaving) { Text("Kapat") } }
        )
    }

    // Genel Sistem Uyarısı Penceresi
    if (diyalogAcikMi) {
        AlertDialog(
            onDismissRequest = { diyalogAcikMi = false },
            title = { Text("Sistem Mesajı", fontWeight = FontWeight.Bold) },
            text = { Text(uyarıMesaji) },
            confirmButton = {
                TextButton(onClick = { diyalogAcikMi = false }) { Text("Tamam") }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun TarihBari(tarih: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CalendarToday,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = tarih,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.75f)
        )
    }
}

@Composable
fun MenuButonItem(model: MenuButonModel, enabled: Boolean = true) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled) { model.tiklamaAksiyonu() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(BeyazYariSeffaf),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = model.ikon,
                contentDescription = model.baslik,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = model.baslik,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
