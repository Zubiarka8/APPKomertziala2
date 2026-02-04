package com.example.appkomertziala.agenda;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Agenda;
import com.example.appkomertziala.db.eredua.Bazkidea;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Agenda (hileroko bisitak) bi formatutan esportatzeko kudeatzailea.
 * Ofiziala: XML hierarkikoa (agenda.xml) — ordezkaritzak datu-basea eguneratzeko erabiltzen du.
 * Irakurgarria: testu-fitxategia (agenda.txt) — bisita bakoitza lerro batean (Data - Bazkidea - Deskribapena).
 * Esportazio eta segurtasun balidazioak: fitxategien egiaztapena (barne-memorian leku nahikoa), try-catch sendoak, log-ak euskaraz.
 */
public class AgendaEsportatzailea {

    private static final String ETIKETA_LOG = "AgendaEsportatzailea";
    private static final String KODEKETA = "UTF-8";

    /** Barne-memorian idazteko fitxategi izen ofizialak. */
    public static final String FITXATEGI_XML = "agenda.xml";
    public static final String FITXATEGI_TXT = "agenda.txt";

    /** Barne-memorian idatzi aurretik beharrezko gutxieneko lekua (byte). Fitxategien egiaztapena. */
    private static final long GUTXIENEKO_LEKU_LIBREA_BYTE = 512L * 1024L;

    private final Context testuingurua;
    private final AppDatabase datuBasea;

    public AgendaEsportatzailea(Context testuingurua) {
        this.testuingurua = testuingurua.getApplicationContext();
        this.datuBasea = AppDatabase.getInstance(this.testuingurua);
    }

    /**
     * Barne-memorian nahikoa leku libre dagoen egiaztatu. Fitxategien egiaztapena (esportazio segurua).
     *
     * @return true leku nahikoa badago, false bestela (log-ean euskaraz erregistratzen da)
     */
    private boolean barneMemorianLekuNahikoa() {
        File karpeta = testuingurua.getFilesDir();
        // If: karpeta null bada, fitxategiak non gorde ezin ditugun
        if (karpeta == null) {
            Log.w(ETIKETA_LOG, "Barne-memoria: fitxategi-karpeta ezin da lortu.");
            return false;
        }
        long libre = karpeta.getFreeSpace();
        // If: 512 KB baino gutxiago libre badago, ez idatzi (diskoa betea)
        if (libre < GUTXIENEKO_LEKU_LIBREA_BYTE) {
            Log.w(ETIKETA_LOG, "Barne-memorian ez dago nahikoa lekurik fitxategia idazteko. Libre: " + libre + " byte, beharrezko gutxienekoa: " + GUTXIENEKO_LEKU_LIBREA_BYTE + " byte.");
            return false;
        }
        return true;
    }

    /**
     * AGENDA taulako (agenda_bisitak) hilabeteko bisitak XML egitura hierarkiko batean gorde.
     * SEGURTASUNA: Uneko komertzialaren bisitak bakarrik esportatzen dira.
     * Fitxategia: agenda.xml. Fitxategien egiaztapena: barne-memorian leku nahikoa; errore-kudeaketa (try-catch) log-ean euskaraz.
     *
     * @return true esportazioa ondo bukatu bada, false akatsen bat gertatu bada (log-ean euskaraz)
     */
    public boolean agendaXMLSortu() {
        // If: leku nahikoa ez badago, ez hasi idazketan
        if (!barneMemorianLekuNahikoa()) {
            Log.e(ETIKETA_LOG, "Agenda XML ez sortu: barne-memorian ez dago nahikoa lekurik.");
            return false;
        }
        try {
            // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
            com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                new com.example.appkomertziala.segurtasuna.SessionManager(testuingurua);
            String komertzialKodea = sessionManager.getKomertzialKodea();
            
            // If: saioa hasi gabe badago, kodea null/hutsa izango da
            if (komertzialKodea == null || komertzialKodea.isEmpty()) {
                Log.e(ETIKETA_LOG, "Agenda XML ez sortu: saioa ez dago hasita.");
                return false;
            }
            
            // SEGURTASUNA: Uneko komertzialaren bisitak bakarrik esportatu
            List<Agenda> hilabetekoak = datuBasea.agendaDao().hilabetearenBisitak(komertzialKodea);
            try (OutputStream irteera = testuingurua.openFileOutput(FITXATEGI_XML, Context.MODE_PRIVATE)) {
                XmlSerializer idazlea = Xml.newSerializer();
                idazlea.setOutput(irteera, KODEKETA);
                idazlea.startDocument(KODEKETA, true);
                idazlea.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                idazlea.startTag(null, "agenda");
                // For: bisita bakoitza XML nodo bihurtu (<bisita>...</bisita>)
                for (Agenda a : hilabetekoak) {
                    idazlea.startTag(null, "bisita");
                    nodoaIdatzi(idazlea, "bisita_data", hutsaEz(a.getBisitaData()));
                    nodoaIdatzi(idazlea, "bazkidea_kodea", hutsaEz(a.getBazkideaKodea()));
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
            Log.e(ETIKETA_LOG, "Agenda XML sortzean akatsa: fitxategia idaztea huts egin du. Salbuespena: " + (e.getMessage() != null ? e.getMessage() : "ezezaguna"), e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA_LOG, "Agenda XML sortzean ustekabeko akatsa. Salbuespenen kudeaketa: " + (e.getMessage() != null ? e.getMessage() : "ezezaguna"), e);
            return false;
        }
    }

    /**
     * Hileroko bisitak (agenda_bisitak) testu-fitxategi irakurgarri batean gorde.
     * SEGURTASUNA: Uneko komertzialaren bisitak bakarrik esportatzen dira.
     * Fitxategia: agenda.txt. Fitxategien egiaztapena eta errore-kudeaketa (try-catch) log-ean euskaraz.
     *
     * @return true esportazioa ondo bukatu bada, false akatsen bat gertatu bada
     */
    public boolean agendaTXTSortu() {
        // If: leku nahikoa ez badago, ez idatzi
        if (!barneMemorianLekuNahikoa()) {
            Log.e(ETIKETA_LOG, "Agenda TXT ez sortu: barne-memorian ez dago nahikoa lekurik.");
            return false;
        }
        try {
            // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
            com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                new com.example.appkomertziala.segurtasuna.SessionManager(testuingurua);
            String komertzialKodea = sessionManager.getKomertzialKodea();
            
            // If: saioa hasi gabe badago, ezin dugu bisitak esportatu
            if (komertzialKodea == null || komertzialKodea.isEmpty()) {
                Log.e(ETIKETA_LOG, "Agenda TXT ez sortu: saioa ez dago hasita.");
                return false;
            }
            
            // SEGURTASUNA: Uneko komertzialaren bisitak bakarrik esportatu (GUZTIAK, ez bakarrik hilabetekoak)
            List<Agenda> hilabetekoak = datuBasea.agendaDao().getVisitsByKomertzial(komertzialKodea);
            Log.d(ETIKETA_LOG, "Agenda TXT sortzen: " + hilabetekoak.size() + " bisita aurkitu dira komertzial kodea: " + komertzialKodea);
            
            // Fitxategiaren bidea lortu
            File fitxategia = new File(testuingurua.getFilesDir(), FITXATEGI_TXT);
            String bidea = fitxategia.getAbsolutePath();
            Log.d(ETIKETA_LOG, "Agenda TXT fitxategia sortzen: " + bidea);
            
            try (OutputStreamWriter idazlea = new OutputStreamWriter(
                    testuingurua.openFileOutput(FITXATEGI_TXT, Context.MODE_PRIVATE), StandardCharsets.UTF_8)) {
                int lerroKopurua = 0;
                // For: bisita bakoitzarentzat lerro bat idatzi (ID | Data | Ordua | ...)
                for (Agenda a : hilabetekoak) {
                    // Datuak bildu
                    long id = a.getId();
                    String data = hutsaEz(a.getBisitaData());
                    String ordua = hutsaEz(a.getOrdua());
                    String bisitarenKomertzialKodea = hutsaEz(a.getKomertzialKodea());
                    String bisitarenBazkideaKodea = hutsaEz(a.getBazkideaKodea());
                    Long bazkideaId = a.getBazkideaId();
                    Long komertzialaId = a.getKomertzialaId();
                    String bazkidea = bazkidearenIzena(a.getBazkideaKodea());
                    String deskribapena = hutsaEz(a.getDeskribapena());
                    String egoera = hutsaEz(a.getEgoera());
                    
                    // Formatua: ID | Data | Ordua | KomertzialKodea | KomertzialaId | BazkideaKodea | BazkideaId | Bazkidea | Deskribapena | Egoera
                    StringBuilder lerroa = new StringBuilder();
                    lerroa.append("ID: ").append(id);
                    lerroa.append(" | Data: ").append(data);
                    // If: eremu bakoitza hutsik ez bada bakarrik gehitzen da lerroari
                    if (!ordua.isEmpty()) {
                        lerroa.append(" | Ordua: ").append(ordua);
                    }
                    if (!bisitarenKomertzialKodea.isEmpty()) {
                        lerroa.append(" | KomertzialKodea: ").append(bisitarenKomertzialKodea);
                    }
                    if (komertzialaId != null) {
                        lerroa.append(" | KomertzialaId: ").append(komertzialaId);
                    }
                    if (!bisitarenBazkideaKodea.isEmpty()) {
                        lerroa.append(" | BazkideaKodea: ").append(bisitarenBazkideaKodea);
                    }
                    if (bazkideaId != null) {
                        lerroa.append(" | BazkideaId: ").append(bazkideaId);
                    }
                    if (!bazkidea.isEmpty()) {
                        lerroa.append(" | Bazkidea: ").append(bazkidea);
                    }
                    if (!deskribapena.isEmpty()) {
                        lerroa.append(" | Deskribapena: ").append(deskribapena);
                    }
                    if (!egoera.isEmpty()) {
                        lerroa.append(" | Egoera: ").append(egoera);
                    }
                    lerroa.append("\n");
                    
                    idazlea.write(lerroa.toString());
                    lerroKopurua++;
                }
                idazlea.flush();
                Log.d(ETIKETA_LOG, "Agenda TXT ondo sortu da: " + lerroKopurua + " lerro idatzi dira");
            }
            
            // If: fitxategia ondo sortu bada true, bestela false
            if (fitxategia.exists()) {
                long tamaina = fitxategia.length();
                Log.d(ETIKETA_LOG, "Agenda TXT fitxategia existitzen da: " + bidea + ", tamaina: " + tamaina + " byte");
            return true;
            } else {
                Log.e(ETIKETA_LOG, "Agenda TXT fitxategia ez da sortu: " + bidea);
                return false;
            }
        } catch (IOException e) {
            Log.e(ETIKETA_LOG, "Agenda TXT sortzean akatsa: fitxategia idaztea huts egin du. Salbuespena: " + (e.getMessage() != null ? e.getMessage() : "ezezaguna"), e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA_LOG, "Agenda TXT sortzean ustekabeko akatsa. Salbuespenen kudeaketa: " + (e.getMessage() != null ? e.getMessage() : "ezezaguna"), e);
            return false;
        }
    }

    /** XML nodo bat idatzi (izen + edukia). */
    private void nodoaIdatzi(XmlSerializer idazlea, String izena, String edukia) throws IOException {
        idazlea.startTag(null, izena);
        // If: edukia hutsik ez bada bakarrik idatzi (XML hutsak saihesteko)
        if (edukia != null && !edukia.isEmpty()) {
            idazlea.text(edukia);
        }
        idazlea.endTag(null, izena);
    }

    /** Bazkidearen izena itzuli kodearen arabera (Bazkidea taula). */
    private String bazkidearenIzena(String bazkideaKodea) {
        // If: kodea hutsik badago, ezer ez itzuli
        if (bazkideaKodea == null || bazkideaKodea.trim().isEmpty()) {
            return "";
        }
        Bazkidea b = datuBasea.bazkideaDao().nanBilatu(bazkideaKodea.trim());
        // If: bazkidea aurkitu bada, izena + abizena (edo NAN) itzuli
        if (b != null) {
            String izena = (b.getIzena() != null ? b.getIzena().trim() : "") + 
                           (b.getAbizena() != null && !b.getAbizena().trim().isEmpty() ? " " + b.getAbizena().trim() : "");
            return izena.isEmpty() ? (b.getNan() != null ? b.getNan() : "") : izena;
        }
        // Bazkidea ez bada aurkitu, kodea bera itzuli
        return bazkideaKodea;
    }

    private static String hutsaEz(String s) {
        return s != null ? s : "";
    }
}
