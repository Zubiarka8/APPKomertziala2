package com.example.appkomertziala.segurtasuna;

import android.util.Log;

import java.util.regex.Pattern;

/**
 * Posta balidatzailea: posta elektronikoaren formatua egiaztatzen du.
 * 
 * Formatu eskatua: posta elektroniko estandarra
 * - Formatua: erabiltzailea@domeinua.extension
 * - Adibideak: gipuzkoa@enpresa.eus, erabiltzailea@example.com
 * 
 * Balidazioak:
 * - Formatu zuzena: RFC 5322 estandarraren arabera
 * - Erabiltzaile zatia: letrak, zifrak, puntuak, gidoiak, azpimarrak
 * - @ ikurra beharrezkoa
 * - Domeinu zatia: letrak, zifrak, puntuak, gidoiak
 * - Luzapen zatia: gutxienez 2 karaktere
 */
public class PostaBalidatzailea {

    private static final String ETIKETA = "PostaBalidatzailea";
    
    /** Posta elektroniko formatuaren patroia (RFC 5322 oinarritua) */
    private static final Pattern POSTA_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    /** Posta luzera maximoa (RFC 5321) */
    private static final int POSTA_LUZERA_MAX = 254;

    /**
     * Posta elektronikoa balidatu: formatua egiaztatu.
     * 
     * @param posta Balidatu behar den posta elektronikoa (String)
     * @return true baliozkoa bada, false bestela
     */
    public static boolean balidatuPosta(String posta) {
        if (posta == null || posta.trim().isEmpty()) {
            Log.w(ETIKETA, "balidatuPosta: Posta null edo hutsa da");
            return false;
        }

        String postaTrimm = posta.trim();
        
        // Luzera maximoa egiaztatu
        if (postaTrimm.length() > POSTA_LUZERA_MAX) {
            Log.w(ETIKETA, "balidatuPosta: Posta luzeegia - " + postaTrimm.length() + " karaktere (max: " + POSTA_LUZERA_MAX + ")");
            return false;
        }
        
        // Formatu egiaztatu
        if (!POSTA_PATTERN.matcher(postaTrimm).matches()) {
            Log.w(ETIKETA, "balidatuPosta: Formatu okerra - " + postaTrimm + 
                  " (esperotakoa: erabiltzailea@domeinua.extension)");
            return false;
        }
        
        // Gehiegizko puntuak edo @ ikurrak egiaztatu
        if (postaTrimm.startsWith(".") || postaTrimm.startsWith("@") || 
            postaTrimm.endsWith(".") || postaTrimm.endsWith("@") ||
            postaTrimm.contains("..") || postaTrimm.contains("@.") || 
            postaTrimm.contains(".@")) {
            Log.w(ETIKETA, "balidatuPosta: Posta formatu okerra - " + postaTrimm);
            return false;
        }
        
        // @ ikurra behin bakarrik agertu behar da
        int atIndex = postaTrimm.indexOf('@');
        if (postaTrimm.indexOf('@', atIndex + 1) != -1) {
            Log.w(ETIKETA, "balidatuPosta: Posta @ ikurra behin baino gehiagotan agertzen da - " + postaTrimm);
            return false;
        }
        
        // Domeinu zatia egiaztatu (@ ondoren)
        String[] zatiak = postaTrimm.split("@");
        if (zatiak.length != 2) {
            Log.w(ETIKETA, "balidatuPosta: Posta @ ikurra beharrezkoa da - " + postaTrimm);
            return false;
        }
        
        String erabiltzailea = zatiak[0];
        String domeinua = zatiak[1];
        
        // Erabiltzaile zatia hutsik ez egon
        if (erabiltzailea.isEmpty()) {
            Log.w(ETIKETA, "balidatuPosta: Erabiltzaile zatia hutsik dago - " + postaTrimm);
            return false;
        }
        
        // Domeinu zatia hutsik ez egon eta puntu bat gutxienez izan behar du
        if (domeinua.isEmpty() || !domeinua.contains(".")) {
            Log.w(ETIKETA, "balidatuPosta: Domeinu zatia hutsik dago edo punturik ez du - " + postaTrimm);
            return false;
        }
        
        // Luzapen zatia gutxienez 2 karaktere izan behar du
        String luzapena = domeinua.substring(domeinua.lastIndexOf('.') + 1);
        if (luzapena.length() < 2) {
            Log.w(ETIKETA, "balidatuPosta: Luzapen zatia gutxienez 2 karaktere izan behar du - " + postaTrimm);
            return false;
        }
        
        Log.d(ETIKETA, "balidatuPosta: Posta baliozkoa da - " + postaTrimm);
        return true;
    }

    /**
     * Posta balidatu eta salbuespena jaurti baliozkoa ez bada.
     * 
     * @param posta Balidatu behar den posta
     * @throws IllegalArgumentException Posta baliozkoa ez bada
     */
    public static void balidatuPostaEtaJaurti(String posta) throws IllegalArgumentException {
        if (!balidatuPosta(posta)) {
            String mezu = "Errorea: Posta elektroniko formatua okerra da. Formatu eskatua: erabiltzailea@domeinua.extension (adibidez: gipuzkoa@enpresa.eus)";
            Log.e(ETIKETA, mezu + " - Posta jaso: " + posta);
            throw new IllegalArgumentException(mezu);
        }
    }

    /**
     * Posta formatua egiaztatu eta errore-mezua itzuli.
     * 
     * @param posta Balidatu behar den posta
     * @return null baliozkoa bada, errore-mezua bestela
     */
    public static String balidatuPostaMezua(String posta) {
        if (posta == null || posta.trim().isEmpty()) {
            // Posta aukerakoa da, hutsik badago onartzen da
            return null;
        }
        
        if (!balidatuPosta(posta)) {
            return "Posta elektroniko formatua okerra da. Formatu eskatua: erabiltzailea@domeinua.extension (adibidez: gipuzkoa@enpresa.eus)";
        }
        
        return null;
    }
}

