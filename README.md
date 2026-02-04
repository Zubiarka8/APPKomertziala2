# APP Komertziala - Komertzialen Agenda eta Eskaeren Kudeaketa

## 📋 Proiektuaren Sarrera

**APP Komertziala** Android aplikazio profesional bat da, komertzialen eguneroko lan-fluxua errazteko diseinatua. Aplikazioak hiru funtzionalitate nagusi eskaintzen ditu:

- **Komertzialen Kudeaketa**: Komertzial bakoitzak bere datuak modu seguruan kudeatu ditzake, saio-kudeaketa zorrotzaren bidez.
- **Agenda Kudeaketa**: Komertzial bakoitzak bere bisitak eta zita-programazioak kudeatu ditzake, bazkideekin izandako harremanak jarraituz.
- **Inbentarioa eta Eskaeren Kudeaketa**: Produktuen katalogoa kudeatu, eskaera berriak sortu, editatu eta jarraitu, katalogoko produktuekin lotuta.

Aplikazioak **XML fitxategien bidezko sinkronizazioa** onartzen du (ordezkaritzatik jasotako datuak), datu-base lokal bat mantentzen du Room Persistence Library erabiliz, eta **segurtasun iragazki zorrotza** inplementatzen du: komertzial bakoitzak bere datuak bakarrik ikus ditzake (`komertzialKodea` WHERE klausulak datu-baseko query guztietan).

---

## 🏗️ Arkitektura

### MVVM eta Repository Patroiak

Aplikazioak **MVVM (Model-View-ViewModel)** arkitektura eta **Repository** patroia erabiltzen ditu, geruzen arteko banaketa garbia bermatzeko:

```
┌─────────────────────────────────────────┐
│           UI Layer (Activities)        │
│  - LoginActivity                        │
│  - MainActivity                         │
│  - AgendaModuluaActivity                │
│  - EskaerakActivity                     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Repository Layer                   │
│  - AgendaRepository                     │
│  - DatuKudeatzailea                     │
│  - XMLKudeatzailea                      │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Data Layer (Room)                  │
│  - AppDatabase                          │
│  - DAO Interfaces                       │
│  - Entity Classes                       │
└─────────────────────────────────────────┘
```

**Abantailak:**
- **Testagarritasuna**: Logika UI-tik bananduta dago, unitate-probak errazagoak dira.
- **Mantentze-erraztasuna**: Geruza bakoitzak bere erantzukizuna du.
- **Errendimendua**: Repository-ek `ExecutorService` erabiltzen du datu-base eragiketak hari nagusitik kanpo exekutatzeko.

---

## 🔑 Klase Nagusiak

### LoginActivity

**Helburua**: Saioa hasiera eta segurtasun geruza.

**Funtzionalitateak:**
- XML fitxategien inportazioa (assets-etik edo gailutik)
- Komertzial hautaketa (datu-basean oinarrituta)
- Erabiltzaile/pasahitza balidazioa (`Logina` taularen arabera)
- Google Maps integrazioa (Donostia zentratua)
- `SessionManager` erabiliz saio-kudeaketa segurua

**Kode adibidea:**
```java
// SEGURTASUNA: SessionManager erabiliz saioa hasi
SessionManager sessionManager = new SessionManager(this);
sessionManager.saioaHasi(komertziala.getKodea(), komertziala.getIzena());
```

### Bazkidea Entitatea

Bazkideen datu-egitura, `bazkideak.xml` fitxategiaren egiturari dagokiona.

**Eremuak:**
- `id` (Primary Key, auto-generatua)
- `nan` (NAN identifikatzailea, indizea)
- `izena`, `abizena`
- `telefonoZenbakia`, `posta`
- `jaiotzeData` (yyyy/MM/dd formatua)
- `argazkia` (argazki fitxategiaren izena)
- `kodea`, `helbidea`, `probintzia`
- `komertzialKodea` (Foreign Key → `komertzialak.kodea`, `ON DELETE CASCADE`)
- `sortutakoData`

**Indizeak:**
- `nan` (bilaketa azkartzeko)
- `izena` (bilaketa azkartzeko)
- `kodea`
- `komertzialKodea` (Foreign Key erlazioa)

### Eskaera Entitateak

Eskaeren kudeaketa hiru entitate desberdinen bidez egiten da:

#### EskaeraGoiburua
Eskaeraren goiburua: zenbakia, data, komertzial kodea/ID, ordezkaritza, bazkidea kodea/ID.

**Gako nagusia**: `zenbakia` (String, Primary Key)

**Indizeak:**
- `komertzialKodea`, `bazkideaKodea`
- `komertzialId`, `bazkideaId`

#### EskaeraXehetasuna
Eskaeraren xehetasunak: artikulu kodea, kantitatea, prezioa.

**Gako nagusia**: `id` (auto-generatua)

**Indizeak:**
- `eskaeraZenbakia` (EskaeraGoiburua-rekin lotura)
- `artikuluKodea` (Katalogoa-rekin lotura)

#### Eskaera (Zaharra)
XML-etik datozen eskaera zaharren formatua (`bazkideak.xml`-eko `<eskaerak>` blokea).

**Eremuak:**
- `eskaeraID`, `prodIzena`, `data`, `kopurua`, `prodArgazkia`
- `bazkideaId` (Foreign Key → `bazkideak.id`, `ON DELETE CASCADE`)

---

## 💾 Datu-basea eta Room

### Room Persistence Library

Aplikazioak **Room Persistence Library 2.6.1** erabiltzen du SQLite datu-base lokal bat kudeatzeko. Room-ek konpilazio garaian SQL kontsultak baliozkotzen ditu eta type-safe DAO interfazeak sortzen ditu.

### Entitate Taulak

| Taula | Gako Nagusia | Deskribapena |
|-------|--------------|--------------|
| `komertzialak` | `id` (auto) | Komertzialen informazioa |
| `bazkideak` | `id` (auto) | Bazkideen datu osoak |
| `katalogoa` | `artikuluKodea` | Produktuen katalogoa |
| `eskaera_goiburuak` | `zenbakia` | Eskaeren goiburuak |
| `eskaera_xehetasunak` | `id` (auto) | Eskaeren xehetasunak |
| `eskaerak` | `id` (auto) | Eskaera zaharrak (XML formatua) |
| `agenda_bisitak` | `id` (auto) | Bisiten agenda |
| `loginak` | `id` (auto) | Erabiltzaile/pasahitza sarbideak |

### Taulen Arteko Erlazioak (@ForeignKey)

#### Bazkidea → Komertziala
```java
@ForeignKey(
    entity = Komertziala.class,
    parentColumns = "kodea",
    childColumns = "komertzialKodea",
    onDelete = ForeignKey.CASCADE
)
```
**Eragina**: Komertzial bat ezabatzen denean, bere bazkideak automatikoki ezabatzen dira.

#### Eskaera → Bazkidea
```java
@ForeignKey(
    entity = Bazkidea.class,
    parentColumns = "id",
    childColumns = "bazkideaId",
    onDelete = ForeignKey.CASCADE
)
```
**Eragina**: Bazkide bat ezabatzen denean, bere eskaerak automatikoki ezabatzen dira.

### Indizeak (indices)

Indizeak kontsultak azkartzeko erabiltzen dira:

**Bazkideak taula:**
- `nan`: NAN bidezko bilaketa azkarra
- `izena`: Izen bidezko bilaketa
- `kodea`: Kode bidezko bilaketa
- `komertzialKodea`: Foreign Key erlazioa azkartzeko

**Agenda taula:**
- `bazkideaKodea`, `bisitaData`, `bazkideaId`, `komertzialaId`, `komertzialKodea`
- Indize konposatua: `(komertzialKodea, bazkideaKodea, bisitaData)`

**EskaeraGoiburua taula:**
- `komertzialKodea`, `bazkideaKodea`, `komertzialId`, `bazkideaId`

---

## 🔄 Migrazioak: Datu-basearen Bertsio Aldaketak eta Eskema Eguneraketak

### Bertsio Kudeaketa

Datu-baseak **15 bertsio** ditu, eskema aldaketak migrazio estrategia baten bidez kudeatzen direlarik. Migrazio bakoitza `Migration` klase baten bidez definitzen da, eta `AppDatabase.getInstance()` deitzean automatikoki exekutatzen da.

### Migrazio Estrategia

**Bertsio igoerak:**
- Migrazio bakoitza `Migration` klase baten bidez definitzen da
- `taulaExistitzenDa()` eta `zutabeaExistitzenDa()` metodoak bikoiztuak saihesteko erabiltzen dira
- Transakzio seguruak: `runInTransaction()` erabiliz (Room-ek automatikoki aplikatzen du)

**Adibidea (MIGRAZIO_13_14 - Table Swap Estrategia):**
```java
private static final Migration MIGRAZIO_13_14 = new Migration(13, 14) {
    @Override
    public void migrate(SupportSQLiteDatabase db) {
        if (!taulaExistitzenDa(db, "bazkideak")) {
            // Taula ez badago, sortu egitura ZUZEKIN (Foreign Key barne)
            db.execSQL("CREATE TABLE bazkideak (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                       "nan TEXT, izena TEXT, ..., " +
                       "FOREIGN KEY(komertzialKodea) REFERENCES komertzialak(kodea) ON DELETE CASCADE)");
            return;
        }

        // Table swap estrategia: SQLite-k ezin du Foreign Key bat gehitu ALTER TABLE-rekin
        // 1. Sortu taula berria egitura ZUZEKIN (Foreign Key barne)
        db.execSQL("CREATE TABLE bazkideak_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                   "nan TEXT, izena TEXT, ..., " +
                   "FOREIGN KEY(komertzialKodea) REFERENCES komertzialak(kodea) ON DELETE CASCADE)");

        // 2. Kopiatu datu guztiak taula zaharretik berrira
        db.execSQL("INSERT INTO bazkideak_new SELECT id, nan, izena, ... FROM bazkideak");

        // 3. Ezabatu taula zaharra eta indize zaharrak
        db.execSQL("DROP INDEX IF EXISTS index_bazkideak_nan");
        db.execSQL("DROP TABLE bazkideak");

        // 4. Aldatu berriaren izena bazkideak izatera
        db.execSQL("ALTER TABLE bazkideak_new RENAME TO bazkideak");

        // 5. Sortu indize berriak
        db.execSQL("CREATE INDEX index_bazkideak_nan ON bazkideak(nan)");
        db.execSQL("CREATE INDEX index_bazkideak_komertzialKodea ON bazkideak(komertzialKodea)");
    }
};
```

### fallbackToDestructiveMigration Estrategia

```java
.fallbackToDestructiveMigration()
```

**Erabilera**: Eskema aldaketa handi bat gertatzen denean (adib. Foreign Key gehitzea taula zaharretan), datu-base zaharra ezabatu eta berria sortzen da. **OHARRA**: Produkzioan datu garrantzitsuak badaude, migrazio espezifikoak idatzi behar dira.

**Migrazio Garrantzitsuenak (Kronologikoa):**

| Bertsioa | Migrazioa | Deskribapena |
|----------|-----------|--------------|
| **2 → 3** | `MIGRAZIO_2_3` | `agenda_bisitak` taula sortu (Agenda modulua) |
| **4 → 5** | `MIGRAZIO_4_5` | `bazkideak` taula sortu (`bazkideak.xml` egitura) |
| **5 → 6** | `MIGRAZIO_5_6` | `katalogoa` taulan `irudia_izena` eremua gehitu |
| **6 → 7** | `MIGRAZIO_6_7` | `eskaera_goiburuak` eta `eskaera_xehetasunak` taulak berriz sortu (indizeekin) |
| **7 → 8** | `MIGRAZIO_7_8` | `eskaera_goiburuak` taulan `bazkideaId` eta `komertzialId` eremuak gehitu |
| **8 → 9** | `MIGRAZIO_8_9` | `agenda_bisitak` taulan `bazkideaId` eta `komertzialaId` eremuak gehitu |
| **9 → 10** | `MIGRAZIO_9_10` | `komertzialak` taulan `abizena`, `posta`, `jaiotzeData`, `argazkia` eremuak gehitu |
| **12 → 13** | `MIGRAZIO_12_13` | `bazkideak` taulan `kodea`, `helbidea`, `probintzia`, `komertzialKodea`, `sortutakoData` eremuak gehitu |
| **13 → 14** | `MIGRAZIO_13_14` | `bazkideak` taula Foreign Key-ekin berriz sortu (table swap estrategia) |

**Migrazio Beste Praktikak:**
- **Idempotentzia**: Migrazio bakoitza hainbat aldiz exekuta daiteke emaitza bera lortuz (`IF NOT EXISTS`, `IF EXISTS` erabiliz).
- **Datuen Kontserbazioa**: Datuak kopiatzen dira taula zaharretik berrira, datu-galerarik gabe.
- **Indizeak**: Indize berriak sortzen dira kontsultak azkartzeko.

---

## ⚙️ Funtzionalitate Kritikoak

### Erosketa Sistema: Balidazio Prozesua eta Eskaeren Integritatea

Aplikazioak **eskaeren balidazio prozesu zorrotza** inplementatzen du, datuen integritatea bermatzeko. Prozesua bi faseetan banatzen da:

#### 1. EskaeraBalidatzailea: Datuak Balidatu

**Helburua**: Eskaera bat datu-basean gordetzeko aurretik, derrigorrezko eremu guztiak beteta daudela eta datu-basean existitzen direla egiaztatu.

**Balidazio Prozesua:**
```java
public void balidatuEskaera(EskaeraGoiburua eskaera) throws IllegalArgumentException {
    // 1. Komertzial kodea balidatu - DERRIORREZKO EREMUA
    String komertzialKodea = eskaera.getKomertzialKodea();
    if (komertzialKodea == null || komertzialKodea.trim().isEmpty()) {
        throw new IllegalArgumentException("Komertzialaren kodea falta da");
    }
    
    // 2. Komertzial kodea datu-basean existitzen dela egiaztatu
    Komertziala komertziala = datuBasea.komertzialaDao().kodeaBilatu(komertzialKodea);
    if (komertziala == null) {
        throw new IllegalArgumentException("Komertzial kodea ez da existitzen: " + komertzialKodea);
    }
    
    // 3. Komertzial IDa ezarri balidazioa gainditu bada
    eskaera.setKomertzialId(komertziala.getId());
    
    // 4. Bazkidea kodea balidatu - DERRIORREZKO EREMUA
    String bazkideaKodea = eskaera.getBazkideaKodea();
    if (bazkideaKodea == null || bazkideaKodea.trim().isEmpty()) {
        throw new IllegalArgumentException("Bazkidearen kodea falta da");
    }
    
    // 5. Bazkidea kodea datu-basean existitzen dela egiaztatu (NAN edo kodea erabiliz)
    Bazkidea bazkidea = datuBasea.bazkideaDao().nanBilatu(bazkideaKodea);
    if (bazkidea == null) {
        bazkidea = datuBasea.bazkideaDao().kodeaBilatu(bazkideaKodea);
        if (bazkidea == null) {
            throw new IllegalArgumentException("Bazkidea kodea ez da existitzen: " + bazkideaKodea);
        }
    }
    
    // 6. Bazkidea IDa ezarri balidazioa gainditu bada
    eskaera.setBazkideaId(bazkidea.getId());
}
```

#### 2. Room Transakzio Seguruak: Eskaera Gorde

**Helburua**: Eskaera goiburua eta xehetasunak transakzio bakar batean gorde, datuen osotasuna bermatzeko.

**Inplementazioa (MainActivity.gordeEskaera()):**
```java
db.runInTransaction(() -> {
    // 1. Eskaera goiburua txertatu
    EskaeraGoiburua goi = new EskaeraGoiburua();
    goi.setZenbakia(zenbakia);
    goi.setData(data);
    goi.setKomertzialKodea(komertzialKodea);
    goi.setKomertzialId(kom.getId());
    db.eskaeraGoiburuaDao().txertatu(goi);
    
    // 2. Eskaera xehetasunak txertatu (saskiko produktu bakoitzarentzat)
    for (SaskiaElementua e : saskia) {
        EskaeraXehetasuna x = new EskaeraXehetasuna();
        x.setEskaeraZenbakia(zenbakia);
        x.setArtikuluKodea(e.artikuluKodea);
        x.setKantitatea(e.kopurua);
        x.setPrezioa(e.salmentaPrezioa);
        db.eskaeraXehetasunaDao().txertatu(x);
    }
    
    // 3. Stock eguneratu (produktu bakoitzaren stock-a murriztu)
    for (SaskiaElementua e : saskia) {
        Katalogoa k = db.katalogoaDao().artikuluaBilatu(e.artikuluKodea);
        if (k != null) {
            int stockBerria = Math.max(0, k.getStock() - e.kopurua);
            db.katalogoaDao().stockaEguneratu(e.artikuluKodea, stockBerria);
        }
    }
});
```

**Abantailak:**
- **Datuen Osotasuna**: Transakzio bakar batean exekutatzen da; errore bat gertatzen bada, guztia atzera egingo da.
- **Stock Sinkronizatua**: Stock eguneratu egiten da eskaera egiten denean, inbentarioa zehatza mantenduz.
- **Balidazio Zorrotza**: `EskaeraBalidatzailea` erabiliz, datu guztiak baliozkoak direla egiaztatzen da gordetzeko aurretik.

### Google Maps: Bazkideen Geolokalizazioa eta Markatzaileen Kudeaketa

Aplikazioak **Google Maps API** erabiltzen du bazkideen geolokalizazioa erakusteko eta kontaktua errazteko.

#### Inplementazioa

**1. MapFragment Konfigurazioa (LoginActivity eta MainActivity):**
```java
@Override
public void onMapReady(@NonNull GoogleMap googleMap) {
    // Donostia zentratu (Gipuzkoa egoitza)
    LatLng donostia = new LatLng(DONOSTIA_LAT, DONOSTIA_LNG);
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(donostia, 14f));
    googleMap.getUiSettings().setZoomControlsEnabled(true);
    
    // Markatzailea (marker) Gipuzkoa egoitzan
    googleMap.addMarker(new MarkerOptions()
            .position(donostia)
            .title(getString(R.string.contact_title)));
}
```

**2. Map Intent (Kontaktua Irekitzeko):**
```java
private void openMap() {
    Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + MAP_QUERY);
    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
    mapIntent.setPackage("com.google.android.apps.maps");
    if (mapIntent.resolveActivity(getPackageManager()) != null) {
        startActivity(mapIntent);
    } else {
        // Fallback: Web bidezko mapa
        Uri fallback = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + MAP_QUERY);
        startActivity(new Intent(Intent.ACTION_VIEW, fallback));
    }
}
```

**Konfigurazioa:**
- **Google Maps API gakoa**: `local.properties` fitxategian (`MAPS_API_KEY`)
- **Play Services Maps**: 18.2.0 bertsioa
- **MapFragment**: `SupportMapFragment` erabiliz XML layout-ean

### Komunikazio Lasterbideak: Deiak eta Posta Elektroniko Bidezko Esportazioak

Aplikazioak **Android Intent** sistema erabiltzen du komunikazio lasterbideak irekitzeko eta fitxategiak bidaltzeko.

#### 1. Deiak (ACTION_DIAL)

**Helburua**: Telefono dialerra ireki zenbaki horrekin (deia egiteko).

**Inplementazioa:**
```java
private void openPhone() {
    Intent callIntent = new Intent(Intent.ACTION_DIAL);
    callIntent.setData(Uri.parse("tel:" + PHONE_NUMBER));
    try {
        startActivity(callIntent);
    } catch (Exception e) {
        Toast.makeText(this, R.string.deia_errorea, Toast.LENGTH_SHORT).show();
    }
}
```

**OHARRA**: `ACTION_DIAL` erabiltzen da (ez `ACTION_CALL`), erabiltzaileak deia egitea erabaki dezan.

#### 2. Posta Elektronikoa (ACTION_SENDTO eta ACTION_SEND_MULTIPLE)

**Helburua**: Fitxategiak Gmail (edo beste posta-app) bidez bidaltzeko, eranskin gisa.

**Inplementazioa (AgendaModuluaActivity.esportatuEtaBidali()):**
```java
private void esportatuEtaBidali() {
    // 1. Fitxategiak esportatu (XML, TXT, CSV)
    AgendaEsportatzailea esportatzailea = new AgendaEsportatzailea(this);
    esportatzailea.agendaXMLSortu();
    esportatzailea.agendaTXTSortu();
    
    // 2. FileProvider erabiliz URI-ak lortu
    ArrayList<Uri> uriak = new ArrayList<>();
    String pakeIzena = getPackageName();
    if (xmlFitx.exists() && xmlFitx.length() > 0) {
        uriak.add(FileProvider.getUriForFile(this, pakeIzena + ".fileprovider", xmlFitx));
    }
    if (txtFitx.exists() && txtFitx.length() > 0) {
        uriak.add(FileProvider.getUriForFile(this, pakeIzena + ".fileprovider", txtFitx));
    }
    
    // 3. Intent sortu (ACTION_SEND_MULTIPLE)
    Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
    intent.setType("*/*");
    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriak);
    intent.putExtra(Intent.EXTRA_SUBJECT, gaia);
    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{HELBIDE_POSTA});
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    
    // 4. Gmail Intent (lehenetsia)
    Intent gmail = new Intent(intent).setPackage("com.google.android.gm");
    if (gmail.resolveActivity(getPackageManager()) != null) {
        startActivity(gmail);
    } else {
        startActivity(Intent.createChooser(intent, getString(R.string.postaz_hautatu)));
    }
}
```

**FileProvider Konfigurazioa:**
- `res/xml/file_paths.xml`: Barne-memoria (`filesDir`) sarbidea
- `AndroidManifest.xml`: FileProvider erregistratu

**Abantailak:**
- **Segurtasuna**: FileProvider erabiliz, fitxategiak modu seguruan partekatzen dira.
- **Flexibilitatea**: Gmail lehenetsia, baina beste posta-app batzuk ere erabil daitezke.
- **Fitxategi Anitzak**: `ACTION_SEND_MULTIPLE` erabiliz, hainbat fitxategi bidal daitezke aldi berean.

### XML bidezko Sinkronizazioa eta "Upsert" Logika

Aplikazioak XML fitxategiak inportatzeko aukera ematen du (`komertzialak.xml`, `bazkideak.xml`, `katalogoa.xml`, `agenda.xml`, `loginak.xml`).

**Upsert Logika:**
- **Existitzen bada**: Eguneratu (`OnConflictStrategy.REPLACE`)
- **Existitzen ez bada**: Txertatu

**Inplementazioa:**
```java
// Bazkidea existitzen den egiaztatu
Bazkidea existitzenDa = bazkideaDao.nanBilatu(bazkidea.getNan().trim());

if (existitzenDa != null) {
    // Eguneratu: ID mantendu eta XML-etik datozen eremuak bakarrik eguneratu
    bazkidea.setId(existitzenDa.getId());
    bazkideaId = bazkideaDao.txertatu(bazkidea); // REPLACE estrategia
    // Ezabatu bazkidearen eskaera zaharrak
    eskaeraDao.ezabatuBazkidearenEskaerak(bazkideaId);
} else {
    // Bazkidea berria txertatu
    bazkideaId = bazkideaDao.txertatu(bazkidea);
}
```

**Transakzio Seguruak:**
```java
int emaitzaKopurua = db.runInTransaction(() -> {
    // Datu guztiak transakzio bakar batean gorde
    // Errore bat gertatzen bada, guztia atzera egingo da
});
```

### Segurtasun Iragazkia

**Helburua**: Komertzial bakoitzak bere datuak bakarrik ikusteko gaitasuna.

**Inplementazioa:**
1. **SessionManager**: Saioa hasi duen komertzialaren kodea gordetzen du
2. **DAO kontsultak**: `komertzialKodea` parametroa beti gehitzen da `WHERE` klausulan
3. **Repository geruza**: `SessionManager` erabiliz uneko komertzialaren kodea lortzen du

**Adibidea (AgendaRepository):**
```java
public void bilatuBezeroaz(@NonNull String filter, @NonNull KargatuCallback callback) {
    executorService.execute(() -> {
        // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
        SessionManager sessionManager = new SessionManager(context);
        String komertzialKodea = sessionManager.getKomertzialKodea();
        
        if (komertzialKodea == null || komertzialKodea.isEmpty()) {
            callback.onEmaitza(new ArrayList<>());
            return;
        }
        
        // SEGURTASUNA: bakarrik uneko komertzialaren bisitak
        List<Agenda> bisitak = agendaDao.bilatuBezeroaz(filter.trim(), komertzialKodea);
        callback.onEmaitza(bisitak);
    });
}
```

**DAO kontsulta (AgendaDao):**
```java
@Query("SELECT * FROM agenda_bisitak WHERE " +
       "(bazkideaKodea LIKE '%' || :filter || '%' OR " +
       "deskribapena LIKE '%' || :filter || '%') AND " +
       "komertzialKodea = :komertzialKodea " +
       "ORDER BY bisitaData DESC")
List<Agenda> bilatuBezeroaz(String filter, String komertzialKodea);
```

### Eskaerak Egin Aurreko Datuen Balidazio Zorrotza

Eskaera bat sortu aurretik, datuen balidazio zorrotza egiten da:

**UI Mailako Balidazioa (BisitaFormularioActivity):**
```java
private boolean baliozkotuFormularioa() {
    // Hutsen kontrolak
    if (data.isEmpty() || deskribapena.isEmpty() || bazkideaEzHautatua) {
        // Erabiltzaileari errore mezuak erakutsi
        return false;
    }
    
    // Formatuen egiaztapena
    if (!dataFormatuaZuzena(data)) {
        // Data YYYY-MM-DD formatuan izan behar du
        return false;
    }
    
    if (!ordua.isEmpty() && !ordua.matches("\\d{2}:\\d{2}")) {
        // Ordua HH:mm formatuan izan behar du
        return false;
    }
    
    return true;
}
```

**Datu-base Mailako Balidazioa:**
```java
// Kanpo-gakoen egiaztapena: bazkidea_kodea Bazkidea taulan existitzen dela ziurtatu
if (datuBasea.bazkideaDao().nanBilatu(bazkideaKodeaFinal) == null) {
    runOnUiThread(() -> Toast.makeText(this, "Bazkidea ez da existitzen", Toast.LENGTH_LONG).show());
    return;
}

// SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
SessionManager sessionManager = new SessionManager(this);
String komertzialKodeaSegurua = sessionManager.getKomertzialKodea();

// Egiaztatu komertzial kodea bat datorrela
if (!komertzialKodeaSegurua.equals(komertzialKodeaFinal)) {
    runOnUiThread(() -> Toast.makeText(this, "Errorea: komertzial kodea ez dator bat", Toast.LENGTH_LONG).show());
    return;
}
```

**Transakzio Seguruak:**
```java
datuBasea.runInTransaction(() -> {
    // Datu guztiak transakzio bakar batean gorde
    // Errore bat gertatzen bada, guztia atzera egingo da
});
```

---

## 🛠️ Teknologia-Stacka

- **Hizkuntza**: Java 11
- **Android SDK**: MinSdk 24, TargetSdk 36, CompileSdk 36
- **Datu-basea**: Room Persistence Library 2.6.1
- **UI**: Material Design Components, ViewBinding
- **Mapak**: Google Maps API (Play Services Maps 18.2.0)
- **Arkitektura**: MVVM + Repository Pattern
- **Thread Management**: ExecutorService (4 thread pool)

---

## 📦 Instalazioa eta Konfigurazioa

### Beharrezkoak

1. **Android Studio** (Arctic Fox edo berriagoa)
- **JDK 11** edo berriagoa
- **Google Maps API gakoa** (`local.properties` fitxategian)

### Konfigurazioa

1. `local.properties` fitxategia sortu proiektuaren erroan:
```properties
MAPS_API_KEY=zure_google_maps_api_gakoa
```

2. XML fitxategiak `app/src/main/assets/` karpetan jarri:
   - `komertzialak.xml`
   - `bazkideak.xml`
   - `katalogoa.xml`
   - `agenda.xml`
   - `loginak.xml`

3. Proiektua eraiki:
```bash
./gradlew build
```

---

## 📝 Kode Estiloa

Proiektuak **euskara hutsa** erabiltzen du kodea idazteko:
- Klaseak, metodoak, aldagaiak: euskaraz
- Iruzkinak: teknikoak eta laburrak, beti euskaraz
- Nomenklatura: `camelCase` (adib. `lortuGuztiak()`, `unekoKomertziala`)

**Adibidea:**
```java
/**
 * Bisita bat txertatu (upsert: existitzen bada eguneratu, bestela sortu).
 * @param agenda Txertatu behar den bisita
 * @param callback Emaitza jaso behar duen callback
 */
public void txertatuBisita(@NonNull Agenda agenda, @Nullable KargatuCallback callback) {
    executorService.execute(() -> {
        try {
            long id = agendaDao.txertatu(agenda);
            if (callback != null) {
                callback.onEmaitza(id > 0 ? Collections.singletonList(agenda) : new ArrayList<>());
            }
        } catch (Exception e) {
            Log.e(ETIKETA, "Errorea bisita txertatzean", e);
            if (callback != null) callback.onEmaitza(null);
        }
    });
}
```

---

## 🔒 Segurtasuna eta Iragazkiak

### SessionManager: Saio-Kudeaketa Segurua

**Helburua**: Saioa hasi duen komertzialaren kodea modu seguruan gordetzen du, SharedPreferences erabiliz.

**Inplementazioa:**
```java
public class SessionManager {
    private static final String PREF_IZENA = "AppKomertziala_Session";
    private static final String GAKOA_KOMMERTZIALA_KODEA = "komertzial_kodea";
    
    public void saioaHasi(String komertzialKodea, String komertzialIzena) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(GAKOA_KOMMERTZIALA_KODEA, komertzialKodea.trim());
        editor.apply();
    }
    
    public String getKomertzialKodea() {
        return sharedPreferences.getString(GAKOA_KOMMERTZIALA_KODEA, null);
    }
}
```

**SEGURTASUNA**: Kodea bakarrik SharedPreferences-en gordetzen da, ez da inoiz Intent-etan edo beste lekuetan erabiltzen behar kodea zuzenean. SessionManager bidez bakarrik.

### Segurtasun Iragazkia: komertzialKodea WHERE Klausulak

**Helburua**: Komertzial bakoitzak bere datuak bakarrik ikusteko gaitasuna. Datuak datu-basean sartu aurretik, `komertzialKodea` parametroa beti gehitzen da `WHERE` klausulan.

#### Inplementazioa Hiru Geruzatan

**1. Repository Geruza (AgendaRepository):**
```java
public void bilatuBezeroaz(@NonNull String filter, @NonNull KargatuCallback callback) {
    executorService.execute(() -> {
        // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
        SessionManager sessionManager = new SessionManager(context);
        String komertzialKodea = sessionManager.getKomertzialKodea();
        
        if (komertzialKodea == null || komertzialKodea.isEmpty()) {
            callback.onEmaitza(new ArrayList<>());
            return;
        }
        
        // SEGURTASUNA: bakarrik uneko komertzialaren bisitak
        List<Agenda> bisitak = agendaDao.bilatuBezeroaz(filter.trim(), komertzialKodea);
        callback.onEmaitza(bisitak);
    });
}
```

**2. DAO Geruza (AgendaDao):**
```java
@Query("SELECT * FROM agenda_bisitak WHERE " +
       "(bazkideaKodea LIKE '%' || :filter || '%' OR " +
       "deskribapena LIKE '%' || :filter || '%') AND " +
       "komertzialKodea = :komertzialKodea " +  // SEGURTASUNA: WHERE klausula
       "ORDER BY bisitaData DESC")
List<Agenda> bilatuBezeroaz(String filter, String komertzialKodea);
```

**3. Activity Geruza (EskaerakActivity):**
```java
private void kargatuEskaerak() {
    new Thread(() -> {
        // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
        SessionManager sessionManager = new SessionManager(this);
        String komertzialKodea = sessionManager.getKomertzialKodea();
        
        if (komertzialKodea == null || komertzialKodea.isEmpty()) {
            // Saioa ez dago hasita
            return;
        }
        
        // SEGURTASUNA: bakarrik uneko komertzialaren eskaerak
        List<EskaeraGoiburua> goiburuak = 
            datuBasea.eskaeraGoiburuaDao().komertzialarenEskaerak(komertzialKodea.trim());
    }).start();
}
```

#### Datu-Ihesak Ekiditeko Mekanismoak

1. **Query Guztietan WHERE Klausula**: Datu-baseko query guztiek `komertzialKodea = :komertzialKodea` klausula dute.
2. **SessionManager Validazioa**: Kodea uneko saioaren kodea dela egiaztatzen da (`kodeaBalidatu()` metodoa).
3. **Repository Abstrakzioa**: UI-ak ez du zuzenean DAO-ak erabiltzen; Repository-ek segurtasun iragazkia aplikatzen du.
4. **Transakzio Seguruak**: Datuak gordetzeko aurretik, komertzial kodea balidatzen da (`EskaeraBalidatzailea`).

**Adibidea (AgendaDao - Query Guztiak):**
```java
// SEGURTASUNA: Komertzial baten bisitak bakarrik
@Query("SELECT * FROM agenda_bisitak WHERE komertzialKodea = :komertzialKodea ORDER BY bisitaData DESC")
List<Agenda> getVisitsByKomertzial(String komertzialKodea);

// SEGURTASUNA: Bilaketa orokorra, uneko komertzialaren bisitak bakarrik
@Query("SELECT DISTINCT a.* FROM agenda_bisitak a " +
       "WHERE a.komertzialKodea = :komertzialKodea AND (" +
       "a.bisitaData LIKE '%' || :filter || '%' OR ...) " +
       "ORDER BY a.bisitaData DESC")
List<Agenda> bilatuOrokorra(String filter, String komertzialKodea);

// SEGURTASUNA: Bisita bat ezabatu, bakarrik uneko komertzialarena bada
@Query("DELETE FROM agenda_bisitak WHERE id = :id AND komertzialKodea = :komertzialKodea")
int ezabatuSegurua(long id, String komertzialKodea);
```

### Balidazio Zorrotza

- **EskaeraBalidatzailea**: Datuak datu-basean sartu aurretik baliozkotzen dira (komertzialKodea, bazkideaKodea).
- **UI Mailako Balidazioa**: Formularioetan hutsen kontrolak eta formatuen egiaztapena.
- **Transakzio Seguruak**: Datuen osotasuna bermatzeko, transakzio bakar batean exekutatzen dira eragiketak.

---

## 📄 Lizentzia

Proiektu hau pribatua da eta jabetza intelektualaren babesa du.

---

## 👥 Garatzaileak

**Techno Basque** - Android garapena eta kode garbia.

---

## 📁 Fitxategien Gida Teknikoa

Proiektuko Java fitxategi guztien deskribapen tekniko eta funtzionala. Fitxategi bakoitzak helburu argi bat du eta proiektuaren osotasunean bere erantzukizuna betetzen du. Karpeta bakoitzaren papera eta fitxategi nagusien arteko uztartzea azaltzen da.

### db.kontsultak: Datuen Kudeaketa

| Fitxategia | Papera | Nola Uztartzen Da |
|------------|--------|-------------------|
| **AppDatabase** | Room datu-basearen instantzia bakarra (singleton) | `getInstance()` deitzean, migrazioak automatikoki exekutatzen dira. DAO interfazeak eskuratzeko erabiltzen da: `db.agendaDao()`, `db.bazkideaDao()`, etab. |
| **AgendaDao** | Agenda (bisitak) taularen kontsultak | **SEGURTASUNA**: Query guztiek `komertzialKodea = :komertzialKodea` klausula dute. `AgendaRepository`-ek erabiltzen du, UI-ak zuzenean ez du erabiltzen. |
| **BazkideaDao** | Bazkideak taularen kontsultak | `XMLKudeatzailea.bazkideakInportatu()` erabiltzen du upsert logikarekin. `BazkideaFormularioActivity`-k erabiltzen du bazkideak kudeatzeko. |
| **EskaeraGoiburuaDao** | Eskaera goiburuak taularen kontsultak | `EskaerakActivity`-k erabiltzen du uneko komertzialaren eskaerak kargatzeko. `MainActivity.gordeEskaera()` erabiltzen du eskaera berriak gordetzeko. |
| **EskaeraXehetasunaDao** | Eskaera xehetasunak taularen kontsultak | `MainActivity.gordeEskaera()` erabiltzen du saskiko produktuak eskaera xehetasun gisa gordetzeko transakzioan. |
| **KatalogoaDao** | Katalogoa taularen kontsultak | `XMLKudeatzailea.katalogoaInportatu()` erabiltzen du wipe-and-load estrategiarekin (asteko eguneraketa). `MainActivity`-k erabiltzen du produktuen zerrenda kargatzeko eta stock eguneratzeko. |
| **KomertzialaDao** | Komertzialak taularen kontsultak | `LoginActivity`-k erabiltzen du komertzial hautatzeko. `EskaeraBalidatzailea` erabiltzen du komertzial kodea balidatzeko. |
| **LoginaDao** | Loginak taularen kontsultak | `LoginActivity.attemptLogin()` erabiltzen du erabiltzaile/pasahitza balidatzeko. `XMLKudeatzailea.loginakInportatu()` erabiltzen du loginak inportatzeko. |
| **HistorialCompraDao** | HistorialCompra taularen kontsultak | `HistorialCompraActivity`-k erabiltzen du erosketa historial guztiak kargatzeko. Bidalketa XML fitxategiak inportatzean erabiltzen da. |
| **AgendaRepository** | Room eta UI-aren arteko geruza abstraktua | `AgendaModuluaActivity`-k erabiltzen du bisitak kargatzeko, bilatzeko eta gordetzeko. `SessionManager` erabiliz segurtasun iragazkia aplikatzen du. `ExecutorService` erabiliz hari nagusitik kanpo exekutatzen da. |

### segurtasuna: Segurtasun Mekanismoak

| Fitxategia | Papera | Nola Uztartzen Da |
|------------|--------|-------------------|
| **SessionManager** | Saioa hasi duen komertzialaren kodea modu seguruan gordetzen du | `LoginActivity`-k erabiltzen du saioa hasi ondoren (`saioaHasi()`). `AgendaRepository` eta `EskaerakActivity` erabiltzen dute uneko komertzialaren kodea lortzeko (`getKomertzialKodea()`). Query guztietan `WHERE komertzialKodea = :komertzialKodea` klausula aplikatzeko oinarria. |
| **EskaeraBalidatzailea** | Eskaerak datu-basean gordetzeko aurretik datuen integritatea egiaztatzen du | `MainActivity.gordeEskaera()` erabiltzen du eskaera balidatzeko aurretik. `balidatuEskaera()` metodoa: komertzialKodea eta bazkideaKodea datu-basean existitzen direla egiaztatzen du. `balidatuEtaGorde()` metodoa: balidatu eta gorde transakzio seguru batean. |

### xml: Datuen Sinkronizazioa eta Txostenak

| Fitxategia | Papera | Nola Uztartzen Da |
|------------|--------|-------------------|
| **XMLKudeatzailea** | XML fitxategiak inportatzeko kudeatzailea | `LoginActivity`-k erabiltzen du XML fitxategiak inportatzeko (`guztiakInportatu()`, `inportatuFitxategia()`). `XmlPullParser` erabiliz XML parseatu. Upsert logika: existitzen bada eguneratu, bestela sortu (`OnConflictStrategy.REPLACE`). Transakzio seguruak: datu guztiak transakzio bakar batean gorde. |
| **XMLEsportatzailea** | Room datu-baseko datuak XML fitxategietara esportatu | `DatuKudeatzailea`-k erabiltzen du esportazioak koordinatzeko (`bazkideBerriakEsportatu()`, `eskaeraBerriakEsportatu()`). `XmlSerializer` erabiliz XML sortu. Barne-memorian idazten du (`Context.openFileOutput()`). |
| **DatuKudeatzailea** | Esportazio eta inportazio logika koordinatzen du | `MainActivity`-k erabiltzen du esportazioak eta inportazioak koordinatzeko. Eguneroko txostena: bazkide berriak eta eskaera berriak. Asteko inportazioa: katalogoa. Hileroko laburpena: agenda. |
| **AgendaEsportatzailea** | Agenda bi formatutan esportatu | `AgendaModuluaActivity`-k erabiltzen du agenda esportatzeko (`esportatuEtaBidali()`). **SEGURTASUNA**: uneko komertzialaren bisitak bakarrik esportatzen dira. XML formatua (ofiziala) eta TXT formatua (iraurgarria). |
| **InbentarioKudeatzailea** | Katalogoa astero inportatzeko kudeatzailea | `MainActivity`-k erabiltzen du katalogoa inportatzeko. Barne-memoriatik edo assets-etik katalogoa inportatu. Wipe-and-load estrategia: aurreko katalogoa ezabatu, XMLko produktuak bakarrik txertatu. |
| **XmlBilatzailea** | Assets-en dauden XML fitxategiak bilatzen ditu | `LoginActivity`-k erabiltzen du falta diren XML fitxategiak zehazteko. `MainActivity`-k erabiltzen du XML falta diren mezuak erakusteko. |

### UI (Activities/Adapters): Erabiltzaile Interfazea

| Fitxategia | Papera | Nola Uztartzen Da |
|------------|--------|-------------------|
| **LoginActivity** | Saioa hasiera eta segurtasun geruza | Pantaila nagusia aplikazioa irekitzean. XML fitxategiak inportatzeko (`XMLKudeatzailea`). Komertzial hautaketa (`KomertzialaDao`). Erabiltzaile/pasahitza balidazioa (`LoginaDao`). Google Maps integrazioa (Donostia zentratua). `SessionManager.saioaHasi()` deitzean saioa hasi. |
| **MainActivity** | Pantaila nagusia (BottomNavigationView) | Tab-ak konfiguratu (Hasiera, Agenda, Bazkideak, Inbentarioa). Google Maps + kontaktua (Map, Call, Email intents). Saskia kudeaketa (`SaskiaAdapter`). Eskaera gordetzea (`EskaeraBalidatzailea`, `EskaeraGoiburuaDao`, `EskaeraXehetasunaDao`, `KatalogoaDao.stockaEguneratu()`). Esportazioak (`DatuKudeatzailea`, `AgendaEsportatzailea`). |
| **AgendaModuluaActivity** | Agenda moduluaren pantaila nagusia | `AgendaRepository` erabiliz bisitak kargatzeko, bilatzeko eta gordetzeko. Bilaketa funtzioa (data, bezeroa, deskribapena). Esportatu eta bidali (`AgendaEsportatzailea`, Gmail Intent). `BisitaFormularioActivity` ireki bisita berria/editatu. |
| **BisitaFormularioActivity** | Bisita berria/editatu formularioa | Datuak balidatu (`baliozkotuFormularioa()`). Bisita gorde transakzio seguru batean (`AgendaDao`, `SessionManager`). MaterialDatePicker eta MaterialTimePicker erabiliz data eta ordua hautatu. |
| **EskaerakActivity** | Eskaeren zerrenda pantaila | **SEGURTASUNA**: `SessionManager` erabiliz uneko komertzialaren eskaerak bakarrik (`EskaeraGoiburuaDao.komertzialarenEskaerak()`). `EskaerakAdapter` erabiliz zerrenda erakutsi. |
| **BazkideaFormularioActivity** | Bazkide berria/editatu formularioa | Bazkidea gorde datu-basean (`BazkideaDao`, upsert logika). Datuak gorde aurretik baieztapena. |
| **ZitaGehituActivity** | Zita berria (EskaeraGoiburua) gehitzeko | Zita gorde datu-basean (`EskaeraGoiburuaDao`). MaterialDatePicker eta MaterialTimePicker erabiliz. |
| **ProduktuDetalaActivity** | Produktu baten informazioa erakusten du | Produktuaren datuak erakutsi (`KatalogoaDao.artikuluaBilatu()`). Produktua saskira gehitu (RESULT_OK bidali `MainActivity`-ra). |
| **HistorialCompraActivity** | Erosketen historiala pantaila | Historial guztiak kargatu (`HistorialCompraDao.guztiak()`). `HistorialCompraAdapter` erabiliz zerrenda erakutsi. |
| **KatalogoaAdapter** | Katalogoa (inbentarioa) zerrenda erakusteko | `MainActivity`-k erabiltzen du produktuen zerrenda erakusteko. `OnErosiClickListener`: produktua saskira gehitu. `OnItemClickListener`: produktuaren detale orria ireki. |
| **SaskiaAdapter** | Erosketa saskiko zerrenda erakusteko | `MainActivity`-k erabiltzen du saskia erakusteko. Kopurua aldatu (+/-), elementu bat kendu. Badge eta guztira eguneratzeko callback. |
| **AgendaBisitaAdapter** | Agenda bisiten zerrenda erakusteko | `AgendaModuluaActivity`-k erabiltzen du bisiten zerrenda erakusteko. `OnBisitaEkintzaListener`: Ikusi, Editatu, Ezabatu botoiak. |
| **EskaerakAdapter** | Eskaeren zerrenda erakusteko | `EskaerakActivity`-k erabiltzen du eskaeren zerrenda erakusteko. |
| **HistorialCompraAdapter** | Erosketa historial zerrenda erakusteko | `HistorialCompraActivity`-k erabiltzen du historial zerrenda erakusteko. |

## 📁 Fitxategien Deskribapena

Proiektuko Java fitxategi guztien deskribapen tekniko eta funtzionala. Fitxategi bakoitzak helburu argi bat du eta proiektuaren osotasunean bere erantzukizuna betetzen du.

### Activities (Pantailak)

| Fitxategia | Helburua | Funtzio Garrantzitsuenak |
|------------|----------|--------------------------|
| **LoginActivity** | Saioa hasiera eta segurtasun geruza | `attemptLogin()`: Erabiltzaile/pasahitza balidazioa<br>`sartuKomertzialGisa()`: Komertzial hautaketa<br>`kargatuXmlGuztiak()`: XML fitxategiak inportatu<br>`onMapReady()`: Google Maps Donostian zentratu |
| **MainActivity** | Pantaila nagusia (BottomNavigationView) | `onCreate()`: Tab-ak konfiguratu (Hasiera, Agenda, Bazkideak, Inbentarioa)<br>`kargatuKatalogoa()`: Produktuen zerrenda kargatu<br>`kargatuBazkideak()`: Bazkideen zerrenda kargatu<br>`gordeEskaera()`: Saskiko produktuak eskaera gisa gorde<br>`onMapReady()`: Google Maps + kontaktua (Map, Call, Email intents) |
| **AgendaModuluaActivity** | Agenda moduluaren pantaila nagusia | `kargatuZerrenda()`: Bisiten zerrenda kargatu (Repository erabiliz)<br>`konfiguratuBilaketa()`: Bilaketa funtzioa (data, bezeroa, deskribapena)<br>`esportatuEtaBidali()`: XML/TXT esportatu eta Gmail bidez bidali<br>`irekiFormularioa()`: Bisita berria/editatu formularioa ireki |
| **BisitaFormularioActivity** | Bisita berria/editatu formularioa | `baliozkotuFormularioa()`: Datuen balidazioa (hutsen kontrolak, formatuak)<br>`gordeBisita()`: Bisita gorde transakzio seguru batean<br>`erakutsiDataHautatzailea()`: MaterialDatePicker erabiliz data hautatu<br>`erakutsiOrduaHautatzailea()`: MaterialTimePicker erabiliz ordua hautatu |
| **EskaerakActivity** | Eskaeren zerrenda pantaila | `kargatuEskaerak()`: **SEGURTASUNA** - bakarrik uneko komertzialaren eskaerak<br>`adapter.eguneratuZerrenda()`: Eskaeren zerrenda RecyclerView-n erakutsi |
| **BazkideaFormularioActivity** | Bazkide berria/editatu formularioa | `erakutsiGordeBaieztapena()`: Datuak gorde aurretik baieztapena<br>`erakutsiEzabatuBaieztapena()`: Bazkidea ezabatu aurretik baieztapena<br>`gordeBazkidea()`: Bazkidea gorde datu-basean (upsert logika) |
| **ZitaGehituActivity** | Zita berria (EskaeraGoiburua) gehitzeko | `gordeCita()`: Zita gorde datu-basean<br>`erakutsiDataHautatzailea()`: Data hautatzailea<br>`erakutsiOrduaHautatzailea()`: Ordua hautatzailea |
| **ProduktuDetalaActivity** | Produktu baten informazioa erakusten du | `beteEdukia()`: Produktuaren datuak erakutsi (irudia, izena, prezioa, stock)<br>`erosiSaskira()`: Produktua saskira gehitu (RESULT_OK bidali) |
| **HistorialCompraActivity** | Erosketen historiala pantaila | `kargatuHistoriala()`: Historial guztiak kargatu datu-basean<br>`adapter.eguneratuZerrenda()`: Historial zerrenda RecyclerView-n erakutsi |

### Room Entities (Datu-base Entitateak)

| Fitxategia | Helburua | Eremu Garrantzitsuenak |
|------------|-------|------------------------|
| **Komertziala** | Komertzialen datu-egitura | `id` (PK, auto), `kodea` (unique), `izena`, `abizena`, `posta`, `jaiotzeData`, `argazkia` |
| **Bazkidea** | Bazkideen datu-egitura | `id` (PK, auto), `nan` (indizea), `izena`, `abizena`, `telefonoZenbakia`, `posta`, `jaiotzeData`, `argazkia`, `kodea`, `helbidea`, `probintzia`, `komertzialKodea` (FK → `komertzialak.kodea`, `ON DELETE CASCADE`), `sortutakoData` |
| **Agenda** | Bisiten agenda entitatea | `id` (PK, auto), `bisitaData`, `ordua`, `komertzialKodea`, `bazkideaKodea`, `bazkideaId` (FK), `komertzialaId` (FK), `deskribapena`, `egoera` |
| **EskaeraGoiburua** | Eskaeraren goiburua | `zenbakia` (PK), `data`, `komertzialKodea`, `komertzialId`, `ordezkaritza`, `bazkideaKodea`, `bazkideaId` |
| **EskaeraXehetasuna** | Eskaeraren xehetasunak | `id` (PK, auto), `eskaeraZenbakia`, `artikuluKodea`, `kantitatea`, `prezioa` |
| **Eskaera** | Eskaera zaharrak (XML formatua) | `id` (PK, auto), `eskaeraID`, `bazkideaId` (FK → `bazkideak.id`, `ON DELETE CASCADE`), `prodIzena`, `data`, `kopurua`, `prodArgazkia` |
| **Katalogoa** | Produktuen katalogoa | `artikuluKodea` (PK), `izena`, `salmentaPrezioa`, `stock`, `irudiaIzena` |
| **Logina** | Erabiltzaile/pasahitza sarbideak | `id` (PK, auto), `erabiltzailea`, `pasahitza`, `komertzialKodea` |
| **HistorialCompra** | Erosketen historiala (bidalketa XML) | `id` (PK, auto), `bidalketaId`, `kodea`, `helmuga`, `data`, `amaituta`, `productoId`, `productoIzena`, `eskatuta`, `bidalita`, `prezioUnit`, `argazkia` |

### DAOs (Data Access Objects)

| Fitxategia | Helburua | Kontsulta Garrantzitsuenak |
|------------|----------|----------------------------|
| **AgendaDao** | Agenda (bisitak) taularen kontsultak | `getVisitsByKomertzial()`: **SEGURTASUNA** - komertzial baten bisitak bakarrik<br>`bilatuBezeroaz()`: Bazkide kodea/izenaren arabera bilatu<br>`bilatuDataTarteaz()`: Data tartearen arabera bilatu<br>`bilatuOrokorra()`: Bilaketa orokorra (data, izena, deskribapena, egoera)<br>`idzBilatuSegurua()`: **SEGURTASUNA** - ID bidezko bilaketa segurua |
| **BazkideaDao** | Bazkideak taularen kontsultak | `nanBilatu()`: NAN baten arabera bazkidea bilatu<br>`bilatu()`: Bilatzailea (NAN, izena, abizena, posta, telefonoa)<br>`txertatu()`: Upsert logika (`OnConflictStrategy.REPLACE`)<br>`ezabatuIdakEzDirenak()`: Sinkronizazioa (id zerrenda = mantendu behar diren id-ak) |
| **EskaeraGoiburuaDao** | Eskaera goiburuak taularen kontsultak | `komertzialarenEskaerak()`: Komertzial kode baten arabera eskaerak<br>`egunekoEskaerak()`: Eguneko eskaerak (esportazioa)<br>`hilabetekoEskaerak()`: Hilabeteko eskaerak (agenda esportazioa) |
| **EskaeraXehetasunaDao** | Eskaera xehetasunak taularen kontsultak | `eskaerarenXehetasunak()`: Eskaera zenbaki baten arabera xehetasunak<br>`txertatuGuztiak()`: Hainbat xehetasun txertatu transakzioan |
| **EskaeraDao** | Eskaera zaharrak taularen kontsultak | `bazkidearenEskaerak()`: Bazkide ID baten arabera eskaerak<br>`ezabatuBazkidearenEskaerak()`: Bazkide baten eskaera guztiak ezabatu (sinkronizazioa) |
| **KatalogoaDao** | Katalogoa taularen kontsultak | `artikuluaBilatu()`: Artikulu kodea baten arabera produktua bilatu<br>`guztiak()`: Produktu guztiak<br>`stockaEguneratu()`: Stock eguneratu eskaera bat egiten denean |
| **KomertzialaDao** | Komertzialak taularen kontsultak | `kodeaBilatu()`: Kode baten arabera komertziala bilatu<br>`guztiak()`: Komertzial guztiak |
| **LoginaDao** | Loginak taularen kontsultak | `sarbideaBalidatu()`: Erabiltzaile/pasahitza balidazioa |
| **HistorialCompraDao** | HistorialCompra taularen kontsultak | `guztiak()`: Historial guztiak<br>`txertatuGuztiak()`: Hainbat historial txertatu transakzioan |

### Repositories eta Kudeatzaileak

| Fitxategia | Helburua | Funtzio Garrantzitsuenak |
|------------|----------|--------------------------|
| **AgendaRepository** | Room eta UI-aren arteko geruza abstraktua | `txertatuBisita()`: Bisita bat txertatu (upsert)<br>`kargatuBisitak()`: **SEGURTASUNA** - uneko komertzialaren bisitak bakarrik<br>`bilatuBezeroaz()`: **SEGURTASUNA** - bezeroaren arabera bilatu<br>`bilatuDataTarteaz()`: **SEGURTASUNA** - data tartearen arabera bilatu<br>`bilatuOrokorra()`: **SEGURTASUNA** - bilaketa orokorra<br>`ezabatuBisita()`: Bisita ezabatu (segurtasun iragazkiarekin)<br>`ExecutorService` erabiliz hari nagusitik kanpo exekutatzen da |
| **XMLKudeatzailea** | XML fitxategiak inportatzeko kudeatzailea | `komertzialakInportatu()`: Wipe-and-load estrategia<br>`bazkideakInportatu()`: **Upsert logika** - existitzen bada eguneratu, bestela sortu<br>`katalogoaInportatu()`: Wipe-and-load (asteko eguneraketa)<br>`agendaInportatu()`: **Upsert logika** - bisitak transakzio bakar batean<br>`loginakInportatu()`: Loginak inportatu<br>`guztiakInportatu()`: XML guztiak ordena egokian inportatu<br>`XmlPullParser` erabiliz XML parseatu |
| **DatuKudeatzailea** | Esportazio eta inportazio logika koordinatzen du | `bazkideBerriakEsportatu()`: Eguneroko txostena (bazkide berriak)<br>`eskaeraBerriakEsportatu()`: Eguneroko txostena (eskaera berriak)<br>`katalogoaAsterokoInportazioaEgin()`: Asteko inportazioa (katalogoa)<br>`agendaHilerokoEsportazioaEgin()`: Hileroko laburpena (agenda) |
| **XMLEsportatzailea** | Room datu-baseko datuak XML fitxategietara esportatu | `bazkideBerriakEsportatu()`: `bazkide_berriak.xml` sortu<br>`eskaeraBerriakEsportatu()`: `eskaera_berriak.xml` sortu<br>`komertzialakEsportatu()`: `komertzialak.xml` sortu<br>`XmlSerializer` erabiliz XML sortu |
| **AgendaEsportatzailea** | Agenda bi formatutan esportatu | `agendaXMLSortu()`: **SEGURTASUNA** - uneko komertzialaren bisitak bakarrik<br>`agendaTXTSortu()`: Testu-fitxategia (iraurgarria)<br>`barneMemorianLekuNahikoa()`: Fitxategien egiaztapena |
| **InbentarioKudeatzailea** | Katalogoa astero inportatzeko kudeatzailea | `katalogoaAsterokoInportazioaEgin()`: Barne-memoriatik edo assets-etik katalogoa inportatu<br>`katalogoaInportatuFluxutik()`: Sarrera-fluxu batetik katalogoa inportatu |

### Segurtasuna eta Util

| Fitxategia | Helburua | Funtzio Garrantzitsuenak |
|------------|----------|--------------------------|
| **SessionManager** | Saioa hasi duen komertzialaren kodea modu seguruan gordetzen du | `saioaHasi()`: Komertzial kodea eta izena gorde (SharedPreferences)<br>`saioaItxi()`: Saioa itxi (datuak ezabatu)<br>`getKomertzialKodea()`: **SEGURTASUNA** - uneko komertzialaren kodea itzuli<br>`getKomertzialIzena()`: Komertzialaren izena itzuli<br>`saioaHasitaDago()`: Saioa hasita dagoen egiaztatu<br>`kodeaBalidatu()`: **SEGURTASUNA** - kodea uneko saioaren kodea dela egiaztatu |
| **EskaeraBalidatzailea** | Eskaerak datu-basean gordetzeko aurretik datuen integritatea egiaztatzen du | `balidatuEskaera()`: Derrigorrezko eremuak beteta daudela egiaztatu (komertzialKodea, bazkideaKodea)<br>`balidatuEtaGorde()`: Balidatu eta gorde transakzio seguru batean<br>`IllegalArgumentException` jaurtitzen du daturen bat falta bada |
| **XmlBilatzailea** | Assets-en dauden XML fitxategiak bilatzen ditu | `faltaDa()`: Assets-en fitxategi hori badagoen ala ez<br>`faltatzenDiren()`: Falta diren XML fitxategien zerrenda<br>`loginakFaltaDa()`, `komertzialakFaltaDa()`, `bazkideakFaltaDa()`, `katalogoaFaltaDa()`: Fitxategi espezifikoak falta diren egiaztatu |

### Adapters (RecyclerView)

| Fitxategia | Helburua | Funtzio Garrantzitsuenak |
|------------|----------|--------------------------|
| **AgendaBisitaAdapter** | Agenda bisiten zerrenda erakusteko | `eguneratuZerrenda()`: Zerrenda eguneratu<br>`OnBisitaEkintzaListener`: Ikusi, Editatu, Ezabatu botoiak<br>`AgendaElementua`: Bisita datuak eta bazkidearen izena |
| **EskaerakAdapter** | Eskaeren zerrenda erakusteko | `eguneratuZerrenda()`: Eskaeren zerrenda eguneratu<br>`EskaeraElementua`: Zenbakia, data, artikulu kopurua, guztira |
| **KatalogoaAdapter** | Katalogoa (inbentarioa) zerrenda erakusteko | `eguneratuZerrenda()`: Produktuen zerrenda eguneratu<br>`OnErosiClickListener`: Erosi botoia (saskira gehitu)<br>`OnItemClickListener`: Item sakatzean detale orria ireki |
| **SaskiaAdapter** | Erosketa saskiko zerrenda erakusteko | `onBindViewHolder()`: Kopurua aldatu (+/-), elementu bat kendu<br>`onSaskiaAldaketa`: Badge eta guztira eguneratzeko callback |
| **HistorialCompraAdapter** | Erosketa historial zerrenda erakusteko | `eguneratuZerrenda()`: Historial zerrenda eguneratu<br>`HistorialElementua`: Produktua, kantitatea, prezio unitarioa, prezio totala |

### AppDatabase

| Fitxategia | Helburua | Funtzio Garrantzitsuenak |
|------------|----------|--------------------------|
| **AppDatabase** | Room datu-basearen instantzia bakarra (singleton) | `getInstance()`: Singleton patroia (synchronized)<br>`addMigrations()`: 15 bertsio migrazio estrategia<br>`fallbackToDestructiveMigration()`: Eskema aldaketa handia<br>`allowMainThreadQueries()`: Kontsulta bat hari nagusian egiten bada<br>`komertzialaDao()`, `bazkideaDao()`, `agendaDao()`, etab.: DAO interfazeak |

---

*Dokumentazio hau proiektuaren arkitektura, funtzionalitate kritikoak eta fitxategi bakoitzaren erantzukizuna deskribatzen ditu. Kodearen xehetasun gehiago lortzeko, kodea kontsultatu edo garatzaileekin jarri harremanetan.*

