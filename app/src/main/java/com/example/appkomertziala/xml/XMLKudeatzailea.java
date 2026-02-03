package com.example.appkomertziala.xml;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Agenda;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.example.appkomertziala.db.eredua.Eskaera;
import com.example.appkomertziala.db.eredua.EskaeraGoiburua;
import com.example.appkomertziala.db.eredua.EskaeraXehetasuna;
import com.example.appkomertziala.db.eredua.Katalogoa;
import com.example.appkomertziala.db.eredua.Komertziala;
import com.example.appkomertziala.db.eredua.Logina;
import com.example.appkomertziala.db.kontsultak.AgendaDao;
import com.example.appkomertziala.db.kontsultak.BazkideaDao;
import com.example.appkomertziala.db.kontsultak.EskaeraDao;
import com.example.appkomertziala.db.kontsultak.EskaeraGoiburuaDao;
import com.example.appkomertziala.db.kontsultak.EskaeraXehetasunaDao;
import com.example.appkomertziala.db.kontsultak.KatalogoaDao;
import com.example.appkomertziala.db.kontsultak.KomertzialaDao;
import com.example.appkomertziala.db.kontsultak.LoginaDao;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * XML fitxategiak barne-memoriatik (edo assets-etik erreserba gisa) irakurtzeko eta datu-basean txertatu/eguneratu (upsert) egiteko kudeatzailea.
 * Ordezkaritzatik jasotako fitxategiak barne-memorian gorde ohi dira. Eragiketa guztiak hila nagusitik kanpo exekutatu behar dira.
 * XmlPullParser erabiltzen du. komertzialak.xml, bazkideak.xml, loginak.xml, katalogoa.xml, agenda.xml.
 */
public class XMLKudeatzailea {

    private static final String ETIKETA = "XMLKudeatzailea";

    private final Context context;
    private final AppDatabase db;

    /** Komertzial ID bat komertzial kodearekin lotzeko mapa (inportazio ordena). */
    private Map<Long, String> komertzialIdKodea;

    public XMLKudeatzailea(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(this.context);
        this.komertzialIdKodea = new HashMap<>();
    }

    /**
     * XML fitxategia barne-memoriatik irakurri; ez badago, assets erabili (probak edo lehen karga).
     * Ordezkaritzatik jasotako fitxategiak barne-memorian gorde ohi dira; hortik irakurri da lehenetsia.
     */
    private InputStream barneFitxategiaEdoAssetsIreki(String fitxategiIzena) throws IOException {
        try {
            return context.openFileInput(fitxategiIzena);
        } catch (FileNotFoundException e) {
            return context.getAssets().open(fitxategiIzena);
        }
    }

    /** Assets-etik fitxategi bat ireki (katalogoa.xml eta beste atzerapen-erabileretarako). */
    private InputStream assetsFitxategiaIreki(String fitxategiIzena) throws IOException {
        return context.getAssets().open(fitxategiIzena);
    }

    /**
     * komertzialak.xml inportatu: barne-memoriatik (edo assets-etik) irakurri, wipe-and-load.
     * Komertzialak taula guztiz ezabatu eta XMLko erregistroak bakarrik txertatzen dira; informazio zaharra ordezkatuta.
     * Etiketak: komertzialak > komertziala > NAN, izena, abizena.
     * Eragiketa hau hila nagusitik kanpo exekutatu behar da.
     */
    public int komertzialakInportatu() throws IOException, XmlPullParserException {
        return komertzialakInportatu(barneFitxategiaEdoAssetsIreki("komertzialak.xml"));
    }

    /**
     * komertzialak.xml assets-etik inportatu eta datu-basea gainidatzi.
     * «Kargatu XML» dialogotik komertzialak.xml hautatzean erabiltzen da, XML eguneratua datu-basean islatzeko.
     */
    public int komertzialakInportatuAssetsetik() throws IOException, XmlPullParserException {
        try (InputStream is = assetsFitxategiaIreki("komertzialak.xml")) {
            return komertzialakInportatu(is, false);
        }
    }

    /**
     * Komertzialak kargatu bakarrik taula hutsik bada (login hasierako karga). Ez du inoiz datu-basean dauden
     * komertzialak gainidazten (gailutik inportatutakoak mantentzen dira).
     */
    public int komertzialakInportatuBakarrikHutsikBada() throws IOException, XmlPullParserException {
        try (InputStream is = barneFitxategiaEdoAssetsIreki("komertzialak.xml")) {
            return komertzialakInportatu(is, true);
        }
    }

    /** Gailutik: sarrera-fluxu batetik komertzialak inportatu. */
    public int komertzialakInportatu(InputStream is) throws IOException, XmlPullParserException {
        return komertzialakInportatu(is, false);
    }

    /**
     * Komertzialak inportatu fluxu batetik. bakarrikHutsikBada true bada, idatzi baino lehen taula hutsik dagoen egiaztatzen du;
     * hutsik ez bada, ez du ezer gainidazten (hasierako karga gailutik inportatutako datuekin lehiatzen ez dadin).
     */
    private int komertzialakInportatu(InputStream is, boolean bakarrikHutsikBada) throws IOException, XmlPullParserException {
        List<Komertziala> zerrenda = new ArrayList<>();
        try (InputStream stream = is) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(stream, "UTF-8");
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, "komertzialak");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) continue;
                String name = parser.getName();
                if ("komertziala".equals(name)) {
                    zerrenda.add(komertzialaElementuaIrakurri(parser));
                } else {
                    atalBatJauzi(parser);
                }
            }
        }
        KomertzialaDao dao = db.komertzialaDao();
        if (bakarrikHutsikBada && !dao.guztiak().isEmpty()) {
            return 0;
        }
        dao.ezabatuGuztiak();
        if (!zerrenda.isEmpty()) {
            dao.txertatuGuztiak(zerrenda);
        }
        komertzialIdKodea.clear();
        for (Komertziala g : dao.guztiak()) {
            komertzialIdKodea.put(g.getId(), g.getKodea());
        }
        return zerrenda.size();
    }

    /** komertziala elementu bat irakurri (NAN, izena, abizena, posta, jaiotzeData, argazkia). */
    private Komertziala komertzialaElementuaIrakurri(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "komertziala");
        String kodea = null;
        String izena = null;
        String abizena = null;
        String posta = null;
        String jaiotzeData = null;
        String argazkia = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            String name = parser.getName();
            switch (name) {
                case "NAN":
                    kodea = testuaIrakurri(parser);
                    break;
                case "izena":
                    izena = testuaIrakurri(parser);
                    break;
                case "abizena":
                    abizena = testuaIrakurri(parser);
                    break;
                case "posta":
                    posta = testuaIrakurri(parser);
                    break;
                case "jaiotzeData":
                    jaiotzeData = testuaIrakurri(parser);
                    break;
                case "argazkia":
                    argazkia = testuaIrakurri(parser);
                    break;
                default:
                    atalBatJauzi(parser);
                    break;
            }
        }
        String izenOsoa = (izena != null ? izena : "").trim() + " " + (abizena != null ? abizena : "").trim();
        if (izenOsoa != null) izenOsoa = izenOsoa.trim();
        if (kodea == null) kodea = "";
        Komertziala k = new Komertziala();
        k.setIzena(izenOsoa != null ? izenOsoa : "");
        k.setKodea(kodea);
        k.setAbizena(abizena != null ? abizena : "");
        k.setPosta(posta != null ? posta : "");
        k.setJaiotzeData(jaiotzeData != null ? jaiotzeData : "");
        k.setArgazkia(argazkia != null ? argazkia : "");
        return k;
    }


    /**
     * bazkideak.xml inportatu: barne-memoriatik (edo assets-etik) irakurri.
     * Formato: bazkideak > bazkidea > NAN, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia > eskaerak
     */
    public int bazkideakInportatu() throws IOException, XmlPullParserException {
        try (InputStream is = barneFitxategiaEdoAssetsIreki("bazkideak.xml")) {
            return bazkideakInportatu(is);
        }
    }

    /** Gailutik: sarrera-fluxu batetik bazkideak inportatu (eskaerak barne). Transakzioan exekutatzen da errendimendu maximoa lortzeko. */
    public int bazkideakInportatu(InputStream is) throws IOException, XmlPullParserException {
        List<BazkideaEtaEskaerak> emaitzak = new ArrayList<>();
        
        // XML irakurri eta parseatu
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(is, "UTF-8");
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "bazkideak");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            if ("bazkidea".equals(parser.getName())) {
                BazkideaEtaEskaerak emaitza = bazkideaElementuaIrakurriBazkideaEtaEskaerak(parser);
                if (emaitza.bazkidea != null) {
                    emaitzak.add(emaitza);
                }
            } else {
                atalBatJauzi(parser);
            }
        }
        
        if (emaitzak.isEmpty()) {
            Log.w(ETIKETA, "bazkideakInportatu: XML hutsik dago edo ez da bazkiderik aurkitu");
            return 0;
        }
        
        Log.d(ETIKETA, "bazkideakInportatu: " + emaitzak.size() + " bazkidea parseatu dira");
        
        // Transakzioan gorde datu guztiak (errendimendu maximoa)
        try {
            int emaitzaKopurua = db.runInTransaction(() -> {
                BazkideaDao bazkideaDao = db.bazkideaDao();
                EskaeraDao eskaeraDao = db.eskaeraDao();
                
                int kopurua = 0;
                int eguneratutakoKopurua = 0;
                for (BazkideaEtaEskaerak emaitza : emaitzak) {
                    Bazkidea bazkidea = emaitza.bazkidea;
                    
                    if (bazkidea.getNan() == null || bazkidea.getNan().trim().isEmpty()) {
                        Log.w(ETIKETA, "bazkideakInportatu: Bazkidea baztertua, NAN hutsik dago");
                        continue;
                    }
                    
                    // Bilatu existitzen den bazkidea NAN erabiliz
                    Bazkidea existitzenDa = bazkideaDao.nanBilatu(bazkidea.getNan().trim());
                    
                    long bazkideaId;
                    if (existitzenDa != null) {
                        // Eguneratu: ID mantendu eta XML-etik datozen eremuak bakarrik eguneratu
                        bazkidea.setId(existitzenDa.getId()); // ID mantendu
                        // txertatu() REPLACE estrategia erabiliz (eguneratu baino fidagarriagoa Room-en)
                        bazkideaId = bazkideaDao.txertatu(bazkidea);
                        if (bazkideaId > 0) {
                            eguneratutakoKopurua++;
                            Log.d(ETIKETA, "bazkideakInportatu: Bazkidea eguneratua (NAN: " + bazkidea.getNan() + ", ID: " + bazkideaId + ")");
                        } else {
                            Log.e(ETIKETA, "bazkideakInportatu: Errorea bazkidea eguneratzean (NAN: " + bazkidea.getNan() + ")");
                            continue;
                        }
                        
                        // Ezabatu bazkidearen eskaera zaharrak
                        eskaeraDao.ezabatuBazkidearenEskaerak(bazkideaId);
                    } else {
                        // Bazkidea berria txertatu
                        bazkideaId = bazkideaDao.txertatu(bazkidea);
                        if (bazkideaId > 0) {
                            kopurua++;
                            Log.d(ETIKETA, "bazkideakInportatu: Bazkidea berria txertatua (NAN: " + bazkidea.getNan() + ", ID: " + bazkideaId + ")");
                        } else {
                            Log.e(ETIKETA, "bazkideakInportatu: Errorea bazkidea txertatzean (NAN: " + bazkidea.getNan() + ")");
                            continue;
                        }
                    }
                    
                    // Eskaerak txertatu bazkideaId-rekin (bazkideaId baliozkoa dela egiaztatu)
                    if (!emaitza.eskaerak.isEmpty() && bazkideaId > 0) {
                        // Egiaztatu bazkidea existitzen dela
                        Bazkidea egiaztatua = bazkideaDao.idzBilatu(bazkideaId);
                        if (egiaztatua == null) {
                            Log.e(ETIKETA, "bazkideakInportatu: Bazkidea ez da existitzen ID: " + bazkideaId + ", eskaerak ez dira txertatuko");
                        } else {
                            for (Eskaera eskaera : emaitza.eskaerak) {
                                eskaera.setBazkideaId(bazkideaId);
                            }
                            try {
                                List<Long> eskaeraIds = eskaeraDao.txertatuGuztiak(emaitza.eskaerak);
                                Log.d(ETIKETA, "bazkideakInportatu: " + eskaeraIds.size() + " eskaera txertatu dira bazkidea ID: " + bazkideaId);
                            } catch (Exception e) {
                                Log.e(ETIKETA, "bazkideakInportatu: Errorea eskaerak txertatzean bazkidea ID: " + bazkideaId, e);
                                throw e; // Transakzioa atzera egingo du
                            }
                        }
                    }
                }
                
                Log.d(ETIKETA, "bazkideakInportatu: Transakzioa osatua - " + kopurua + " berri, " + eguneratutakoKopurua + " eguneratutako");
                return kopurua > 0 ? kopurua : (eguneratutakoKopurua > 0 ? eguneratutakoKopurua : emaitzak.size());
            });
            
            // Egiaztatu datuak gordeta daudela
            BazkideaDao bazkideaDao = db.bazkideaDao();
            List<Bazkidea> gordeta = bazkideaDao.guztiak();
            Log.d(ETIKETA, "bazkideakInportatu: Datu-basean " + gordeta.size() + " bazkidea daude gordeta");
            
            return emaitzaKopurua;
        } catch (Exception e) {
            Log.e(ETIKETA, "bazkideakInportatu: Errorea transakzioan", e);
            throw new RuntimeException("Errorea bazkideak inportatzean: " + e.getMessage(), e);
        }
    }
    
    /** Bazkidea eta bere eskaerak irakurtzeko emaitza klasea. */
    private static class BazkideaEtaEskaerak {
        Bazkidea bazkidea;
        List<Eskaera> eskaerak = new ArrayList<>();
    }

    /** bazkidea elementu bat irakurri taula bazkideak-erako eta bere eskaerak (Bazkidea entitatea + Eskaera).
     * Formato XML: bazkidea > NAN, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia > eskaerak > eskaera
     */
    private BazkideaEtaEskaerak bazkideaElementuaIrakurriBazkideaEtaEskaerak(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "bazkidea");
        String nan = null;
        String izena = null;
        String abizena = null;
        String telefonoZenbakia = null;
        String posta = null;
        String jaiotzeData = null;
        String argazkia = null;
        List<Eskaera> eskaerak = new ArrayList<>();
        
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            String name = parser.getName();
            if ("eskaerak".equals(name)) {
                // Irakurri eskaerak blokea
                eskaerakBlokeaIrakurri(parser, eskaerak);
                continue;
            }
            switch (name) {
                case "NAN":
                    nan = testuaIrakurri(parser);
                    break;
                case "izena":
                    izena = testuaIrakurri(parser);
                    break;
                case "abizena":
                    abizena = testuaIrakurri(parser);
                    break;
                case "telefonoZenbakia":
                    telefonoZenbakia = testuaIrakurri(parser);
                    break;
                case "posta":
                    posta = testuaIrakurri(parser);
                    break;
                case "jaiotzeData":
                    jaiotzeData = testuaIrakurri(parser);
                    break;
                case "argazkia":
                    argazkia = testuaIrakurri(parser);
                    break;
                default:
                    atalBatJauzi(parser);
                    break;
            }
        }
        
        Bazkidea b = new Bazkidea();
        b.setNan(nan != null ? nan.trim() : "");
        b.setIzena(izena != null ? izena.trim() : "");
        b.setAbizena(abizena != null ? abizena.trim() : "");
        b.setTelefonoZenbakia(telefonoZenbakia != null ? telefonoZenbakia.trim() : "");
        b.setPosta(posta != null ? posta.trim() : "");
        b.setJaiotzeData(jaiotzeData != null ? jaiotzeData.trim() : "");
        b.setArgazkia(argazkia != null ? argazkia.trim() : "");
        
        BazkideaEtaEskaerak emaitza = new BazkideaEtaEskaerak();
        emaitza.bazkidea = b;
        emaitza.eskaerak = eskaerak;
        return emaitza;
    }
    
    /** eskaerak bloke bat irakurri (eskaera elementuak). */
    private void eskaerakBlokeaIrakurri(XmlPullParser parser, List<Eskaera> eskaerak) 
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "eskaerak");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            if ("eskaera".equals(parser.getName())) {
                Eskaera eskaera = eskaeraElementuaIrakurri(parser);
                if (eskaera != null) {
                    eskaerak.add(eskaera);
                }
            } else {
                atalBatJauzi(parser);
            }
        }
    }
    
    /** eskaera elementu bat irakurri eta Eskaera entitatea sortu (eskaeraID, prodIzena, data, kopurua, prodArgazkia). */
    private Eskaera eskaeraElementuaIrakurri(XmlPullParser parser) 
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "eskaera");
        String eskaeraID = null;
        String prodIzena = null;
        String data = null;
        String kopurua = null;
        String prodArgazkia = null;
        
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            String name = parser.getName();
            switch (name) {
                case "eskaeraID":
                    eskaeraID = testuaIrakurri(parser);
                    break;
                case "prodIzena":
                    prodIzena = testuaIrakurri(parser);
                    break;
                case "data":
                    data = testuaIrakurri(parser);
                    break;
                case "kopurua":
                    kopurua = testuaIrakurri(parser);
                    break;
                case "prodArgazkia":
                    prodArgazkia = testuaIrakurri(parser);
                    break;
                default:
                    atalBatJauzi(parser);
                    break;
            }
        }
        
        if (eskaeraID == null || eskaeraID.trim().isEmpty()) {
            return null;
        }
        
        Eskaera eskaera = new Eskaera();
        eskaera.setEskaeraID(eskaeraID.trim());
        eskaera.setProdIzena(prodIzena != null ? prodIzena.trim() : "");
        eskaera.setData(data != null ? data.trim() : "");
        eskaera.setKopurua(kopurua != null ? (int) parseLongSafe(kopurua, 0) : 0);
        eskaera.setProdArgazkia(prodArgazkia != null ? prodArgazkia.trim() : "");
        // bazkideaId geroago eguneratuko da bazkidea gordeta dagoenean
        
        return eskaera;
    }


    /** Gaurko data yyyy-MM-dd formatuan (eguneko alta esportazioetarako). */
    private static String gaurkoData() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /**
     * loginak.xml inportatu (assets-etik): sinkronizazioa (insert, update, delete). Erabiltzailea gakoa.
     * Etiketak: loginak > erabiltzailea > id, email, pasahitza.
     */
    public int loginakInportatu() throws IOException, XmlPullParserException {
        try (InputStream is = assetsFitxategiaIreki("loginak.xml")) {
            return loginakInportatu(is);
        }
    }

    /** Gailutik: sarrera-fluxu batetik loginak inportatu. */
    public int loginakInportatu(InputStream is) throws IOException, XmlPullParserException {
        List<Logina> zerrenda = new ArrayList<>();
        List<Komertziala> komertzialak = db.komertzialaDao().guztiak();
        for (int i = 0; i < komertzialak.size(); i++) {
            komertzialIdKodea.put((long) (i + 1), komertzialak.get(i).getKodea());
        }
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(is, "UTF-8");
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "loginak");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            if ("erabiltzailea".equals(parser.getName())) {
                zerrenda.add(erabiltzaileaElementuaIrakurri(parser));
            } else {
                atalBatJauzi(parser);
            }
        }
        List<String> xmlErabiltzaileak = zerrenda.stream().map(Logina::getErabiltzailea).filter(e -> e != null && !e.isEmpty()).distinct().collect(Collectors.toList());
        LoginaDao dao = db.loginaDao();
        if (xmlErabiltzaileak.isEmpty()) {
            dao.ezabatuGuztiak();
        } else {
            dao.ezabatuErabiltzaileakEzDirenak(xmlErabiltzaileak);
        }
        for (Logina l : zerrenda) {
            Logina exist = dao.erabiltzaileaBilatu(l.getErabiltzailea());
            if (exist != null) {
                dao.eguneratu(l);
            } else {
                dao.txertatu(l);
            }
        }
        return zerrenda.size();
    }

    /** erabiltzailea elementu bat irakurri (id, email, pasahitza). */
    private Logina erabiltzaileaElementuaIrakurri(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "erabiltzailea");
        long id = 1;
        String email = "";
        String pasahitza = "";
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            String name = parser.getName();
            switch (name) {
                case "id":
                    id = parseLongSafe(testuaIrakurri(parser), 1);
                    break;
                case "email":
                    email = testuaIrakurri(parser);
                    break;
                case "pasahitza":
                    pasahitza = testuaIrakurri(parser);
                    break;
                default:
                    atalBatJauzi(parser);
                    break;
            }
        }
        String komertzialKodea = komertzialIdKodea.get(id);
        if (komertzialKodea == null && !komertzialIdKodea.isEmpty()) {
            komertzialKodea = komertzialIdKodea.values().iterator().next();
        }
        if (komertzialKodea == null) komertzialKodea = "";
        return new Logina(email != null ? email : "", pasahitza != null ? pasahitza : "", komertzialKodea);
    }

    /**
     * katalogoa.xml inportatu (assets-etik): sinkronizazioa (insert, update, delete). ArtikuluKodea gakoa.
     * Etiketak: katalogoa > produktua > id, izena, prezioa, stock, irudia_path (drawable izena, adib. macbook.jpg).
     */
    public int katalogoaInportatu() throws IOException, XmlPullParserException {
        try (InputStream is = assetsFitxategiaIreki("katalogoa.xml")) {
            return katalogoaInportatuSarreraFluxutik(is);
        }
    }

    /**
     * Barne-memorian gordetako fitxategitik katalogoa inportatu (ordezkaritzatik jasotakoa — asteko inportazioa).
     * Fitxategi-izena aplikazioko barne direktoriari erlatiboa (openFileInput).
     */
    public int katalogoaInportatuBarneFitxategitik(String fitxategiIzena) throws IOException, XmlPullParserException {
        try (InputStream is = context.openFileInput(fitxategiIzena)) {
            return katalogoaInportatuSarreraFluxutik(is);
        }
    }

    /**
     * Katalogoa sarrera-fluxu batetik inportatu (ordezkaritzatik jasotako fitxategia — asteko inportazioa).
     * Wipe-and-load: katalogoa taula guztiz ezabatu eta XMLko produktuak bakarrik txertatzen dira.
     * Aurreko asteko stock eta prezio guztiak balio gabe uzten dira; XML da egia bakarra.
     * Egitura: katalogoa > produktua > id, izena, prezioa, stock, irudia_path.
     */
    public int katalogoaInportatuSarreraFluxutik(InputStream is) throws IOException, XmlPullParserException {
        List<Katalogoa> zerrenda = new ArrayList<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(is, "UTF-8");
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "katalogoa");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            if ("produktua".equals(parser.getName())) {
                zerrenda.add(produktuaElementuaIrakurri(parser));
            } else {
                atalBatJauzi(parser);
            }
        }
        KatalogoaDao dao = db.katalogoaDao();
        dao.ezabatuGuztiak();
        if (!zerrenda.isEmpty()) {
            dao.txertatuGuztiak(zerrenda);
        }
        return zerrenda.size();
    }

    /** produktua elementu bat irakurri (id, izena, prezioa, stock, irudia_path). irudia_path drawable baliabidearen izena. */
    private Katalogoa produktuaElementuaIrakurri(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "produktua");
        String id = "";
        String izena = "";
        double prezioa = 0;
        int stock = 0;
        String irudiaIzena = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            String name = parser.getName();
            switch (name) {
                case "id":
                    id = testuaIrakurri(parser);
                    break;
                case "izena":
                    izena = testuaIrakurri(parser);
                    break;
                case "prezioa":
                    prezioa = parseDoubleSafe(testuaIrakurri(parser), 0);
                    break;
                case "stock":
                    stock = (int) parseLongSafe(testuaIrakurri(parser), 0);
                    break;
                case "irudia_path":
                    irudiaIzena = testuaIrakurri(parser);
                    break;
                default:
                    atalBatJauzi(parser);
                    break;
            }
        }
        Katalogoa k = new Katalogoa(id != null ? id : "", izena != null ? izena : "", prezioa, stock, irudiaIzena);
        return k;
    }

    /** Elementu baten testu-edukia irakurri. */
    private String testuaIrakurri(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result != null ? result.trim() : "";
    }

    /** Hurrengo END_TAG arte jauzi (elementu osoa). */
    private void atalBatJauzi(XmlPullParser parser) throws IOException, XmlPullParserException {
        if (parser.getEventType() != XmlPullParser.START_TAG) return;
        int zorrotz = 1;
        while (zorrotz != 0) {
            int e = parser.next();
            if (e == XmlPullParser.START_TAG) zorrotz++;
            else if (e == XmlPullParser.END_TAG) zorrotz--;
        }
    }

    private static long parseLongSafe(String s, long defaultValue) {
        if (s == null || s.isEmpty()) return defaultValue;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double parseDoubleSafe(String s, double defaultValue) {
        if (s == null || s.isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Assets-eko XML fitxategi guztien izenak itzultzen ditu (.xml bukatzekoak).
     * Hautaketa-dialogoarentzat erabiltzen da.
     */
    public String[] assetsXmlFitxategiak() throws IOException {
        String[] guztiak = context.getAssets().list("");
        if (guztiak == null) return new String[0];
        List<String> xmlak = new ArrayList<>();
        for (String izena : guztiak) {
            if (izena != null && izena.endsWith(".xml")) {
                xmlak.add(izena);
            }
        }
        String[] emaitza = xmlak.toArray(new String[0]);
        Arrays.sort(emaitza);
        return emaitza;
    }

    /**
     * agenda.xml inportatu. Bi formatu onartzen dira:
     * - Agenda (agenda_bisitak): bisita > bisita_data, bazkidea_kodea, deskribapena, egoera.
     * - EskaeraGoiburua (zitak zaharrak): bisita > zenbakia, data, komertzialKodea, ordezkaritza, bazkideaKodea.
     */
    public int agendaInportatu(InputStream is) throws IOException, XmlPullParserException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(is, "UTF-8");
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "agenda");
        int count = 0;
        AgendaDao agendaDao = db.agendaDao();
        EskaeraGoiburuaDao eskaeraDao = db.eskaeraGoiburuaDao();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            if ("bisita".equals(parser.getName())) {
                Map<String, String> map = bisitaElementuaMap(parser);
                if (map.containsKey("bisita_data")) {
                    Agenda a = new Agenda();
                    a.setBisitaData(trimm(map.get("bisita_data")));
                    // Compatibilidad: aceptar tanto partner_kodea como bazkidea_kodea
                    String bazkideaKodea = trimm(map.get("bazkidea_kodea"));
                    if (bazkideaKodea == null || bazkideaKodea.isEmpty()) {
                        bazkideaKodea = trimm(map.get("partner_kodea"));
                    }
                    a.setBazkideaKodea(bazkideaKodea);
                    a.setDeskribapena(trimm(map.get("deskribapena")));
                    a.setEgoera(trimm(map.get("egoera")));
                    agendaDao.txertatu(a);
                    count++;
                } else {
                    EskaeraGoiburua goi = bisitaMapToEskaeraGoiburua(map);
                    if (goi.getKomertzialKodea() != null && !goi.getKomertzialKodea().isEmpty()) {
                        Komertziala kom = db.komertzialaDao().kodeaBilatu(goi.getKomertzialKodea().trim());
                        if (kom != null) goi.setKomertzialId(kom.getId());
                    }
                    if (goi.getBazkideaKodea() != null && !goi.getBazkideaKodea().isEmpty()) {
                        Bazkidea bazkidea = db.bazkideaDao().nanBilatu(goi.getBazkideaKodea().trim());
                        if (bazkidea != null) goi.setBazkideaId(bazkidea.getId());
                    }
                    eskaeraDao.txertatu(goi);
                    count++;
                }
            } else {
                atalBatJauzi(parser);
            }
        }
        return count;
    }

    private static String trimm(String s) {
        return s != null ? s.trim() : "";
    }

    private Map<String, String> bisitaElementuaMap(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "bisita");
        Map<String, String> map = new HashMap<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            String name = parser.getName();
            String value = testuaIrakurri(parser);
            map.put(name, value != null ? value : "");
        }
        return map;
    }

    private EskaeraGoiburua bisitaMapToEskaeraGoiburua(Map<String, String> map) {
        String zenbakia = trimm(map.get("zenbakia"));
        if (zenbakia == null || zenbakia.isEmpty()) zenbakia = "agenda_" + System.currentTimeMillis();
        EskaeraGoiburua goi = new EskaeraGoiburua();
        goi.setZenbakia(zenbakia);
        goi.setData(trimm(map.get("data")));
        goi.setKomertzialKodea(trimm(map.get("komertzialKodea")));
        goi.setOrdezkaritza(trimm(map.get("ordezkaritza")));
        // Compatibilidad: aceptar tanto partnerKodea como bazkideaKodea
        String bazkideaKodea = trimm(map.get("bazkideaKodea"));
        if (bazkideaKodea == null || bazkideaKodea.isEmpty()) {
            bazkideaKodea = trimm(map.get("partnerKodea"));
        }
        goi.setBazkideaKodea(bazkideaKodea);
        return goi;
    }

    /** bisita elementu bat irakurri (zenbakia, data, komertzialKodea, ordezkaritza, bazkideaKodea). */
    private EskaeraGoiburua bisitaElementuaIrakurri(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "bisita");
        String zenbakia = "";
        String data = "";
        String komertzialKodea = "";
        String ordezkaritza = "";
        String bazkideaKodea = "";
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            String name = parser.getName();
            String value = testuaIrakurri(parser);
            switch (name) {
                case "zenbakia": zenbakia = value; break;
                case "data": data = value; break;
                case "komertzialKodea": komertzialKodea = value; break;
                case "ordezkaritza": ordezkaritza = value; break;
                case "bazkideaKodea": bazkideaKodea = value; break;
                case "partnerKodea": bazkideaKodea = value; break; // Compatibilidad
                default: atalBatJauzi(parser); break;
            }
        }
        if (zenbakia == null || zenbakia.isEmpty()) zenbakia = "agenda_" + System.currentTimeMillis();
        EskaeraGoiburua goi = new EskaeraGoiburua();
        goi.setZenbakia(zenbakia.trim());
        goi.setData(data != null ? data.trim() : "");
        goi.setKomertzialKodea(komertzialKodea != null ? komertzialKodea.trim() : "");
        goi.setOrdezkaritza(ordezkaritza != null ? ordezkaritza.trim() : "");
        goi.setBazkideaKodea(bazkideaKodea != null ? bazkideaKodea.trim() : "");
        return goi;
    }

    /**
     * Gailutik hautatutako fitxategi bat inportatzen du (Uri / InputStream).
     * Fitxategi-izenaren arabera: komertzialak.xml, bazkideak.xml, loginak.xml, katalogoa.xml, agenda.xml.
     */
    public void inportatuSarreraFluxutik(InputStream is, String fitxategiIzena) throws IOException, XmlPullParserException {
        if (fitxategiIzena == null) fitxategiIzena = "";
        String izena = fitxategiIzena.contains("/") ? fitxategiIzena.substring(fitxategiIzena.lastIndexOf('/') + 1) : fitxategiIzena;
        izena = izena.trim().toLowerCase(Locale.ROOT);
        switch (izena) {
            case "komertzialak.xml": {
                byte[] data = irakurriGuztia(is);
                komertzialakInportatu(new ByteArrayInputStream(data), false);
                try (java.io.OutputStream out = context.openFileOutput("komertzialak.xml", Context.MODE_PRIVATE)) {
                    out.write(data);
                }
                break;
            }
            case "partnerrak.xml":
                // partnerrak.xml ya no se usa, se usa bazkideak.xml
                break;
            case "bazkideak.xml":
                bazkideakInportatu(is);
                break;
            case "loginak.xml":
                loginakInportatu(is);
                break;
            case "katalogoa.xml":
                katalogoaInportatuSarreraFluxutik(is);
                break;
            case "agenda.xml":
                agendaInportatu(is);
                break;
            default:
                throw new IllegalArgumentException("Fitxategi mota hau ezin da inportatu: " + izena);
        }
    }

    private static byte[] irakurriGuztia(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    /**
     * Fitxategi bat inportatzen du izenaren arabera (barne-memoriatik edo assets-etik).
     * Onartutako fitxategiak: komertzialak.xml, bazkideak.xml, loginak.xml, katalogoa.xml, agenda.xml.
     * agenda.xml barne-memorian gordeta egon behar du (esportatu ondoren inportatzeko).
     */
    public void inportatuFitxategia(String fitxategiIzena) throws IOException, XmlPullParserException {
        if (fitxategiIzena == null) fitxategiIzena = "";
        switch (fitxategiIzena) {
            case "komertzialak.xml":
                komertzialakInportatuAssetsetik();
                break;
            case "partnerrak.xml":
                // partnerrak.xml ya no se usa, se usa bazkideak.xml
                break;
            case "bazkideak.xml": {
                int kopurua = bazkideakInportatu();
                Log.d(ETIKETA, "inportatuFitxategia: bazkideak.xml inportatua - " + kopurua + " bazkidea prozesatu dira");
                break;
            }
            case "loginak.xml":
                loginakInportatu();
                break;
            case "katalogoa.xml":
                katalogoaInportatu();
                break;
            case "agenda.xml":
                try (InputStream is = context.openFileInput("agenda.xml")) {
                    agendaInportatu(is);
                }
                break;
            default:
                throw new IllegalArgumentException("Fitxategi mota hau ezin da inportatu: " + fitxategiIzena);
        }
    }

    /**
     * XML guztiak ordena egokian inportatzen ditu (barne-memoriatik edo assets-etik): komertzialak -> bazkideak -> loginak -> katalogoa.
     * Komertzialak assets-etik kargatzen dira (XML eguneratua datu-basean islatzeko).
     */
    public void guztiakInportatu() throws IOException, XmlPullParserException {
        komertzialakInportatuAssetsetik();
        bazkideakInportatu();
        loginakInportatu();
        katalogoaInportatu();
    }
}
