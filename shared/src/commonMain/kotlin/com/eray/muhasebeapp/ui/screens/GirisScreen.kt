package com.eray.muhasebeapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eray.muhasebeapp.data.model.LoginRequest
import com.eray.muhasebeapp.data.model.RegisterRequest
import com.eray.muhasebeapp.data.network.AuthService
import com.eray.muhasebeapp.util.AppConfig.SURUM
import com.eray.muhasebeapp.util.AppConfig.SOZLESME_SURUMU
import com.eray.muhasebeapp.util.AppConfig.SOZLESME_URL
import com.eray.muhasebeapp.rememberUrlAcici
import hesapbenim.shared.generated.resources.Res
import hesapbenim.shared.generated.resources.hesap_benim_uzun_logo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

private val ArkaPlanGradyan = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF13B0A5),
        Color(0xFF0E7C8C),
        Color(0xFF14406B),
        Color(0xFF0B1F3A)
    )
)

private val BeyazYariSeffaf = Color.White.copy(alpha = 0.14f)

@Composable
fun GirisScreen(
    onSifremiUnuttum: (() -> Unit)? = null,
    onGirisBasarili: (
        kullaniciId: Long,
        adSoyad: String,
        email: String
    ) -> Unit
) {
    var isKayitModu by remember { mutableStateOf(false) }
    var sozlesmeKabul by remember { mutableStateOf(false) }
    val urlAcici = rememberUrlAcici()

    var adSoyad by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefon by remember { mutableStateOf("") }
    var sifre by remember { mutableStateOf("") }
    var sifreTekrar by remember { mutableStateOf("") }

    var sifreGoster by remember { mutableStateOf(false) }
    var sifreTekrarGoster by remember { mutableStateOf(false) }

    var yukleniyor by remember { mutableStateOf(false) }
    var hataMesaji by remember { mutableStateOf<String?>(null) }

    // =========================================================
    // KAYIT E-POSTA DOĞRULAMA
    // =========================================================

    var kayitEmailDogrulamaAcik by remember { mutableStateOf(false) }
    var bekleyenKayitRequest by remember { mutableStateOf<RegisterRequest?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val authService = remember { AuthService() }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val emailFocusRequester = remember { FocusRequester() }
    val telefonFocusRequester = remember { FocusRequester() }
    val sifreFocusRequester = remember { FocusRequester() }
    val sifreTekrarFocusRequester = remember { FocusRequester() }

    fun sonrakiAlanaGit(focusRequester: FocusRequester) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // =========================================================
    // VALIDASYON
    // =========================================================

    fun emailGecerliMi(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailRegex.matches(email.trim())
    }

    fun sifreSekizKarakterMi(): Boolean = sifre.length >= 8
    fun sifreBuyukHarfVarMi(): Boolean = sifre.any { it.isUpperCase() }
    fun sifreKucukHarfVarMi(): Boolean = sifre.any { it.isLowerCase() }
    fun sifreRakamVarMi(): Boolean = sifre.any { it.isDigit() }
    fun sifreOzelKarakterVarMi(): Boolean = sifre.any { !it.isLetterOrDigit() }
    fun sifreBoslukIceriyorMu(): Boolean = sifre.any { it.isWhitespace() }

    fun formGecerliMi(): Boolean {
        hataMesaji = null
        val girisDegeri = email.trim()

        if (!isKayitModu) {
            if (girisDegeri.isBlank()) {
                hataMesaji = "E-posta adresinizi giriniz."
                return false
            }
            if (!emailGecerliMi(girisDegeri)) {
                hataMesaji = "Geçerli bir e-posta adresi giriniz."
                return false
            }
            if (sifre.isBlank()) {
                hataMesaji = "Şifrenizi giriniz."
                return false
            }
            return true
        }

        if (adSoyad.isBlank()) {
            hataMesaji = "Ad soyad veya işletme adı boş bırakılamaz."
            return false
        }
        if (girisDegeri.isBlank()) {
            hataMesaji = "E-posta adresi boş bırakılamaz."
            return false
        }
        if (!emailGecerliMi(girisDegeri)) {
            hataMesaji = "Geçerli bir e-posta adresi giriniz."
            return false
        }
        if (telefon.isBlank()) {
            hataMesaji = "Telefon numarası boş bırakılamaz."
            return false
        }
        if (!telefon.all { it.isDigit() }) {
            hataMesaji = "Telefon numarası yalnızca rakamlardan oluşmalıdır."
            return false
        }
        if (telefon.startsWith("0")) {
            hataMesaji = "Telefon numarasını başında 0 olmadan giriniz. Örnek: 5321234567"
            return false
        }
        if (telefon.length != 10) {
            hataMesaji = "Telefon numarası 10 haneli olmalıdır. Örnek: 5321234567"
            return false
        }
        if (!telefon.startsWith("5")) {
            hataMesaji = "Geçerli bir cep telefonu numarası giriniz. Örnek: 5321234567"
            return false
        }
        if (sifre.isBlank()) {
            hataMesaji = "Şifre boş bırakılamaz."
            return false
        }
        if (!sifreSekizKarakterMi()) {
            hataMesaji = "Şifre en az 8 karakter olmalıdır."
            return false
        }
        if (!sifreBuyukHarfVarMi()) {
            hataMesaji = "Şifre en az 1 büyük harf içermelidir."
            return false
        }
        if (!sifreKucukHarfVarMi()) {
            hataMesaji = "Şifre en az 1 küçük harf içermelidir."
            return false
        }
        if (!sifreRakamVarMi()) {
            hataMesaji = "Şifre en az 1 rakam içermelidir."
            return false
        }
        if (!sifreOzelKarakterVarMi()) {
            hataMesaji = "Şifre en az 1 özel karakter içermelidir."
            return false
        }
        if (sifreBoslukIceriyorMu()) {
            hataMesaji = "Şifre boşluk içeremez."
            return false
        }
        if (sifreTekrar.isBlank()) {
            hataMesaji = "Şifrenizi tekrar giriniz."
            return false
        }
        if (sifre != sifreTekrar) {
            hataMesaji = "Girdiğiniz şifreler birbiriyle eşleşmiyor."
            return false
        }
        return true
    }

    // =========================================================
    // GİRİŞ VEYA KAYIT
    // =========================================================

    fun girisVeyaKayitYap() {
        if (isKayitModu && !sozlesmeKabul) {
            hataMesaji = "Kayıt olmak için Kullanıcı Sözleşmesi’ni kabul etmelisiniz."
            return
        }

        if (!formGecerliMi() || yukleniyor) {
            return
        }

        keyboardController?.hide()
        focusManager.clearFocus()

        hataMesaji = null
        yukleniyor = true

        coroutineScope.launch {
            if (isKayitModu) {
                val temizEmail = email.trim().lowercase()
                val temizTelefon = telefon.trim()

                val request = RegisterRequest(
                    adSoyad = adSoyad.trim(),
                    email = temizEmail,
                    telefon = temizTelefon,
                    sifre = sifre,
                    kullaniciSozlesmesiKabul = sozlesmeKabul,
                    sozlesmeSurumu = SOZLESME_SURUMU
                )

                val result = authService.register(request)
                yukleniyor = false

                result.onSuccess { res ->
                    if (res.basarili) {
                        bekleyenKayitRequest = request
                        kayitEmailDogrulamaAcik = true
                    } else {
                        println("Kayıt Hatası (Sunucu): ${res.mesaj}")
                        hataMesaji = res.mesaj.ifBlank { "Doğrulama kodu gönderilemedi." }
                    }
                }.onFailure { err ->
                    println("Kayıt Hatası (Kritik): ${err.message}")
                    hataMesaji = "İşlem başarısız. Tekrar deneyin."
                }
            } else {
                val girisDegeri = email.trim().lowercase()
                val result = authService.login(
                    LoginRequest(
                        email = girisDegeri,
                        sifre = sifre
                    )
                )
                yukleniyor = false

                result.onSuccess { res ->
                    if (res.basarili && res.kullaniciId != null) {
                        onGirisBasarili(
                            res.kullaniciId,
                            res.adSoyad.orEmpty(),
                            res.email.orEmpty()
                        )
                    } else {
                        println("Giriş Hatası (Sunucu): ${res.mesaj}")
                        hataMesaji = res.mesaj.ifBlank { "Giriş yapılamadı." }
                    }
                }.onFailure { err ->
                    println("Giriş Hatası (Kritik): ${err.message}")
                    hataMesaji = "İşlem başarısız. Tekrar deneyin."
                }
            }
        }
    }

    // =========================================================
    // KAYIT E-POSTA DOĞRULAMA EKRANI
    // =========================================================

    if (kayitEmailDogrulamaAcik && bekleyenKayitRequest != null) {
        KayitEmailDogrulamaScreen(
            request = bekleyenKayitRequest!!,
            authService = authService,
            onGeri = {
                kayitEmailDogrulamaAcik = false
                bekleyenKayitRequest = null
            },
            onKayitTamamlandi = { kullaniciId, yeniAdSoyad, yeniEmail ->
                kayitEmailDogrulamaAcik = false
                bekleyenKayitRequest = null
                onGirisBasarili(kullaniciId, yeniAdSoyad, yeniEmail)
            }
        )
        return
    }

    // =========================================================
    // ANA GİRİŞ EKRANI
    // =========================================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArkaPlanGradyan)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Image(
                painter = painterResource(Res.drawable.hesap_benim_uzun_logo),
                contentDescription = "Hesap Benim Logo",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(80.dp)
            )

            Text(
                text = if (isKayitModu) "Yeni İşletme Hesabı Oluşturun" else "Hesabınıza Giriş Yapın",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 10.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // =================================================
            // GİRİŞ / KAYIT SEKME
            // =================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(4.dp)
            ) {
                // Giriş Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (!isKayitModu) Color(0xFF13B0A5) else Color.Transparent)
                        .clickable {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            isKayitModu = false
                            hataMesaji = null
                            adSoyad = ""
                            telefon = ""
                            sifreTekrar = ""
                            sifre = ""
                            sifreGoster = false
                            sifreTekrarGoster = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Giriş Yap",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (!isKayitModu) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Kayıt Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isKayitModu) Color(0xFF13B0A5) else Color.Transparent)
                        .clickable {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            isKayitModu = true
                            hataMesaji = null
                            email = ""
                            sifre = ""
                            sifreTekrar = ""
                            sifreGoster = false
                            sifreTekrarGoster = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Kayıt Ol",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isKayitModu) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BeyazYariSeffaf),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Ad Soyad
                    AnimatedVisibility(visible = isKayitModu) {
                        OutlinedTextField(
                            value = adSoyad,
                            onValueChange = {
                                adSoyad = it
                                hataMesaji = null
                            },
                            label = { Text(text = "Ad Soyad veya İşletme Adı", color = Color.White.copy(alpha = 0.8f)) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(emailFocusRequester) }),
                            colors = girisTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // E-Posta
                    val emailHatali = email.isNotEmpty() && !emailGecerliMi(email)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { yeniDeger ->
                            email = yeniDeger.trim()
                            hataMesaji = null
                        },
                        label = { Text(text = "E-posta Adresi", color = Color.White.copy(alpha = 0.8f)) },
                        placeholder = { Text(text = "ornek@mail.com", color = Color.White.copy(alpha = 0.4f)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color.White) },
                        isError = emailHatali,
                        supportingText = {
                            if (emailHatali) {
                                Text(text = "Geçerli bir e-posta adresi giriniz", color = Color(0xFFFF8A80), fontSize = 11.sp)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                if (isKayitModu) {
                                    sonrakiAlanaGit(telefonFocusRequester)
                                } else {
                                    sonrakiAlanaGit(sifreFocusRequester)
                                }
                            }
                        ),
                        colors = girisTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(emailFocusRequester)
                    )

                    // Telefon
                    AnimatedVisibility(visible = isKayitModu) {
                        OutlinedTextField(
                            value = telefon,
                            onValueChange = { yeniDeger ->
                                telefon = yeniDeger.filter { it.isDigit() }.take(10)
                                hataMesaji = null
                            },
                            label = { Text(text = "Telefon Numarası", color = Color.White.copy(alpha = 0.8f)) },
                            placeholder = { Text(text = "5321234567", color = Color.White.copy(alpha = 0.4f)) },
                            supportingText = {
                                Text(text = "Başında 0 olmadan giriniz", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                            },
                            leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color.White) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { sonrakiAlanaGit(sifreFocusRequester) }),
                            colors = girisTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(telefonFocusRequester)
                        )
                    }

                    // Şifre
                    OutlinedTextField(
                        value = sifre,
                        onValueChange = {
                            sifre = it
                            hataMesaji = null
                        },
                        label = { Text(text = "Şifre", color = Color.White.copy(alpha = 0.8f)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White) },
                        trailingIcon = {
                            IconButton(onClick = { sifreGoster = !sifreGoster }) {
                                Icon(
                                    imageVector = if (sifreGoster) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        },
                        visualTransformation = if (sifreGoster) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isKayitModu) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { if (isKayitModu) sonrakiAlanaGit(sifreTekrarFocusRequester) },
                            onDone = { if (!isKayitModu) girisVeyaKayitYap() }
                        ),
                        colors = girisTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(sifreFocusRequester)
                    )

                    // Şifre Kuralları
                    AnimatedVisibility(visible = isKayitModu) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SifreKuralSatiri(tamamlandi = sifreSekizKarakterMi(), metin = "En az 8 karakter")
                            SifreKuralSatiri(tamamlandi = sifreBuyukHarfVarMi(), metin = "En az 1 büyük harf")
                            SifreKuralSatiri(tamamlandi = sifreKucukHarfVarMi(), metin = "En az 1 küçük harf")
                            SifreKuralSatiri(tamamlandi = sifreRakamVarMi(), metin = "En az 1 rakam")
                            SifreKuralSatiri(tamamlandi = sifreOzelKarakterVarMi(), metin = "En az 1 özel karakter")
                            SifreKuralSatiri(tamamlandi = sifre.isNotEmpty() && !sifreBoslukIceriyorMu(), metin = "Boşluk içermemeli")
                        }
                    }

                    // Şifre Tekrar
                    AnimatedVisibility(visible = isKayitModu) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = sifreTekrar,
                                onValueChange = {
                                    sifreTekrar = it
                                    hataMesaji = null
                                },
                                label = { Text(text = "Şifre Tekrar", color = Color.White.copy(alpha = 0.8f)) },
                                leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White) },
                                trailingIcon = {
                                    IconButton(onClick = { sifreTekrarGoster = !sifreTekrarGoster }) {
                                        Icon(
                                            imageVector = if (sifreTekrarGoster) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                },
                                visualTransformation = if (sifreTekrarGoster) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { girisVeyaKayitYap() }),
                                colors = girisTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(sifreTekrarFocusRequester)
                            )

                            if (sifreTekrar.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = if (sifre == sifreTekrar) "✓ Şifreler eşleşiyor" else "Şifreler eşleşmiyor",
                                    color = if (sifre == sifreTekrar) Color(0xFF6EE7B7) else Color(0xFFFF8A80),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }

                    // Kullanıcı Sözleşmesi Onayı (Yalnızca Kayıt Modunda)
                    if (isKayitModu) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = sozlesmeKabul, onCheckedChange = { sozlesmeKabul = it })
                            Text(
                                text = "Kullanıcı Sözleşmesi’ni okudum ve kabul ediyorum.",
                                color = Color.White,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClickLabel = "Kullanıcı Sözleşmesi’ni web sitesinde aç") {
                                        urlAcici.ac(SOZLESME_URL)
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }

                    // Hata Mesajı
                    AnimatedVisibility(visible = hataMesaji != null) {
                        Text(
                            text = hataMesaji.orEmpty(),
                            color = Color(0xFFFF8A80),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Buton
                    Button(
                        onClick = { girisVeyaKayitYap() },
                        enabled = !yukleniyor && (!isKayitModu || sozlesmeKabul),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF13B0A5),
                            disabledContainerColor = Color(0xFF13B0A5).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (yukleniyor) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = if (isKayitModu) "Kayıt Ol" else "Giriş Yap",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Şifremi Unuttum
                    if (!isKayitModu && onSifremiUnuttum != null) {
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onSifremiUnuttum()
                            },
                            enabled = !yukleniyor,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.White,
                                disabledContentColor = Color.White.copy(alpha = 0.5f)
                            )
                        ) {
                            Text("Şifremi unuttum", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        Text(
            text = "v$SURUM",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

// =============================================================
// KAYIT E-POSTA DOĞRULAMA
// =============================================================

@Composable
private fun KayitEmailDogrulamaScreen(
    request: RegisterRequest,
    authService: AuthService,
    onGeri: () -> Unit,
    onKayitTamamlandi: (
        kullaniciId: Long,
        adSoyad: String,
        email: String
    ) -> Unit
) {
    var kod by remember { mutableStateOf("") }
    var yukleniyor by remember { mutableStateOf(false) }
    var tekrarGonderiliyor by remember { mutableStateOf(false) }

    var hataMesaji by remember { mutableStateOf<String?>(null) }
    var bilgiMesaji by remember {
        mutableStateOf<String?>("6 haneli doğrulama kodu e-posta adresinize gönderildi.")
    }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArkaPlanGradyan)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    enabled = !yukleniyor && !tekrarGonderiliyor,
                    onClick = onGeri
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "E-posta Doğrulama",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(45.dp))
                    .background(BeyazYariSeffaf),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MarkEmailUnread,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "E-posta Adresinizi Doğrulayın",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${request.email} adresine gönderilen 6 haneli doğrulama kodunu girin.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = BeyazYariSeffaf)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Doğrulama Kodu",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = kod,
                        onValueChange = { yeniDeger ->
                            kod = yeniDeger.filter { it.isDigit() }.take(6)
                            hataMesaji = null
                        },
                        enabled = !yukleniyor && !tekrarGonderiliyor,
                        label = { Text(text = "6 Haneli Kod") },
                        placeholder = { Text(text = "000000") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Pin, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        enabled = kod.length == 6 && !yukleniyor && !tekrarGonderiliyor,
                        onClick = {
                            hataMesaji = null
                            bilgiMesaji = null
                            yukleniyor = true

                            coroutineScope.launch {
                                val result = authService.kayitEmailDogrula(
                                    email = request.email,
                                    kod = kod
                                )
                                yukleniyor = false

                                result.onSuccess { response ->
                                    if (response.basarili && response.kullaniciId != null) {
                                        onKayitTamamlandi(
                                            response.kullaniciId,
                                            response.adSoyad ?: request.adSoyad,
                                            response.email ?: request.email
                                        )
                                    } else {
                                        hataMesaji = response.mesaj.ifBlank { "Doğrulama kodu hatalı." }
                                    }
                                }.onFailure { error ->
                                    println("Kayıt E-posta Doğrulama Hatası: ${error.message}")
                                    hataMesaji = "Doğrulama işlemi başarısız. Tekrar deneyin."
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (yukleniyor) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(imageVector = Icons.Default.Verified, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "E-postayı Doğrula", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        enabled = !yukleniyor && !tekrarGonderiliyor,
                        onClick = {
                            tekrarGonderiliyor = true
                            hataMesaji = null
                            bilgiMesaji = null

                            coroutineScope.launch {
                                val result = authService.register(request)
                                tekrarGonderiliyor = false

                                result.onSuccess { response ->
                                    if (response.basarili) {
                                        kod = ""
                                        bilgiMesaji = "Yeni doğrulama kodu e-posta adresinize gönderildi."
                                    } else {
                                        hataMesaji = response.mesaj.ifBlank { "Kod tekrar gönderilemedi." }
                                    }
                                }.onFailure { error ->
                                    println("Kod Tekrar Gönderme Hatası: ${error.message}")
                                    hataMesaji = "Kod tekrar gönderilemedi. Tekrar deneyin."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (tekrarGonderiliyor) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Kod Gönderiliyor...", color = Color.White)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Kodu Tekrar Gönder", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = bilgiMesaji != null) {
                Text(
                    text = bilgiMesaji.orEmpty(),
                    color = Color(0xFFB9F6CA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
            }

            AnimatedVisibility(visible = hataMesaji != null) {
                Text(
                    text = hataMesaji.orEmpty(),
                    color = Color(0xFFFF8A80),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// =============================================================
// TEXTFIELD RENKLERİ
// =============================================================

@Composable
private fun girisTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
    cursorColor = Color.White,
    errorBorderColor = Color(0xFFFF8A80),
    errorLabelColor = Color(0xFFFF8A80),
    errorCursorColor = Color(0xFFFF8A80),
    focusedSupportingTextColor = Color.White.copy(alpha = 0.55f),
    unfocusedSupportingTextColor = Color.White.copy(alpha = 0.55f)
)

// =============================================================
// ŞİFRE KURAL SATIRI
// =============================================================

@Composable
private fun SifreKuralSatiri(tamamlandi: Boolean, metin: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (tamamlandi) "✓" else "•",
            color = if (tamamlandi) Color(0xFF6EE7B7) else Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontWeight = if (tamamlandi) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = metin,
            color = if (tamamlandi) Color(0xFF6EE7B7) else Color.White.copy(alpha = 0.65f),
            fontSize = 12.sp
        )
    }
}