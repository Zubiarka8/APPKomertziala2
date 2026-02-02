package com.example.appkomertziala.xml;

import android.content.Context;
import android.util.Xml;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.example.appkomertziala.db.eredua.Katalogoa;
import com.example.appkomertziala.db.eredua.Komertziala;
import com.example.appkomertziala.db.eredua.Logina;
import com.example.appkomertziala.db.eredua.Partnerra;
import com.example.appkomertziala.db.kontsultak.BazkideaDao;
import com.example.appkomertziala.db.kontsultak.KatalogoaDao;
import com.example.appkomertziala.db.kontsultak.KomertzialaDao;
import com.example.appkomertziala.db.kontsultak.LoginaDao;
import com.example.appkomertziala.db.kontsultak.PartnerraDao;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * XML fitxategiak barne-memorian irakurtzeko eta datu-basean txertatu/eguneratu (upsert) egiteko kudeatzailea.
 * XmlPullParser erabiltzen du. komertzialak.xml, partnerrak.xml, bazkideak.xml, loginak.xml, katalogoa.xml.
 */
public class XMLKudeatzailea {

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
     * Assets-etik fitxategi baten izena jasota, InputStream itzultzen du.
     * Barne-memorian irakurtzeko erabiltzen da.
     */
    private InputStream assetsFitxategiaIreki(String fitxategiIzena) throws IOException {
        return context.getAssets().open(fitxategiIzena);
    }

    /**
     * komertzialak.xml inportatu: sinkronizazioa (insert, update, delete). Duplikaturik ez.
     * Etiketak: komertzialak > komertziala > NAN, izena, abizena.
     * XML da erreferentzia: DB-n dauden baina XML-en ez daudenak ezabatzen dira.
     */
    public int komertzialakInportatu() throws IOException, XmlPullParserException {
        List<Komertziala> zerrenda = new ArrayList<>();
        try (InputStream is = assetsFitxategiaIreki("komertzialak.xml")) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(is, "UTF-8");
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
        List<String> xmlCodeak = zerrenda.stream().map(Komertziala::getKodea).filter(k -> k != null && !k.isEmpty()).distinct().collect(Collectors.toList());
        KomertzialaDao dao = db.komertzialaDao();
        if (xmlCodeak.isEmpty()) {
            dao.ezabatuGuztiak();
        } else {
            dao.ezabatuKodeakEzDirenak(xmlCodeak);
        }
        for (Komertziala k : zerrenda) {
            Komertziala exist = dao.kodeaBilatu(k.getKodea());
            if (exist != null) {
                k.setId(exist.getId());
                dao.eguneratu(k);
            } else {
                dao.txertatu(k);
            }
        }
        komertzialIdKodea.clear();
        for (Komertziala g : dao.guztiak()) {
            komertzialIdKodea.put(g.getId(), g.getKodea());
        }
        return zerrenda.size();
    }

    /** komertziala elementu bat irakurri (NAN, izena, abizena). */
    private Komertziala komertzialaElementuaIrakurri(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "komertziala");
        String kodea = null;
        String izena = null;
        String abizena = null;
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
                default:
                    atalBatJauzi(parser);
                    break;
            }
        }
        String izenOsoa = (izena != null ? izena : "") + " " + (abizena != null ? abizena : "").trim();
        if (kodea == null) kodea = "";
        return new Komertziala(izenOsoa, kodea);
    }

    /**
     * partnerrak.xml inportatu: sinkronizazioa (insert, update, delete). Bazkideak (id >= 1000) mantentzen dira.
     * Etiketak: partnerrak > partner > id, izena, helbidea, komertzial_id.
     */
    public int partnerrakInportatu() throws IOException, XmlPullParserException {
        List<Partnerra> zerrenda = new ArrayList<>();
        try (InputStream is = assetsFitxategiaIreki("partnerrak.xml")) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(is, "UTF-8");
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, "partnerrak");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) continue;
                if ("partner".equals(parser.getName())) {
                    zerrenda.add(partnerElementuaIrakurri(parser));
                } else {
                    atalBatJauzi(parser);
                }
            }
        }
        PartnerraDao dao = db.partnerraDao();
        List<Long> mantenduIds = new ArrayList<>(zerrenda.stream().map(Partnerra::getId).collect(Collectors.toList()));
        for (Partnerra p : dao.guztiak()) {
            if (p.getId() >= 1000) mantenduIds.add(p.getId());
        }
        if (mantenduIds.isEmpty()) {
            dao.ezabatuGuztiak();
        } else {
            dao.ezabatuIdakEzDirenak(mantenduIds);
        }
        if (!zerrenda.isEmpty()) {
            dao.txertatuGuztiak(zerrenda);
        }
        return zerrenda.size();
    }

    /** partner elementu bat irakurri (id, izena, helbidea, komertzial_id). */
    private Partnerra partnerElementuaIrakurri(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "partner");
        long id = 0;
        String izena = "";
        String helbidea = "";
        long komertzialId = 1;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            String name = parser.getName();
            switch (name) {
                case "id":
                    id = parseLongSafe(testuaIrakurri(parser), 0);
                    break;
                case "izena":
                    izena = testuaIrakurri(parser);
                    break;
                case "helbidea":
                    helbidea = testuaIrakurri(parser);
                    break;
                case "komertzial_id":
                    komertzialId = parseLongSafe(testuaIrakurri(parser), 1);
                    break;
                default:
                    atalBatJauzi(parser);
                    break;
            }
        }
        String komertzialKodea = komertzialIdKodea.get(komertzialId);
        if (komertzialKodea == null && !komertzialIdKodea.isEmpty()) {
            komertzialKodea = komertzialIdKodea.values().iterator().next();
        }
        if (komertzialKodea == null) komertzialKodea = "";
        Partnerra p = new Partnerra();
        p.setId(id);
        p.setKodea(String.valueOf(id));
        p.setIzena(izena != null ? izena : "");
        p.setHelbidea(helbidea != null ? helbidea : "");
        p.setProbintzia(null);
        p.setKomertzialKodea(komertzialKodea);
        p.setSortutakoData(gaurkoData());
        return p;
    }

    /**
     * bazkideak.xml inportatu: sinkronizazioa (insert, update, delete). Partnerrak (id &lt; 1000) mantentzen dira.
     * Etiketak: bazkideak > bazkidea > NAN, izena, abizena, ...
     */
    public int bazkideakInportatu() throws IOException, XmlPullParserException {
        if (komertzialIdKodea.isEmpty()) {
            List<Komertziala> k = db.komertzialaDao().guztiak();
            for (int i = 0; i < k.size(); i++) {
                komertzialIdKodea.put((long) (i + 1), k.get(i).getKodea());
            }
        }
        List<Partnerra> zerrenda = new ArrayList<>();
        String lehenKodea = komertzialIdKodea.isEmpty() ? "" : komertzialIdKodea.values().iterator().next();
        try (InputStream is = assetsFitxategiaIreki("bazkideak.xml")) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(is, "UTF-8");
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, "bazkideak");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) continue;
                if ("bazkidea".equals(parser.getName())) {
                    Partnerra p = bazkideaElementuaIrakurri(parser, 1000L + zerrenda.size(), lehenKodea);
                    if (p != null) zerrenda.add(p);
                } else {
                    atalBatJauzi(parser);
                }
            }
        }
        PartnerraDao dao = db.partnerraDao();
        List<Long> mantenduIds = new ArrayList<>(zerrenda.stream().map(Partnerra::getId).collect(Collectors.toList()));
        for (Partnerra p : dao.guztiak()) {
            if (p.getId() < 1000) mantenduIds.add(p.getId());
        }
        if (mantenduIds.isEmpty()) {
            dao.ezabatuGuztiak();
        } else {
            dao.ezabatuIdakEzDirenak(mantenduIds);
        }
        if (!zerrenda.isEmpty()) {
            dao.txertatuGuztiak(zerrenda);
        }
        // Taula bazkideak bete (bazkideak.xml egitura)
        List<Bazkidea> bazkideakZerrenda = new ArrayList<>();
        try (InputStream is = assetsFitxategiaIreki("bazkideak.xml")) {
            XmlPullParser parser2 = Xml.newPullParser();
            parser2.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser2.setInput(is, "UTF-8");
            parser2.nextTag();
            parser2.require(XmlPullParser.START_TAG, null, "bazkideak");
            while (parser2.next() != XmlPullParser.END_TAG) {
                if (parser2.getEventType() != XmlPullParser.START_TAG) continue;
                if ("bazkidea".equals(parser2.getName())) {
                    Bazkidea bazkidea = bazkideaElementuaIrakurriBazkidea(parser2);
                    if (bazkidea != null) bazkideakZerrenda.add(bazkidea);
                } else {
                    atalBatJauzi(parser2);
                }
            }
        }
        BazkideaDao bazkideaDao = db.bazkideaDao();
        bazkideaDao.ezabatuGuztiak();
        if (!bazkideakZerrenda.isEmpty()) {
            bazkideaDao.txertatuGuztiak(bazkideakZerrenda);
        }
        return zerrenda.size();
    }

    /** bazkidea elementu bat irakurri taula bazkideak-erako (Bazkidea entitatea). */
    private Bazkidea bazkideaElementuaIrakurriBazkidea(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "bazkidea");
        String nan = null;
        String izena = null;
        String abizena = null;
        String telefonoZenbakia = null;
        String posta = null;
        String jaiotzeData = null;
        String argazkia = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            String name = parser.getName();
            if ("eskaerak".equals(name)) {
                atalBatJauzi(parser);
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
        b.setNan(nan != null ? nan : "");
        b.setIzena(izena != null ? izena : "");
        b.setAbizena(abizena != null ? abizena : "");
        b.setTelefonoZenbakia(telefonoZenbakia != null ? telefonoZenbakia : "");
        b.setPosta(posta != null ? posta : "");
        b.setJaiotzeData(jaiotzeData != null ? jaiotzeData : "");
        b.setArgazkia(argazkia != null ? argazkia : "");
        return b;
    }

    /** bazkidea elementu bat irakurri (NAN, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia); eskaerak atala jauzi. */
    private Partnerra bazkideaElementuaIrakurri(XmlPullParser parser, long idOffset, String komertzialKodea) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "bazkidea");
        String kodea = null;
        String izena = null;
        String abizena = null;
        String telefonoa = null;
        String posta = null;
        String jaiotzeData = null;
        String argazkia = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            String name = parser.getName();
            if ("eskaerak".equals(name)) {
                atalBatJauzi(parser);
                continue;
            }
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
                case "telefonoZenbakia":
                    telefonoa = testuaIrakurri(parser);
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
        if (kodea == null) kodea = "BAZ-" + idOffset;
        String izenOsoa = (izena != null ? izena : "") + " " + (abizena != null ? abizena : "").trim();
        Partnerra p = new Partnerra();
        p.setId(idOffset);
        p.setKodea(kodea);
        p.setIzena(izenOsoa);
        p.setHelbidea("");
        p.setProbintzia(null);
        p.setKomertzialKodea(komertzialKodea);
        p.setSortutakoData(gaurkoData());
        p.setTelefonoa(telefonoa);
        p.setPosta(posta);
        p.setJaiotzeData(jaiotzeData);
        p.setArgazkia(argazkia);
        return p;
    }

    /** Gaurko data yyyy-MM-dd formatuan (eguneko alta esportazioetarako). */
    private static String gaurkoData() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /**
     * loginak.xml inportatu: sinkronizazioa (insert, update, delete). Erabiltzailea gakoa.
     * Etiketak: loginak > erabiltzailea > id, email, pasahitza.
     */
    public int loginakInportatu() throws IOException, XmlPullParserException {
        List<Logina> zerrenda = new ArrayList<>();
        List<Komertziala> komertzialak = db.komertzialaDao().guztiak();
        for (int i = 0; i < komertzialak.size(); i++) {
            komertzialIdKodea.put((long) (i + 1), komertzialak.get(i).getKodea());
        }
        try (InputStream is = assetsFitxategiaIreki("loginak.xml")) {
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
     * Egitura bera: katalogoa > produktua > id, izena, prezioa, stock. Datu-basea berritu (upsert).
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
        List<String> xmlArtikuluKodeak = zerrenda.stream().map(Katalogoa::getArtikuluKodea).filter(a -> a != null && !a.isEmpty()).distinct().collect(Collectors.toList());
        KatalogoaDao dao = db.katalogoaDao();
        if (xmlArtikuluKodeak.isEmpty()) {
            dao.ezabatuGuztiak();
        } else {
            dao.ezabatuArtikuluKodeakEzDirenak(xmlArtikuluKodeak);
        }
        for (Katalogoa k : zerrenda) {
            Katalogoa exist = dao.artikuluaBilatu(k.getArtikuluKodea());
            if (exist != null) {
                if (k.getIrudiaIzena() == null || k.getIrudiaIzena().trim().isEmpty()) {
                    k.setIrudiaIzena(exist.getIrudiaIzena());
                }
                dao.eguneratu(k);
            } else {
                dao.txertatu(k);
            }
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
     * Assets-eko fitxategi bat inportatzen du izenaren arabera.
     * Onartutako fitxategiak: komertzialak.xml, partnerrak.xml, bazkideak.xml, loginak.xml, katalogoa.xml.
     * Beste motak ez badira onartzen, IllegalArgumentException jaurtitzen du.
     */
    public void inportatuFitxategia(String fitxategiIzena) throws IOException, XmlPullParserException {
        if (fitxategiIzena == null) fitxategiIzena = "";
        switch (fitxategiIzena) {
            case "komertzialak.xml":
                komertzialakInportatu();
                break;
            case "partnerrak.xml":
                partnerrakInportatu();
                break;
            case "bazkideak.xml":
                bazkideakInportatu();
                break;
            case "loginak.xml":
                loginakInportatu();
                break;
            case "katalogoa.xml":
                katalogoaInportatu();
                break;
            default:
                throw new IllegalArgumentException("Fitxategi mota hau ezin da inportatu: " + fitxategiIzena);
        }
    }

    /**
     * Assets-eko XML guztiak ordena egokian inportatzen ditu: komertzialak -> partnerrak -> bazkideak -> loginak -> katalogoa.
     * Upsert erabiltzen du gatazkak saihesteko.
     */
    public void guztiakInportatu() throws IOException, XmlPullParserException {
        komertzialakInportatu();
        partnerrakInportatu();
        bazkideakInportatu();
        loginakInportatu();
        katalogoaInportatu();
    }
}
