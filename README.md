# Blockchains: maišos funkcija (Java)
 
## 1. Paleidimas
 
Projektas parašytas Java, IntelliJ IDEA projektas `Blockchains`. Visos klasės yra `src` aplanke:
 
| Klasė | Paskirtis |
|---|---|
| `Hash` | maišos funkcija (`Hash.hash(byte[])` grąžina 64 simbolių hex eilutę) |
| `Main` | pagrindinė programa |
| `Determinismtest` | determinizmo testas |
| `Benchmark` | spartos matavimas |
| `Collisiontest` | kolizijų paieška |
| `Avalanchetest` | lavinos efekto testas |
| `Preimagetest` | spėjimo (preimage) atakos testas |
 
Paleidimas iš IntelliJ IDEA: dešinysis pelės klavišas ant norimos klasės (išskyrus `Hash`) → **Run**.
 
Paleidimas iš komandinės eilutės:
 
```
javac -d out src/*.java
java -cp out Main
java -cp out Determinismtest
java -cp out Benchmark
java -cp out Collisiontest
java -cp out Avalanchetest
java -cp out Preimagetest
```
 
Reikia JDK 25.
 
---
 
## 2. Įvestis ir išvestis
 
- **Įvestis:** `byte[]` bet kokio ilgio, įskaitant tuščią. Maišomi tiksliai pateikti baitai, joks tekstas ar eilučių pabaigos simboliai automatiškai nekeičiami.
- **Dydžio ribos:** visa įvestis laikoma atmintyje kaip `byte[]` (riba – Java masyvo dydis, apie 2 GB). Testuota nuo 10 baitų (kolizijų testas) iki 16 200 000 baitų (spartos testas).
- **Išvestis:** visada 256 bitai = 64 mažosios šešioliktainės raidės (8 žodžiai po 32 bitus, formatas `%08x`).
- **Pradiniai nuliai išsaugomi:** kiekvienas žodis spausdinamas lygiai 8 simboliais.
- **Tekstas į baitus:** [nurodyk, kokia koduotė naudojama `Main`, pvz. UTF-8].
---
 
## 3. Algoritmai
 
Abi versijos naudoja 8 žodžių (32 bitų) būseną `h[0..7]`, 32 baitų blokus ir tą pačią pradinę reikšmę `INIT`.
 
### V1 (be DI)
 
```
h ← INIT
m ← pranešimas ‖ 0x80 ‖ 0x00… (iki 32 baitų kartotinio)
kiekvienam 32 baitų blokui (w0 … w7 – big-endian žodžiai):
    h[i] ⊕= w[i]
    4 raundai, kiekvienam i = 0 … 7:
        next ← h[i+1],  previous ← h[i-1]          (indeksai moduliu 8)
        h[i] += next
        h[i] ⊕= rotl(previous, 7)
        h[i] ⊕= rotl(h[i], 2)
        h[i] += rotl(next, 2)
        h[i] ⊕= rotr(previous, 15)
grąžinti hex(h[0]) ‖ … ‖ hex(h[7])
```
 
Trūkumai: raundas yra grįžtamas (nėra feed-forward), visi raundai vienodi, nėra ilgio papildyme, tik 4 raundai.
 
### V2 (su DI)
 
```
h ← INIT
m ← pranešimas ‖ 0x80 ‖ 0x00… ‖ ilgis bitais (64 bitų big-endian), kad ilgis būtų 32 baitų kartotinis
kiekvienam 32 baitų blokui:
    saved ← h
    w[0..7] ← bloko žodžiai (big-endian)
    t = 8 … 63:  w[t] ← w[t-8] + σ0(w[t-7]) + w[t-3] + σ1(w[t-2])
    8 raundai, kiekvienam i = 0 … 7, t = 8·raundas + i:
        next ← h[i+1],  previous ← h[i-1],  opposite ← h[i+4]     (indeksai moduliu 8)
        x ← h[i] + next
        x ⊕= Σ1(previous)
        x ⊕= σ0(x)
        x += σ1(next)
        x ⊕= Σ0(previous)
        x += Ch(previous, next, opposite)
        x ⊕= Maj(previous, next, opposite)
        x += K[t] + w[t]
        h[i] ← x
    h[i] += saved[i]                                   (feed-forward)
grąžinti hex(h[0]) ‖ … ‖ hex(h[7])
```
 
Naudojamos SHA-256 stiliaus funkcijos:
 
- Σ0(x) = rotr(x,2) ⊕ rotr(x,13) ⊕ rotr(x,22)
- Σ1(x) = rotr(x,6) ⊕ rotr(x,11) ⊕ rotr(x,25)
- σ0(x) = rotr(x,7) ⊕ rotr(x,18) ⊕ (x >>> 3)
- σ1(x) = rotr(x,17) ⊕ rotr(x,19) ⊕ (x >>> 10)
- Ch(x,y,z) = (x ∧ y) ⊕ (¬x ∧ z), Maj(x,y,z) = (x ∧ y) ⊕ (x ∧ z) ⊕ (y ∧ z)
- K[t] – pirmų 64 pirminių skaičių kubinių šaknų trupmeninės dalys.
Sprendimų pagrindimas:
 
- Feed-forward (Davies–Meyer) padaro bloko apdorojimą nebegrįžtamą.
- Ilgis papildyme daro papildymą vienareikšmį ir atskiria įvestis, besiskiriančias tik galiniais nuliais.
- Raundų konstantos `K[t]` skiria raundus vienus nuo kitų.
- Message schedule išplečia 8 bloko žodžius iki 64 ir paskleidžia kiekvieną įvesties bitą per visus žingsnius.
- Ch ir Maj įneša netiesiškumą, kurio patys sudėtis, XOR ir rotacijos neduoda.
- 8 raundai × 8 žodžiai = 64 žingsniai, tiek pat kiek SHA-256.
---
 
## 4. Versijos
 
| Versija | Aprašymas | DI |
|---|---|---|
| V1 | Pradinis kodas: 4 raundai, paprastos rotacijos, papildymas be ilgio | ne |
| V2 | Galutinė versija su DI pakeitimais (žingsniai žemiau) | taip |
 
DI pakeitimai V2 versijoje buvo daromi etapais:
 
1. Σ0, Σ1, σ0, σ1 vietoj paprastų rotacijų.
2. Feed-forward ir ilgio papildymas.
3. Raundų konstantos, message schedule, Ch ir Maj, 8 raundai.
Vienodos sąlygos: abi versijos testuotos tuo pačiu testų rinkiniu, su tomis pačiomis įvestimis.
 
---
 
# Maišos funkcijos testų rezultatai — dvi versijos
 
Žemiau pateikiami realūs testų paleidimo rezultatai dviem `Hash.java`
versijoms: V1 (be DI pagalbos) ir V2 (su DI pagalba). Abi versijos
testuotos tuo pačiu testų rinkiniu (`Determinismtest`, `Benchmark`,
`Collisiontest`, `Avalanchetest`, `Preimagetest`), kad rezultatus būtų
galima tiesiogiai lyginti.

---

## Testavimo aplinka
 
Visi testai (determinizmas, sparta, kolizijos, lavinos efektas, spėjimas) paleisti tame pačiame kompiuteryje:
 
| Komponentas | Parametrai |
|---|---|
| Procesorius | AMD Ryzen 7 8845HS w/ Radeon 780M Graphics |
| Branduoliai / gijos | 8 branduoliai, 16 loginių procesorių |
| Bazinis dažnis | 3.80 GHz |
| Talpykla | L1 512 KB, L2 8.0 MB, L3 16.0 MB |
| Operatyvioji atmintis | 32 GB LPDDR5 |
| Vaizdo plokštė | AMD Radeon 780M (integruota) |
| Operacinė sistema | Windows 11 |
| JDK | 25 |
 
Maišos funkcija skaičiuojama tik procesoriuje, vaizdo plokštė nenaudojama. Absoliutūs spartos rezultatai (ms) priklauso nuo šios įrangos, todėl kitame kompiuteryje skirsis, bet V1 ir V2 santykis turėtų išlikti panašus.
## 4. Determinizmas

### V1 (be AI)
```
a) 10x kartotinis hash(A): c1b69a8f5fd60bad5f558d72984eef8c177d82aeef09d5071ce5c7b110c20447
   Visi 10 kartotinių kvietimų sutapo: true

b) hash(A) #1: c1b69a8f5fd60bad5f558d72984eef8c177d82aeef09d5071ce5c7b110c20447
   hash(B):    e4cad11cf3ed2134ad9aa7416d83950306a457b765699d5a85e8dffa21103272
   hash(A) #2: c1b69a8f5fd60bad5f558d72984eef8c177d82aeef09d5071ce5c7b110c20447
   hash(A) #1 == hash(A) #2: true

c) Ankstesnio paleidimo hash(A): c1b69a8f5fd60bad5f558d72984eef8c177d82aeef09d5071ce5c7b110c20447
   Šio paleidimo hash(A):        c1b69a8f5fd60bad5f558d72984eef8c177d82aeef09d5071ce5c7b110c20447
   Sutampa su ankstesniu paleidimu: true
```

### V2 (su AI)
```
a) 10x kartotinis hash(A): 9bfad2d7087450be66d79460333344c20596b634e826d4b6ef9f66d093050c05
   Visi 10 kartotinių kvietimų sutapo: true

b) hash(A) #1: 9bfad2d7087450be66d79460333344c20596b634e826d4b6ef9f66d093050c05
   hash(B):    1deffcf7da46b82a069551efb1b8595941ef27db1e165b94659eada467810051
   hash(A) #2: 9bfad2d7087450be66d79460333344c20596b634e826d4b6ef9f66d093050c05
   hash(A) #1 == hash(A) #2: true

c) Ankstesnio paleidimo hash(A): 9bfad2d7087450be66d79460333344c20596b634e826d4b6ef9f66d093050c05
   Šio paleidimo hash(A):        9bfad2d7087450be66d79460333344c20596b634e826d4b6ef9f66d093050c05
   Sutampa su ankstesniu paleidimu: true
```

**Išvada:** abi versijos deterministinės — kartotiniai kvietimai, seka A→B→A
ir atskiri JVM paleidimai duoda nuoseklius rezultatus.

---

## 5. Sparta

### V1 (be AI)

| Baitų | Vidurkis (ms) |
|---:|---:|
| 81 | ~0.4 |
| 2 592 | ~0.4 |
| 20 736 | ~0.3 |
| 331 776 | ~1.0 |
| 1 327 104 | ~3.9 |
| 2 654 208 | ~10 |
| 5 308 416 | ~19 |
| 10 616 832 | ~38 |
| 16 200 000 (visas failas) | **~58** |

### V2 (su AI)

| Baitų (apytiksliai, nuskaityta iš grafiko) | Vidurkis (ms) |
|---:|---:|
| ~331 776 | ~5 |
| ~1 327 104 | ~15 |
| ~2 654 208 | ~25 |
| ~5 308 416 | ~50 |
| ~10 616 832 | ~193 |
| ~16 200 000 (visas failas) | **~288** |

**Palyginimas:** abi versijos auga maždaug tiesiškai (O(n)) didėjant
įvesties dydžiui — tai patvirtina ir kolizijų testo generavimo laikai
(žr. 5 skyrių): V2 ilgiausiam 1000 baitų ilgiui užtrunka **~2.8× ilgiau**
nei V1 generuojant/maišant tą patį 200 000 įvesčių kiekį (3789 ms vs 1360 ms),
o trumpiausiam 10 baitų ilgiui skirtumas kur kas mažesnis (525 ms vs 425 ms,
~1.2×) — tai rodo, kad V2 turi didesnį **pastovų darbą blokui** (pvz.
daugiau raundų ar papildomus veiksmus), kurio poveikis labiausiai matomas
ilgesnėms, daugiau blokų turinčioms įvestims.

---

## 6. Kolizijos

Kiekvienam ilgiui {10, 100, 500, 1000} baitų: 100 000 porų (200 000
įvesčių), tikrinta poromis, visu rinkiniu ir struktūruotais atvejais.

### V1 (be AI)

| Ilgis (B) | Generavimo laikas | Porų kolizijos | Viso rinkinio kolizijos | Struktūruoti atvejai |
|---:|---:|---:|---:|---:|
| 10 | 425 ms | 0 | 0 | 0 / 5 |
| 100 | 386 ms | 0 | 0 | 0 / 5 |
| 500 | 820 ms | 0 | 0 | 0 / 5 |
| 1000 | 1360 ms | 0 | 0 | 0 / 5 |

### V2 (su AI)

| Ilgis (B) | Generavimo laikas | Porų kolizijos | Viso rinkinio kolizijos | Struktūruoti atvejai |
|---:|---:|---:|---:|---:|
| 10 | 525 ms | 0 | 0 | 0 / 5 |
| 100 | 702 ms | 0 | 0 | 0 / 5 |
| 500 | 1987 ms | 0 | 0 | 0 / 5 |
| 1000 | 3789 ms | 0 | 0 | 0 / 5 |

**Išvada:** nė viena versija nerado kolizijų nei porų, nei viso rinkinio,
nei struktūruotų atvejų patikroje — abi elgiasi vienodai gerai šiuo
aspektu, V2 tiesiog skaičiuoja lėčiau.

---

## 7. Lavinos efektas

100 000 porų (25 000 × 4 ilgiai), keičiamas vienas simbolis.

### V1 (be AI)

| Ilgis | MinBit | MaxBit | VidBit | MinHex | MaxHex | VidHex |
|---:|---:|---:|---:|---:|---:|---:|
| 10 | 97 | 159 | 128.017 | 49 | 64 | 60.014 |
| 100 | 97 | 159 | 127.941 | 50 | 64 | 60.000 |
| 500 | 93 | 159 | 128.055 | 51 | 64 | 60.006 |
| 1000 | 93 | 161 | 128.033 | 50 | 64 | 60.005 |

**Bendrai:** bitų skirtumas min=93 max=161 vidurkis=**128.012** (idealu 128.0/256);
hex skirtumas vidurkis=**60.006**/64.

Histograma (grupuota po 8 bitus):
```
[88-95]:    (2)
[96-103]:   (124)
[104-111]:  ####### (1752)
[112-119]:  ############################################# (12574)
[120-127]:  ########################################################### (33000)
[128-135]:  ######################################################## (35074)
[136-143]:  ################################################################### (14803)
[144-151]:  ########## (2509)
[152-159]:  # (160)
[160-167]:  (2)
[168-175]:  (0)
```

### V2 (su AI)

| Ilgis | MinBit | MaxBit | VidBit | MinHex | MaxHex | VidHex |
|---:|---:|---:|---:|---:|---:|---:|
| 10 | 96 | 160 | 128.058 | 51 | 64 | 60.007 |
| 100 | 94 | 157 | 128.092 | 50 | 64 | 60.006 |
| 500 | 98 | 159 | 128.094 | 50 | 64 | 60.010 |
| 1000 | 97 | 162 | 128.026 | 50 | 64 | 60.010 |

**Bendrai:** bitų skirtumas min=94 max=162 vidurkis=**128.067** (idealu 128.0/256);
hex skirtumas vidurkis=**60.008**/64.

Histograma (grupuota po 8 bitus):
```
[88-95]:    (1)
[96-103]:   (117)
[104-111]:  ####### (1855)
[112-119]:  ############################################# (12200)
[120-127]:  ################################################################ (32909)
[128-135]:  ################################################################### (35294)
[136-143]:  ############################################################### (14903)
[144-151]:  ########## (2552)
[152-159]:  # (167)
[160-167]:  (2)
[168-175]:  (0)
```

**Palyginimas:** abi versijos labai artimos idealiam atsitiktiniam elgesiui
(vidurkis ~128.0 iš 256 bitų abiem atvejais, skirtumas tarp versijų — tik
0.055 bito, nereikšmingas). Pasiskirstymų formos beveik tapačios — jokia
versija nerodo pranašumo ar silpnybės lavinos efekto atžvilgiu.

---

## 8. Spėjimas (preimage)

### V1 (be AI)

| Scenarijus | Bandymų | Laikas | Sutampantys kandidatai |
|---|---:|---:|---|
| a) Be druskos, pilna maiša (taikinys `dfed6652...`) | 10 000 | 104 ms | `[4821]` |
| a-sutrump.) 12 bitų maiša | 10 000 | 32 ms | `[3053, 4052, 4821, 6975, 7107]` |
| b) Su vieša druska (`b7f3-vieša-druska-01`) | 10 000 | 31 ms | `[4821]` |
| c) Be druskos, 5 taikiniai (bendra lentelė) | 10 000 (lentelė) | 23 ms | 5/5 taikinių iškart |
| c) Su skirtinga druska, 5 taikiniai | 50 000 | 118 ms | kiekvienam atskirai |
| d) Su slaptu `r` (taikinys `8a32f5cf...`) | 10 000 | — | `[]` (0, kaip tikėtasi) |

### V2 (su AI)

| Scenarijus | Bandymų | Laikas | Sutampantys kandidatai |
|---|---:|---:|---|
| a) Be druskos, pilna maiša (taikinys `5df172b2...`) | 10 000 | 135 ms | `[4821]` |
| a-sutrump.) 12 bitų maiša | 10 000 | 47 ms | `[0860, 4821, 6403]` |
| b) Su vieša druska (`b7f3-vieša-druska-01`) | 10 000 | 61 ms | `[4821]` |
| c) Be druskos, 5 taikiniai (bendra lentelė) | 10 000 (lentelė) | 41 ms | 5/5 taikinių iškart |
| c) Su skirtinga druska, 5 taikiniai | 50 000 | 151 ms | kiekvienam atskirai |
| d) Su slaptu `r` (taikinys `0cfbe104...`) | 10 000 | — | `[]` (0, kaip tikėtasi) |

**Palyginimas:** abi versijos elgiasi kokybiškai vienodai — pilnai maišai
rastas tik teisingas PIN, sutrumpintai maišai rasti keli atsitiktiniai
sutapimai (V1: 5, V2: 3 — abu atitinka teorinį lūkestį ~2.44 esant 12 bitų
nupjovimui ir 10 000 kandidatų), vieša druska nepailgina atakos prieš vieną
taikinį, kelių taikinių su skirtinga druska atveju bandymų skaičius
kartotinai padaugėja (50 000 = 5 × 10 000), o slaptas `r` abiem versijoms
visiškai užkerta kelią brute-force paieškai. Vienintelis skirtumas —
absoliutus laikas (V2 lėtesnė dėl 4 skyriuje aptartos priežasties).

---

## Bendra išvada

| Savybė | V1 (be AI) | V2 (su AI) |
|---|---|---|
| Determinizmas | ✅ | ✅ |
| Sparta (1000 B, 200k kartų) | 1360 ms | 3789 ms (~2.8× lėčiau) |
| Kolizijos (800k patikrų) | 0 | 0 |
| Lavinos efektas (vidurkis/256) | 128.012 | 128.067 |
| Preimage apsauga su `r` | veikia | veikia |

Abi versijos vienodai gerai tenkina saugumo savybes (kolizijos, lavinos
efektas, preimage) — statistiškai reikšmingo skirtumo tarp jų nėra. Vienintelis
pastebimas, reprodukuojamas skirtumas yra sparta: V2 yra apie 2–3 kartus
lėtesnė už V1 didesnėms įvestims, tačiau abi išlieka tiesinės (O(n))
sudėtingumo.

# 9. Šaltiniai ir DI naudojimas

## Šaltiniai

- NIST FIPS 180-4: SHA-256 funkcijos Σ0, Σ1, σ0, σ1, Ch, Maj, raundų konstantos (pirminių skaičių kubinių šaknų trupmeninės dalys), message schedule ir papildymo schema.
- Merkle-Damgård ir Davies-Meyer konstrukcijos: ilgio padding'as ir feed-forward (`h = P(h ^ m) + h_senas`).
- Java `MessageDigest` (SHA-256): naudojamas tik palyginimui ir kaip rekomenduojama alternatyva produkcijoje.
- Java `VarHandle` ir `byteArrayViewVarHandle`: baitų skaitymas big-endian formatu pirmoje optimizacijos versijoje.

Naudota Claude (Anthropic), modelis Claude Sonnet 5.5, per claude.ai pokalbį.

## DI pasiūlymai
 
| DI pasiūlymas | Sprendimas | Patikra |
|---|---|---|
| Greičio ir sudėtingumo optimizacija: be `pad()` kopijavimo (atmintis O(n) → O(1)), išvynioti `% 8` indeksai, būsena lokaliuose kintamuosiuose, `VarHandle` vietoj `readWord`, `char[]` vietoj `String.format` | priimta kaip tarpinis žingsnis, vėliau grįžta prie originalios struktūros | galutinė versija išmatuota `Benchmark` testu: visas failas (16 200 000 B) ~288 ms (V1 ~58 ms), augimas tiesinis O(n) |
| Hash dizaino tobulinimas: feed-forward, raundų konstantos (`GOLDEN`), 8 + 4 raundai, ilgio padding'as, finalizacija | priimta kaip kryptis, įgyvendinta vėlesniuose žingsniuose | paleista visu testų rinkiniu (`Determinismtest`, `Benchmark`, `Collisiontest`, `Avalanchetest`, `Preimagetest`); rezultatai žemiau |
| Pilnas SHA-256 įgyvendinimas (mokymuisi) | atmesta, nes išlaikytas savo originalus kodas | nepaleista (palyginimas su `MessageDigest` ir vektoriumi `abc` neatliktas) |
| Σ0, Σ1, σ0, σ1 originaliame kode vietoj paprastų rotacijų | priimta | `Avalanchetest`: 100 000 porų, vidurkis 128.067 iš 256 bitų (min 94, max 162), hex 60.008/64, histograma beveik tokia pati kaip V1 |
| Feed-forward ir ilgio padding'as (`0x80` + nuliai + 64 bitų ilgis) | priimta | `Determinismtest`: 10 kartotinių kvietimų, seka A→B→A ir atskiri paleidimai sutampa; `Collisiontest`: 0 kolizijų (800 000 patikrų, struktūruoti atvejai 0 / 5); `Preimagetest`: su slaptu `r` kandidatų 0 |
| Raundų konstantos K[64], message schedule (8 → 64 žodžiai), Ch ir Maj, 8 raundai (64 žingsniai) | priimta | `Collisiontest`: 0 kolizijų visiems ilgiams (10, 100, 500, 1000 B); sparta ~2.8× lėtesnė nei V1 (1000 B: 3789 ms vs 1360 ms), tai tikėtina dėl papildomų žingsnių |
| Perspėjimas: funkcija nėra kriptografiškai audituota ir nėra SHA-256; saugumui naudoti `MessageDigest`, slaptažodžiams Argon2id arba bcrypt | priimta kaip pastaba | – |
