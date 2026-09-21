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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.data.model.*
import com.eray.muhasebeapp.data.network.ApiService
import com.eray.muhasebeapp.rememberUrlAcici
import com.eray.muhasebeapp.telefonLinkOlustur
import com.eray.muhasebeapp.whatsappLinkOlustur
import com.eray.muhasebeapp.formatTarih
import com.eray.muhasebeapp.parseTarihMillis
import kotlinx.coroutines.launch

private fun bakiyeMetniVeRengi(
    bakiye: Double
): Pair<String, Color> {

    val formatliBakiye =
        formatMusteriCariIkiBasamak(bakiye)

    return when {

        bakiye > 0 ->
            "₺$formatliBakiye (Borçlu)" to
                    Color(0xFFFF3B30)

        bakiye < 0 ->
            "₺${formatMusteriCariIkiBasamak(-bakiye)} (Alacaklı)" to
                    Color(0xFF007AFF)

        else ->
            "₺0.00 (Dengede)" to
                    Color(0xFF34C759)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusterilerScreen(
    apiService: ApiService,
    onNavigateBack: () -> Unit
) {

    var yenilemeTetikleyici by remember {
        mutableStateOf(0)
    }

    var musteriler by remember {
        mutableStateOf<List<Musteri>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    val scope =
        rememberCoroutineScope()

    // ---------------------------------------------------------
    // ARAMA
    // ---------------------------------------------------------

    var aramaAcik by remember {
        mutableStateOf(false)
    }

    var aramaMetni by remember {
        mutableStateOf("")
    }

    val filtrelenmisMusteriler =
        remember(
            musteriler,
            aramaMetni
        ) {

            val aranacak =
                aramaMetni.trim()

            if (aranacak.isBlank()) {

                musteriler

            } else {

                musteriler.filter { musteri ->

                    musteri.ad.contains(
                        aranacak,
                        ignoreCase = true
                    )
                }
            }
        }

    // ---------------------------------------------------------
    // VERİLERİ YÜKLE
    // ---------------------------------------------------------

    LaunchedEffect(
        yenilemeTetikleyici
    ) {

        loading = true

        try {

            musteriler =
                apiService.getMusteriler()

        } catch (e: Exception) {
            println("Müşteri Yükleme Hatası: ${e.message}")
            e.printStackTrace()

        } finally {

            loading = false
        }
    }

    // ---------------------------------------------------------
    // DIALOGLAR
    // ---------------------------------------------------------

    var dialogAcikMi by remember {
        mutableStateOf(false)
    }

    var detayGosterilenMusteri by remember {
        mutableStateOf<Musteri?>(null)
    }

    var duzenlenenMusteri by remember {
        mutableStateOf<Musteri?>(null)
    }

    var bakiyeDuzenlenenMusteri by remember {
        mutableStateOf<Musteri?>(null)
    }

    var tahsilatMusteri by remember {
        mutableStateOf<Musteri?>(null)
    }

    var raporMusteri by remember {
        mutableStateOf<Musteri?>(null)
    }

    var silinecekMusteri by remember {
        mutableStateOf<Musteri?>(null)
    }

    var genelMusteriDialogAcikMi by remember {
        mutableStateOf(false)
    }

    var horizontalDragAccumulator by remember {
        mutableStateOf(0f)
    }

    val toplamBakiye =
        remember(musteriler) {

            musteriler.sumOf {
                it.bakiye
            }
        }

    // ---------------------------------------------------------
    // SAYFA
    // ---------------------------------------------------------

    Scaffold(

        containerColor =
            Color(0xFFF2F2F7),

        modifier =
            Modifier.pointerInput(Unit) {

                detectHorizontalDragGestures(

                    onDragStart = {

                        horizontalDragAccumulator =
                            0f
                    },

                    onDragEnd = {

                        if (
                            horizontalDragAccumulator >
                            150f
                        ) {

                            onNavigateBack()
                        }
                    },

                    onDragCancel = {

                        horizontalDragAccumulator =
                            0f
                    },

                    onHorizontalDrag = {
                            change,
                            dragAmount ->

                        change.consume()

                        horizontalDragAccumulator +=
                            dragAmount
                    }
                )
            },

        // -----------------------------------------------------
        // ÜST BAR
        // -----------------------------------------------------

        topBar = {

            CenterAlignedTopAppBar(

                title = {

                    Text(
                        text = "Müşteriler",
                        fontSize = 18.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick =
                            onNavigateBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Geri",

                            tint =
                                Color.Black
                        )
                    }
                },

                actions = {

                    IconButton(
                        onClick = {

                            aramaAcik =
                                !aramaAcik

                            if (!aramaAcik) {

                                aramaMetni = ""
                            }
                        }
                    ) {

                        Icon(
                            imageVector =
                                if (aramaAcik) {
                                    Icons.Default.Close
                                } else {
                                    Icons.Default.Search
                                },

                            contentDescription =
                                if (aramaAcik) {
                                    "Aramayı Kapat"
                                } else {
                                    "Müşteri Ara"
                                },

                            tint =
                                Color(0xFF007AFF)
                        )
                    }
                },

                colors =
                    TopAppBarDefaults
                        .centerAlignedTopAppBarColors(

                            containerColor =
                                Color.White,

                            titleContentColor =
                                Color.Black,

                            navigationIconContentColor =
                                Color.Black,

                            actionIconContentColor =
                                Color(0xFF007AFF)
                        )
            )
        },

        // -----------------------------------------------------
        // EKLE BUTONU
        // -----------------------------------------------------

        floatingActionButton = {

            FloatingActionButton(

                onClick = {

                    dialogAcikMi =
                        true
                },

                containerColor =
                    Color(0xFF007AFF),

                contentColor =
                    Color.White,

                shape =
                    RoundedCornerShape(
                        16.dp
                    )
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Add,

                    contentDescription =
                        "Müşteri Ekle",

                    tint =
                        Color.White
                )
            }
        }

    ) { padding ->

        Column(

            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()

        ) {

            // -------------------------------------------------
            // LOADING
            // -------------------------------------------------

            if (
                loading &&
                musteriler.isEmpty()
            ) {

                LinearProgressIndicator(
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            // -------------------------------------------------
            // ARAMA ALANI
            // -------------------------------------------------

            if (aramaAcik) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White
                            )
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 10.dp,
                                bottom = 12.dp
                            )
                ) {

                    OutlinedTextField(

                        value =
                            aramaMetni,

                        onValueChange = {

                            aramaMetni = it
                        },

                        placeholder = {

                            Text(
                                text =
                                    "Müşteri adına göre ara...",
                                color =
                                    Color(0xFF8E8E93),
                                fontSize =
                                    14.sp
                            )
                        },

                        leadingIcon = {

                            Icon(
                                imageVector =
                                    Icons.Default.Search,

                                contentDescription =
                                    null,

                                tint =
                                    Color(0xFF8E8E93)
                            )
                        },

                        trailingIcon = {

                            if (
                                aramaMetni.isNotEmpty()
                            ) {

                                IconButton(

                                    onClick = {

                                        aramaMetni = ""
                                    }

                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.Cancel,

                                        contentDescription =
                                            "Aramayı Temizle",

                                        tint =
                                            Color(0xFF8E8E93)
                                    )
                                }
                            }
                        },

                        singleLine = true,

                        shape =
                            RoundedCornerShape(
                                14.dp
                            ),

                        colors =
                            OutlinedTextFieldDefaults
                                .colors(

                                    focusedContainerColor =
                                        Color(0xFFF7F7F9),

                                    unfocusedContainerColor =
                                        Color(0xFFF7F7F9),

                                    focusedBorderColor =
                                        Color(0xFF007AFF),

                                    unfocusedBorderColor =
                                        Color(0xFFE5E5EA),

                                    focusedTextColor =
                                        Color.Black,

                                    unfocusedTextColor =
                                        Color.Black,

                                    cursorColor =
                                        Color(0xFF007AFF)
                                ),

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min = 52.dp
                                )
                    )

                    if (
                        aramaMetni.isNotBlank()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    6.dp
                                )
                        )

                        Text(

                            text =
                                "${filtrelenmisMusteriler.size} müşteri bulundu",

                            fontSize =
                                12.sp,

                            color =
                                Color(0xFF8E8E93),

                            modifier =
                                Modifier.padding(
                                    start = 4.dp
                                )
                        )
                    }
                }
            }

            // -------------------------------------------------
            // ÖZET KARTI
            // -------------------------------------------------

            Card(

                shape =
                    RoundedCornerShape(
                        14.dp
                    ),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation =
                            0.dp
                    ),

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 12.dp
                        )

            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 15.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    // MÜŞTERİ SAYISI

                    Column(

                        modifier =
                            Modifier.weight(1f),

                        verticalArrangement =
                            Arrangement.Center

                    ) {

                        Text(

                            text =
                                "Toplam Müşteri",

                            fontSize =
                                12.sp,

                            color =
                                Color(0xFF8E8E93),

                            maxLines = 1
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    3.dp
                                )
                        )

                        Text(

                            text =
                                musteriler.size
                                    .toString(),

                            fontSize =
                                22.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                Color(0xFF007AFF)
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.width(
                                12.dp
                            )
                    )

                    // TOPLAM BORÇ

                    Column(

                        modifier =
                            Modifier.weight(1f),

                        horizontalAlignment =
                            Alignment.End,

                        verticalArrangement =
                            Arrangement.Center

                    ) {

                        Text(

                            text =
                                "Toplam Müşteri Borcu",

                            fontSize =
                                12.sp,

                            color =
                                Color(0xFF8E8E93),

                            textAlign =
                                TextAlign.End,

                            maxLines = 1,

                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    3.dp
                                )
                        )

                        Text(

                            text =
                                "₺${
                                    formatMusteriCariIkiBasamak(
                                        toplamBakiye
                                    )
                                }",

                            fontSize =
                                22.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                if (
                                    toplamBakiye > 0
                                ) {

                                    Color(
                                        0xFFFF9500
                                    )

                                } else {

                                    Color(
                                        0xFF34C759
                                    )
                                },

                            textAlign =
                                TextAlign.End,

                            maxLines = 1,

                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // -------------------------------------------------
            // GENEL MÜŞTERİ
            // -------------------------------------------------

            Card(

                shape =
                    RoundedCornerShape(
                        14.dp
                    ),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation =
                            0.dp
                    ),

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal =
                                16.dp
                        )
                        .padding(
                            bottom =
                                12.dp
                        )
                        .clickable {

                            genelMusteriDialogAcikMi =
                                true
                        }

            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                14.dp
                            ),

                    horizontalArrangement =
                        Arrangement
                            .SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    Row(

                        modifier =
                            Modifier.weight(1f),

                        verticalAlignment =
                            Alignment.CenterVertically,

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )
                    ) {

                        Box(

                            modifier =
                                Modifier
                                    .size(
                                        40.dp
                                    )
                                    .background(
                                        Color(
                                            0xFF8E8E93
                                        ).copy(
                                            alpha =
                                                0.12f
                                        ),
                                        RoundedCornerShape(
                                            10.dp
                                        )
                                    ),

                            contentAlignment =
                                Alignment.Center

                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Groups,

                                contentDescription =
                                    null,

                                tint =
                                    Color(0xFF8E8E93)
                            )
                        }

                        Text(

                            text =
                                "Genel Müşteri Satışları",

                            fontSize =
                                15.sp,

                            fontWeight =
                                FontWeight.SemiBold,

                            color =
                                Color.Black,

                            maxLines = 1,

                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }

                    Icon(

                        imageVector =
                            Icons.Default.ChevronRight,

                        contentDescription =
                            null,

                        tint =
                            Color(0xFF8E8E93)
                    )
                }
            }

            // -------------------------------------------------
            // MÜŞTERİ LİSTESİ
            // -------------------------------------------------

            when {

                musteriler.isEmpty() &&
                        !loading -> {

                    Box(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Column(

                            horizontalAlignment =
                                Alignment.CenterHorizontally

                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.PeopleOutline,

                                contentDescription =
                                    null,

                                tint =
                                    Color(0xFF8E8E93),

                                modifier =
                                    Modifier.size(
                                        46.dp
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        10.dp
                                    )
                            )

                            Text(

                                text =
                                    "Henüz müşteri eklenmedi",

                                color =
                                    Color(0xFF8E8E93),

                                fontSize =
                                    15.sp
                            )
                        }
                    }
                }

                filtrelenmisMusteriler
                    .isEmpty() &&
                        aramaMetni
                            .isNotBlank() -> {

                    Box(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Column(

                            horizontalAlignment =
                                Alignment.CenterHorizontally

                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.SearchOff,

                                contentDescription =
                                    null,

                                tint =
                                    Color(0xFF8E8E93),

                                modifier =
                                    Modifier.size(
                                        46.dp
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        10.dp
                                    )
                            )

                            Text(

                                text =
                                    "Müşteri bulunamadı",

                                fontSize =
                                    15.sp,

                                fontWeight =
                                    FontWeight.SemiBold,

                                color =
                                    Color.Black
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        4.dp
                                    )
                            )

                            Text(

                                text =
                                    "\"$aramaMetni\" ile eşleşen müşteri yok",

                                fontSize =
                                    13.sp,

                                color =
                                    Color(0xFF8E8E93),

                                textAlign =
                                    TextAlign.Center,

                                modifier =
                                    Modifier.padding(
                                        horizontal =
                                            24.dp
                                    )
                            )
                        }
                    }
                }

                else -> {

                    LazyColumn(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),

                        contentPadding =
                            PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 96.dp
                            ),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )

                    ) {

                        items(

                            items =
                                filtrelenmisMusteriler,

                            key = { musteri ->

                                musteri.id
                                    ?: musteri.ad
                            }

                        ) { musteri ->

                            MusteriKart(

                                musteri =
                                    musteri,

                                onTikla = {

                                    detayGosterilenMusteri =
                                        musteri
                                },

                                onSil = {

                                    silinecekMusteri =
                                        musteri
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================
    // YENİ MÜŞTERİ EKLE
    // =========================================================

    if (dialogAcikMi) {

        var isSaving by remember {
            mutableStateOf(false)
        }

        var errorMessage by remember {
            mutableStateOf<String?>(null)
        }

        MusteriEkleDialog(

            isSaving =
                isSaving,

            errorMessage =
                errorMessage,

            onDismiss = {

                if (!isSaving) {

                    dialogAcikMi =
                        false
                }
            },

            onKaydet = {
                    ad,
                    telefon,
                    adres,
                    bakiye ->

                scope.launch {

                    isSaving = true

                    errorMessage = null

                    try {

                        val result =
                            apiService.createMusteri(

                                Musteri(
                                    ad = ad,
                                    telefon = telefon,
                                    adres = adres,
                                    bakiye = bakiye
                                )
                            )

                        if (result.isSuccess) {

                            yenilemeTetikleyici++

                            dialogAcikMi =
                                false

                        } else {
                            println("Müşteri Kayıt Hatası (Sunucu): ${result.exceptionOrNull()?.message}")
                            errorMessage = "İşlem başarısız. Tekrar deneyin."
                        }

                    } catch (e: Exception) {
                        println("Müşteri Kayıt Hatası (Kritik): ${e.message}")
                        errorMessage = "İşlem başarısız. Tekrar deneyin."
                        e.printStackTrace()

                    } finally {

                        isSaving = false
                    }
                }
            }
        )
    }

    // =========================================================
    // MÜŞTERİ DETAY
    // =========================================================

    detayGosterilenMusteri
        ?.let { musteri ->

            MusteriDetayDialog(

                apiService =
                    apiService,

                musteri =
                    musteri,

                onDismiss = {

                    detayGosterilenMusteri =
                        null
                },

                onDuzenle = {

                    duzenlenenMusteri =
                        musteri

                    detayGosterilenMusteri =
                        null
                },

                onBakiyeDuzenle = {

                    bakiyeDuzenlenenMusteri =
                        musteri

                    detayGosterilenMusteri =
                        null
                },

                onTahsilatGir = {

                    tahsilatMusteri =
                        musteri

                    detayGosterilenMusteri =
                        null
                },

                onRaporGoster = {

                    raporMusteri =
                        musteri

                    detayGosterilenMusteri =
                        null
                }
            )
        }

    // =========================================================
    // MÜŞTERİ DÜZENLE
    // =========================================================

    duzenlenenMusteri
        ?.let { musteri ->

            MusteriDuzenleDialog(

                musteri =
                    musteri,

                onDismiss = {

                    duzenlenenMusteri =
                        null
                },

                onKaydet = {
                        ad,
                        telefon,
                        adres ->

                    scope.launch {

                        try {

                            apiService.updateMusteri(

                                musteri.id
                                    ?: 0L,

                                musteri.copy(
                                    ad = ad,
                                    telefon = telefon,
                                    adres = adres
                                )
                            )

                            yenilemeTetikleyici++

                        } catch (
                            e: Exception
                        ) {

                            e.printStackTrace()
                        }
                    }

                    duzenlenenMusteri =
                        null
                }
            )
        }

    // =========================================================
    // BAKİYE DÜZENLE
    // =========================================================

    bakiyeDuzenlenenMusteri
        ?.let { musteri ->

            BakiyeDuzenleDialog(

                musteri =
                    musteri,

                onDismiss = {

                    bakiyeDuzenlenenMusteri =
                        null
                },

                onKaydet = {
                        yeniBakiye ->

                    scope.launch {

                        try {

                            apiService.updateMusteri(

                                musteri.id
                                    ?: 0L,

                                musteri.copy(
                                    bakiye =
                                        yeniBakiye
                                )
                            )

                            yenilemeTetikleyici++

                        } catch (
                            e: Exception
                        ) {

                            e.printStackTrace()
                        }
                    }

                    bakiyeDuzenlenenMusteri =
                        null
                }
            )
        }

    // =========================================================
    // TAHSİLAT
    // =========================================================

    tahsilatMusteri
        ?.let { musteri ->

            TahsilatGirDialog(

                musteri =
                    musteri,

                onDismiss = {

                    tahsilatMusteri =
                        null
                },

                onKaydet = {
                        tahsilatTutari ->

                    scope.launch {

                        try {

                            apiService.createTahsilat(

                                TahsilatRequest(
                                    musteriId =
                                        musteri.id
                                            ?: 0L,

                                    tutar =
                                        tahsilatTutari
                                )
                            )

                            yenilemeTetikleyici++

                        } catch (
                            e: Exception
                        ) {

                            e.printStackTrace()
                        }
                    }

                    tahsilatMusteri =
                        null
                }
            )
        }

    // =========================================================
    // RAPOR
    // =========================================================

    raporMusteri
        ?.let { musteri ->

            TarihAralikliSatisRaporDialog(

                apiService =
                    apiService,

                musteri =
                    musteri,

                onDismiss = {

                    raporMusteri =
                        null
                }
            )
        }

    // =========================================================
    // GENEL MÜŞTERİ
    // =========================================================

    if (
        genelMusteriDialogAcikMi
    ) {

        GenelMusteriSatisDialog(

            apiService =
                apiService,

            onDismiss = {

                genelMusteriDialogAcikMi =
                    false
            }
        )
    }

    // =========================================================
    // MÜŞTERİ SİL
    // =========================================================

    silinecekMusteri
        ?.let { musteri ->

            AlertDialog(

                onDismissRequest = {

                    silinecekMusteri =
                        null
                },

                title = {

                    Text(
                        text =
                            "Müşteriyi Sil",

                        fontWeight =
                            FontWeight.Bold
                    )
                },

                text = {

                    Text(
                        "\"${musteri.ad}\" müşterisini silmek istediğinize emin misiniz? Bu işlem geri alınamaz."
                    )
                },

                confirmButton = {

                    Button(

                        onClick = {

                            scope.launch {

                                try {

                                    apiService.deleteMusteri(
                                        musteri.id
                                            ?: 0L
                                    )

                                    yenilemeTetikleyici++

                                } catch (
                                    e: Exception
                                ) {

                                    e.printStackTrace()
                                }
                            }

                            silinecekMusteri =
                                null
                        },

                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        Color(
                                            0xFFFF3B30
                                        )
                                )

                    ) {

                        Text(
                            text = "Sil",
                            color =
                                Color.White
                        )
                    }
                },

                dismissButton = {

                    TextButton(

                        onClick = {

                            silinecekMusteri =
                                null
                        }

                    ) {

                        Text(
                            text =
                                "Vazgeç",

                            color =
                                Color(
                                    0xFF007AFF
                                )
                        )
                    }
                },

                shape =
                    RoundedCornerShape(
                        16.dp
                    ),

                containerColor =
                    Color.White
            )
        }
}

// =============================================================
// MÜŞTERİ KARTI
// =============================================================

@Composable
fun MusteriKart(
    musteri: Musteri,
    onTikla: () -> Unit,
    onSil: () -> Unit
) {

    val (
        bakiyeFormatli,
        renk
    ) =
        bakiyeMetniVeRengi(
            musteri.bakiye
        )

    Card(

        shape =
            RoundedCornerShape(
                14.dp
            ),

        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        Color.White
                ),

        elevation =
            CardDefaults
                .cardElevation(
                    defaultElevation =
                        0.dp
                ),

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {

                    onTikla()
                }

    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            14.dp,
                        vertical =
                            13.dp
                    ),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            // SOL TARAF

            Row(

                modifier =
                    Modifier
                        .weight(1f)
                        .padding(
                            end = 8.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )

            ) {

                Box(

                    modifier =
                        Modifier
                            .size(
                                42.dp
                            )
                            .background(
                                Color(
                                    0xFF007AFF
                                ).copy(
                                    alpha =
                                        0.12f
                                ),
                                RoundedCornerShape(
                                    11.dp
                                )
                            ),

                    contentAlignment =
                        Alignment.Center

                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Person,

                        contentDescription =
                            null,

                        tint =
                            Color(0xFF007AFF),

                        modifier =
                            Modifier.size(
                                22.dp
                            )
                    )
                }

                Column(

                    modifier =
                        Modifier.weight(1f)

                ) {

                    Text(

                        text =
                            musteri.ad,

                        fontSize =
                            15.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            Color.Black,

                        maxLines = 1,

                        overflow =
                            TextOverflow.Ellipsis
                    )

                    if (
                        musteri.telefon
                            .isNotBlank()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    2.dp
                                )
                        )

                        Text(

                            text =
                                musteri.telefon,

                            fontSize =
                                12.sp,

                            color =
                                Color(0xFF8E8E93),

                            maxLines = 1,

                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // SAĞ TARAF

            Row(

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.spacedBy(
                        4.dp
                    )

            ) {

                Text(

                    text =
                        bakiyeFormatli,

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        renk,

                    maxLines = 1
                )

                IconButton(

                    onClick =
                        onSil,

                    modifier =
                        Modifier.size(
                            34.dp
                        )

                ) {

                    Icon(
                        imageVector =
                            Icons.Default.DeleteOutline,

                        contentDescription =
                            "Sil",

                        tint =
                            Color(0xFFFF3B30),

                        modifier =
                            Modifier.size(
                                20.dp
                            )
                    )
                }
            }
        }
    }
}

// =============================================================
// MÜŞTERİ DETAY
// =============================================================

@Composable
fun MusteriDetayDialog(
    apiService: ApiService,
    musteri: Musteri,
    onDismiss: () -> Unit,
    onDuzenle: () -> Unit,
    onBakiyeDuzenle: () -> Unit,
    onTahsilatGir: () -> Unit,
    onRaporGoster: () -> Unit
) {

    val urlAcici =
        rememberUrlAcici()

    val (
        bakiyeFormatli,
        renk
    ) =
        bakiyeMetniVeRengi(
            musteri.bakiye
        )

    var gecmisSatislar by remember {
        mutableStateOf<List<Satis>>(
            emptyList()
        )
    }

    var satisKalemleri by remember {

        mutableStateOf<
                Map<Long, List<SatisKalemi>>
                >(
            emptyMap()
        )
    }

    LaunchedEffect(
        musteri.id
    ) {

        try {

            val tumSatislar =
                apiService
                    .getSatislar()
                    .filter {

                        it.musteriId ==
                                musteri.id
                    }

            gecmisSatislar =
                tumSatislar.take(6)

            val kalemMap =
                mutableMapOf<
                        Long,
                        List<SatisKalemi>
                        >()

            gecmisSatislar
                .forEach { satis ->

                    kalemMap[
                        satis.id ?: 0L
                    ] =
                        apiService
                            .getSatisKalemler(
                                satis.id
                                    ?: 0L
                            )
                }

            satisKalemleri =
                kalemMap

        } catch (
            e: Exception
        ) {

            e.printStackTrace()
        }
    }

    AlertDialog(

        onDismissRequest =
            onDismiss,

        containerColor =
            Color.White,

        title = {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Text(
                    text =
                        musteri.ad,

                    fontWeight =
                        FontWeight.Bold,

                    modifier =
                        Modifier.weight(1f),

                    maxLines = 1,

                    overflow =
                        TextOverflow.Ellipsis
                )

                IconButton(

                    onClick =
                        onDuzenle,

                    modifier =
                        Modifier.size(
                            32.dp
                        )

                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Edit,

                        contentDescription =
                            "Düzenle",

                        tint =
                            Color(0xFF007AFF)
                    )
                }
            }
        },

        text = {

            Column(

                modifier =
                    Modifier
                        .heightIn(
                            max =
                                460.dp
                        )
                        .verticalScroll(
                            rememberScrollState()
                        )

            ) {

                Text(

                    text =
                        musteri.adres,

                    fontSize =
                        13.sp,

                    color =
                        Color(0xFF8E8E93)
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )

                Text(

                    text =
                        "Mevcut Durum: $bakiyeFormatli",

                    fontSize =
                        14.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        renk
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )

                Column(

                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )

                ) {

                    IletisimButonu(

                        baslik =
                            "Tahsilat Gir (Ödeme Al)",

                        ikon =
                            Icons.Default.Payments,

                        renk =
                            Color(0xFF34C759),

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        onTahsilatGir()
                    }

                    IletisimButonu(

                        baslik =
                            "Tarih Aralıklı Satış Detayı",

                        ikon =
                            Icons.Default.DateRange,

                        renk =
                            Color(0xFFD435CD),

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        onRaporGoster()
                    }

                    IletisimButonu(

                        baslik =
                            "Doğrudan Bakiye/Borç Düzenle",

                        ikon =
                            Icons.Default.Edit,

                        renk =
                            Color(0xFF5856D6),

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        onBakiyeDuzenle()
                    }

                    Row(

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            ),

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        IletisimButonu(

                            baslik =
                                "Ara",

                            ikon =
                                Icons.Default.Call,

                            renk =
                                Color(0xFF007AFF),

                            modifier =
                                Modifier.weight(1f)

                        ) {

                            urlAcici.ac(
                                telefonLinkOlustur(
                                    musteri.telefon
                                )
                            )
                        }

                        IletisimButonu(

                            baslik =
                                "WhatsApp",

                            ikon =
                                Icons.Default.Chat,

                            renk =
                                Color(0xFF34C759),

                            modifier =
                                Modifier.weight(1f)

                        ) {

                            urlAcici.ac(
                                whatsappLinkOlustur(
                                    musteri.telefon
                                )
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )

                Text(

                    text =
                        "GEÇMİŞ SATIŞLAR (SON 6)",

                    fontSize =
                        12.sp,

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        Color(0xFF8E8E93)
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )

                if (
                    gecmisSatislar.isEmpty()
                ) {

                    Text(

                        text =
                            "Henüz kayıtlı satış yapılmadı",

                        fontSize =
                            13.sp,

                        color =
                            Color(0xFF8E8E93)
                    )

                } else {

                    Column(

                        verticalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )

                    ) {

                        gecmisSatislar
                            .forEach { satis ->

                                GecmisSatisKarti(

                                    satis =
                                        satis,

                                    kalemler =
                                        satisKalemleri[
                                            satis.id
                                                ?: 0L
                                        ] ?: emptyList()
                                )
                            }
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(

                    text =
                        "Kapat",

                    color =
                        Color(0xFF007AFF),

                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    )
}

// =============================================================
// TAHSİLAT
// =============================================================

@Composable
fun TahsilatGirDialog(
    musteri: Musteri,
    onDismiss: () -> Unit,
    onKaydet: (tutar: Double) -> Unit
) {
    var tutarText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun kaydet() {
        val tutar = tutarText.replace(',', '.').toDoubleOrNull() ?: 0.0
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
        title = { Text("Tahsilat İşle", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("“${musteri.ad}” isimli müşteriden nakit ödeme alıyorsunuz.", fontSize = 13.sp, color = Color(0xFF8E8E93))
                Text("Güncel Borç: ₺${formatMusteriCariIkiBasamak(musteri.bakiye)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                OutlinedTextField(
                    value = tutarText,
                    onValueChange = { tutarText = it },
                    label = { Text("Alınan Tutar (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { kaydet() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { kaydet() }) {
                Text("Tahsilatı Kaydet", color = Color(0xFF34C759), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

// =============================================================
// TARİH ARALIKLI MÜŞTERİ SATIŞ RAPORU
// =============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarihAralikliSatisRaporDialog(
    apiService: ApiService,
    musteri: Musteri,
    onDismiss: () -> Unit
) {

    var baslangicSeciciAcik by remember {
        mutableStateOf(false)
    }

    var bitisSeciciAcik by remember {
        mutableStateOf(false)
    }

    val baslangicTarihState =
        rememberDatePickerState()

    val bitisTarihState =
        rememberDatePickerState()

    var tumSatislar by remember {
        mutableStateOf<List<Satis>>(
            emptyList()
        )
    }

    var loadingSatislar by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(
        musteri.id
    ) {

        try {

            tumSatislar =
                apiService
                    .getSatislar()
                    .filter {

                        it.musteriId ==
                                musteri.id
                    }

        } catch (
            e: Exception
        ) {

            e.printStackTrace()

        } finally {

            loadingSatislar =
                false
        }
    }

    val filtrelenmisSatislar =
        remember(
            tumSatislar,
            baslangicTarihState
                .selectedDateMillis,
            bitisTarihState
                .selectedDateMillis
        ) {

            tumSatislar.filter {
                    satis ->

                val satisZamani =
                    parseTarihMillis(
                        satis.tarih
                    )

                val baslangicKosulu =
                    baslangicTarihState
                        .selectedDateMillis
                        ?.let {

                            satisZamani >= it

                        } ?: true

                val bitisKosulu =
                    bitisTarihState
                        .selectedDateMillis
                        ?.let {

                            satisZamani <=
                                    (
                                            it +
                                                    86400000L
                                            )

                        } ?: true

                baslangicKosulu &&
                        bitisKosulu
            }
        }

    val gruplanmisSatislar =
        remember(
            filtrelenmisSatislar
        ) {

            filtrelenmisSatislar
                .groupBy { satis ->

                    formatTarih(
                        satis.tarih
                    ).substringBefore(
                        " "
                    )
                }
        }

    val toplamRaporTutari =
        remember(
            filtrelenmisSatislar
        ) {

            filtrelenmisSatislar
                .sumOf {

                    it.toplamTutar
                        ?: 0.0
                }
        }

    AlertDialog(

        onDismissRequest =
            onDismiss,

        containerColor =
            Color.White,

        title = {

            Text(

                text =
                    "Tarih Bazlı Satış Detayı",

                fontWeight =
                    FontWeight.Bold
            )
        },

        text = {

            Column(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(
                            max = 480.dp
                        )

            ) {

                Text(

                    text =
                        "${musteri.ad} için tarih filtreli satış raporu.",

                    fontSize =
                        13.sp,

                    color =
                        Color(0xFF8E8E93)
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )

                Row(

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        ),

                    modifier =
                        Modifier.fillMaxWidth()

                ) {

                    Button(

                        onClick = {

                            baslangicSeciciAcik =
                                true
                        },

                        colors =
                            ButtonDefaults
                                .buttonColors(

                                    containerColor =
                                        Color(
                                            0xFFF2F2F7
                                        ),

                                    contentColor =
                                        Color.Black
                                ),

                        shape =
                            RoundedCornerShape(
                                8.dp
                            ),

                        modifier =
                            Modifier.weight(1f)

                    ) {

                        val basMetni =
                            baslangicTarihState
                                .selectedDateMillis
                                ?.let {

                                    formatTarih(
                                        it.toString()
                                    ).substringBefore(
                                        " "
                                    )

                                } ?: "Başlangıç Seç"

                        Text(

                            text =
                                basMetni,

                            fontSize =
                                12.sp,

                            fontWeight =
                                FontWeight.Medium
                        )
                    }

                    Button(

                        onClick = {

                            bitisSeciciAcik =
                                true
                        },

                        colors =
                            ButtonDefaults
                                .buttonColors(

                                    containerColor =
                                        Color(
                                            0xFFF2F2F7
                                        ),

                                    contentColor =
                                        Color.Black
                                ),

                        shape =
                            RoundedCornerShape(
                                8.dp
                            ),

                        modifier =
                            Modifier.weight(1f)

                    ) {

                        val bitMetni =
                            bitisTarihState
                                .selectedDateMillis
                                ?.let {

                                    formatTarih(
                                        it.toString()
                                    ).substringBefore(
                                        " "
                                    )

                                } ?: "Bitiş Seç"

                        Text(

                            text =
                                bitMetni,

                            fontSize =
                                12.sp,

                            fontWeight =
                                FontWeight.Medium
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )

                HorizontalDivider(

                    color =
                        Color(
                            0xFFF2F2F7
                        ),

                    thickness =
                        1.dp
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween

                ) {

                    Text(

                        text =
                            "Dönem Toplam Satış:",

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            Color.Black
                    )

                    Text(

                        text =
                            "₺${
                                formatMusteriCariIkiBasamak(
                                    toplamRaporTutari
                                )
                            }",

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            Color(0xFF34C759)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )

                if (
                    loadingSatislar
                ) {

                    LinearProgressIndicator(
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                } else if (
                    gruplanmisSatislar
                        .isEmpty()
                ) {

                    Box(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Text(

                            text =
                                "Seçilen aralıkta satış kaydı bulunamadı.",

                            color =
                                Color(
                                    0xFF8E8E93
                                ),

                            fontSize =
                                13.sp
                        )
                    }

                } else {

                    LazyColumn(

                        modifier =
                            Modifier.weight(1f),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )

                    ) {

                        gruplanmisSatislar
                            .forEach {
                                    (
                                        tarihBasligi,
                                        satislarListesi
                                    ) ->

                                item {

                                    Text(

                                        text =
                                            tarihBasligi,

                                        fontSize =
                                            14.sp,

                                        fontWeight =
                                            FontWeight.Bold,

                                        color =
                                            Color(
                                                0xFF007AFF
                                            ),

                                        modifier =
                                            Modifier.padding(
                                                top =
                                                    10.dp,
                                                bottom =
                                                    2.dp
                                            )
                                    )
                                }

                                items(
                                    satislarListesi
                                ) { satis ->

                                    var kalemler by remember {

                                        mutableStateOf<
                                                List<SatisKalemi>
                                                >(
                                            emptyList()
                                        )
                                    }

                                    LaunchedEffect(
                                        satis.id
                                    ) {

                                        try {

                                            kalemler =
                                                apiService
                                                    .getSatisKalemler(
                                                        satis.id
                                                            ?: 0L
                                                    )

                                        } catch (
                                            e: Exception
                                        ) {

                                        }
                                    }

                                    val satisSaati =
                                        formatSaat(
                                            satis.tarih
                                        )

                                    Column(

                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    Color(
                                                        0xFFF2F2F7
                                                    ),
                                                    RoundedCornerShape(
                                                        8.dp
                                                    )
                                                )
                                                .padding(
                                                    10.dp
                                                )

                                    ) {

                                        Row(

                                            modifier =
                                                Modifier
                                                    .fillMaxWidth(),

                                            horizontalArrangement =
                                                Arrangement
                                                    .SpaceBetween,

                                            verticalAlignment =
                                                Alignment
                                                    .CenterVertically

                                        ) {

                                            Text(

                                                text =
                                                    "Satış",

                                                fontSize =
                                                    11.sp,

                                                fontWeight =
                                                    FontWeight
                                                        .SemiBold,

                                                color =
                                                    Color(
                                                        0xFF8E8E93
                                                    )
                                            )

                                            Text(

                                                text =
                                                    satisSaati,

                                                fontSize =
                                                    12.sp,

                                                fontWeight =
                                                    FontWeight.Bold,

                                                color =
                                                    Color(
                                                        0xFF007AFF
                                                    )
                                            )
                                        }

                                        Spacer(
                                            modifier =
                                                Modifier.height(
                                                    4.dp
                                                )
                                        )

                                        HorizontalDivider(

                                            color =
                                                Color(
                                                    0xFFE5E5EA
                                                ),

                                            thickness =
                                                0.5.dp
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.height(
                                                    4.dp
                                                )
                                        )

                                        kalemler
                                            .forEach {
                                                    kalem ->

                                                Row(

                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(
                                                                vertical =
                                                                    2.dp
                                                            ),

                                                    horizontalArrangement =
                                                        Arrangement
                                                            .SpaceBetween,

                                                    verticalAlignment =
                                                        Alignment
                                                            .CenterVertically

                                                ) {

                                                    Text(

                                                        text =
                                                            "${kalem.urunAdi} (${kalem.adet} ${kalem.birim} × ₺${formatMusteriCariIkiBasamak(kalem.birimFiyat)})",

                                                        fontSize =
                                                            13.sp,

                                                        color =
                                                            Color.Black,

                                                        modifier =
                                                            Modifier
                                                                .weight(
                                                                    1f
                                                                )
                                                    )

                                                    Text(

                                                        text =
                                                            "₺${formatMusteriCariIkiBasamak(kalem.toplam)}",

                                                        fontSize =
                                                            13.sp,

                                                        fontWeight =
                                                            FontWeight
                                                                .Bold,

                                                        color =
                                                            Color(
                                                                0xFF3C3C43
                                                            )
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

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(

                    text =
                        "Kapat",

                    color =
                        Color(
                            0xFF007AFF
                        ),

                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    )

    if (
        baslangicSeciciAcik
    ) {

        DatePickerDialog(

            onDismissRequest = {

                baslangicSeciciAcik =
                    false
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        baslangicSeciciAcik =
                            false
                    }

                ) {

                    Text(
                        "Seç"
                    )
                }
            }

        ) {

            DatePicker(
                state =
                    baslangicTarihState
            )
        }
    }

    if (
        bitisSeciciAcik
    ) {

        DatePickerDialog(

            onDismissRequest = {

                bitisSeciciAcik =
                    false
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        bitisSeciciAcik =
                            false
                    }

                ) {

                    Text(
                        "Seç"
                    )
                }
            }

        ) {

            DatePicker(
                state =
                    bitisTarihState
            )
        }
    }
}

// =============================================================
// GENEL MÜŞTERİ SATIŞLARI
// =============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenelMusteriSatisDialog(
    apiService: ApiService,
    onDismiss: () -> Unit
) {

    var baslangicSeciciAcik by remember {
        mutableStateOf(false)
    }

    var bitisSeciciAcik by remember {
        mutableStateOf(false)
    }

    val baslangicTarihState =
        rememberDatePickerState()

    val bitisTarihState =
        rememberDatePickerState()

    var tumSatislar by remember {
        mutableStateOf<List<Satis>>(
            emptyList()
        )
    }

    var loadingSatislar by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {

        try {

            tumSatislar =
                apiService
                    .getSatislar()
                    .filter {

                        it.musteriId ==
                                null
                    }

        } catch (
            e: Exception
        ) {

            e.printStackTrace()

        } finally {

            loadingSatislar =
                false
        }
    }

    val filtrelenmisSatislar =
        remember(
            tumSatislar,
            baslangicTarihState
                .selectedDateMillis,
            bitisTarihState
                .selectedDateMillis
        ) {

            tumSatislar.filter {
                    satis ->

                val satisZamani =
                    parseTarihMillis(
                        satis.tarih
                    )

                val baslangicKosulu =
                    baslangicTarihState
                        .selectedDateMillis
                        ?.let {

                            satisZamani >= it

                        } ?: true

                val bitisKosulu =
                    bitisTarihState
                        .selectedDateMillis
                        ?.let {

                            satisZamani <=
                                    (
                                            it +
                                                    86400000L
                                            )

                        } ?: true

                baslangicKosulu &&
                        bitisKosulu
            }
        }

    val gruplanmisSatislar =
        remember(
            filtrelenmisSatislar
        ) {

            filtrelenmisSatislar
                .groupBy { satis ->

                    formatTarih(
                        satis.tarih
                    ).substringBefore(
                        " "
                    )
                }
        }

    val toplamRaporTutari =
        remember(
            filtrelenmisSatislar
        ) {

            filtrelenmisSatislar
                .sumOf {

                    it.toplamTutar
                        ?: 0.0
                }
        }

    AlertDialog(

        onDismissRequest =
            onDismiss,

        containerColor =
            Color.White,

        title = {

            Text(

                text =
                    "Genel Müşteri Satışları",

                fontWeight =
                    FontWeight.Bold
            )
        },

        text = {

            Column(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(
                            max = 480.dp
                        )

            ) {

                Text(

                    text =
                        "Müşteri seçilmeden yapılan satışların tarih filtreli dökümü.",

                    fontSize =
                        13.sp,

                    color =
                        Color(0xFF8E8E93)
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )

                Row(

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        ),

                    modifier =
                        Modifier.fillMaxWidth()

                ) {

                    Button(

                        onClick = {

                            baslangicSeciciAcik =
                                true
                        },

                        colors =
                            ButtonDefaults
                                .buttonColors(

                                    containerColor =
                                        Color(
                                            0xFFF2F2F7
                                        ),

                                    contentColor =
                                        Color.Black
                                ),

                        shape =
                            RoundedCornerShape(
                                8.dp
                            ),

                        modifier =
                            Modifier.weight(1f)

                    ) {

                        val basMetni =
                            baslangicTarihState
                                .selectedDateMillis
                                ?.let {

                                    formatTarih(
                                        it.toString()
                                    ).substringBefore(
                                        " "
                                    )

                                } ?: "Başlangıç Seç"

                        Text(

                            text =
                                basMetni,

                            fontSize =
                                12.sp,

                            fontWeight =
                                FontWeight.Medium
                        )
                    }

                    Button(

                        onClick = {

                            bitisSeciciAcik =
                                true
                        },

                        colors =
                            ButtonDefaults
                                .buttonColors(

                                    containerColor =
                                        Color(
                                            0xFFF2F2F7
                                        ),

                                    contentColor =
                                        Color.Black
                                ),

                        shape =
                            RoundedCornerShape(
                                8.dp
                            ),

                        modifier =
                            Modifier.weight(1f)

                    ) {

                        val bitMetni =
                            bitisTarihState
                                .selectedDateMillis
                                ?.let {

                                    formatTarih(
                                        it.toString()
                                    ).substringBefore(
                                        " "
                                    )

                                } ?: "Bitiş Seç"

                        Text(

                            text =
                                bitMetni,

                            fontSize =
                                12.sp,

                            fontWeight =
                                FontWeight.Medium
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )

                HorizontalDivider(
                    color =
                        Color(0xFFF2F2F7),
                    thickness =
                        1.dp
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween

                ) {

                    Text(

                        text =
                            "Dönem Toplam Satış:",

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            Color.Black
                    )

                    Text(

                        text =
                            "₺${
                                formatMusteriCariIkiBasamak(
                                    toplamRaporTutari
                                )
                            }",

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            Color(0xFF34C759)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )

                if (
                    loadingSatislar
                ) {

                    LinearProgressIndicator(
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                } else if (
                    gruplanmisSatislar
                        .isEmpty()
                ) {

                    Box(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Text(

                            text =
                                "Seçilen aralıkta satış kaydı bulunamadı.",

                            color =
                                Color(0xFF8E8E93),

                            fontSize =
                                13.sp
                        )
                    }

                } else {

                    LazyColumn(

                        modifier =
                            Modifier.weight(1f),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )

                    ) {

                        gruplanmisSatislar
                            .forEach {
                                    (
                                        tarihBasligi,
                                        satislarListesi
                                    ) ->

                                item {

                                    Text(

                                        text =
                                            tarihBasligi,

                                        fontSize =
                                            14.sp,

                                        fontWeight =
                                            FontWeight.Bold,

                                        color =
                                            Color(0xFF007AFF),

                                        modifier =
                                            Modifier.padding(
                                                top = 10.dp,
                                                bottom = 2.dp
                                            )
                                    )
                                }

                                items(
                                    satislarListesi
                                ) { satis ->

                                    var kalemler by remember {

                                        mutableStateOf<
                                                List<SatisKalemi>
                                                >(
                                            emptyList()
                                        )
                                    }

                                    LaunchedEffect(
                                        satis.id
                                    ) {

                                        try {

                                            kalemler =
                                                apiService
                                                    .getSatisKalemler(
                                                        satis.id
                                                            ?: 0L
                                                    )

                                        } catch (
                                            e: Exception
                                        ) {

                                        }
                                    }

                                    val satisSaati =
                                        formatSaat(
                                            satis.tarih
                                        )

                                    Column(

                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    Color(
                                                        0xFFF2F2F7
                                                    ),
                                                    RoundedCornerShape(
                                                        8.dp
                                                    )
                                                )
                                                .padding(
                                                    10.dp
                                                )

                                    ) {

                                        Row(

                                            modifier =
                                                Modifier
                                                    .fillMaxWidth(),

                                            horizontalArrangement =
                                                Arrangement
                                                    .SpaceBetween,

                                            verticalAlignment =
                                                Alignment
                                                    .CenterVertically

                                        ) {

                                            Text(

                                                text =
                                                    "Satış",

                                                fontSize =
                                                    11.sp,

                                                fontWeight =
                                                    FontWeight
                                                        .SemiBold,

                                                color =
                                                    Color(
                                                        0xFF8E8E93
                                                    )
                                            )

                                            Text(

                                                text =
                                                    satisSaati,

                                                fontSize =
                                                    12.sp,

                                                fontWeight =
                                                    FontWeight.Bold,

                                                color =
                                                    Color(
                                                        0xFF007AFF
                                                    )
                                            )
                                        }

                                        Spacer(
                                            modifier =
                                                Modifier.height(
                                                    4.dp
                                                )
                                        )

                                        HorizontalDivider(

                                            color =
                                                Color(
                                                    0xFFE5E5EA
                                                ),

                                            thickness =
                                                0.5.dp
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.height(
                                                    4.dp
                                                )
                                        )

                                        kalemler
                                            .forEach {
                                                    kalem ->

                                                Row(

                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(
                                                                vertical =
                                                                    2.dp
                                                            ),

                                                    horizontalArrangement =
                                                        Arrangement
                                                            .SpaceBetween,

                                                    verticalAlignment =
                                                        Alignment
                                                            .CenterVertically

                                                ) {

                                                    Text(

                                                        text =
                                                            "${kalem.urunAdi} (${kalem.adet} ${kalem.birim} × ₺${formatMusteriCariIkiBasamak(kalem.birimFiyat)})",

                                                        fontSize =
                                                            13.sp,

                                                        color =
                                                            Color.Black,

                                                        modifier =
                                                            Modifier.weight(
                                                                1f
                                                            )
                                                    )

                                                    Text(

                                                        text =
                                                            "₺${formatMusteriCariIkiBasamak(kalem.toplam)}",

                                                        fontSize =
                                                            13.sp,

                                                        fontWeight =
                                                            FontWeight.Bold,

                                                        color =
                                                            Color(
                                                                0xFF3C3C43
                                                            )
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

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(

                    text =
                        "Kapat",

                    color =
                        Color(0xFF007AFF),

                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    )

    if (
        baslangicSeciciAcik
    ) {

        DatePickerDialog(

            onDismissRequest = {

                baslangicSeciciAcik =
                    false
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        baslangicSeciciAcik =
                            false
                    }

                ) {

                    Text(
                        "Seç"
                    )
                }
            }

        ) {

            DatePicker(
                state =
                    baslangicTarihState
            )
        }
    }

    if (
        bitisSeciciAcik
    ) {

        DatePickerDialog(

            onDismissRequest = {

                bitisSeciciAcik =
                    false
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        bitisSeciciAcik =
                            false
                    }

                ) {

                    Text(
                        "Seç"
                    )
                }
            }

        ) {

            DatePicker(
                state =
                    bitisTarihState
            )
        }
    }
}

// =============================================================
// GEÇMİŞ SATIŞ KARTI
// =============================================================

@Composable
fun GecmisSatisKarti(
    satis: Satis,
    kalemler: List<SatisKalemi>
) {

    Column(

        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFFF2F2F7),
                    RoundedCornerShape(
                        10.dp
                    )
                )
                .padding(
                    10.dp
                )

    ) {

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Text(

                text =
                    "Satış",

                fontSize =
                    13.sp,

                fontWeight =
                    FontWeight.SemiBold,

                color =
                    Color(0xFF3C3C43)
            )

            Column(

                horizontalAlignment =
                    Alignment.End

            ) {

                Text(

                    text =
                        "₺${
                            formatMusteriCariIkiBasamak(
                                satis.toplamTutar
                                    ?: 0.0
                            )
                        }",

                    fontSize =
                        14.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Color(0xFF34C759)
                )

                Text(

                    text =
                        formatTarih(
                            satis.tarih
                        ),

                    fontSize =
                        10.sp,

                    color =
                        Color(0xFF8E8E93)
                )
            }
        }

        if (
            kalemler.isNotEmpty()
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            HorizontalDivider(

                color =
                    Color(0xFFE5E5EA),

                thickness =
                    1.dp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            kalemler.forEach {
                    kalem ->

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical =
                                    2.dp
                            ),

                    horizontalArrangement =
                        Arrangement.SpaceBetween

                ) {

                    Text(

                        text =
                            "${kalem.urunAdi} — ${kalem.adet} ${kalem.birim} × ₺${formatMusteriCariIkiBasamak(kalem.birimFiyat)}",

                        fontSize =
                            12.sp,

                        color =
                            Color(0xFF8E8E93),

                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                    Text(

                        text =
                            "₺${formatMusteriCariIkiBasamak(kalem.toplam)}",

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight.Medium,

                        color =
                            Color(0xFF3C3C43)
                    )
                }
            }
        }
    }
}

// =============================================================
// İLETİŞİM BUTONU
// =============================================================

@Composable
fun IletisimButonu(
    baslik: String,
    ikon: ImageVector,
    renk: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Row(

        modifier =
            modifier
                .height(
                    44.dp
                )
                .background(
                    renk.copy(
                        alpha =
                            0.12f
                    ),
                    RoundedCornerShape(
                        10.dp
                    )
                )
                .clickable {

                    onClick()
                },

        horizontalArrangement =
            Arrangement.Center,

        verticalAlignment =
            Alignment.CenterVertically

    ) {

        Icon(

            imageVector =
                ikon,

            contentDescription =
                null,

            tint =
                renk,

            modifier =
                Modifier.size(
                    18.dp
                )
        )

        Spacer(
            modifier =
                Modifier.width(
                    6.dp
                )
        )

        Text(

            text =
                baslik,

            color =
                renk,

            fontWeight =
                FontWeight.SemiBold,

            fontSize =
                14.sp
        )
    }
}

// =============================================================
// MÜŞTERİ EKLE
// =============================================================

@Composable
fun MusteriEkleDialog(
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
        keyboardController?.hide()
        focusManager.clearFocus()
        val temizBakiyeText = bakiye.replace(',', '.')
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
        title = { Text("Yeni Müşteri Kaydı", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = ad,
                    onValueChange = { ad = it },
                    label = { Text("Ad Soyad") },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
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
                    label = { Text("Mevcut Başlangıç Borcu (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { kaydet() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(bakiyeFocusRequester),
                    enabled = !isSaving
                )
                if (isSaving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = !isSaving, onClick = { kaydet() }) {
                Text(if (isSaving) "Kaydediliyor..." else "Kaydet", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

// =============================================================
// MÜŞTERİ DÜZENLE
// =============================================================

@Composable
fun MusteriDuzenleDialog(
    musteri: Musteri,
    onDismiss: () -> Unit,
    onKaydet: (ad: String, telefon: String, adres: String) -> Unit
) {
    var ad by remember { mutableStateOf(musteri.ad) }
    var telefon by remember { mutableStateOf(musteri.telefon) }
    var adres by remember { mutableStateOf(musteri.adres) }
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
        title = { Text("Müşteri Bilgilerini Düzenle", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ad,
                    onValueChange = { ad = it },
                    label = { Text("Ad Soyad") },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
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
                    "Not: Borç/Bakiye bilgisi bu ekrandan değiştirilemez, satış veya tahsilat menülerinden otomatik güncellenmesi önerilir.",
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { kaydet() }) {
                Text("Kaydet", color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

// =============================================================
// BAKİYE DÜZENLE
// =============================================================

@Composable
fun BakiyeDuzenleDialog(
    musteri: Musteri,
    onDismiss: () -> Unit,
    onKaydet: (yeniBakiye: Double) -> Unit
) {
    var yeniBakiyeText by remember { mutableStateOf(musteri.bakiye.toString()) }
    val (bakiyeFormatli, _) = bakiyeMetniVeRengi(musteri.bakiye)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun kaydet() {
        val yeniBakiye = yeniBakiyeText.replace(',', '.').toDoubleOrNull()
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
                modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    "Dikkat: Gireceğiniz tutar müşterinin güncel net borcu sayılacaktır. Müşteri size borçluysa düz rakam (Örn: 500), eğer siz müşteriye borçluysanız eksi değer (Örn: -200) giriniz.",
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
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) { Text("İptal", color = Color(0xFF8E8E93)) }
        }
    )
}

// =============================================================
// SAAT FORMATLA
// =============================================================

fun formatSaat(
    tarih: String?
): String {

    if (
        tarih.isNullOrBlank()
    ) {

        return ""
    }

    if (
        tarih.contains("T")
    ) {

        return try {

            val timePart =
                tarih.substringAfter(
                    "T"
                )

            val parts =
                timePart.split(
                    ":"
                )

            if (
                parts.size >= 2
            ) {

                "${parts[0]}:${parts[1]}"

            } else {

                timePart.take(
                    5
                )
            }

        } catch (
            _: Exception
        ) {

            ""
        }
    }

    val millis =
        tarih.toLongOrNull()
            ?: return ""

    val toplamSaniye =
        millis / 1000

    val gunIciSaniye =
        toplamSaniye %
                86400

    val toplamSaatSaniye =
        gunIciSaniye +
                (
                        3 *
                                3600
                        )

    val duzeltilmisSaniye =
        if (
            toplamSaatSaniye >=
            86400
        ) {

            toplamSaatSaniye -
                    86400

        } else {

            toplamSaatSaniye
        }

    val saat =
        (
                duzeltilmisSaniye /
                        3600
                )
            .toString()
            .padStart(
                2,
                '0'
            )

    val dakika =
        (
                (
                        duzeltilmisSaniye %
                                3600
                        ) /
                        60
                )
            .toString()
            .padStart(
                2,
                '0'
            )

    return "$saat:$dakika"
}

// =============================================================
// PARA FORMATLA
// =============================================================

private fun formatMusteriCariIkiBasamak(
    deger: Double
): String {

    val negatifMi =
        deger < 0

    val mutlakDeger =
        if (
            negatifMi
        ) {

            -deger

        } else {

            deger
        }

    val yuvarlanmis =
        (
                (
                        mutlakDeger *
                                100.0
                        ) +
                        0.5
                )
            .toLong() /
                100.0

    val tamKisim =
        yuvarlanmis
            .toLong()

    val kesirKisim =
        (
                (
                        (
                                yuvarlanmis -
                                        tamKisim
                                ) *
                                100.0
                        ) +
                        0.5
                )
            .toLong()

    val kesirStr =
        kesirKisim
            .toString()
            .padStart(
                2,
                '0'
            )

    return "${
        if (
            negatifMi
        ) {
            "-"
        } else {
            ""
        }
    }$tamKisim.$kesirStr"
}
