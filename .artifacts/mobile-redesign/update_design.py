from pathlib import Path
import re, shutil
root=Path('shared/src/commonMain/kotlin/com/eray/muhasebeapp')
names=['Musteriler','Tedarikciler','Urunler','Satis','Alis','Masraf','Stok','Raporlama']
def endblock(s,start,left='{',right='}'):
    depth=0
    for i in range(start,len(s)):
        if s[i]==left: depth+=1
        elif s[i]==right:
            depth-=1
            if depth==0: return i+1
    raise ValueError('unclosed block')
def replace_body(s,name,body):
    i=s.index('fun '+name+'('); start=s.index('{',s.index(')',i))+0
    return s[:start+1]+'\n'+body+'\n'+s[endblock(s,start)-1:]
colors={'007AFF':'Navy','5856D6':'Teal','34C759':'Success','FFFF9500':'Warning','FF9500':'Warning','FF3B30':'Danger','8E8E93':'Muted','F2F2F7':'Background','F7F7F9':'Background','E3E3E8':'Soft','E5E5EA':'Border','C6C6C8':'Border','3C3C43':'Ink','5AC8FA':'Teal'}
titles={'Musteriler':('Müşteriler','Cari hesaplar ve tahsilatlar'),'Tedarikciler':('Tedarikçiler','İş ortakları ve ödemeler'),'Satis':('Satışlar','Yeni satış ve son işlemler'),'Alis':('Alışlar','Tedarik ve alış işlemleri'),'Masraf':('Masraflar','Giderlerinizi kontrol altında tutun'),'Stok':('Stoklar','Ürün miktarları ve stok hareketleri'),'Raporlama':('Raporlar','İşletmenizin finansal görünümü')}
for name in names:
 p=root/'ui/screens'/f'{name}Screen.kt'; shutil.copy2(p,Path('.artifacts/mobile-redesign')/p.name);s=p.read_text(encoding='utf-8')
 s=s.replace('import androidx.compose.foundation.background','import androidx.compose.foundation.BorderStroke\nimport com.eray.muhasebeapp.ui.*\nimport androidx.compose.foundation.background',1)
 # Replace the whole-screen swipe recognizer: it also consumed horizontal filter scrolling.
 m=re.search(r'\.pointerInput\(Unit\)\s*\{',s)
 if m: s=s[:m.start()]+s[endblock(s,s.index('{',m.start())):]
 s=re.sub(r'    var horizontalDragAccumulator by remember\s*\{\s*mutableStateOf\(0f\)\s*\}\s*\n','',s,count=1)
 for old,new in colors.items(): s=re.sub(r'Color\(\s*0xFF'+old+r'\s*\)',f'BrandColors.{new}',s)
 s=s.replace('Color.Black','BrandColors.Ink')
 s=re.sub(r'RoundedCornerShape\(\s*(?:12|14)\.dp\s*\)', 'RoundedCornerShape(20.dp)',s)
 # Soft outlined surfaces for existing record cards and dialogs.
 s=re.sub(r'(colors\s*=\s*CardDefaults.cardColors\()',r'border = BorderStroke(1.dp, BrandColors.Border),\n        \1',s)
 # Content insets are owned once by MainStructure, including keyboard and cutouts.
 if name!='Urunler':
  s=s.replace('    Scaffold(','    Scaffold(\n        contentWindowInsets = WindowInsets(0, 0, 0, 0),',1)
  start=s.index('topBar = {'); op=s.index('{',start); end=endblock(s,op)
  old=s[op+1:end-1];actions=''
  if 'actions = {' in old:
   a=old.index('actions = {');a=old.index('{',a);actions=old[a+1:endblock(old,a)-1]
   actions=re.sub(r'tint\s*=\s*BrandColors\.\w+', 'tint = Color.White',actions)
  title,sub=titles[name]
  new='topBar = {\n            BrandTopBar("'+title+'", "'+sub+'", onNavigateBack) {'+actions+'\n            }\n        }'
  s=s[:start]+new+s[end:]
 else:
  start=s.index('        Box(',s.index('// --- Üst Bar ---'));op=s.index('{',start);end=endblock(s,op)
  s=s[:start]+'''        BrandTopBar(
            title = if (kritikStokFiltresiAcik) "Kritik stok" else "Ürünler",
            subtitle = "Ürün kataloğu ve fiyat yönetimi",
            onBack = onNavigateBack
        ) {
            IconButton(onClick = { urunEklemeDialogGoster = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ürün Ekle", tint = Color.White)
            }
        }'''+s[end:]
 # Shared summary cards retain all existing values and filtering behavior.
 for f in [name+'OzetKart']:
  if 'fun '+f+'(' in s:
   tail=', containerColor = containerColor' if name=='Urunler' else ''
   s=replace_body(s,f,'    BrandMetric(baslik, deger, renk, modifier'+tail+')')
 if name=='Raporlama': s=replace_body(s,'RaporOzetKart','    BrandMetric(baslik, deger, renk, modifier)')
 # Avoid text clipping when system font size is enlarged.
 s=s.replace('.height(48.dp)', '.heightIn(min = 48.dp)').replace('.height(50.dp)', '.heightIn(min = 50.dp)').replace('.height(52.dp)', '.heightIn(min = 52.dp)')
 s=s.replace('IconButton(onClick = onSil, modifier = Modifier.size(28.dp))','IconButton(onClick = onSil, modifier = Modifier.size(48.dp))')
 p.write_text(s,encoding='utf-8')
# Shared app shell: explicit brand theme and one owner for system/keyboard insets.
p=root/'ui/MainStructure.kt';shutil.copy2(p,Path('.artifacts/mobile-redesign')/p.name);s=p.read_text(encoding='utf-8');start=s.index(') {');s=s[:start+3]+'\n    HesapBenimTheme {\n'+s[start+3:];last=s.rfind('}');s=s[:last]+'    }\n'+s[last:]
s=s.replace('containerColor = Color(0xFFF2F2F7)','containerColor = BrandColors.Background,\n        contentWindowInsets = WindowInsets.safeDrawing')
s=s.replace('                paddingValues\n            )','                paddingValues\n            ).consumeWindowInsets(paddingValues).imePadding()')
p.write_text(s,encoding='utf-8')
