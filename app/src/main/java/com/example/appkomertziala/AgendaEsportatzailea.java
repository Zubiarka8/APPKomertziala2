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
import java.util.List;

/**
 * Agenda (hileroko bisitak) bi formatutan esportatzeko kudeatzailea.
 * Ofiziala: XML hierarkikoa (agenda.xml) — ordezkaritzak datu-basea eguneratzeko erabiltzen du.
 * Irakurgarria: testu-fitxategia (agenda.txt) — bisita bakoitza lerro batean (Data - Partnerra - Deskribapena).
 */
public class AgendaEsportatzailea {

    private static final String ETIKETA = "AgendaEsportatzailea";
    private static final String KODEKETA = "UTF-8";

    /** Barne-memorian idazteko fitxategi izen ofizialak. */
    public static final String FITXATEGI_XML = "agenda.xml";
    public static final String FITXATEGI_TXT = "agenda.txt";

    private final Context testuingurua;
    private final AppDatabase datuBasea;

    public AgendaEsportatzailea(Context testuingurua) {
        this.testuingurua = testuingurua.getApplicationContext();
        this.datuBasea = AppDatabase.getInstance(this.testuingurua);
    }

    /**
     * AGENDA taulako (agenda_bisitak) hilabeteko bisitak XML egitura hierarkiko batean gorde.
     * Fitxategia: agenda.xml. Soilik agenda (bisitak), ez eskaera/pedidoak.
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
                idazlea.startTag(null, "agenda");
                for (Agenda a : hilabetekoak) {
                    idazlea.startTag(null, "bisita");
                    nodoaIdatzi(idazlea, "bisita_data", hutsaEz(a.getBisitaData()));
                    nodoaIdatzi(idazlea, "partner_kodea", hutsaEz(a.getPartnerKodea()));
                    nodoaIdatzi(idazlea, "deskribapena", hutsaEz(a.getDeskribapena()));
                    nodoaIdatzi(idazlea, "egoera", hutsaEz(a.getEgoera()));
                    idazlea.endTag(null, "bisita");
                }
                idazlea.endTag(null, "agenda");
                idazlea.endDocument();
                idazlea.flush();
            }
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Agenda XML sortzean akatsa: fitxategia idaztea huts egin du.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Agenda XML sortzean ustekabeko akatsa.", e);
            return false;
        }
    }

    /**
     * Hileroko bisitak (agenda_bisitak) testu-fitxategi irakurgarri batean gorde.
     * Fitxategia: agenda.txt. Bisita bakoitza lerro batean: Data - Partnerra - Deskribapena.
     *
     * @return true esportazioa ondo bukatu bada, false akatsen bat gertatu bada
     */
    public boolean agendaTXTSortu() {
        try {
            List<Agenda> hilabetekoak = datuBasea.agendaDao().hilabetearenBisitak();
            try (OutputStreamWriter idazlea = new OutputStreamWriter(
                    testuingurua.openFileOutput(FITXATEGI_TXT, Context.MODE_PRIVATE), StandardCharsets.UTF_8)) {
                for (Agenda a : hilabetekoak) {
                    String data = hutsaEz(a.getBisitaData());
                    String partnerra = partnerrarenIzena(a.getPartnerKodea());
                    String deskribapena = hutsaEz(a.getDeskribapena());
                    idazlea.write(data + " - " + partnerra + " - " + deskribapena + "\n");
                }
                idazlea.flush();
            }
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Agenda TXT sortzean akatsa: fitxategia idaztea huts egin du.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Agenda TXT sortzean ustekabeko akatsa.", e);
            return false;
        }
    }

    private void nodoaIdatzi(XmlSerializer idazlea, String izena, String edukia) throws IOException {
        idazlea.startTag(null, izena);
        if (edukia != null && !edukia.isEmpty()) {
            idazlea.text(edukia);
        }
        idazlea.endTag(null, izena);
    }

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
}
