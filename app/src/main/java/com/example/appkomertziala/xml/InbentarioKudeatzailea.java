package com.example.appkomertziala.xml;

import android.content.Context;

import com.example.appkomertziala.db.AppDatabase;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Inbentarioa (katalogoa) astero ordezkaritzatik jasotako XML fitxategia irakurtzeko eta
 * datu-basean produktu bakoitzaren kodea, izena, prezioa eta stock-a kargatzeko kudeatzailea.
 *
 * Zergatik da garrantzitsua asteko eguneraketa: ordezkaritzak prezioak eta stock-a eguneratzen
 * ditu astero; inportazioa egitean datu-base lokala sinkronizatzen da, komertzialak katalogo
 * eguneraturik ikusteko.
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
     * Ordezkaritzatik astero jasotako XML fitxategia irakurri eta produktu guztiak datu-basean kargatu.
     * Lehenengo barne-memorian 'katalogoa_ordezkaritza.xml' bilatzen du; ez badago, assets-eko 'katalogoa.xml' erabiltzen du.
     * Produktu bakoitzaren kodea, izena, salmenta-prezioa, stock-a eta irudia_izena datu-basean gordetzen dira.
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
     * Astero ordezkaritzatik jasotako XML bera prozesatzen du: produktu bakoitzaren kodea, izena, prezioa, stock-a.
     *
     * @param sarreraFluxua XML edukia duen fluxua
     * @return inportatutako produktu kopurua
     */
    public int katalogoaInportatuFluxutik(InputStream sarreraFluxua) throws IOException, XmlPullParserException {
        return xmlKudeatzailea.katalogoaInportatuSarreraFluxutik(sarreraFluxua);
    }
}
