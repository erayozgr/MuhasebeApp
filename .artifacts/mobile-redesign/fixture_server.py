from http.server import BaseHTTPRequestHandler, HTTPServer
import json
products=[dict(id=1,ad='Seramik Kahve Fincanı',barkod='8690012345678',alisFiyati=120.5,satisFiyati=185.9,stokAdedi=42,birim='Adet',kdvOrani=20),dict(id=2,ad='Özel Harman Filtre Kahve 1 kg',barkod='8690098765432',alisFiyati=450,satisFiyati=620,stokAdedi=3,birim='Paket',kdvOrani=10)]
customers=[dict(id=1,ad='Ada Tasarım Atölyesi',telefon='5321234567',adres='İstanbul',bakiye=12540.5),dict(id=2,ad='Kuzey Mimarlık ve İç Mekân Tasarım Stüdyosu',telefon='5331234567',adres='Ankara',bakiye=-2750)]
suppliers=[dict(id=1,ad='Ege Kahve Tedarik',telefon='5321234567',adres='İzmir',bakiye=8500)]
sales=[dict(id=1,musteriId=1,musteriAdi='Ada Tasarım Atölyesi',tarih='2026-09-17',toplamTutar=12540.5)]
buys=[dict(id=1,tedarikciId=1,tedarikciAdi='Ege Kahve Tedarik',tarih='2026-09-17',toplamTutar=8500)]
lines=[dict(id=1,satisId=1,alisId=1,urunId=1,urunAdi='Seramik Kahve Fincanı',adet=12,birim='Adet',birimFiyat=185.9,toplam=2230.8)]
data={'musteriler':customers,'tedarikciler':suppliers,'urunler':products,'satislar':sales,'alislar':buys,'masraflar':[dict(id=1,kategori='Kira',aciklama='Eylül ayı ofis kirası',tutar=17500,tarih='2026-09-17')],'stok-hareketleri':[],'tahsilatlar':[],'tedarikci-odemeleri':[]}
class Handler(BaseHTTPRequestHandler):
 def do_GET(self):
  path=self.path.split('/api/')[-1].split('?')[0]
  body=lines if 'kalemler' in path else products if path.startswith('urunler') else data.get(path,[])
  self.send_response(200);self.send_header('Content-Type','application/json');self.end_headers();self.wfile.write(json.dumps(body).encode())
 def do_POST(self):
  self.send_response(409);self.end_headers()
HTTPServer(('127.0.0.1',8765),Handler).serve_forever()
