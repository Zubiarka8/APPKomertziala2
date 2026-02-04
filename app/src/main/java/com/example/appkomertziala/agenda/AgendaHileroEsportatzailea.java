package com.example.appkomertziala.agenda;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Agenda;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.example.appkomertziala.db.eredua.Komertziala;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Agenda moduluaren hileroko esportazioa (XML, TXT eta CSV formatuak).
 * Hilabete zehatz baten bisitak esportatzen ditu ordezkaritzara bidaltzeko.
 * Java modernoa erabiliz, memoriaren kudeaketa gupidagabea.
 */
public class AgendaHileroEsportatzailea {

    private static final String ETIKETA = "AgendaHileroEsportatzailea";
    private static final String KODEKETA = "UTF-8";
    private static final long GUTXIENEKO_LEKU_LIBREA_BYTE = 512L * 1024L;

    /** Barne-memorian idazteko fitxategi izenak. */
    public static final String FITXATEGI_XML = "agenda.xml";
    public static final String FITXATEGI_TXT = "agenda.txt";
    public static final String FITXATEGI_CSV = "agenda.csv";

    private final Context testuingurua;
    private final AppDatabase datuBasea;

    public AgendaHileroEsportatzailea(Context testuingurua) {
        this.testuingurua = testuingurua.getApplicationContext();
        this.datuBasea = AppDatabase.getInstance(this.testuingurua);
    }

    /**
     * Uneko hilabeteko bisitak esportatu (XML, TXT eta CSV).
     * SEGURTASUNA: Uneko komertzialaren bisitak bakarrik esportatzen dira.
     * @return true esportazioa ondo bukatu bada, false bestela
     */
    public boolean esportatuUnekoHilabetea() {
        // Uneko data lortu: urtea (yyyy) eta hilabetea (01-12)
        Calendar calendar = Calendar.getInstance();
        String urtea = String.valueOf(calendar.get(Calendar.YEAR));
        String hilabetea = String.format(Locale.US, "%02d", calendar.get(Calendar.MONTH) + 1);
        return esportatuHilabetea(urtea, hilabetea);
    }

    /**
     * Hilabete zehatz baten bisitak esportatu (XML, TXT eta CSV).
     * SEGURTASUNA: Uneko komertzialaren bisitak bakarrik esportatzen dira.
     * @param urtea Urtea (yyyy)
     * @param hilabetea Hilabetea (MM, 01-12)
     * @return true esportazioa ondo bukatu bada, false bestela
     */
    public boolean esportatuHilabetea(String urtea, String hilabetea) {
        // If: leku nahikoa ez badago, ez hasi
        if (!barneMemorianLekuNahikoa()) {
            Log.e(ETIKETA, "Esportazioa ez egin: barne-memorian ez dago nahikoa lekurik.");
            return false;
        }

        try {
            // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
            com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                new com.example.appkomertziala.segurtasuna.SessionManager(testuingurua);
            String komertzialKodea = sessionManager.getKomertzialKodea();
            
            // If: saioa hasi gabe badago, kodea null/hutsa
            if (komertzialKodea == null || komertzialKodea.isEmpty()) {
                Log.e(ETIKETA, "Esportazioa ez egin: saioa ez dago hasita.");
                return false;
            }
            
            // SEGURTASUNA: Uneko komertzialaren bisitak bakarrik esportatu
            List<Agenda> bisitak = datuBasea.agendaDao().hilabetearenBisitak(komertzialKodea, urtea, hilabetea);
            
            // TXT eta CSV fitxategiak sortu (biak ondo bukatu behar dira)
            boolean txtOndo = agendaTXTSortu(bisitak);
            boolean csvOndo = agendaCSVSortu(bisitak);
            
            boolean guztiakOndo = txtOndo && csvOndo;
            // If: biak ondo bukatu badira, log bat idatzi
            if (guztiakOndo) {
                Log.d(ETIKETA, "Hilabetearen bisitak esportatu dira: " + bisitak.size() + " bisita (TXT eta CSV)");
            }
            return guztiakOndo;
        } catch (Exception e) {
            Log.e(ETIKETA, "Errorea hilabetearen bisitak esportatzean", e);
            return false;
        }
    }

    /**
     * Bisitak XML formatuan esportatu.
     */
    private boolean agendaXMLSortu(List<Agenda> bisitak) {
        try {
            XmlSerializer idazlea = Xml.newSerializer();
            try (java.io.OutputStream irteera = testuingurua.openFileOutput(FITXATEGI_XML, Context.MODE_PRIVATE)) {
                idazlea.setOutput(irteera, KODEKETA);
                idazlea.startDocument(KODEKETA, true);
                idazlea.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                idazlea.startTag(null, "agenda");
                // For: bisita bakoitza XML nodo bihurtu
                for (Agenda a : bisitak) {
                    idazlea.startTag(null, "bisita");
                    nodoaIdatzi(idazlea, "bisita_data", hutsaEz(a.getBisitaData()));
                    nodoaIdatzi(idazlea, "ordua", hutsaEz(a.getOrdua()));
                    nodoaIdatzi(idazlea, "komertzial_kodea", hutsaEz(a.getKomertzialKodea()));
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
            Log.e(ETIKETA, "Errorea XML esportatzean", e);
            return false;
        }
    }

    /**
     * Bisitak TXT formatuan esportatu (irakurgarria).
     */
    private boolean agendaTXTSortu(List<Agenda> bisitak) {
        try {
            try (OutputStreamWriter idazlea = new OutputStreamWriter(
                    testuingurua.openFileOutput(FITXATEGI_TXT, Context.MODE_PRIVATE), StandardCharsets.UTF_8)) {
                // For: bisita bakoitzarentzat lerro bat (Data Ordua - Komertziala - Bazkidea - Deskribapena [Egoera])
                for (Agenda a : bisitak) {
                    String data = hutsaEz(a.getBisitaData());
                    String ordua = hutsaEz(a.getOrdua());
                    String komertziala = komertzialarenIzena(a.getKomertzialKodea());
                    String bazkidea = bazkidearenIzena(a.getBazkideaKodea());
                    String deskribapena = hutsaEz(a.getDeskribapena());
                    String egoera = hutsaEz(a.getEgoera());
                    
                    String lerroa = String.format(Locale.getDefault(), "%s %s - %s - %s - %s [%s]%n",
                            data, ordua, komertziala, bazkidea, deskribapena, egoera);
                    idazlea.write(lerroa);
                }
                idazlea.flush();
            }
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Errorea TXT esportatzean", e);
            return false;
        }
    }

    /**
     * Bisitak CSV formatuan esportatu (Excel edo beste aplikazioekin irekitzeko).
     */
    private boolean agendaCSVSortu(List<Agenda> bisitak) {
        try {
            try (OutputStreamWriter idazlea = new OutputStreamWriter(
                    testuingurua.openFileOutput(FITXATEGI_CSV, Context.MODE_PRIVATE), StandardCharsets.UTF_8)) {
                
                // CSV goiburua (BOM UTF-8 Excel-en ondo irakurtzeko)
                idazlea.write('\ufeff');
                idazlea.write("Data,Ordua,Komertziala,Bazkidea,Deskribapena,Egoera\n");
                // For: bisita bakoitzarentzat CSV lerro bat (komak banatuta)
                for (Agenda a : bisitak) {
                    String data = hutsaEz(a.getBisitaData());
                    String ordua = hutsaEz(a.getOrdua());
                    String komertziala = komertzialarenIzena(a.getKomertzialKodea());
                    String bazkidea = bazkidearenIzena(a.getBazkideaKodea());
                    String deskribapena = csvEremuaEskapatu(hutsaEz(a.getDeskribapena()));
                    String egoera = hutsaEz(a.getEgoera());
                    
                    String lerroa = String.format(Locale.getDefault(), "%s,%s,%s,%s,\"%s\",%s%n",
                            data, ordua, komertziala, bazkidea, deskribapena, egoera);
                    idazlea.write(lerroa);
                }
                idazlea.flush();
            }
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Errorea CSV esportatzean", e);
            return false;
        }
    }

    /**
     * CSV eremu bat eskapatu (komak eta komillak).
     */
    private String csvEremuaEskapatu(String testua) {
        // If: null bada, hutsa itzuli
        if (testua == null) return "";
        // CSV-n komilla bikoitzak ("") eskapatu behar dira (Excel formatua)
        return testua.replace("\"", "\"\"");
    }

    /**
     * XML nodo bat idatzi.
     */
    private void nodoaIdatzi(XmlSerializer idazlea, String izena, String edukia) throws IOException {
        idazlea.startTag(null, izena);
        // If: edukia hutsik ez bada bakarrik idatzi
        if (edukia != null && !edukia.isEmpty()) {
            idazlea.text(edukia);
        }
        idazlea.endTag(null, izena);
    }

    /**
     * Komertzialaren izena itzuli kodea erabiliz.
     */
    private String komertzialarenIzena(String komertzialKodea) {
        // If: kodea hutsik badago, ezer ez itzuli
        if (komertzialKodea == null || komertzialKodea.trim().isEmpty()) {
            return "";
        }
        Komertziala k = datuBasea.komertzialaDao().kodeaBilatu(komertzialKodea.trim());
        // If: komertziala aurkitu bada, izena + abizena itzuli
        if (k != null) {
            String izena = k.getIzena() != null ? k.getIzena().trim() : "";
            String abizena = k.getAbizena() != null && !k.getAbizena().trim().isEmpty() 
                    ? " " + k.getAbizena().trim() : "";
            return izena + abizena;
        }
        return komertzialKodea;
    }

    /**
     * Bazkidearen izena itzuli kodea erabiliz.
     */
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
        return bazkideaKodea;
    }

    /**
     * Barne-memorian nahikoa leku libre dagoen egiaztatu.
     */
    private boolean barneMemorianLekuNahikoa() {
        File karpeta = testuingurua.getFilesDir();
        // If: karpeta null bada, ezin dugu fitxategirik idatzi
        if (karpeta == null) {
            Log.w(ETIKETA, "Barne-memoria: fitxategi-karpeta ezin da lortu.");
            return false;
        }
        long libre = karpeta.getFreeSpace();
        // If: 512 KB baino gutxiago libre badago, ez idatzi
        if (libre < GUTXIENEKO_LEKU_LIBREA_BYTE) {
            Log.w(ETIKETA, "Barne-memorian ez dago nahikoa lekurik. Libre: " + libre + " byte");
            return false;
        }
        return true;
    }

    private static String hutsaEz(String s) {
        return s != null ? s : "";
    }
}
