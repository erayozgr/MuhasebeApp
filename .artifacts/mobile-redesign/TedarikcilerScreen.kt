package com.eray.muhasebeapp.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.data.model.*
import com.eray.muhasebeapp.data.network.ApiService
import com.eray.muhasebeapp.*
import kotlinx.coroutines.launch
private fun tedarikciBakiyeMetniVeRengi(bakiye: Double): Pair<String, Color> {
    val formatliBakiye = formatTedarikciCariIkiBasamak(bakiye)
    return when {
        bakiye > 0 -> "₺$formatliBakiye (Borcumuz)" to Color(0xFFFF3B30)
        bakiye < 0 -> "₺${formatTedarikciCariIkiBasamak(-bakiye)} (Alacaklıyız)" to Color(0xFF007AFF)
        else -> "₺0.00 (Dengede)" to Color(0xFF34C759)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TedarikcilerScreen(apiService: ApiService,onNavigateBack: () -> Unit) {
    var yenilemeTetikleyici by remember { mutableStateOf(0) }
    var tedarikciler by remember { mutableStateOf<List<Tedarikci>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var aramaAcik by remember { mutableStateOf(false) }
    var aramaMetni by remember { mutableStateOf("") }
    val filtrelenmisTedarikciler = remember(tedarikciler, aramaMetni) {
        val aranacak = aramaMetni.trim()
        if (aranacak.isBlank()) tedarikciler
        else tedarikciler.filter { it.ad.contains(aranacak, ignoreCase = true) }
    }
    LaunchedEffect(yenilemeTetikleyici) {
        loading = true
        try {
            tedarikciler = apiService.getTedarikciler()
        } catch (e: Exception) {
            println("Tedarikçi Yükleme Hatası: ${e.message}")
            e.printStackTrace()
        } finally {
            loading = false
        }
    }
    var dialogAcikMi by remember { mutableStateOf(false) }
    var detayGosterilenTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var duzenlenenTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var bakiyeDuzenlenenTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var odemeTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var raporTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var silinecekTedarikci by remember { mutableStateOf<Tedarikci?>(null) }
    var genelTedarikciDialogAcikMi by remember { mutableStateOf(false) }
    var horizontalDragAccumulator by remember { mutableStateOf(0f) }
    val toplamBorc = remember(tedarikciler) { tedarikciler.sumOf { it.bakiye } }
    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        modifier = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { horizontalDragAccumulator = 0f },
                onDragEnd = { if (horizontalDragAccumulator > 150f) onNavigateBack() },
                onDragCancel = { horizontalDragAccumulator = 0f },
                onHorizontalDrag = { change, dragAmount -> change.consume(); horizontalDragAccumulator += dragAmount }
            )
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tedarikçiler", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        aramaAcik = !aramaAcik
                        if (!aramaAcik) aramaMetni = ""
                    }) {
                        Icon(
                            imageVector = if (aramaAcik) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (aramaAcik) "Aramayı Kapat" else "Tedarikçi Ara",
                            tint = Color(0xFF5856D6)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black,
                    actionIconContentColor = Color(0xFF5856D6)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { dialogAcikMi = true },
                containerColor = Color(0xFF5856D6),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tedarikçi Ekle", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (loading && tedarikciler.isEmpty()) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (aramaAcik) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp)
                ) {
                    OutlinedTextField(
                        value = aramaMetni,
                        onValueChange = { aramaMetni = it },
                        placeholder = { Text("Tedarikçi adına göre ara...", color = Color(0xFF8E8E93), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8E8E93)) },
                        trailingIcon = {
                            if (aramaMetni.isNotEmpty()) {
                                IconButton(onClick = { aramaMetni = "" }) {
                                    Icon(Icons.Default.Cancel, contentDescription = "Aramayı Temizle", tint = Color(0xFF8E8E93))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF7F7F9),
                            unfocusedContainerColor = Color(0xFFF7F7F9),
                            focusedBorderColor = Color(0xFF5856D6),
                            unfocusedBorderColor = Color(0xFFE5E5EA),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            cursorColor = Color(0xFF5856D6)
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    )
                    if (aramaMetni.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${filtrelenmisTedarikciler.size} tedarikçi bulundu", fontSize = 12.sp, color = Color(0xFF8E8E93), modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Text("Toplam Tedarikçi", fontSize = 12.sp, color = Color(0xFF8E8E93), maxLines = 1)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(tedarikciler.size.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5856D6))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                        Text("Toplam Borcumuz", fontSize = 12.sp, color = Color(0xFF8E8E93), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            "₺${formatTedarikciCariIkiBasamak(toplamBorc)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (toplamBorc > 0) Color(0xFFFF9500) else Color(0xFF34C759),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp).clickable { genelTedarikciDialogAcikMi = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(Color(0xFF8E8E93).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF8E8E93))
                        }
                        Text("Genel Tedarikçi Alışları", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF8E8E93))
                }
            }
            when {
                tedarikciler.isEmpty() && !loading -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(46.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Henüz tedarikçi eklenmedi", color = Color(0xFF8E8E93), fontSize = 15.sp)
                        }
                    }
                }
                filtrelenmisTedarikciler.isEmpty() && aramaMetni.isNotBlank() -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(46.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Tedarikçi bulunamadı", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("\"$aramaMetni\" ile eşleşen tedarikçi yok", fontSize = 13.sp, color = Color(0xFF8E8E93))
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtrelenmisTedarikciler) { tedarikci ->
                            TedarikciKart(
                                tedarikci = tedarikci,
                                onTikla = { detayGosterilenTedarikci = tedarikci },
                                onSil = { silinecekTedarikci = tedarikci }
                            )
                        }
                    }
                }
            }
        }
    }
    if (dialogAcikMi) {
        var isSaving by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        TedarikciEkleDialog(
            isSaving = isSaving,
            errorMessage = errorMessage,
            onDismiss = { if (!isSaving) dialogAcikMi = false },
            onKaydet = { ad, telefon, adres, bakiye ->
                scope.launch {
                    isSaving = true
                    errorMessage = null
                    try {
                        val result = apiService.createTedarikci(Tedarikci(ad = ad, telefon = telefon, adres = adres, bakiye = bakiye))
                        if (result.isSuccess) {
                            yenilemeTetikleyici++
                            dialogAcikMi = false
                        } else {
                            println("Tedarikçi Kayıt Hatası (Sunucu): ${result.exceptionOrNull()?.message}")
                            errorMessage = "İşlem başarısız. Tekrar deneyin."
                        }
                    } catch (e: Exception) {
                        println("Tedarikçi Kayıt Hatası (Kritik): ${e.message}")
                        errorMessage = "İşlem başarısız. Tekrar deneyin."
                        e.printStackTrace()
                    } finally { isSaving = false }
                }
            }
        )
    }
    detayGosterilenTedarikci?.let { tedarikci ->
        TedarikciDetayDialog(
            apiService = apiService,
            tedarikci = tedarikci,
            onDismiss = { detayGosterilenTedarikci = null },
            onDuzenle = { duzenlenenTedarikci = tedarikci; detayGosterilenTedarikci = null },
            onBakiyeDuzenle = { bakiyeDuzenlenenTedarikci = tedarikci; detayGosterilenTedarikci = null },
            onOdemeYapGir = { odemeTedarikci = tedarikci; detayGosterilenTedarikci = null },
            onRaporGoster = { raporTedarikci = tedarikci; detayGosterilenTedarikci = null }
        )
    }
    duzenlenenTedarikci?.let { tedarikci ->
        TedarikciDuzenleDialog(
            tedarikci = tedarikci,
            onDismiss = { duzenlenenTedarikci = null },
            onKaydet = { ad, telefon, adres ->
                scope.launch {
                    try {
                        apiService.updateTedarikci(tedarikci.id ?: 0L, tedarikci.copy(ad = ad, telefon = telefon, adres = adres))
                        yenilemeTetikleyici++
                    } catch (e: Exception) { e.printStackTrace() }
                }
                duzenlenenTedarikci = null
            }
        )
    }
    bakiyeDuzenlenenTedarikci?.let { tedarikci ->
        TedarikciBakiyeDuzenleDialog(
            tedarikci = tedarikci,
            onDismiss = { bakiyeDuzenlenenTedarikci = null },
            onKaydet = { yeniBakiye ->
                scope.launch {
                    try {
                        apiService.updateTedarikci(tedarikci.id ?: 0L, tedarikci.copy(bakiye = yeniBakiye))
                        yenilemeTetikleyici++
                    } catch (e: Exception) { e.printStackTrace() }
                }
                bakiyeDuzenlenenTedarikci = null
            }
        )
    }
    odemeTedarikci?.let { tedarikci ->
        TedarikciOdemeGirDialog(
            tedarikci = tedarikci,
            onDismiss = { odemeTedarikci = null },
            onKaydet = { odemeTutari ->
                scope.launch {
                    try {
                        apiService.createTedarikciOdemesi(TedarikciOdemeRequest(tedarikciId = tedarikci.id ?: 0L, tutar = odemeTutari))
                        yenilemeTetikleyici++
                    } catch (e: Exception) { e.printStackTrace() }
                }
                odemeTedarikci = null
            }
        )
    }
    raporTedarikci?.let { tedarikci ->
        TarihAralikliAlisRaporDialog(apiService = apiService, tedarikci = tedarikci, onDismiss = { raporTedarikci = null })
    }
    if (genelTedarikciDialogAcikMi) {
        GenelTedarikciAlisDialog(apiService = apiService, onDismiss = { genelTedarikciDialogAcikMi = false })
    }
    silinecekTedarikci?.let { tedarikci ->
        AlertDialog(
            onDismissRequest = { silinecekTedarikci = null },
            title = { Text("Tedarikçiyi Sil", fontWeight = FontWeight.Bold) },
            text = { Text("\"${tedarikci.ad}\" tedarikçisini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                apiService.deleteTedarikci(tedarikci.id ?: 0L)
                                yenilemeTetikleyici++
                            } catch (e: Exception) { e.printStackTrace() }
                        }
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
fun TedarikciKart(tedarikci: Tedarikci,onTikla: () -> Unit,onSil: () -> Unit) {
    val (bakiyeFormatli, renk) = tedarikciBakiyeMetniVeRengi(tedarikci.bakiye)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().clickable { onTikla() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(42.dp).background(Color(0xFF5856D6).copy(alpha = 0.12f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF5856D6), modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(tedarikci.ad, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (tedarikci.telefon.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(tedarikci.telefon, fontSize = 12.sp, color = Color(0xFF8E8E93), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(bakiyeFormatli, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = renk, maxLines = 1)
                IconButton(onClick = onSil, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Sil", tint = Color(0xFFFF3B30), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
@Composable
fun TedarikciDetayDialog(
    apiService: ApiService,
    tedarikci: Tedarikci,
    onDismiss: () -> Unit,
    onDuzenle: () -> Unit,
    onBakiyeDuzenle: () -> Unit,
    onOdemeYapGir: () -> Unit,
    onRaporGoster: () -> Unit
) {
    val urlAcici = rememberUrlAcici()
    val (bakiyeFormatli, renk) = tedarikciBakiyeMetniVeRengi(tedarikci.bakiye)
    var gecmisAlislar by remember { mutableStateOf<List<Alis>>(emptyList()) }
    var alisKalemleri by remember { mutableStateOf<Map<Long, List<AlisKalemi>>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(tedarikci.id) {
        try {
            val tumAlislar = apiService.getAlislar().filter { it.tedarikciId == tedarikci.id }
            gecmisAlislar = tumAlislar.take(6)
            val kalemMap = mutableMapOf<Long, List<AlisKalemi>>()
            gecmisAlislar.forEach { a ->
                kalemMap[a.id ?: 0L] = apiService.getAlisKalemler(a.id ?: 0L)
            }
            alisKalemleri = kalemMap
        } catch (e: Exception) { e.printStackTrace() }
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
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
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
                        baslik = "Ödeme Yap (Tedarikçiye Öde)",
                        ikon = Icons.Default.Payments,
                        renk = Color(0xFF34C759),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOdemeYapGir() }
                    )
                    TedarikciIletisimButonu(
                        baslik = "Tarih Aralıklı Alış Detayı",
                        ikon = Icons.Default.DateRange,
                        renk = Color(0xFFD435CD),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onRaporGoster() }
                    )
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
                    "GEÇMİŞ ALIŞLAR (SON 6)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93)
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (gecmisAlislar.isEmpty()) {
                    Text("Henüz kayıtlı alış yapılmadı", fontSize = 13.sp, color = Color(0xFF8E8E93))
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
fun TedarikciOdemeGirDialog(
    tedarikci: Tedarikci,
    onDismiss: () -> Unit,
    onKaydet: (tutar: Double) -> Unit
) {
    var tutarText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun kaydet() {
        val temizTutarText = tutarText.replace(',', '.')
        val tutar = temizTutarText.toDoubleOrNull() ?: 0.0
        if (tutar > 0.0) {
            keyboardController?.hide()
            focusManager.clearFocus()
            onKaydet(tutar)
        }
    }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        containerColor = Color.White,
        title = { Text("Ödeme İşle", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            ) {
                Text("${tedarikci.ad} firmasına nakit/havale ödeme yapıyorsunuz.", fontSize = 13.sp, color = Color(0xFF8E8E93))
                Text("Güncel Borcumuz: ₺${formatTedarikciCariIkiBasamak(tedarikci.bakiye)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                OutlinedTextField(
                    value = tutarText,
                    onValueChange = { tutarText = it },
                    label = { Text("Ödenen Tutar (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { kaydet() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { kaydet() }) {
                Text("Ödemeyi Kaydet", color = Color(0xFF34C759), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onDismiss()
            }) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarihAralikliAlisRaporDialog(
    apiService: ApiService,
    tedarikci: Tedarikci,
    onDismiss: () -> Unit
) {
    var baslangicSeciciAcik by remember { mutableStateOf(false) }
    var bitisSeciciAcik by remember { mutableStateOf(false) }
    val baslangicTarihState = rememberDatePickerState()
    val bitisTarihState = rememberDatePickerState()
    var tumAlislar by remember { mutableStateOf<List<Alis>>(emptyList()) }
    var loadingAlislar by remember { mutableStateOf(true) }
    LaunchedEffect(tedarikci.id) {
        try {
            tumAlislar = apiService.getAlislar().filter { it.tedarikciId == tedarikci.id }
        } catch (e: Exception) { e.printStackTrace() }
        finally { loadingAlislar = false }
    }
    val filtrelenmisAlislar = remember(tumAlislar, baslangicTarihState.selectedDateMillis, bitisTarihState.selectedDateMillis) {
        tumAlislar.filter { alis ->
            val alisZamani = parseTarihMillis(alis.tarih)
            val baslangicKosulu = baslangicTarihState.selectedDateMillis?.let { alisZamani >= it } ?: true
            val bitisKosulu = bitisTarihState.selectedDateMillis?.let { alisZamani <= (it + 86400000L) } ?: true
            baslangicKosulu && bitisKosulu
        }
    }
    val gruplanmisAlislar = remember(filtrelenmisAlislar) {
        filtrelenmisAlislar.groupBy { alis -> formatTarih(alis.tarih).substringBefore(" ") }
    }
    val toplamRaporTutari = remember(filtrelenmisAlislar) {
        filtrelenmisAlislar.sumOf { it.toplamTutar }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Tarih Bazlı Alış Detayı", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                Text("${tedarikci.ad} firmasından yapılan tarih filtreli alış raporu.", fontSize = 13.sp, color = Color(0xFF8E8E93))
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { baslangicSeciciAcik = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val basMetni = baslangicTarihState.selectedDateMillis?.let { formatTarih(it.toString()).substringBefore(" ") } ?: "Başlangıç Seç"
                        Text(basMetni, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = { bitisSeciciAcik = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val bitMetni = bitisTarihState.selectedDateMillis?.let { formatTarih(it.toString()).substringBefore(" ") } ?: "Bitiş Seç"
                        Text(bitMetni, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dönem Toplam Alış:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("₺${formatTedarikciCariIkiBasamak(toplamRaporTutari)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (loadingAlislar) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (gruplanmisAlislar.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Seçilen aralıkta alış kaydı bulunamadı.", color = Color(0xFF8E8E93), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gruplanmisAlislar.forEach { (tarihBasligi, alislarListesi) ->
                            item {
                                Text(
                                    text = tarihBasligi,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5856D6),
                                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                )
                            }
                            items(alislarListesi) { alis ->
                                var kalemler by remember { mutableStateOf<List<AlisKalemi>>(emptyList()) }
                                LaunchedEffect(alis.id) {
                                    try { kalemler = apiService.getAlisKalemler(alis.id ?: 0L) } catch (e: Exception) {}
                                }
                                val alisSaati = formatSaat(alis.tarih)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF2F2F7), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Alış",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF8E8E93)
                                        )
                                        Text(
                                            text = alisSaati,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF5856D6)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    kalemler.forEach { kalem ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${kalem.urunAdi} (${kalem.adet} × ₺${formatTedarikciCariIkiBasamak(kalem.birimFiyat)})",
                                                fontSize = 13.sp,
                                                color = Color.Black,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "₺${formatTedarikciCariIkiBasamak(kalem.toplam)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF3C3C43)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold) }
        }
    )
    if (baslangicSeciciAcik) {
        DatePickerDialog(
            onDismissRequest = { baslangicSeciciAcik = false },
            confirmButton = {
                TextButton(onClick = { baslangicSeciciAcik = false }) { Text("Seç") }
            }
        ) { DatePicker(state = baslangicTarihState) }
    }
    if (bitisSeciciAcik) {
        DatePickerDialog(
            onDismissRequest = { bitisSeciciAcik = false },
            confirmButton = {
                TextButton(onClick = { bitisSeciciAcik = false }) { Text("Seç") }
            }
        ) { DatePicker(state = bitisTarihState) }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenelTedarikciAlisDialog(
    apiService: ApiService,
    onDismiss: () -> Unit
) {
    var baslangicSeciciAcik by remember { mutableStateOf(false) }
    var bitisSeciciAcik by remember { mutableStateOf(false) }
    val baslangicTarihState = rememberDatePickerState()
    val bitisTarihState = rememberDatePickerState()
    var tumAlislar by remember { mutableStateOf<List<Alis>>(emptyList()) }
    var loadingAlislar by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        try {
            tumAlislar = apiService.getAlislar().filter { it.tedarikciId == null }
        } catch (e: Exception) { e.printStackTrace() }
        finally { loadingAlislar = false }
    }
    val filtrelenmisAlislar = remember(tumAlislar, baslangicTarihState.selectedDateMillis, bitisTarihState.selectedDateMillis) {
        tumAlislar.filter { alis ->
            val alisZamani = parseTarihMillis(alis.tarih)
            val baslangicKosulu = baslangicTarihState.selectedDateMillis?.let { alisZamani >= it } ?: true
            val bitisKosulu = bitisTarihState.selectedDateMillis?.let { alisZamani <= (it + 86400000L) } ?: true
            baslangicKosulu && bitisKosulu
        }
    }
    val gruplanmisAlislar = remember(filtrelenmisAlislar) {
        filtrelenmisAlislar.groupBy { alis -> formatTarih(alis.tarih).substringBefore(" ") }
    }
    val toplamRaporTutari = remember(filtrelenmisAlislar) {
        filtrelenmisAlislar.sumOf { it.toplamTutar }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Genel Tedarikçi Alışları", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                Text("Tedarikçi seçilmeden yapılan alışların tarih filtreli dökümü.", fontSize = 13.sp, color = Color(0xFF8E8E93))
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { baslangicSeciciAcik = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val basMetni = baslangicTarihState.selectedDateMillis?.let { formatTarih(it.toString()).substringBefore(" ") } ?: "Başlangıç Seç"
                        Text(basMetni, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = { bitisSeciciAcik = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val bitMetni = bitisTarihState.selectedDateMillis?.let { formatTarih(it.toString()).substringBefore(" ") } ?: "Bitiş Seç"
                        Text(bitMetni, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dönem Toplam Alış:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("₺${formatTedarikciCariIkiBasamak(toplamRaporTutari)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (loadingAlislar) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (gruplanmisAlislar.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Seçilen aralıkta alış kaydı bulunamadı.", color = Color(0xFF8E8E93), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gruplanmisAlislar.forEach { (tarihBasligi, alislarListesi) ->
                            item {
                                Text(
                                    text = tarihBasligi,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5856D6),
                                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                )
                            }
                            items(alislarListesi) { alis ->
                                var kalemler by remember { mutableStateOf<List<AlisKalemi>>(emptyList()) }
                                LaunchedEffect(alis.id) {
                                    try { kalemler = apiService.getAlisKalemler(alis.id ?: 0L) } catch (e: Exception) {}
                                }
                                val alisSaati = formatSaat(alis.tarih)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF2F2F7), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Alış",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF8E8E93)
                                        )
                                        Text(
                                            text = alisSaati,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF5856D6)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    kalemler.forEach { kalem ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${kalem.urunAdi} (${kalem.adet} × ₺${formatTedarikciCariIkiBasamak(kalem.birimFiyat)})",
                                                fontSize = 13.sp,
                                                color = Color.Black,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "₺${formatTedarikciCariIkiBasamak(kalem.toplam)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF3C3C43)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold) }
        }
    )
    if (baslangicSeciciAcik) {
        DatePickerDialog(
            onDismissRequest = { baslangicSeciciAcik = false },
            confirmButton = {
                TextButton(onClick = { baslangicSeciciAcik = false }) { Text("Seç") }
            }
        ) { DatePicker(state = baslangicTarihState) }
    }
    if (bitisSeciciAcik) {
        DatePickerDialog(
            onDismissRequest = { bitisSeciciAcik = false },
            confirmButton = {
                TextButton(onClick = { bitisSeciciAcik = false }) { Text("Seç") }
            }
        ) { DatePicker(state = bitisTarihState) }
    }
}
@Composable
fun GecmisAlisKarti(alis: Alis, kalemler: List<AlisKalemi>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Alış", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3C3C43))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₺${formatTedarikciCariIkiBasamak(alis.toplamTutar)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9500)
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
                            "${kalem.urunAdi} — ${kalem.adet} × ₺${formatTedarikciCariIkiBasamak(kalem.birimFiyat)}",
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "₺${formatTedarikciCariIkiBasamak(kalem.toplam)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF3C3C43)
                        )
                    }
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
    isSaving: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onKaydet: (ad: String, telefon: String, adres: String, bakiye: Double) -> Unit
) {
    var ad by remember { mutableStateOf("") }
    var telefon by remember { mutableStateOf("") }
    var adres by remember { mutableStateOf("") }
    var bakiye by remember { mutableStateOf("0") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val telefonFocusRequester = remember { FocusRequester() }
    val adresFocusRequester = remember { FocusRequester() }
    val bakiyeFocusRequester = remember { FocusRequester() }

    fun sonrakiAlanaGit(focusRequester: FocusRequester) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun kaydet() {
        if (ad.isBlank() || isSaving) return
        val temizBakiyeText = bakiye.replace(',', '.')
        keyboardController?.hide()
        focusManager.clearFocus()
        onKaydet(ad, telefon, adres, temizBakiyeText.toDoubleOrNull() ?: 0.0)
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                keyboardController?.hide()
                focusManager.clearFocus()
                onDismiss()
            }
        },
        containerColor = Color.White,
        title = { Text("Yeni Tedarikçi Kaydı", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            ) {
                if (errorMessage != null) {
                    Text(errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = ad,
                    onValueChange = { ad = it },
                    label = { Text("Firma / Ad Soyad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(telefonFocusRequester) }),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = telefon,
                    onValueChange = { telefon = it },
                    label = { Text("Telefon (05XX XXX XX XX)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(adresFocusRequester) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(telefonFocusRequester),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = adres,
                    onValueChange = { adres = it },
                    label = { Text("Adres") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(bakiyeFocusRequester) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(adresFocusRequester),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = bakiye,
                    onValueChange = { bakiye = it },
                    label = { Text("Mevcut Başlangıç Borcumuz (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { kaydet() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(bakiyeFocusRequester),
                    enabled = !isSaving
                )
                if (isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !isSaving, onClick = { kaydet() }) {
                Text(if (isSaving) "Kaydediliyor..." else "Kaydet", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                },
                enabled = !isSaving
            ) { Text("İptal", color = Color(0xFF8E8E93)) }
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val telefonFocusRequester = remember { FocusRequester() }
    val adresFocusRequester = remember { FocusRequester() }

    fun sonrakiAlanaGit(focusRequester: FocusRequester) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun kaydet() {
        if (ad.isBlank()) return
        keyboardController?.hide()
        focusManager.clearFocus()
        onKaydet(ad, telefon, adres)
    }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        containerColor = Color.White,
        title = { Text("Tedarikçi Bilgilerini Düzenle", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = ad,
                    onValueChange = { ad = it },
                    label = { Text("Firma / Ad Soyad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(telefonFocusRequester) }),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = telefon,
                    onValueChange = { telefon = it },
                    label = { Text("Telefon") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(adresFocusRequester) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(telefonFocusRequester)
                )
                OutlinedTextField(
                    value = adres,
                    onValueChange = { adres = it },
                    label = { Text("Adres") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { kaydet() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(adresFocusRequester)
                )
                Text(
                    "Not: Borç bilgisi bu ekrandan değiştirilemez, mal alışı veya ödeme menülerinden otomatik güncellenmesi önerilir.",
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { kaydet() }) {
                Text("Kaydet", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onDismiss()
            }) { Text("İptal", color = Color(0xFF8E8E93)) }
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun kaydet() {
        val temizBakiyeText = yeniBakiyeText.replace(',', '.')
        val yeniBakiye = temizBakiyeText.toDoubleOrNull()
        if (yeniBakiye != null) {
            keyboardController?.hide()
            focusManager.clearFocus()
            onKaydet(yeniBakiye)
        }
    }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
        containerColor = Color.White,
        title = { Text("Net Borç Ayarla", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            ) {
                Text("Mevcut durum: $bakiyeFormatli", fontSize = 13.sp, color = Color(0xFF8E8E93))
                OutlinedTextField(
                    value = yeniBakiyeText,
                    onValueChange = { yeniBakiyeText = it },
                    label = { Text("Yeni Net Borç Tutarı (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { kaydet() }),
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
            TextButton(onClick = { kaydet() }) {
                Text("Güncelle", color = Color(0xFF5856D6), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onDismiss()
            }) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}
private fun formatTedarikciCariIkiBasamak(deger: Double): String {
    val negatifMi = deger < 0
    val mutlakDeger = if (negatifMi) -deger else deger
    val yuvarlanmis = ((mutlakDeger * 100.0) + 0.5).toLong() / 100.0
    val tamKisim = yuvarlanmis.toLong()
    val kesirKisim = (((yuvarlanmis - tamKisim) * 100.0) + 0.5).toLong()
    val kesirStr = kesirKisim.toString().padStart(2, '0')
    return "${if (negatifMi) "-" else ""}$tamKisim.$kesirStr"
}
