package com.example.appkomertziala.segurtasuna;

import android.util.Log;

import java.util.regex.Pattern;

/**
 * NAN (DNI) balidatzailea: DNI/NAN identifikatzailearen formatua eta baliozkotasuna egiaztatzen du.
 * 
 * Formatu eskatua: DNI espainiarra (8 zifra + letra kontrol)
 * - Formatu 1: 12345678A (zifrak + letra, espazio gabe)
 * - Formatu 2: 12345678-A (zifrak + gidoia + letra)
 * 
 * Balidazioak:
 * - Formatu zuzena: 8 zifra + letra kontrol
 * - Letra kontrol baliozkoa: algoritmo espainiarra erabiliz egiaztatzen da
 * - Zifrak bakarrik ez dira onartzen (letra kontrol beharrezkoa da)
 */
public class NanBalidatzailea {

    private static final String ETIKETA = "NanBalidatzailea";
    
    /** DNI formatuaren patroia: 8 zifra + letra (gidoiarekin edo gabe) */
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}[-]?[A-Za-z]$");
    
    /** Letra kontrol taula (DNI algoritmo espainiarra) */
    private static final String LETRAK_KONTROL = "TRWAGMYFPDXBNJZSQVHLCKE";

    /**
     * NAN/DNI balidatu: formatua eta letra kontrol egiaztatu.
     * 
     * @param nan Balidatu behar den NAN/DNI
     * @return true baliozkoa bada, false bestela
     */
    public static boolean balidatuNan(String nan) {
        if (nan == null || nan.trim().isEmpty()) {
            Log.w(ETIKETA, "balidatuNan: NAN null edo hutsa da");
            return false;
        }

        String nanTrimm = nan.trim().toUpperCase();
        
        // Formatu egiaztatu: 8 zifra + letra (gidoiarekin edo gabe)
        if (!DNI_PATTERN.matcher(nanTrimm).matches()) {
            Log.w(ETIKETA, "balidatuNan: Formatu okerra - " + nanTrimm + " (esperotakoa: 12345678A edo 12345678-A)");
            return false;
        }

        // Gidoia kendu formatu uniforme baterako
        String nanGarbia = nanTrimm.replace("-", "");
        
        // Zifrak eta letra bereizi
        String zifrak = nanGarbia.substring(0, 8);
        char letraJaso = nanGarbia.charAt(8);
        
        // Zifrak baliozkoak direla egiaztatu (8 zifra)
        try {
            int zenbakia = Integer.parseInt(zifrak);
            
            // Letra kontrol kalkulatu
            int indizea = zenbakia % 23;
            char letraEsperatua = LETRAK_KONTROL.charAt(indizea);
            
            // Letra kontrol egiaztatu
            if (letraJaso != letraEsperatua) {
                Log.w(ETIKETA, "balidatuNan: Letra kontrol okerra - " + nanTrimm + 
                      " (esperotakoa: " + zifrak + letraEsperatua + ")");
                return false;
            }
            
            Log.d(ETIKETA, "balidatuNan: NAN baliozkoa da - " + nanTrimm);
            return true;
            
        } catch (NumberFormatException e) {
            Log.w(ETIKETA, "balidatuNan: Zifrak parseatu ezin dira - " + nanTrimm);
            return false;
        } catch (Exception e) {
            Log.w(ETIKETA, "balidatuNan: Errorea balidatzean - " + nanTrimm + ": " + 
                  (e.getMessage() != null ? e.getMessage() : "ezezaguna"));
            return false;
        }
    }

    /**
     * NAN balidatu eta salbuespena jaurti baliozkoa ez bada.
     * 
     * @param nan Balidatu behar den NAN
     * @throws IllegalArgumentException NAN baliozkoa ez bada
     */
    public static void balidatuNanEtaJaurti(String nan) throws IllegalArgumentException {
        if (!balidatuNan(nan)) {
            String mezu = "Errorea: NAN formatua okerra edo baliozkoa ez da. Formatu eskatua: 8 zifra + letra kontrol (adibidez: 12345678A)";
            Log.e(ETIKETA, mezu + " - NAN jaso: " + nan);
            throw new IllegalArgumentException(mezu);
        }
    }

    /**
     * NAN formatua normalizatu (gidoiak kendu, letra maiuskulaz).
     * 
     * @param nan Normalizatu behar den NAN
     * @return NAN normalizatua (8 zifra + letra maiuskulaz, gidoirik gabe), null baliozkoa ez bada
     */
    public static String normalizatuNan(String nan) {
        if (nan == null || nan.trim().isEmpty()) {
            return null;
        }

        String nanTrimm = nan.trim().toUpperCase().replace("-", "").replace(" ", "");
        
        // Formatu egiaztatu
        if (!DNI_PATTERN.matcher(nanTrimm).matches()) {
            return null;
        }
        
        return nanTrimm;
    }

    /**
     * NAN formatua egiaztatu eta errore-mezua itzuli.
     * 
     * @param nan Balidatu behar den NAN
     * @return null baliozkoa bada, errore-mezua bestela
     */
    public static String balidatuNanMezua(String nan) {
        if (nan == null || nan.trim().isEmpty()) {
            return "NAN beharrezkoa da";
        }
        
        if (!balidatuNan(nan)) {
            return "NAN formatua okerra edo baliozkoa ez da. Formatu eskatua: 8 zifra + letra kontrol (adibidez: 12345678A)";
        }
        
        return null;
    }
}

