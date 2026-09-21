from pathlib import Path
import subprocess,time
adb='C:/Users/eozgu/AppData/Local/Android/Sdk/platform-tools/adb.exe'
def run(*args):return subprocess.run([adb,'-s','emulator-5554',*args],capture_output=True,check=True,timeout=40).stdout
for name in ['Musteriler','Tedarikciler','Satis','Alis','Masraf','Stok','Raporlama','Urunler']:
 print(run('shell','am','start','-W','-S','-n','com.eray.muhasebeapp/.MainActivity','--es','screen',name).decode(),flush=True)
 expected={'Musteriler':'Ada Tasarım','Tedarikciler':'Ege Kahve','Satis':'Ada Tasarım','Alis':'Ege Kahve','Masraf':'ofis kirası','Stok':'Seramik','Raporlama':'12540','Urunler':'Seramik'}[name]
 for attempt in range(8):
  run('shell','uiautomator','dump','/sdcard/design-ui.xml')
  xml=run('shell','cat','/sdcard/design-ui.xml').decode()
  if expected in xml:break
  time.sleep(2)
 else:raise RuntimeError('Fixture did not appear: '+name)
 Path('.artifacts/mobile-redesign/'+name+'.xml').write_text(xml,encoding='utf-8')
 Path('.artifacts/mobile-redesign/'+name+'.png').write_bytes(run('exec-out','screencap','-p'))
 print('Captured '+name,flush=True)
print(run('logcat','-d','-s','AndroidRuntime:E').decode(),flush=True)
