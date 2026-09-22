# Predviđanje cijena dionica metodama strojnog učenja

Desktop aplikacija u Javi koja dohvaća povijesne podatke o dionicama, izračunava
tehničke pokazatelje, trenira tri modela strojnog učenja i simulira trgovinsku
strategiju nad ispitnim skupom podataka.

Izrađena u sklopu završnog rada na prijediplomskom studiju **Elektroničko poslovanje
i programsko inženjerstvo**, Fakultet turizma i ruralnog razvoja u Požegi,
Sveučilište Josipa Jurja Strossmayera u Osijeku.

**Autor:** Gabrijel Matanović

![Usporedba modela](docs/screenshots/03-usporedba-modela.png)

---

## Sadržaj

- [Mogućnosti](#mogućnosti)
- [Rezultati](#rezultati)
- [Snimke zaslona](#snimke-zaslona)
- [Tehnologije](#tehnologije)
- [Pokretanje](#pokretanje)
- [Podatci](#podatci)
- [Arhitektura](#arhitektura)
- [Ograničenja](#ograničenja)

---

## Mogućnosti

- Dohvat povijesnih podataka putem Alpha Vantage API-ja ili uvoz iz CSV datoteke
- Tehnički pokazatelji: pomični prosjek (20 dana) i RSI (14 dana)
- Normalizacija (min-maks ili standardizacija) i **kronološka** podjela na skup za
  učenje i skup za ispitivanje
- Tri modela: linearna regresija, slučajna šuma, višeslojni perceptron
- Vrednovanje mjerama RMSE, MAE i R²
- Simulacija trgovinske strategije s usporedbom naspram strategije „drži i čekaj"
- Izvoz podataka i predikcija u CSV

---

## Rezultati

### Skup podataka

| | |
|---|---|
| Dionica | IBM |
| Izvor | Stooq (uvoz iz CSV-a) |
| Ukupno zapisa | 16 273 trgovinska dana (1962. – 2026.) |
| Skup za učenje | 13 018 zapisa (80 %) |
| Skup za ispitivanje | 3 255 zapisa (20 %), 3. 10. 2013. – 4. 9. 2026. |
| Normalizacija | min-maks |
| Obilježja | zaključna cijena i obujam za 5 prethodnih dana, SMA(20), RSI(14) |

### Točnost predikcija

RMSE i MAE izraženi su u normaliziranoj ljestvici (0 – 1).

| Model | RMSE | MAE | R² | Vrijeme treniranja |
|---|---:|---:|---:|---:|
| **Linearna regresija** | **0,0095** | **0,0050** | **0,9964** | **937 ms** |
| Višeslojni perceptron | 0,1029 | 0,0497 | 0,5769 | 16 796 ms |
| Slučajna šuma | 0,1416 | 0,0630 | 0,1989 | 2 468 ms |

### Simulacija trgovinske strategije

Početni ulog 10 000 $, transakcijski trošak 0,1 %, razdoblje 3. 10. 2013. – 4. 9. 2026.

| Model | Dobit | Prinos | Udio dobitnih | Najveći pad | Prosj. pogreška |
|---|---:|---:|---:|---:|---:|
| **Višeslojni perceptron** | **30 901,22 $** | **309,01 %** | 40,7 % | **29,44 %** | 16,2332 |
| Linearna regresija | 15 804,25 $ | 158,04 % | **51,8 %** | 44,46 % | **1,6286** |
| Slučajna šuma | 1 078,55 $ | 10,79 % | 48,4 % | 46,85 % | 20,5638 |
| *Drži i čekaj* | *7 996,00 $* | *79,96 %* | – | – | – |

### Glavni nalaz

**Najtočniji model nije donio najveću dobit.**

Poredak modela po točnosti gotovo je obrnut od poretka po prinosu. Linearna regresija
ima deset puta manju prosječnu pogrešku od višeslojnog perceptrona, a ostvarila je
upola manji prinos.

Razlog je što strategija ne koristi predviđenu *razinu* cijene, nego samo *smjer*
njezine promjene u odnosu na prethodni dan. Model može sustavno griješiti u razini, a
dovoljno često pogađati smjer.

Iz toga slijede tri zaključka:

1. **Visok R² ovdje ne znači dobro predviđanje.** Cijena se od dana do dana mijenja
   malo u odnosu na svoju razinu, pa i model koji samo preslika jučerašnju cijenu
   postiže R² blizu 1. Linearna regresija je naučila razinu cijene, ne njezinu promjenu.
2. **Mjerilo određuje pobjednika.** Aplikacija bira najbolji model po RMSE-u i
   odabire linearnu regresiju. Da je mjerilo prinos, izabrala bi perceptron.
3. **Stabla odluke ne ekstrapoliraju.** Slučajna šuma ne može predvidjeti cijenu višu
   od najviše viđene u učenju. Kako je cijena u ispitnom razdoblju rasla, sustavno
   podcjenjuje, pa ostvaruje najlošiji rezultat, ispod pasivne strategije.

### Oprez pri tumačenju

- Perceptron ima **najniži udio dobitnih transakcija (40,7 %)**. Dobit dolazi iz
  malog broja velikih pogodaka, ne iz dosljednosti, pa je osjetljiva na slučajnost.
- Najveći pad portfelja kreće se od **29 % do 47 %**, dakle prinos prati visok rizik.
- Rezultati vrijede za **jednu dionicu i jedno razdoblje** i ne smiju se poopćavati.
- Simulacija ne obuhvaća proklizavanje cijene, likvidnost ni porezna davanja.

Rezultati su u skladu s hipotezom o učinkovitosti tržišta (Fama, 1970): nadmašivanje
pasivne strategije u jednom razdoblju na jednoj dionici ne dokazuje da je model
pouzdano prediktivan.

---

## Snimke zaslona

### Podatci

Uvoz 16 273 zapisa, prikaz kretanja cijene s pomičnim prosjekom i indeksa relativne snage.

![Podatci](docs/screenshots/01-podatci.png)

### Treniranje modela

Priprema podataka, usporedba triju modela i automatski odabir najboljeg po RMSE-u.

![Treniranje modela](docs/screenshots/02-treniranje-modela.png)

### Usporedba svih modela u simulaciji

Rezultat usporedbe sa strategijom „drži i čekaj". Na grafikonu predikcija vidljivo je
kako perceptron griješi u razini cijene, a ipak ostvaruje najveći prinos.

![Usporedba modela](docs/screenshots/03-usporedba-modela.png)

### Simulacija za pojedinačni model

Simulacija za linearnu regresiju: predikcije gotovo prate stvarnu cijenu, ali to se
ne prevodi u najveću dobit.

![Simulacija linearne regresije](docs/screenshots/04-simulacija-linearna-regresija.png)

### Izvoz i postavke

Pristupni ključ prikazuje se samo djelomično.

![Izvoz i postavke](docs/screenshots/05-izvoz-i-postavke.png)

---

## Tehnologije

| Tehnologija | Namjena |
|---|---|
| Java 17 | programski jezik i izvedbeno okruženje |
| JavaFX 21 | grafičko korisničko sučelje (FXML + CSS) |
| Weka 3.8.6 | algoritmi strojnog učenja |
| Apache Maven | ovisnosti i izgradnja |
| OkHttp, Gson | komunikacija s API-jem |
| Apache Commons CSV | uvoz i izvoz podataka |
| SLF4J, Logback | bilježenje događaja |
| JUnit 5, Mockito | jedinično testiranje |

---

## Pokretanje

### Preduvjeti

- JDK 17
- Apache Maven 3.8+

### Izgradnja i pokretanje

```bash
mvn clean package -DskipTests
java -jar target/stock-predictor-1.0.0.jar
```

> **Ne koristiti `mvn javafx:run`.** Taj način stavlja biblioteke na module path,
> zbog čega pada inicijalizacija Weke
> (`NoClassDefFoundError: Could not initialize class weka.core.Capabilities`).
> Izvršni JAR sve stavlja na classpath i radi ispravno.

### Windows izvršna datoteka

Uz JDK 17 (`jpackage` je uključen):

| Skripta | Rezultat | Treba Java? |
|---|---|---|
| `napravi-exe.bat` | mapa s `.exe` | ne |
| `napravi-instalaciju.bat` | instalacijski `.exe` (treba WiX Toolset **v3**) | ne |

---

## Podatci

### Uvoz iz CSV-a (preporučeno)

1. Na [stooq.com](https://stooq.com) potražiti dionicu (npr. `IBM.US`)
2. **Historical data** → na dnu stranice **Download data in csv file**
3. U aplikaciji: kartica *Podatci* → **Uvezi CSV**

Očekivani stupci: `Date, Open, High, Low, Close, Volume`.

### Alpha Vantage API

Aplikacija **ne sadrži** pristupni ključ. Besplatni ključ:
[alphavantage.co/support/#api-key](https://www.alphavantage.co/support/#api-key)

```bash
# Windows (PowerShell)
$env:ALPHA_VANTAGE_API_KEY="vas_kljuc"

# Linux / macOS
export ALPHA_VANTAGE_API_KEY=vas_kljuc
```

Ili unijeti na kartici *Izvoz i postavke*.

Besplatna razina ograničena je na 25 zahtjeva dnevno, a `outputsize=full` dostupan je
samo uz plaćeni plan, pa API vraća oko 100 posljednjih trgovinskih dana. Za veće
skupove koristiti CSV.

---

## Arhitektura

MVC obrazac proširen slojem servisa.

```
src/main/java/com/stockpredictor/
├── app/         StockPredictorApp, Launcher
├── config/      AppConfig, ServiceProvider
├── controller/  Main, Dashboard, Model, Backtest, Export
├── model/       StockData, DataSet, ModelMetrics, BacktestResult, ...
├── service/     AlphaVantageData, DataPreparation, ModelTrainer, Backtesting, CSV, Chart
└── util/        FeatureExtractor, DataNormalizer
```

Tijek podataka:

```
dohvat / uvoz  →  FeatureExtractor (SMA, RSI)  →  DataNormalizer
      →  kronološka podjela 80/20  →  ModelTrainerService (Weka)
      →  ModelMetrics (RMSE, MAE, R²)  →  BacktestingService  →  BacktestResult
```

### Strategija u simulaciji

- predviđeni rast > 0,1 % → kupnja cijelim raspoloživim iznosom
- predviđeni pad > 0,1 % → prodaja svih dionica
- trošak transakcije 0,1 %

### Testiranje

```bash
mvn test
```

Četiri testna razreda: `FeatureExtractorTest`, `DataPreparationServiceTest`,
`CSVServiceTest`, `AlphaVantageDataServiceTest`. Testovi koji zahtijevaju mrežni
poziv izvode se samo kad je postavljen pristupni ključ.

---

## Ograničenja

- Koriste se isključivo podatci o cijenama i obujmu, bez temeljnih pokazatelja,
  vijesti ili raspoloženja na tržištu.
- Hiperparametri modela postavljeni su ručno, bez sustavne optimizacije.
- Parametri normalizacije računaju se na cijelom skupu, što je blagi oblik
  propuštanja informacija iz ispitnog skupa.
- Skup uključuje podatke od 1962., čiji tržišni uvjeti nisu usporedivi s današnjima.

---

## Literatura

- Breiman, L. (2001) Random Forests. *Machine Learning*, 45 (1), str. 5–32.
- Fama, E. F. (1970) Efficient Capital Markets: A Review of Theory and Empirical Work.
  *The Journal of Finance*, 25 (2), str. 383–417.
- Murphy, J. J. (1999) *Technical Analysis of the Financial Markets*. New York: New York
  Institute of Finance.
- Wilder, J. W. (1978) *New Concepts in Technical Trading Systems*. Greensboro: Trend Research.
- Witten, I. H. et al. (2016) *Data Mining: Practical Machine Learning Tools and
  Techniques*. 4. izd. Cambridge: Morgan Kaufmann.

---

## Odricanje od odgovornosti

Projekt je izrađen u obrazovne svrhe. Predikcije ne predstavljaju savjet za ulaganje i
ne smiju se koristiti za donošenje stvarnih financijskih odluka.


