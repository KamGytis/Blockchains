# Blockchains
# Savarankiškai sukurta maišos funkcija
 
Java realizacija: Hash.java (maišos algoritmas) + Main.java (CLI programa).
Algoritmas sukurtas nuo nulio (ARX tipo konstrukcija — sudėtis, rotacija, XOR),
nekopijuoja ir neiškviečia SHA-256, MD5 ar kitos bibliotekos maišos funkcijos.
 
## 1. Įvestis
 
- Palaikomas kintamo ilgio tekstas, įskaitant tuščią įvestį.
- Rankinė įvestis koduojama UTF-8 (StandardCharsets.UTF_8).
- Failo režimu maiša skaičiuojama pagal tikslius failo turinio baitus
  (Files.readAllBytes), o ne pagal failo pavadinimą.
- Praktiniai dydžio apribojimai: kadangi duomenys laikomi viename
  byte[] masyve, o Java masyvo ilgis ribojamas int tipu, didžiausias
  teoriškai apdorojamas dydis — apie 2³¹ − 1 baitas (~2 GB).
## 2. Fiksuota išvestis
 
- Išvesties dydis: 256 bitai (8 × 32 bitų žodžiai).
- Formatuojama kaip 64 hex simbolių eilutė, visada su pradiniais nuliais
  (String.format("%08x", ...) kiekvienam žodžiui).
- Nuosekliai naudojamos mažosios hex raidės.
## 3. Determinizmas
 
Maišos funkcijoje nenaudojama jokia laiko ar atsitiktinumo šaltinio priklausomybė
(nėra Random, System.currentTimeMillis() ir pan. Hash.hash() viduje) —
tie patys baitai visada duoda tą pačią 256 bitų reikšmę, nepriklausomai nuo
paleidimo karto ar aplinkos.
 
## 4. Efektyvumas
 
Išmatuotas Hash.hash() vykdymo laikas didėjant atsitiktinio dvejetainio
turinio dydžiui (matavimai atlikti su JIT apšilimu ir kelių iteracijų vidurkiu):
 
| Įvesties dydis (baitų) | Vidutinis laikas |
|---:|---:|
| 1 000 | 0 ms |
| 10 000 | 0 ms |
| 100 000 | 1 ms |
| 1 000 000 | 6 ms |
| 10 000 000 | 50 ms |
| 100 000 000 | 490 ms |
 
