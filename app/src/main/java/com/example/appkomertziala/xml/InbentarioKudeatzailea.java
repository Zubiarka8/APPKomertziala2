package com.example.appkomertziala.xml;

import android.content.Context;

import com.example.appkomertziala.db.AppDatabase;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Inbentarioa (katalogoa) astero ordezkaritzatik jasotako XML fitxategia barne-memoriatik irakurtzeko
 * eta datu-basean produktu bakoitzaren kodea, izena, salmenta_prezioa, stock_a eta irudia_izena
 * kargatzeko kudeatzailea.
 *
 * Asteko eguneraketa: ordezkaritzak prezioak eta stock eguneratzen ditu astero; inportazioak
 * wipe-and-load erabiltzen du (katalogoa guztiz ezabatu, XMLko datuak bakarrik txertatu).
 * Eragiketa hau hila nagusitik kanpo exekutatu behar da (deiallea Thread edo Executor bidez).
 */
public class InbentarioKudeatzailea {

    private final Context context;
    private final AppDatabase datuBasea;
    private final XMLKudeatzailea xmlKudeatzailea;

    /** Astero jasotako katalogoa XML (barne-memorian gordeta). Lehenetsia: katalogoa_ordezkaritza.xml */
    private static final String KATALOGOA_ASTERO_ORDEZKARITZA = "katalogoa_ordezkaritza.xml";

    public InbentarioKudeatzailea(Context context) {
        this.context = context.getApplicationContext();
        this.datuBasea = AppDatabase.getInstance(this.context);
        this.xmlKudeatzailea = new XMLKudeatzailea(this.context);
    }

    /**
     * Barne-memorian gordetako XML fitxategia irakurri eta katalogoa datu-basean berritu (wipe-and-load).
     * Lehenengo barne-memorian 'katalogoa_ordezkaritza.xml' bilatzen du; ez badago, assets-eko 'katalogoa.xml' erabiltzen du.
     * XMLtik erauzitako eremuak: artikulu_kodea, izena, salmenta_prezioa, stock_a, irudia_izena.
     * Hila nagusitik kanpo deitu behar da.
     *
     * @return inportatutako produktu kopurua
     */
    public int katalogoaAsterokoInportazioaEgin() throws IOException, XmlPullParserException {
        try {
            return xmlKudeatzailea.katalogoaInportatuBarneFitxategitik(KATALOGOA_ASTERO_ORDEZKARITZA);
        } catch (IOException e) {
            // Barne-fitxategirik ez; assets-eko katalogoa.xml erabili (probak edo lehendik kargatutakoa)
            return xmlKudeatzailea.katalogoaInportatu();
        }
    }

    /**
     * Sarrera-fluxu batetik katalogoa inportatu (adib. fitxategi hautatzailetik).
     * Wipe-and-load: aurreko katalogoa ezabatu, XMLko produktuak bakarrik txertatu.
     *
     * @param sarreraFluxua XML edukia duen fluxua
     * @return inportatutako produktu kopurua
     */
    public int katalogoaInportatuFluxutik(InputStream sarreraFluxua) throws IOException, XmlPullParserException {
        return xmlKudeatzailea.katalogoaInportatuSarreraFluxutik(sarreraFluxua);
    }
}
