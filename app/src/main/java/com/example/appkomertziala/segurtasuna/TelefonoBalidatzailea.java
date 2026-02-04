package com.example.appkomertziala.segurtasuna;

import android.util.Log;

import java.util.regex.Pattern;

/**
 * Telefono balidatzailea: telefono zenbaki espainiarraren formatua egiaztatzen du.
 * 
 * Formatu eskatua: telefono zenbaki espainiarra
 * - Finkoa: 9 zifra (8 edo 9 hasieran)
 * - Mugikorra: 9 zifra (6, 7 edo 9 hasieran)
 * - Prefijoarekin: +34 edo 0034 (aukerakoa)
 * - Separadoreak: espazioak edo gidoiak onartzen dira (aukerakoak)
 * 
 * Adibideak:
 * - 612345678 (mugikorra)
 * - 912345678 (finkoa)
 * - +34 612 345 678
 * - 0034 612 345 678
 * - 612-345-678
 * 
 * Balidazioak:
 * - 9 zifra izan behar du (prefijoak kenduta)
 * - Lehenengo zifra: 6, 7, 8 edo 9 (mugikorra edo finkoa)
 * - Separadoreak onartzen dira baina ez dira beharrezkoak
 */
public class TelefonoBalidatzailea {

    private static final String ETIKETA = "TelefonoBalidatzailea";
    
    /** Telefono formatuaren patroia (9 zifra, lehenengo zifra 6, 7, 8 edo 9) */
    private static final Pattern TELEFONO_PATTERN = Pattern.compile("^[6-9]\\d{8}$");
    
    /** Telefono luzera (zifrak bakarrik, prefijoak kenduta) */
    private static final int TELEFONO_LUZERA = 9;

    /**
     * Telefono zenbakia balidatu: formatua egiaztatu.
     * 
     * @param telefono Balidatu behar den telefono zenbakia
     * @return true baliozkoa bada, false bestela
     */
    public static boolean balidatuTelefonoa(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) {
            Log.w(ETIKETA, "balidatuTelefonoa: Telefono null edo hutsa da");
            return false;
        }

        String telefonoTrimm = telefono.trim();
        
        // Separadoreak kendu (espazioak, gidoiak, parentesiak)
        String telefonoGarbia = telefonoTrimm.replaceAll("[\\s\\-\\(\\)]", "");
        
        // Prefijo espainiarra kendu (+34 edo 0034)
        if (telefonoGarbia.startsWith("+34")) {
            telefonoGarbia = telefonoGarbia.substring(3);
        } else if (telefonoGarbia.startsWith("0034")) {
            telefonoGarbia = telefonoGarbia.substring(4);
        } else if (telefonoGarbia.startsWith("34") && telefonoGarbia.length() == 11) {
            // 34 prefijoarekin baina + gabe
            telefonoGarbia = telefonoGarbia.substring(2);
        }
        
        // Luzera egiaztatu (9 zifra izan behar du)
        if (telefonoGarbia.length() != TELEFONO_LUZERA) {
            Log.w(ETIKETA, "balidatuTelefonoa: Luzera okerra - " + telefonoGarbia.length() + 
                  " zifra (esperotakoa: " + TELEFONO_LUZERA + ")");
            return false;
        }
        
        // Zifrak bakarrik direla egiaztatu
        if (!telefonoGarbia.matches("\\d+")) {
            Log.w(ETIKETA, "balidatuTelefonoa: Zifrak bakarrik izan behar dira - " + telefonoTrimm);
            return false;
        }
        
        // Formatu egiaztatu (6, 7, 8 edo 9 hasieran)
        if (!TELEFONO_PATTERN.matcher(telefonoGarbia).matches()) {
            Log.w(ETIKETA, "balidatuTelefonoa: Formatu okerra - " + telefonoTrimm + 
                  " (esperotakoa: 9 zifra, 6, 7, 8 edo 9 hasieran)");
            return false;
        }
        
        Log.d(ETIKETA, "balidatuTelefonoa: Telefono baliozkoa da - " + telefonoTrimm + " (garbia: " + telefonoGarbia + ")");
        return true;
    }

    /**
     * Telefono balidatu eta salbuespena jaurti baliozkoa ez bada.
     * 
     * @param telefono Balidatu behar den telefono
     * @throws IllegalArgumentException Telefono baliozkoa ez bada
     */
    public static void balidatuTelefonoaEtaJaurti(String telefono) throws IllegalArgumentException {
        if (!balidatuTelefonoa(telefono)) {
            String mezu = "Errorea: Telefono zenbaki formatua okerra da. Formatu eskatua: 9 zifra (6, 7, 8 edo 9 hasieran). Adibideak: 612345678, 912345678, +34 612 345 678";
            Log.e(ETIKETA, mezu + " - Telefono jaso: " + telefono);
            throw new IllegalArgumentException(mezu);
        }
    }

    /**
     * Telefono formatua normalizatu (separadoreak kendu, prefijoak kendu).
     * 
     * @param telefono Normalizatu behar den telefono
     * @return Telefono normalizatua (9 zifra bakarrik), null baliozkoa ez bada
     */
    public static String normalizatuTelefonoa(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) {
            return null;
        }

        String telefonoTrimm = telefono.trim();
        
        // Separadoreak kendu
        String telefonoGarbia = telefonoTrimm.replaceAll("[\\s\\-\\(\\)]", "");
        
        // Prefijo espainiarra kendu
        if (telefonoGarbia.startsWith("+34")) {
            telefonoGarbia = telefonoGarbia.substring(3);
        } else if (telefonoGarbia.startsWith("0034")) {
            telefonoGarbia = telefonoGarbia.substring(4);
        } else if (telefonoGarbia.startsWith("34") && telefonoGarbia.length() == 11) {
            telefonoGarbia = telefonoGarbia.substring(2);
        }
        
        // Balidatu
        if (telefonoGarbia.length() == TELEFONO_LUZERA && TELEFONO_PATTERN.matcher(telefonoGarbia).matches()) {
            return telefonoGarbia;
        }
        
        return null;
    }

    /**
     * Telefono formatua egiaztatu eta errore-mezua itzuli.
     * 
     * @param telefono Balidatu behar den telefono
     * @return null baliozkoa bada, errore-mezua bestela
     */
    public static String balidatuTelefonoaMezua(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) {
            return "Telefonoa beharrezkoa da";
        }
        
        if (!balidatuTelefonoa(telefono)) {
            return "Telefono zenbaki formatua okerra da. Formatu eskatua: 9 zifra (6, 7, 8 edo 9 hasieran). Adibideak: 612345678, 912345678";
        }
        
        return null;
    }
}

