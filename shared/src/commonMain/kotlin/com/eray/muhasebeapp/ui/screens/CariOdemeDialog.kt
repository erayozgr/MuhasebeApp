package com.eray.muhasebeapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eray.muhasebeapp.data.model.*
import com.eray.muhasebeapp.data.network.ApiService
import com.eray.muhasebeapp.formatTarih
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

private data class OdemeGecmisKaydi(val id: Long, val tarih: String, val tutar: Double)

@Composable
internal fun CariOdemeDialog(apiService: ApiService, id: Long, ad: String, bakiye: Double, tahsilatMi: Boolean, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var tutar by remember { mutableStateOf("") }
    var hata by remember { mutableStateOf<String?>(null) }
    var gecmisHata by remember { mutableStateOf(false) }
    var yukleniyor by remember { mutableStateOf(true) }
    var kaydediliyor by remember { mutableStateOf(false) }
    var tekrar by remember { mutableStateOf(0) }
    var gecmis by remember { mutableStateOf<List<OdemeGecmisKaydi>>(emptyList()) }
    var duzenlenen by remember { mutableStateOf<OdemeGecmisKaydi?>(null) }
    var tarih by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(id, tekrar) {
        yukleniyor = true
        gecmisHata = false
        try {
            gecmis = if (tahsilatMi) apiService.tahsilatGecmisi().filter { it.musteriId == id && it.id != null }.sortedByDescending { it.tarih }.map { OdemeGecmisKaydi(it.id!!, it.tarih, it.tutar) }
                else apiService.odemeGecmisi().filter { it.tedarikciId == id && it.id != null }.sortedByDescending { it.tarih }.map { OdemeGecmisKaydi(it.id!!, it.tarih, it.tutar) }
        } catch (e: Exception) { gecmisHata = true }
        finally { yukleniyor = false }
    }
    val miktar = ondalikSayi(tutar)
    val kalan = miktarYuvarla(bakiye + (duzenlenen?.tutar ?: 0.0) - (miktar ?: 0.0))
    val tarihGecerli = duzenlenen == null || runCatching { LocalDate.parse(tarih) }.isSuccess
    AlertDialog(
        onDismissRequest = { if (!kaydediliyor) onDismiss() },
        title = { Text(if (duzenlenen != null) "İşlemi düzenle" else if (tahsilatMi) "Tahsilat yap" else "Ödeme yap") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(ad)
                Text("Güncel bakiye: ₺${miktarMetni(bakiye)}${if (bakiye < 0) " (avans / alacak)" else " (borç)"}")
                OutlinedTextField(tutar, { tutar = ondalikGirdi(it) }, label = { Text("Tutar (₺)") }, enabled = !kaydediliyor,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = tutar.isNotBlank() && (miktar == null || miktar <= 0))
                if (duzenlenen != null) {
                    OutlinedTextField(tarih, { tarih = it }, label = { Text("Tarih (YYYY-MM-DD)") }, enabled = !kaydediliyor, isError = !tarihGecerli)
                    TextButton(enabled = !kaydediliyor, onClick = { duzenlenen = null; tutar = ""; hata = null }) { Text("Düzenlemeden vazgeç / yeni işlem") }
                }
                Text("İşlem sonrası bakiye: ₺${miktarMetni(kalan)}${if (kalan < 0) " (avans / alacak)" else ""}")
                hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Önceki işlemler")
                when {
                    yukleniyor -> Text("Yükleniyor…")
                    gecmisHata -> {
                        Text("Geçmiş yüklenemedi. Yeni kayıt girebilirsiniz.")
                        TextButton(onClick = { tekrar++ }) { Text("Tekrar dene") }
                    }
                    gecmis.isEmpty() -> Text("Henüz işlem yok.")
                    else -> {
                        Text("${gecmis.size} işlem • Toplam ₺${miktarMetni(gecmis.sumOf { it.tutar })}")
                        gecmis.forEach { kayit ->
                            Text("${formatTarih(kayit.tarih)} — ₺${miktarMetni(kayit.tutar)}")
                            TextButton(enabled = !kaydediliyor, onClick = { duzenlenen = kayit; tutar = kayit.tutar.toString(); tarih = kayit.tarih.take(10); hata = null }) { Text("Düzenle") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = !kaydediliyor && miktar != null && miktar > 0 && tarihGecerli, onClick = {
            kaydediliyor = true
            hata = null
            scope.launch {
                try {
                    val kayit = duzenlenen
                    if (kayit != null) {
                        val yeniTarih = if (tarih == kayit.tarih.take(10)) kayit.tarih else tarih
                        if (tahsilatMi) apiService.updateTahsilat(kayit.id, TahsilatRequest(id, miktar!!, yeniTarih)).getOrThrow()
                        else apiService.updateTedarikciOdemesi(kayit.id, TedarikciOdemeRequest(id, miktar!!, yeniTarih)).getOrThrow()
                    } else if (tahsilatMi) apiService.createTahsilat(TahsilatRequest(id, miktar!!)).getOrThrow()
                    else apiService.createTedarikciOdemesi(TedarikciOdemeRequest(id, miktar!!)).getOrThrow()
                    onSaved()
                } catch (e: Exception) { hata = e.message ?: "İşlem kaydedilemedi." }
                finally { kaydediliyor = false }
            }
        }) { Text(if (kaydediliyor) "Kaydediliyor…" else "Kaydet") } },
        dismissButton = { TextButton(enabled = !kaydediliyor, onClick = onDismiss) { Text("İptal") } }
    )
}
