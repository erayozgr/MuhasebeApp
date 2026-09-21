from pathlib import Path
import subprocess,time
adb='C:/Users/eozgu/AppData/Local/Android/Sdk/platform-tools/adb.exe'
def run(*args):return subprocess.run([adb,'-s','emulator-5554',*args],capture_output=True,check=True).stdout
for i in range(20):
 if run('shell','getprop','sys.boot_completed').strip()==b'1':break
 time.sleep(2)
else:raise RuntimeError('Emulator not booted yet')
print(run('install','-r','androidApp/build/outputs/apk/debug/androidApp-debug.apk').decode())
run('shell','input','keyevent','82')
run('shell','am','start','-S','-n','com.eray.muhasebeapp/.MainActivity','--es','screen','Urunler')
time.sleep(4)
Path('.artifacts/mobile-redesign/Urunler.png').write_bytes(run('exec-out','screencap','-p'))
print('Products screenshot captured')
