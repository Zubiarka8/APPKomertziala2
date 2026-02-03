package com.example.appkomertziala.segurtasuna;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * SessionManager: Saioa hasi duen komertzialaren kodea modu seguruan gordetzen du.
 * SharedPreferences erabiliz, uneko erabiltzailearen identifikazioa mantentzen du.
 * Sarbide kontrol zorrotza aplikatzeko oinarria.
 * 
 * SEGURTASUNA: Kodea bakarrik SharedPreferences-en gordetzen da, ez da inoiz Intent-etan
 * edo beste lekuetan erabiltzen behar kodea zuzenean. SessionManager bidez bakarrik.
 */
public class SessionManager {

    private static final String ETIKETA = "SessionManager";
    private static final String PREF_IZENA = "AppKomertziala_Session";
    private static final String GAKOA_KOMMERTZIALA_KODEA = "komertzial_kodea";
    private static final String GAKOA_KOMMERTZIALA_IZENA = "komertzial_izena";

    private final SharedPreferences sharedPreferences;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = this.context.getSharedPreferences(PREF_IZENA, Context.MODE_PRIVATE);
    }

    /**
     * Saioa hasi: komertzialaren kodea eta izena gorde.
     * Login arrakastatsu baten ondoren deitu behar da.
     * 
     * @param komertzialKodea Komertzialaren kodea (NAN edo identifikatzailea)
     * @param komertzialIzena Komertzialaren izena (erakusteko)
     */
    public void saioaHasi(String komertzialKodea, String komertzialIzena) {
        if (komertzialKodea == null || komertzialKodea.trim().isEmpty()) {
            Log.w(ETIKETA, "saioaHasi: komertzialKodea hutsik dago, saioa ez da hasiko");
            return;
        }
        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(GAKOA_KOMMERTZIALA_KODEA, komertzialKodea.trim());
        if (komertzialIzena != null) {
            editor.putString(GAKOA_KOMMERTZIALA_IZENA, komertzialIzena.trim());
        } else {
            editor.putString(GAKOA_KOMMERTZIALA_IZENA, "");
        }
        editor.apply();
        
        Log.d(ETIKETA, "Saioa hasi da komertzialarekin: " + komertzialKodea);
    }

    /**
     * Saioa itxi: komertzialaren datuak ezabatu.
     * Logout baten ondoren deitu behar da.
     */
    public void saioaItxi() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(GAKOA_KOMMERTZIALA_KODEA);
        editor.remove(GAKOA_KOMMERTZIALA_IZENA);
        editor.apply();
        
        Log.d(ETIKETA, "Saioa itxi da");
    }

    /**
     * Uneko komertzialaren kodea itzuli.
     * SEGURTASUNA: Kodea bakarrik irakurtzeko erabili, ez inoiz aldatzeko edo beste erabiltzaile baten kodea jartzeko.
     * 
     * @return Komertzialaren kodea, null saioa hasi gabe badago
     */
    public String getKomertzialKodea() {
        String kodea = sharedPreferences.getString(GAKOA_KOMMERTZIALA_KODEA, null);
        if (kodea != null) {
            kodea = kodea.trim();
        }
        return kodea != null && !kodea.isEmpty() ? kodea : null;
    }

    /**
     * Uneko komertzialaren izena itzuli (erakusteko).
     * 
     * @return Komertzialaren izena, null saioa hasi gabe badago
     */
    public String getKomertzialIzena() {
        String izena = sharedPreferences.getString(GAKOA_KOMMERTZIALA_IZENA, null);
        if (izena != null) {
            izena = izena.trim();
        }
        return izena != null && !izena.isEmpty() ? izena : null;
    }

    /**
     * Saioa hasita dagoen egiaztatu.
     * 
     * @return true saioa hasita badago (komertzial kodea existitzen bada), false bestela
     */
    public boolean saioaHasitaDago() {
        String kodea = getKomertzialKodea();
        boolean hasita = kodea != null && !kodea.isEmpty();
        if (!hasita) {
            Log.d(ETIKETA, "Saioa ez dago hasita");
        }
        return hasita;
    }

    /**
     * Komertzial kodea balidatu: uneko saioaren kodea dela egiaztatu.
     * SEGURTASUNA: Datuak atzitzean, kodea uneko saioaren kodea dela egiaztatu behar da.
     * 
     * @param kodea Egiaztatu behar den kodea
     * @return true kodea uneko saioaren kodea bada, false bestela
     */
    public boolean kodeaBalidatu(String kodea) {
        if (kodea == null || kodea.trim().isEmpty()) {
            return false;
        }
        String unekoKodea = getKomertzialKodea();
        boolean baliozkoa = unekoKodea != null && unekoKodea.equals(kodea.trim());
        if (!baliozkoa) {
            Log.w(ETIKETA, "Kodea balidatu: kodea ez da uneko saioaren kodea. Esperatua: " + unekoKodea + ", Jasotakoa: " + kodea);
        }
        return baliozkoa;
    }
}

