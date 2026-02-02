package com.example.appkomertziala.xml;

import android.content.Context;
import android.util.Xml;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.example.appkomertziala.db.eredua.EskaeraGoiburua;
import com.example.appkomertziala.db.eredua.EskaeraXehetasuna;
import com.example.appkomertziala.db.eredua.Katalogoa;
import com.example.appkomertziala.db.eredua.Komertziala;
import com.example.appkomertziala.db.eredua.Partnerra;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Room datu-baseko datuak XML fitxategietara esportatzeko kudeatzailea.
 * Barne-memorian idazten du (Context.openFileOutput). XmlSerializer erabiltzen du
 * egitura hierarkiko eta garbia bermatzeko.
 */
public class XMLEsportatzailea {

    private static final String KODEKETA = "UTF-8";

    private final Context testuingurua;
    private final AppDatabase datuBasea;

    public XMLEsportatzailea(Context testuingurua) {
        this.testuingurua = testuingurua.getApplicationContext();
        this.datuBasea = AppDatabase.getInstance(this.testuingurua);
    }

    /**
     * PARTNERRA taulatik eguneko alta duten bazkide zerrenda erauzi eta bazkide_berriak.xml fitxategian gorde.
     * Eguneroko txostena: centralera egunero bidaltzeko soilik eguneko erregistro berriak hautatzen dira (sortutakoData = gaur).
     * Emaitza bakoitza XML nodo baten bihurtzen da: bazkide_berriak > bazkidea > id, kodea, izena, helbidea, probintzia, komertzialKodea.
     */
    public void bazkideBerriakEsportatu() throws IOException {
        List<Partnerra> zerrenda = datuBasea.partnerraDao().egunekoAltaGuztiak();
        String fitxategiIzena = "bazkide_berriak.xml";
        try (OutputStream irteera = testuingurua.openFileOutput(fitxategiIzena, Context.MODE_PRIVATE)) {
            XmlSerializer idazlea = Xml.newSerializer();
            idazlea.setOutput(irteera, KODEKETA);
            idazlea.startDocument(KODEKETA, true);
            idazlea.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            // Erroote nodoa: bazkide_berriak
            idazlea.startTag(null, "bazkide_berriak");
            for (Partnerra p : zerrenda) {
                idazlea.startTag(null, "bazkidea");
                nodoaIdatzi(idazlea, "id", String.valueOf(p.getId()));
                nodoaIdatzi(idazlea, "kodea", hutsaEz(p.getKodea()));
                nodoaIdatzi(idazlea, "izena", hutsaEz(p.getIzena()));
                nodoaIdatzi(idazlea, "helbidea", hutsaEz(p.getHelbidea()));
                nodoaIdatzi(idazlea, "probintzia", hutsaEz(p.getProbintzia()));
                nodoaIdatzi(idazlea, "komertzialKodea", hutsaEz(p.getKomertzialKodea()));
                idazlea.endTag(null, "bazkidea");
            }
            idazlea.endTag(null, "bazkide_berriak");
            idazlea.endDocument();
            idazlea.flush();
        }
    }

    /**
     * Komertzialak taulako datu guztiak komertzialak.xml fitxategian gorde (assets/komertzialak.xml egitura).
     * Nodoa: komertzialak > komertziala > NAN (kodea), izena.
     */
    public void komertzialakEsportatu() throws IOException {
        List<Komertziala> zerrenda = datuBasea.komertzialaDao().guztiak();
        String fitxategiIzena = "komertzialak.xml";
        try (OutputStream irteera = testuingurua.openFileOutput(fitxategiIzena, Context.MODE_PRIVATE)) {
            XmlSerializer idazlea = Xml.newSerializer();
            idazlea.setOutput(irteera, KODEKETA);
            idazlea.startDocument(KODEKETA, true);
            idazlea.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            idazlea.startTag(null, "komertzialak");
            for (Komertziala k : zerrenda) {
                idazlea.startTag(null, "komertziala");
                nodoaIdatzi(idazlea, "NAN", hutsaEz(k.getKodea()));
                nodoaIdatzi(idazlea, "izena", hutsaEz(k.getIzena()));
                idazlea.endTag(null, "komertziala");
            }
            idazlea.endTag(null, "komertzialak");
            idazlea.endDocument();
            idazlea.flush();
        }
    }

    /**
     * Bazkideak taulako datu guztiak bazkideak.xml fitxategian gorde (assets/bazkideak.xml egitura).
     * Nodoa: bazkideak > bazkidea > NAN, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia > eskaerak.
     */
    public void bazkideakEsportatu() throws IOException {
        bazkideakEsportatu(datuBasea.bazkideaDao().guztiak());
    }

    /**
     * Emandako bazkide zerrenda bazkideak.xml fitxategian idatzi (formulario bat gorde/ezabatu baino lehen XML eguneratzeko).
     * Nodoa: bazkideak > bazkidea > NAN, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia > eskaerak.
     */
    public void bazkideakEsportatu(List<Bazkidea> zerrenda) throws IOException {
        if (zerrenda == null) zerrenda = new ArrayList<>();
        String fitxategiIzena = "bazkideak.xml";
        try (OutputStream irteera = testuingurua.openFileOutput(fitxategiIzena, Context.MODE_PRIVATE)) {
            XmlSerializer idazlea = Xml.newSerializer();
            idazlea.setOutput(irteera, KODEKETA);
            idazlea.startDocument(KODEKETA, true);
            idazlea.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            idazlea.startTag(null, "bazkideak");
            for (Bazkidea b : zerrenda) {
                idazlea.startTag(null, "bazkidea");
                nodoaIdatzi(idazlea, "NAN", hutsaEz(b.getNan()));
                nodoaIdatzi(idazlea, "izena", hutsaEz(b.getIzena()));
                nodoaIdatzi(idazlea, "abizena", hutsaEz(b.getAbizena()));
                nodoaIdatzi(idazlea, "telefonoZenbakia", hutsaEz(b.getTelefonoZenbakia()));
                nodoaIdatzi(idazlea, "posta", hutsaEz(b.getPosta()));
                nodoaIdatzi(idazlea, "jaiotzeData", dataFormatuaBazkideak(hutsaEz(b.getJaiotzeData())));
                nodoaIdatzi(idazlea, "argazkia", hutsaEz(b.getArgazkia()));
                idazlea.startTag(null, "eskaerak");
                idazlea.endTag(null, "eskaerak");
                idazlea.endTag(null, "bazkidea");
            }
            idazlea.endTag(null, "bazkideak");
            idazlea.endDocument();
            idazlea.flush();
        }
    }

    /**
     * Eguneko lehen eskaera bidalketa egituran eskaera_berriak.xml fitxategian gorde.
     * Egitura (bidalketa_2_20260130140648.xml bezala): bidalketa > BidalketaId, Kodea, Helmuga, Data, Amaituta, Lerroak > Lerro.
     */
    public void eskaeraBerriakEsportatu() throws IOException {
        List<EskaeraGoiburua> goiburuak = datuBasea.eskaeraGoiburuaDao().egunekoEskaerak();
        String fitxategiIzena = "eskaera_berriak.xml";
        try (OutputStream irteera = testuingurua.openFileOutput(fitxategiIzena, Context.MODE_PRIVATE)) {
            XmlSerializer idazlea = Xml.newSerializer();
            idazlea.setOutput(irteera, KODEKETA);
            idazlea.startDocument(KODEKETA, true);
            idazlea.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            idazlea.startTag(null, "bidalketa");
            if (!goiburuak.isEmpty()) {
                EskaeraGoiburua goi = goiburuak.get(0);
                nodoaIdatzi(idazlea, "BidalketaId", "1");
                nodoaIdatzi(idazlea, "Kodea", hutsaEz(goi.getZenbakia()));
                nodoaIdatzi(idazlea, "Helmuga", hutsaEz(goi.getOrdezkaritza()));
                nodoaIdatzi(idazlea, "Data", dataFormatuaBidalketa(hutsaEz(goi.getData())));
                nodoaIdatzi(idazlea, "Amaituta", "true");
                List<EskaeraXehetasuna> xehetasunak = datuBasea.eskaeraXehetasunaDao().eskaerarenXehetasunak(goi.getZenbakia());
                idazlea.startTag(null, "Lerroak");
                for (EskaeraXehetasuna x : xehetasunak) {
                    Katalogoa prod = datuBasea.katalogoaDao().artikuluaBilatu(x.getArtikuluKodea());
                    String izenaProd = prod != null ? hutsaEz(prod.getIzena()) : hutsaEz(x.getArtikuluKodea());
                    idazlea.startTag(null, "Lerro");
                    nodoaIdatzi(idazlea, "ProductoId", hutsaEz(x.getArtikuluKodea()));
                    nodoaIdatzi(idazlea, "Izena", izenaProd);
                    nodoaIdatzi(idazlea, "Eskatuta", String.valueOf(x.getKantitatea()));
                    nodoaIdatzi(idazlea, "Bidalita", String.valueOf(x.getKantitatea()));
                    nodoaIdatzi(idazlea, "PrezioUnit", String.valueOf(x.getPrezioa()));
                    idazlea.endTag(null, "Lerro");
                }
                idazlea.endTag(null, "Lerroak");
            }
            idazlea.endTag(null, "bidalketa");
            idazlea.endDocument();
            idazlea.flush();
        }
    }

    /**
     * Hileroko laburpena: agendan erregistratutako bisita (eskaera) guztiak agenda.xml fitxategi bakar batean gorde centralera bidaltzeko.
     * Hilero exekutatu behar da; uneko hilabeteko eskaera goiburuak data atributuaren arabera iragazten dira.
     * Nodoa: agenda > bisita > zenbakia, data, komertzialKodea, ordezkaritza, partnerKodea.
     */
    public void agendaEsportatu() throws IOException {
        List<EskaeraGoiburua> hilabetekoak = datuBasea.eskaeraGoiburuaDao().hilabetekoEskaerak();
        String fitxategiIzena = "agenda.xml";
        try (OutputStream irteera = testuingurua.openFileOutput(fitxategiIzena, Context.MODE_PRIVATE)) {
            XmlSerializer idazlea = Xml.newSerializer();
            idazlea.setOutput(irteera, KODEKETA);
            idazlea.startDocument(KODEKETA, true);
            idazlea.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            idazlea.startTag(null, "agenda");
            for (EskaeraGoiburua goi : hilabetekoak) {
                idazlea.startTag(null, "bisita");
                nodoaIdatzi(idazlea, "zenbakia", hutsaEz(goi.getZenbakia()));
                nodoaIdatzi(idazlea, "data", hutsaEz(goi.getData()));
                nodoaIdatzi(idazlea, "komertzialKodea", hutsaEz(goi.getKomertzialKodea()));
                nodoaIdatzi(idazlea, "ordezkaritza", hutsaEz(goi.getOrdezkaritza()));
                nodoaIdatzi(idazlea, "partnerKodea", hutsaEz(goi.getPartnerKodea()));
                idazlea.endTag(null, "bisita");
            }
            idazlea.endTag(null, "agenda");
            idazlea.endDocument();
            idazlea.flush();
        }
    }

    /**
     * Katalogoa taulako artikulu guztiak katalogoa.xml fitxategian gorde (astero esportatzeko / Gmail bidez bidaltzeko).
     * Nodoa: katalogoa > produktua > id, izena, prezioa, stock, irudia_path (inportazioarekin bateragarria).
     */
    public void katalogoaEsportatu() throws IOException {
        List<Katalogoa> zerrenda = datuBasea.katalogoaDao().guztiak();
        String fitxategiIzena = "katalogoa.xml";
        try (OutputStream irteera = testuingurua.openFileOutput(fitxategiIzena, Context.MODE_PRIVATE)) {
            XmlSerializer idazlea = Xml.newSerializer();
            idazlea.setOutput(irteera, KODEKETA);
            idazlea.startDocument(KODEKETA, true);
            idazlea.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            idazlea.startTag(null, "katalogoa");
            for (Katalogoa k : zerrenda) {
                idazlea.startTag(null, "produktua");
                nodoaIdatzi(idazlea, "id", hutsaEz(k.getArtikuluKodea()));
                nodoaIdatzi(idazlea, "izena", hutsaEz(k.getIzena()));
                nodoaIdatzi(idazlea, "prezioa", String.valueOf(k.getSalmentaPrezioa()));
                nodoaIdatzi(idazlea, "stock", String.valueOf(k.getStock()));
                nodoaIdatzi(idazlea, "irudia_path", hutsaEz(k.getIrudiaIzena()));
                idazlea.endTag(null, "produktua");
            }
            idazlea.endTag(null, "katalogoa");
            idazlea.endDocument();
            idazlea.flush();
        }
    }

    // ----- TXT esportazioak (testu laua, Gmail eranskin gisa bidaltzeko) -----

    /** Eguneko bazkide berriak bazkide_berriak.txt fitxategian (testu laua). */
    public void bazkideBerriakEsportatuTxt() throws IOException {
        List<Partnerra> zerrenda = datuBasea.partnerraDao().egunekoAltaGuztiak();
        String fitxategiIzena = "bazkide_berriak.txt";
        try (Writer idazlea = new OutputStreamWriter(testuingurua.openFileOutput(fitxategiIzena, Context.MODE_PRIVATE), StandardCharsets.UTF_8)) {
            idazlea.write("=== BAZKIDE BERRIAK (eguneko altak) ===\n\n");
            for (Partnerra p : zerrenda) {
                idazlea.write("Id: " + p.getId() + "\n");
                idazlea.write("Kodea: " + hutsaEz(p.getKodea()) + "\n");
                idazlea.write("Izena: " + hutsaEz(p.getIzena()) + "\n");
                idazlea.write("Helbidea: " + hutsaEz(p.getHelbidea()) + "\n");
                idazlea.write("Probintzia: " + hutsaEz(p.getProbintzia()) + "\n");
                idazlea.write("KomertzialKodea: " + hutsaEz(p.getKomertzialKodea()) + "\n");
                idazlea.write("---\n");
            }
            idazlea.flush();
        }
    }

    /** Eguneko eskaera berriak eskaera_berriak.txt fitxategian (testu laua). */
    public void eskaeraBerriakEsportatuTxt() throws IOException {
        List<EskaeraGoiburua> goiburuak = datuBasea.eskaeraGoiburuaDao().egunekoEskaerak();
        String fitxategiIzena = "eskaera_berriak.txt";
        try (Writer idazlea = new OutputStreamWriter(testuingurua.openFileOutput(fitxategiIzena, Context.MODE_PRIVATE), StandardCharsets.UTF_8)) {
            idazlea.write("=== ESKAERA BERRIAK (egunekoak) ===\n\n");
            for (EskaeraGoiburua goi : goiburuak) {
                idazlea.write("Eskaera zenbakia: " + hutsaEz(goi.getZenbakia()) + " | Data: " + hutsaEz(goi.getData()) + "\n");
                idazlea.write("KomertzialKodea: " + hutsaEz(goi.getKomertzialKodea()) + " | Ordezkaritza: " + hutsaEz(goi.getOrdezkaritza()) + " | PartnerKodea: " + hutsaEz(goi.getPartnerKodea()) + "\n");
                List<EskaeraXehetasuna> xehetasunak = datuBasea.eskaeraXehetasunaDao().eskaerarenXehetasunak(goi.getZenbakia());
                for (EskaeraXehetasuna x : xehetasunak) {
                    idazlea.write("  - " + hutsaEz(x.getArtikuluKodea()) + " | Kantitatea: " + x.getKantitatea() + " | Prezioa: " + x.getPrezioa() + "\n");
                }
                idazlea.write("---\n");
            }
            idazlea.flush();
        }
    }

    /** Hilabeteko agenda agenda.txt fitxategian (testu laua). */
    public void agendaEsportatuTxt() throws IOException {
        List<EskaeraGoiburua> hilabetekoak = datuBasea.eskaeraGoiburuaDao().hilabetekoEskaerak();
        String fitxategiIzena = "agenda.txt";
        try (Writer idazlea = new OutputStreamWriter(testuingurua.openFileOutput(fitxategiIzena, Context.MODE_PRIVATE), StandardCharsets.UTF_8)) {
            idazlea.write("=== AGENDA (hilabeteko bisitak) ===\n\n");
            for (EskaeraGoiburua goi : hilabetekoak) {
                idazlea.write("Zenbakia: " + hutsaEz(goi.getZenbakia()) + " | Data: " + hutsaEz(goi.getData()) + "\n");
                idazlea.write("KomertzialKodea: " + hutsaEz(goi.getKomertzialKodea()) + " | Ordezkaritza: " + hutsaEz(goi.getOrdezkaritza()) + " | PartnerKodea: " + hutsaEz(goi.getPartnerKodea()) + "\n");
                idazlea.write("---\n");
            }
        }
    }

    /** Katalogoa katalogoa.txt fitxategian (testu laua). */
    public void katalogoaEsportatuTxt() throws IOException {
        List<Katalogoa> zerrenda = datuBasea.katalogoaDao().guztiak();
        String fitxategiIzena = "katalogoa.txt";
        try (Writer idazlea = new OutputStreamWriter(testuingurua.openFileOutput(fitxategiIzena, Context.MODE_PRIVATE), StandardCharsets.UTF_8)) {
            idazlea.write("=== KATALOGOA ===\n\n");
            for (Katalogoa k : zerrenda) {
                idazlea.write("ArtikuluKodea: " + hutsaEz(k.getArtikuluKodea()) + " | Izena: " + hutsaEz(k.getIzena()) + " | Prezioa: " + k.getSalmentaPrezioa() + " | Stock: " + k.getStock() + "\n");
            }
        }
    }

    /**
     * XML idazleari elementu bat idazten dio: startTag, testua, endTag.
     * Atributurik ez duen nodo sinplea (testu-edukia barruan).
     */
    private void nodoaIdatzi(XmlSerializer idazlea, String izena, String edukia) throws IOException {
        idazlea.startTag(null, izena);
        if (edukia != null && !edukia.isEmpty()) {
            idazlea.text(edukia);
        }
        idazlea.endTag(null, izena);
    }

    /** String nulua edo hutsa "" bihurtzen du (XML-n hutsik ez uzteko). */
    private static String hutsaEz(String s) {
        return s != null ? s : "";
    }

    /** Data yyyy-MM-dd edo yyyy-MM-dd HH:mm → yyyy/MM/dd (bazkideak.xml eta bidalketa formatua). */
    private static String dataFormatuaBazkideak(String data) {
        if (data == null || data.isEmpty()) return "";
        String zatia = data.contains(" ") ? data.substring(0, data.indexOf(" ")) : data;
        return zatia.replace("-", "/");
    }

    private static String dataFormatuaBidalketa(String data) {
        return dataFormatuaBazkideak(data);
    }
}
