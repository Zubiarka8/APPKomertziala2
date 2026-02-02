package com.example.appkomertziala.xml;

import android.content.Context;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Room datu-basearen esportazio eta inportazio logika maiztasunaren arabera koordinatzen duen kudeatzailea.
 * Eguneroko txostena (bazkide berriak, eskaera berriak), asteko inportazioa (katalogoa) eta hileroko laburpena (agenda).
 * Metodo bakoitzak akatsen kontrola du (try-catch).
 */
public class DatuKudeatzailea {

    private static final String ETIKETA = "DatuKudeatzailea";

    /** Ordezkaritzatik jasotako katalogo-fitxategiaren izena barne-memorian (asteko inportazioa). */
    private static final String KATALOGOA_ORDEZKARITZA = "katalogoa_ordezkaritza.xml";

    private final Context testuingurua;
    private final XMLEsportatzailea esportatzailea;
    private final XMLKudeatzailea inportatzailea;

    public DatuKudeatzailea(Context testuingurua) {
        this.testuingurua = testuingurua.getApplicationContext();
        this.esportatzailea = new XMLEsportatzailea(this.testuingurua);
        this.inportatzailea = new XMLKudeatzailea(this.testuingurua);
    }

    /**
     * Bazkide berriak esportatu (egunero).
     * PARTNERRA taulatik eguneko alta guztiak erauzi eta bazkide_berriak.xml fitxategia sortu.
     * Garrantzitsua: centralera egunero bidaltzeko soilik eguneko erregistro berriak bidaltzea, datu-kopuru handia saihesteko.
     *
     * @return true esportazioa ondo bukatu bada, false akatsen bat gertatu bada
     */
    public boolean bazkideBerriakEsportatu() {
        try {
            esportatzailea.bazkideBerriakEsportatu();
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Bazkide berriak esportatzean akatsa: fitxategia idaztea edo datu-basea irakurtzea huts egin du.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Bazkide berriak esportatzean ustekabeko akatsa.", e);
            return false;
        }
    }

    /** Bazkide berriak TXT formatuan esportatu (Gmail eranskin gisa). */
    public boolean bazkideBerriakEsportatuTxt() {
        try {
            esportatzailea.bazkideBerriakEsportatuTxt();
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Bazkide berriak TXT esportatzean akatsa.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Bazkide berriak TXT esportatzean ustekabeko akatsa.", e);
            return false;
        }
    }

    /**
     * Eskaera berriak esportatu (egunero).
     * Eguneko eskaera guztiak (ESKAERA_GOIBURUA eta ESKAERA_XEHETASUNA) XML egitura hierarkikoan gorde (eskaera_berriak.xml).
     * Garrantzitsua: egunero eskaera berrien laburpena centralera bidaltzea, eguneroko salmenta-jarraipena ahalbidetzen du.
     *
     * @return true esportazioa ondo bukatu bada, false akatsen bat gertatu bada
     */
    public boolean eskaeraBerriakEsportatu() {
        try {
            esportatzailea.eskaeraBerriakEsportatu();
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Eskaera berriak esportatzean akatsa: fitxategia idaztea edo datu-basea irakurtzea huts egin du.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Eskaera berriak esportatzean ustekabeko akatsa.", e);
            return false;
        }
    }

    /** Bazkideak taula bazkideak.xml fitxategian esportatu (bazkideak.xml egitura: NAN, izena, abizena, eskaerak). */
    public boolean bazkideakEsportatu() {
        try {
            esportatzailea.bazkideakEsportatu();
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Bazkideak esportatzean akatsa.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Bazkideak esportatzean ustekabeko akatsa.", e);
            return false;
        }
    }

    /** Eskaera berriak TXT formatuan esportatu (Gmail eranskin gisa). */
    public boolean eskaeraBerriakEsportatuTxt() {
        try {
            esportatzailea.eskaeraBerriakEsportatuTxt();
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Eskaera berriak TXT esportatzean akatsa.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Eskaera berriak TXT esportatzean ustekabeko akatsa.", e);
            return false;
        }
    }

    /**
     * Katalogoa esportatu (astero). Katalogo guztia katalogoa.xml fitxategian gorde Gmail bidez bidaltzeko.
     *
     * @return true esportazioa ondo bukatu bada, false akatsen bat gertatu bada
     */
    public boolean katalogoaEsportatu() {
        try {
            esportatzailea.katalogoaEsportatu();
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Katalogoa esportatzean akatsa: fitxategia idaztea edo datu-basea irakurtzea huts egin du.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Katalogoa esportatzean ustekabeko akatsa.", e);
            return false;
        }
    }

    /** Katalogoa TXT formatuan esportatu (Gmail eranskin gisa). */
    public boolean katalogoaEsportatuTxt() {
        try {
            esportatzailea.katalogoaEsportatuTxt();
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Katalogoa TXT esportatzean akatsa.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Katalogoa TXT esportatzean ustekabeko akatsa.", e);
            return false;
        }
    }

    /**
     * Katalogoa eguneratu (astero).
     * Ordezkaritzatik jasotako artikuluen informazioa (kodea, izena, prezioa, stock) inportatu eta datu-basea berritu.
     * Garrantzitsua: astero katalogoa sinkronizatzea prezio eta stock eguneratuak lortzeko; egunero ez da beharrezkoa.
     * Lehenengo barne-memorian 'katalogoa_ordezkaritza.xml' bilatzen du; ez badago, assets-eko 'katalogoa.xml' erabiltzen du.
     *
     * @return true inportazioa ondo bukatu bada, false akatsen bat gertatu bada edo fitxategirik ez bada
     */
    public boolean katalogoaEguneratu() {
        try {
            // Lehenengo ordezkaritzatik jasotako barne-fitxategia probatu (astero jasotzen den fitxategia)
            try {
                inportatzailea.katalogoaInportatuBarneFitxategitik(KATALOGOA_ORDEZKARITZA);
                return true;
            } catch (FileNotFoundException e) {
                // Barne-fitxategirik ez; assets-eko katalogoa erabili (probak edo lehendik dagoen fitxategia)
                inportatzailea.katalogoaInportatu();
                return true;
            }
        } catch (IOException e) {
            Log.e(ETIKETA, "Katalogoa eguneratzean akatsa: fitxategia irakurtzea huts egin du.", e);
            return false;
        } catch (XmlPullParserException e) {
            Log.e(ETIKETA, "Katalogoa eguneratzean akatsa: XML formatua baliogabea.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Katalogoa eguneratzean ustekabeko akatsa.", e);
            return false;
        }
    }

    /**
     * Katalogoa barne-memorian gordetako fitxategi jakin batetik eguneratu (astero).
     * Ordezkaritzak fitxategi hau gorde dezake barne direktorian; gero deia honi deitu.
     *
     * @param barneFitxategiIzena openFileInput-erako fitxategi-izena (adib. katalogoa_ordezkaritza.xml)
     * @return true inportazioa ondo bukatu bada, false akatsen bat gertatu bada
     */
    public boolean katalogoaEguneratuFitxategitik(String barneFitxategiIzena) {
        if (barneFitxategiIzena == null || barneFitxategiIzena.trim().isEmpty()) {
            Log.w(ETIKETA, "Katalogoa eguneratu: fitxategi-izena hutsa.");
            return false;
        }
        try {
            inportatzailea.katalogoaInportatuBarneFitxategitik(barneFitxategiIzena.trim());
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Katalogoa eguneratzean akatsa: fitxategia irakurtzea huts egin du.", e);
            return false;
        } catch (XmlPullParserException e) {
            Log.e(ETIKETA, "Katalogoa eguneratzean akatsa: XML formatua baliogabea.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Katalogoa eguneratzean ustekabeko akatsa.", e);
            return false;
        }
    }

    /**
     * Agenda esportatu (hilero).
     * Agendan erregistratutako bisita guztiak (eskaera goiburuak) XML fitxategi bakar batean gorde (agenda.xml) centralera bidaltzeko.
     * Garrantzitsua: hilero exekutatu behar da hileroko laburpena lortzeko; egunero ez da beharrezkoa.
     *
     * @return true esportazioa ondo bukatu bada, false akatsen bat gertatu bada
     */
    public boolean agendaEsportatu() {
        try {
            esportatzailea.agendaEsportatu();
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Agenda esportatzean akatsa: fitxategia idaztea edo datu-basea irakurtzea huts egin du.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Agenda esportatzean ustekabeko akatsa.", e);
            return false;
        }
    }

    /** Agenda TXT formatuan esportatu (Gmail eranskin gisa). */
    public boolean agendaEsportatuTxt() {
        try {
            esportatzailea.agendaEsportatuTxt();
            return true;
        } catch (IOException e) {
            Log.e(ETIKETA, "Agenda TXT esportatzean akatsa.", e);
            return false;
        } catch (Exception e) {
            Log.e(ETIKETA, "Agenda TXT esportatzean ustekabeko akatsa.", e);
            return false;
        }
    }
}
