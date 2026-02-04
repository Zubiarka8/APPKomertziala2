package com.example.appkomertziala.segurtasuna;

import android.util.Log;

import java.util.regex.Pattern;

/**
 * NAN (DNI) validator: validates DNI/NAN identifier format.
 *
 * Required format: 8 digits + letter (control letter not validated)
 * - Format 1: 12345678A (digits + letter, no spaces)
 * - Format 2: 12345678-A (digits + hyphen + letter)
 *
 * Validations:
 * - Correct format: 8 digits + letter (any letter)
 * - Digits only are not accepted (letter is required)
 */
public class NanBalidatzailea {

    private static final String ETIKETA = "NanBalidatzailea";
    
    /** DNI format pattern: 8 digits + letter (with or without hyphen) */
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}[-]?[A-Za-z]$");

    /**
     * Validates NAN/DNI: checks format (8 digits + letter).
     * Control letter is not validated, only format.
     *
     * @param nan NAN/DNI to validate
     * @return true if valid, false otherwise
     */
    public static boolean balidatuNan(String nan) {
        if (nan == null || nan.trim().isEmpty()) {
            Log.w(ETIKETA, "balidatuNan: NAN null edo hutsa da");
            return false;
        }

        String nanTrimm = nan.trim().toUpperCase();
        
        // Check format: 8 digits + letter (with or without hyphen)
        if (!DNI_PATTERN.matcher(nanTrimm).matches()) {
            Log.w(ETIKETA, "balidatuNan: Formatu okerra - " + nanTrimm + " (esperotakoa: 12345678A edo 12345678-A)");
            return false;
        }

        Log.d(ETIKETA, "balidatuNan: NAN formatua zuzena da - " + nanTrimm);
            return true;
    }

    /**
     * Validates NAN and throws exception if invalid.
     *
     * @param nan NAN to validate
     * @throws IllegalArgumentException If NAN is invalid
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

