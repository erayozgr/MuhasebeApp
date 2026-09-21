from pathlib import Path
import re
root=Path('shared/src/commonMain/kotlin/com/eray/muhasebeapp/ui/screens')
def end(s,start):
 d=0
 for i in range(start,len(s)):
  if s[i]=='{': d+=1
  elif s[i]=='}':
   d-=1
   if d==0:return i+1
 raise ValueError()
def body(s,name,new):
 i=s.index('fun '+name+'(');a=s.index('{',s.index(')',i));return s[:a+1]+'\n'+new+'\n'+s[end(s,a)-1:]
for n,party,fmt,var,icon,label in [('Musteriler','Musteri','formatMusteriCariIkiBasamak','musteriler','Person','Müşteri'),('Tedarikciler','Tedarikci','formatTedarikciCariIkiBasamak','tedarikciler','LocalShipping','Tedarikçi')]:
 p=root/(n+'Screen.kt');s=p.read_text(encoding='utf-8');obj=party[0].lower()+party[1:]
 balance='bakiyeMetniVeRengi' if n=='Musteriler' else 'tedarikciBakiyeMetniVeRengi'
 s=body(s,party+'Kart',f'''    val (balance, accent) = {balance}({obj}.bakiye)
    BrandRecordCard(
        title = {obj}.ad, detail = {obj}.telefon,
        value = balance, caption = "Cari bakiye",
        icon = Icons.Default.{icon}, accent = accent, onClick = onTikla
    ) {{
        IconButton(onClick = onSil) {{
            Icon(Icons.Default.DeleteOutline, contentDescription = "{label} sil", tint = BrandColors.Danger)
        }}
    }}''')
 # Replace only the first overview card, preserving following general-account shortcuts.
 a=s.index('            Card(');b=s.index('{',a);e=end(s,b)
 total='toplamBakiye' if n=='Musteriler' else 'toplamBorc'
 s=s[:a]+f'''            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {{
                BrandMetric("Toplam {label}", {var}.size.toString(), BrandColors.Navy, Modifier.weight(1f))
                BrandMetric("{'Müşteri borcu' if n=='Musteriler' else 'Toplam borcumuz'}", "₺${{{fmt}({total})}}",
                    if ({total} > 0) BrandColors.Warning else BrandColors.Success, Modifier.weight(1f))
            }}'''+s[e:]
 p.write_text(s,encoding='utf-8')
p=root/'UrunlerScreen.kt';s=p.read_text(encoding='utf-8');s=body(s,'UrunSatiri','''    BrandRecordCard(
        title = urun.ad,
        detail = "Barkod: ${urun.barkod.ifEmpty { "—" }} · KDV %${urun.kdvOrani}",
        value = "₺${urun.satisFiyati.toUrunParaFormat()}",
        caption = "Satış fiyatı · Stok: ${urun.stokAdedi} ${urun.birim}",
        icon = Icons.Default.Inventory2,
        accent = if (urun.stokAdedi <= 5L) BrandColors.Danger else BrandColors.Teal
    ) {
        IconButton(onClick = onDuzenle) { Icon(Icons.Default.Edit, "Ürünü düzenle", tint = BrandColors.Navy) }
        IconButton(onClick = onSil) { Icon(Icons.Default.DeleteOutline, "Ürünü sil", tint = BrandColors.Danger) }
    }''')
a=s.index('            Card(',s.index('// --- Ürün Listesi ---'));b=s.index('{',a);e=end(s,b)
s=s[:a]+'''            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(urunListesi) { _, urun ->
                    UrunSatiri(urun, onDuzenle = { duzenlenecekUrun = urun }, onSil = { silinecekUrun = urun })
                }
            }'''+s[e:];p.write_text(s,encoding='utf-8')
p=root/'StokScreen.kt';s=p.read_text(encoding='utf-8');s=body(s,'StokKart','''    val critical = urun.stokAdedi <= 5L
    BrandRecordCard(
        title = urun.ad, detail = urun.barkod,
        value = "${urun.stokAdedi} ${urun.birim}",
        caption = if (critical) "Kritik stok · Stok hareketi eklemek için dokunun" else "Mevcut stok · Hareket eklemek için dokunun",
        icon = if (critical) Icons.Default.Warning else Icons.Default.Inventory2,
        accent = if (critical) BrandColors.Danger else BrandColors.Teal,
        onClick = onDuzenle
    )''');s=s.replace('                LazyColumn(\n','                LazyColumn(\n                    modifier = Modifier.weight(1f),',1)
s=s.replace('                singleLine = true,','                shape = RoundedCornerShape(18.dp),\n                singleLine = true,',1)
s=s.replace('Column(verticalArrangement = Arrangement.spacedBy(12.dp))','Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp))',1)
if 'import androidx.compose.foundation.verticalScroll' not in s:s=s.replace('import androidx.compose.foundation.background','import androidx.compose.foundation.verticalScroll\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.background',1)
p.write_text(s,encoding='utf-8')
p=root/'MasrafScreen.kt';s=p.read_text(encoding='utf-8');s=body(s,'MasrafKart','''    BrandRecordCard(
        title = masraf.kategori, detail = masraf.aciklama,
        value = "₺${formatMasrafIkiBasamak(masraf.tutar)}",
        caption = formatTarih(masraf.tarih).substringBefore(" "),
        icon = kategoriIkonu(masraf.kategori), accent = BrandColors.Danger
    ) {
        IconButton(onClick = onSil) { Icon(Icons.Default.DeleteOutline, "Masrafı sil", tint = BrandColors.Danger) }
    }''');p.write_text(s,encoding='utf-8')
for n in ['Satis','Alis']:
 p=root/(n+'Screen.kt');s=p.read_text(encoding='utf-8')
 func='SepetKalemKart' if n=='Satis' else 'AlisSepetKalemKart'
 # Find the actual local cart component by its signature.
 match=re.search(r'private fun (\w+)\(kalem:.*?onSil:',s);func=match.group(1)
 price='satisFiyati' if n=='Satis' else 'alisFiyati';fmt='formatSatisFiyatiIkiBasamak' if n=='Satis' else 'formatAlisFiyatiIkiBasamak'
 s=body(s,func,f'''    BrandRecordCard(
        title = kalem.urun.ad,
        detail = "${{kalem.adet}} ${{kalem.urun.birim}} × ₺${{{fmt}(kalem.{price})}}",
        value = "₺${{{fmt}(kalem.toplam)}}", caption = "Kalem toplamı",
        icon = Icons.Default.ShoppingBag, accent = BrandColors.Teal
    ) {{
        IconButton(onClick = onSil) {{ Icon(Icons.Default.DeleteOutline, "Sepetten çıkar", tint = BrandColors.Danger) }}
    }}''')
 # Let selected customer/supplier names occupy remaining width instead of overflowing the dropdown chevron.
 s=s.replace('Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {','Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {',1)
 p.write_text(s,encoding='utf-8')
# Remove unused swipe imports from the redesigned screens.
for n in ['Musteriler','Tedarikciler','Urunler','Satis','Alis','Masraf','Stok','Raporlama']:
 p=root/(n+'Screen.kt');s=p.read_text(encoding='utf-8');s=s.replace('import androidx.compose.foundation.gestures.detectHorizontalDragGestures\n','').replace('import androidx.compose.ui.input.pointer.pointerInput\n','')
 p.write_text(s,encoding='utf-8')
