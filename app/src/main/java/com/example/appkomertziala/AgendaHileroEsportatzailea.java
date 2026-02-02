package com.example.appkomertziala;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Agenda;
import com.example.appkomertziala.db.eredua.Partnerra;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Agenda moduluaren hileroko esportazio duala.
 * Barne-memorian bi fitxategi sortzen ditu: agenda_hilero.xml (ofiziala, ordezkaritzek prozesatzeko)
 * eta agenda_oharra.txt (irakurgarria, komertzialaren oharrentzat).
 */
public class AgendaHileroEsportatzailea {

    private static final String ETIKETA = "AgendaHileroEsportatzailea";
    private static final String KODEKETA = "UTF-8";

    /** Barne-memorian idazteko fitxategi izen ofizialak. */
    public static final String FITXATEGI_XML = "agenda_hilero.xml";
    public static final String FITXATEGI_TXT = "agenda_oharra.txt";

    private final Context testuingurua;
    private final AppDatabase datuBasea;

    public AgendaHileroEsportatzailea(Context testuingurua) {
        this.testuingurua = testuingurua.getApplicationContext();
        this.datuBasea = AppDatabase.getInstance(this.testuingurua);
    }

    /**
     * Uneko hilabeteko bisitak XML egitura hierarkiko batean gorde.
     * Fitxategia: agenda_hilero.xml. Ordezkaritzek datu-basea eguneratzeko erabiltzen duten fitxategi ofiziala.
     *
     * @return true esportazioa ondo bukatu bada, false akatsen bat gertatu bada
     */
    public boolean agendaXMLSortu() {
        try {
            List<Agenda> hilabetekoak = datuBasea.agendaDao().hilabetearenBisitak();
            try (OutputStream irteera = testuingurua.openFileOutput(FITXATEGI_XML, Context.MODE_PRIVATE)) {
                XmlSerializer idazlea = Xml.newSerializer();
                idazlea.setOutput(irteera, KODEKETA);
                idazlea.startDocument(KODEKETA, true);
                idazlea.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                idazlea.startTag(null, "agenda_hilero");
                for (Agenda bisita : hilabetekoak) {
                    idazlea.startTag(null, "bisita");
                    nodoaIdatzi(idazlea, "id", String.valueOf(bisita.getId()));
                    nodoaIdatzi(idazlea, "bisita_data", hutsaEz(bisita.getBisitaData()));
                    nodoaIdatzi(idazlea, "partner_kodea", hutsaEz(bisita.getPartnerKodea()));
                    nodoaIdatzi(idazlea, "deskribapena", hutsaEz(bisita.getDeskribapena()));
                    nodoaIdatzi(idazlea, "egoera", hutsaEz(bisita.getEgoera()));
                    idazlea.endTag(null, "bisita");
                }
                idazlea.endTag(null, "agenda_hilero");
                idazlea.endDocument();
                idazlea.flush();
            }
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Agenda XML sortzean akatsa: barne-memorian idaztea huts egin du.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Agenda XML sortzean ustekabeko akatsa.", e);
            return false;
        }
    }

    /**
     * Uneko hilabeteko bisitak testu-fitxategi irakurgarri batean gorde.
     * Fitxategia: agenda_oharra.txt. Bisita bakoitza lerro batean: Data - Partnerra - Deskribapena - Egoera.
     * Komertzialaren oharrentzat kopia irakurgarria.
     *
     * @return true esportazioa ondo bukatu bada, false akatsen bat gertatu bada
     */
    public boolean agendaTXTSortu() {
        try {
            List<Agenda> hilabetekoak = datuBasea.agendaDao().hilabetearenBisitak();
            try (OutputStreamWriter idazlea = new OutputStreamWriter(
                    testuingurua.openFileOutput(FITXATEGI_TXT, Context.MODE_PRIVATE), StandardCharsets.UTF_8)) {
                for (Agenda bisita : hilabetekoak) {
                    String data = hutsaEz(bisita.getBisitaData());
                    String partnerra = partnerrarenIzena(bisita.getPartnerKodea());
                    String deskribapena = hutsaEz(bisita.getDeskribapena());
                    String egoera = hutsaEz(bisita.getEgoera());
                    idazlea.write(data + " - " + partnerra + " - " + deskribapena + " - " + egoera + "\n");
                }
                idazlea.flush();
            }
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Agenda TXT sortzean akatsa: barne-memorian idaztea huts egin du.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Agenda TXT sortzean ustekabeko akatsa.", e);
            return false;
        }
    }

    /** XML nodo bat idatzi (izen + edukia). */
    private void nodoaIdatzi(XmlSerializer idazlea, String izena, String edukia) throws IOException {
        idazlea.startTag(null, izena);
        if (edukia != null && !edukia.isEmpty()) {
            idazlea.text(edukia);
        }
        idazlea.endTag(null, izena);
    }

    /** Partnerraren izena itzuli kodearen arabera; hutsa kode bera. */
    private String partnerrarenIzena(String partnerKodea) {
        if (partnerKodea == null || partnerKodea.trim().isEmpty()) {
            return "";
        }
        Partnerra p = datuBasea.partnerraDao().kodeaBilatu(partnerKodea.trim());
        return p != null ? hutsaEz(p.getIzena()) : partnerKodea;
    }

    private static String hutsaEz(String s) {
        return s != null ? s : "";
    }

    /**
     * Uneko hilabetearen izena itzuli postaz bidaltzeko gaian erabiltzeko.
     * Formatua: "2025eko otsaila" (adibidez).
     */
    public static String unekoHilabetearenIzena() {
        Calendar cal = Calendar.getInstance();
        String[] hilak = {"urtarrila", "otsaila", "martxoa", "apirila", "maiatza", "ekaina",
                "uztaila", "abuztua", "iraila", "urria", "azaroa", "abendua"};
        int h = cal.get(Calendar.MONTH);
        int u = cal.get(Calendar.YEAR);
        return u + "eko " + (h >= 0 && h < hilak.length ? hilak[h] : "");
    }
}
