import subprocess,time,re,xml.etree.ElementTree as ET
from pathlib import Path
adb='C:/Users/eozgu/AppData/Local/Android/Sdk/platform-tools/adb.exe'
def run(*args):return subprocess.run([adb,'-s','emulator-5554',*args],capture_output=True,check=True,timeout=40).stdout
def tree():
 run('shell','uiautomator','dump','/sdcard/design-ui.xml')
 return ET.fromstring(run('shell','cat','/sdcard/design-ui.xml'))
def tap(node):
 x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
 run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
run('shell','wm','size','900x1600')
run('shell','wm','density','420')
run('shell','settings','put','system','font_scale','1.3')
print(run('shell','am','start','-W','-S','-n','com.eray.muhasebeapp/.MainActivity','--es','screen','Urunler').decode(),flush=True)
for i in range(8):
 ui=tree()
 if any('Seramik' in n.get('text','') for n in ui.iter('node')):break
 time.sleep(2)
else:raise RuntimeError('Products not loaded')
Path('.artifacts/mobile-redesign/compact-large-font.png').write_bytes(run('exec-out','screencap','-p'))
tap(next(n for n in ui.iter('node') if n.get('content-desc')=='Ürün Ekle'))
ui=tree()
assert any('Yeni Ürün Ekle' in n.get('text','') for n in ui.iter('node'))
tap(next(n for n in ui.iter('node') if n.get('class')=='android.widget.EditText'))
time.sleep(2)
ui=tree()
Path('.artifacts/mobile-redesign/compact-keyboard.png').write_bytes(run('exec-out','screencap','-p'))
assert any('Kaydet' in n.get('text','') for n in ui.iter('node'))
run('shell','input','keyevent','4')
run('shell','settings','put','system','font_scale','1.0')
run('shell','wm','size','reset')
run('shell','wm','density','reset')
print('PASS: compact viewport, 1.3 font scale, new product dialog and save action with keyboard.',flush=True)
