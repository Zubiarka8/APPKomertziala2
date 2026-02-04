package com.example.appkomertziala.segurtasuna;

import android.util.Log;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.example.appkomertziala.db.eredua.EskaeraGoiburua;
import com.example.appkomertziala.db.eredua.Komertziala;

/**
 * Eskaera balidatzailea: eskaerak datu-basean gordetzeko aurretik datuen integritatea egiaztatzen du.
 * 
 * SEGURTASUNA: Datu guztiak beharrezkoak dira. Bazkidearen edo Komertzialaren kodea falta bada,
 * operazioa eteten da eta salbuespen bat jaurtitzen da.
 * 
 * Lege suprema: Integritatea edo ezer ez. Sistema mediocreak ez ditugu onartzen.
 */
public class EskaeraBalidatzailea {

    private static final String ETIKETA = "EskaeraBalidatzailea";
    private final AppDatabase datuBasea;

    public EskaeraBalidatzailea(AppDatabase datuBasea) {
        this.datuBasea = datuBasea;
    }

    /**
     * EskaeraGoiburua balidatu: derrigorrezko eremu guztiak beteta daudela egiaztatu.
     * 
     * Derrigorrezko eremuak:
     * - komertzialKodea: EZIN da null edo hutsik egon
     * - bazkideaKodea: EZIN da null edo hutsik egon (bazkidea beharrezkoa da eskaera egiteko)
     * 
     * @param eskaera Balidatu behar den EskaeraGoiburua
     * @throws IllegalArgumentException Daturen bat falta bada, salbuespen bat jaurtitzen da mezu argi batekin
     */
    public void balidatuEskaera(EskaeraGoiburua eskaera) throws IllegalArgumentException {
        if (eskaera == null) {
            String mezu = "Errorea: Eskaera null da. Ezin da balidatu.";
            Log.e(ETIKETA, mezu);
            throw new IllegalArgumentException(mezu);
        }

        // Komertzial kodea balidatu - DERRIORREZKO EREMUA
        String komertzialKodea = eskaera.getKomertzialKodea();
        if (komertzialKodea == null || komertzialKodea.trim().isEmpty()) {
            String mezu = "Errorea: Komertzialaren kodea falta da. Eskaera ezin da prozesatu.";
            Log.e(ETIKETA, "balidatuEskaera: " + mezu + " (zenbakia: " + eskaera.getZenbakia() + ")");
            throw new IllegalArgumentException(mezu);
        }

        // Komertzial kodea datu-basean existitzen dela egiaztatu
        komertzialKodea = komertzialKodea.trim();
        Komertziala komertziala = datuBasea.komertzialaDao().kodeaBilatu(komertzialKodea);
        if (komertziala == null) {
            String mezu = "Errorea: Komertzial kodea ez da existitzen datu-basean: " + komertzialKodea;
            Log.e(ETIKETA, "balidatuEskaera: " + mezu + " (zenbakia: " + eskaera.getZenbakia() + ")");
            throw new IllegalArgumentException(mezu);
        }

        // Komertzial IDa ezarri balidazioa gainditu bada
        eskaera.setKomertzialId(komertziala.getId());
        eskaera.setKomertzialKodea(komertzialKodea);

        // Bazkidea kodea balidatu - DERRIORREZKO EREMUA
        String bazkideaKodea = eskaera.getBazkideaKodea();
        if (bazkideaKodea == null || bazkideaKodea.trim().isEmpty()) {
            String mezu = "Errorea: Bazkidearen kodea falta da. Eskaera ezin da prozesatu.";
            Log.e(ETIKETA, "balidatuEskaera: " + mezu + " (zenbakia: " + eskaera.getZenbakia() + ")");
            throw new IllegalArgumentException(mezu);
        }

        // Bazkidea kodea datu-basean existitzen dela egiaztatu
        bazkideaKodea = bazkideaKodea.trim();
        Bazkidea bazkidea = datuBasea.bazkideaDao().nanBilatu(bazkideaKodea);
        if (bazkidea == null) {
            // Bazkidea ez bada aurkitu NAN-arekin, kodea erabiliz saiatu (bazkideak taulan kodea eremua badago)
            bazkidea = datuBasea.bazkideaDao().kodeaBilatu(bazkideaKodea);
            if (bazkidea == null) {
                String mezu = "Errorea: Bazkidea kodea ez da existitzen datu-basean: " + bazkideaKodea + ". Eskaera ezin da prozesatu.";
                Log.e(ETIKETA, "balidatuEskaera: " + mezu + " (zenbakia: " + eskaera.getZenbakia() + ")");
                throw new IllegalArgumentException(mezu);
            }
        }

        // Bazkidea IDa ezarri balidazioa gainditu bada
        eskaera.setBazkideaId(bazkidea.getId());
        eskaera.setBazkideaKodea(bazkideaKodea);

        // Zenbakia balidatu (lehenetsia bada ere, baliozkoa izan behar du)
        String zenbakia = eskaera.getZenbakia();
        if (zenbakia == null || zenbakia.trim().isEmpty()) {
            String mezu = "Errorea: Eskaeraren zenbakia falta da.";
            Log.e(ETIKETA, "balidatuEskaera: " + mezu);
            throw new IllegalArgumentException(mezu);
        }

        Log.d(ETIKETA, "balidatuEskaera: Eskaera baliozkoa da - zenbakia: " + eskaera.getZenbakia() + 
              ", komertzial: " + komertzialKodea + ", bazkidea: " + bazkideaKodea);
    }

    /**
     * Eskaera balidatu eta gorde transakzio seguru batean.
     * Lehenik balidatzen du, gero bakarrik gordetzen du datu-basean.
     * 
     * @param eskaera Gorde behar den EskaeraGoiburua
     * @return Txertatutako eskaeraren IDa
     * @throws IllegalArgumentException Daturen bat falta bada
     */
    public long balidatuEtaGorde(EskaeraGoiburua eskaera) throws IllegalArgumentException {
        // Lehenik balidatu - datuen integritatea bermatzeko
        balidatuEskaera(eskaera);
        
        // Balidazioa gainditu bada, bakarrik gorde transakzio seguru batean
        return datuBasea.runInTransaction(() -> {
            return datuBasea.eskaeraGoiburuaDao().txertatu(eskaera);
        });
    }
}

