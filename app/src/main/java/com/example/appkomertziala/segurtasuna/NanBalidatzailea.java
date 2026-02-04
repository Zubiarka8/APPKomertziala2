package com.example.appkomertziala.segurtasuna;

import android.util.Log;

import java.util.regex.Pattern;

/**
 * NAN (DNI) balidatzailea: DNI/NAN identifikatzailearen formatua egiaztatzen du.
 * 
 * Formatu eskatua: 8 zifra + letra (letra kontrolik egiaztatzen ez da)
 * - Formatu 1: 12345678A (zifrak + letra, espazio gabe)
 * - Formatu 2: 12345678-A (zifrak + gidoia + letra)
 * 
 * Balidazioak:
 * - Formatu zuzena: 8 zifra + letra (edozein letra)
 * - Zifrak bakarrik ez dira onartzen (letra beharrezkoa da)
 */
public class NanBalidatzailea {

    private static final String ETIKETA = "NanBalidatzailea";
    
    /** DNI formatuaren patroia: 8 zifra + letra (gidoiarekin edo gabe) */
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}[-]?[A-Za-z]$");

    /**
     * NAN/DNI balidatu: formatua egiaztatu (8 zifra + letra).
     * Letra kontrolik egiaztatzen ez da, formatua bakarrik.
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
        
        Log.d(ETIKETA, "balidatuNan: NAN formatua zuzena da - " + nanTrimm);
        return true;
    }

    /**
     * NAN balidatu eta salbuespena jaurti baliozkoa ez bada.
     * 
     * @param nan Balidatu behar den NAN
     * @throws IllegalArgumentException NAN baliozkoa ez bada
     */
    public static void balidatuNanEtaJaurti(String nan) throws IllegalArgumentException {
        if (!balidatuNan(nan)) {
            String mezu = "Errorea: NAN formatua okerra da. Formatu eskatua: 8 zifra + letra (adibidez: 12345678A)";
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
            return "NAN formatua okerra da. Formatu eskatua: 8 zifra + letra (adibidez: 12345678A)";
        }
        
        return null;
    }
}

