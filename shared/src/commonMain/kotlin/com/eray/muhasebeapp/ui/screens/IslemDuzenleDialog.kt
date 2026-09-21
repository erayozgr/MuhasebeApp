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
import com.eray.muhasebeapp.data.model.Urun
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

internal data class DuzenlenenKalem(val urunId: Long, val ad: String, val birim: String, val adet: String, val fiyat: String)

@Composable
internal fun IslemDuzenleDialog(
    baslik: String,
    tarih: String?,
    ilkKalemler: List<DuzenlenenKalem>,
    urunler: List<Urun>,
    satisMi: Boolean,
    onDismiss: () -> Unit,
    onKaydet: suspend (List<DuzenlenenKalem>, String) -> Unit
) {
    var kalemler by remember { mutableStateOf(ilkKalemler) }
    var tarihText by remember { mutableStateOf(tarih.orEmpty().take(10)) }
    var hata by remember { mutableStateOf<String?>(null) }
    var onay by remember { mutableStateOf(false) }
    var kaydediliyor by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val toplam = kalemler.sumOf { (ondalikSayi(it.adet) ?: 0.0) * (ondalikSayi(it.fiyat) ?: 0.0) }
    val stokUyarilari = if (satisMi) kalemler.groupBy { it.urunId }.mapNotNull { (id, rows) ->
        val urun = urunler.find { it.id == id } ?: return@mapNotNull null
        val eski = ilkKalemler.filter { it.urunId == id }.sumOf { ondalikSayi(it.adet) ?: 0.0 }
        val kalan = miktarYuvarla(urun.stokAdedi + eski - rows.sumOf { ondalikSayi(it.adet) ?: 0.0 })
        if (kalan < 0) "${urun.ad}: işlem sonrası stok ${miktarMetni(kalan)} ${urun.birim}. Satışa devam edebilirsiniz." else null
    } else emptyList()
    AlertDialog(
        onDismissRequest = { if (!kaydediliyor) onDismiss() },
        title = { Text(if (onay) "Değişiklikleri onayla" else baslik) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cari hesap korunur. Toplam: ₺${miktarMetni(toplam)}")
                if (!onay) {
                    OutlinedTextField(tarihText, { tarihText = it }, label = { Text("Tarih (YYYY-MM-DD)") }, enabled = !kaydediliyor)
                    kalemler.forEachIndexed { index, kalem ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(kalem.ad)
                            OutlinedTextField(kalem.adet, { value -> kalemler = kalemler.toMutableList().apply { this[index] = kalem.copy(adet = ondalikGirdi(value)) } },
                                label = { Text("Miktar (${kalem.birim})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), enabled = !kaydediliyor)
                            OutlinedTextField(kalem.fiyat, { value -> kalemler = kalemler.toMutableList().apply { this[index] = kalem.copy(fiyat = ondalikGirdi(value)) } },
                                label = { Text("Birim fiyat (₺)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), enabled = !kaydediliyor)
                            TextButton(onClick = { kalemler = kalemler.filterIndexed { i, _ -> i != index } }, enabled = !kaydediliyor) { Text("Kalemi kaldır") }
                        }
                    }
                    Box {
                        TextButton(onClick = { menu = true }, enabled = !kaydediliyor) { Text("Ürün ekle / değiştir") }
                        DropdownMenu(menu, { menu = false }) {
                            urunler.forEach { urun ->
                                DropdownMenuItem(text = { Text(urun.ad) }, onClick = {
                                    kalemler = kalemler + DuzenlenenKalem(urun.id ?: 0L, urun.ad, urun.birim, "1", (if (satisMi) urun.satisFiyati else urun.alisFiyati).toString())
                                    menu = false
                                })
                            }
                        }
                    }
                } else {
                    Text(tarihText)
                    kalemler.forEach { Text("${it.ad}: ${it.adet} ${it.birim} × ₺${it.fiyat}") }
                    Text("Kaydedildiğinde stoklar ve cari bakiye güncellenir.")
                }
                stokUyarilari.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !kaydediliyor, onClick = {
                hata = null
                if (!onay) {
                    val tarihGecerli = runCatching { LocalDate.parse(tarihText) }.isSuccess
                    if (kalemler.isEmpty() || kalemler.any { (ondalikSayi(it.adet) ?: 0.0) <= 0 || (ondalikSayi(it.fiyat) ?: -1.0) < 0 } || !tarihGecerli) {
                        hata = "Geçerli tarih, pozitif miktar ve sıfır veya pozitif fiyat girin. En az bir kalem gereklidir."
                    } else onay = true
                } else {
                    kaydediliyor = true
                    scope.launch {
                        try { onKaydet(kalemler, tarihText); onDismiss() }
                        catch (e: Exception) { hata = e.message ?: "Kayıt güncellenemedi." }
                        finally { kaydediliyor = false }
                    }
                }
            }) { Text(if (kaydediliyor) "Kaydediliyor…" else if (onay) "Onayla ve kaydet" else "Devam") }
        },
        dismissButton = { TextButton(enabled = !kaydediliyor, onClick = { if (onay) onay = false else onDismiss() }) { Text(if (onay) "Geri" else "İptal") } }
    )
}
