package com.example.appkomertziala.segurtasuna;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Data balidatzailea: datuen formatua eta baliozkotasuna egiaztatzen du.
 * 
 * Formatu eskatua: yyyy/MM/dd (urtea/mes/eguna, barra garez bananduta)
 * 
 * Balidazioak:
 * - Formatu zuzena: yyyy/MM/dd
 * - Data baliozkoa: urtea, hilabetea eta eguna baliozkoak dira
 * - Egunak hilabetearen arabera baliozkoak dira (adibidez, otsailak 28/29 egun bakarrik)
 * - Urte bisurteak kontuan hartzen dira
 */
public class DataBalidatzailea {

    private static final String ETIKETA = "DataBalidatzailea";
    private static final String DATA_FORMAT = "yyyy/MM/dd";
    private static final String DATA_FORMAT_ALTERNATIBOA = "yyyy-MM-dd"; // Formatua bihurtzeko

    /**
     * Data balidatu: formatua eta baliozkotasuna egiaztatu.
     * 
     * @param data Balidatu behar den data
     * @return true baliozkoa bada, false bestela
     */
    public static boolean balidatuData(String data) {
        if (data == null || data.trim().isEmpty()) {
            Log.w(ETIKETA, "balidatuData: Data null edo hutsa da");
            return false;
        }

        String dataTrimm = data.trim();
        
        // Lehenik formatua egiaztatu: yyyy/MM/dd
        if (!dataTrimm.matches("\\d{4}/\\d{2}/\\d{2}")) {
            Log.w(ETIKETA, "balidatuData: Formatu okerra - " + dataTrimm + " (esperotakoa: yyyy/MM/dd)");
            return false;
        }

        // Data parseatu eta baliozkotasuna egiaztatu
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
            sdf.setLenient(false); // Garrantzitsua: datu baliozkoak bakarrik onartzen ditu
            Date parsedDate = sdf.parse(dataTrimm);
            
            // Parseatutako data formatu berarekin konparatu (gehiegikeriak saihesteko)
            String berriroFormatua = sdf.format(parsedDate);
            if (!berriroFormatua.equals(dataTrimm)) {
                Log.w(ETIKETA, "balidatuData: Data ez da baliozkoa - " + dataTrimm);
                return false;
            }
            
            // Data gaurkoa edo etorkizunekoa izan daiteke (ez da muga jarri)
            Log.d(ETIKETA, "balidatuData: Data baliozkoa da - " + dataTrimm);
            return true;
            
        } catch (ParseException e) {
            Log.w(ETIKETA, "balidatuData: Parse errorea - " + dataTrimm + ": " + (e.getMessage() != null ? e.getMessage() : "ezezaguna"));
            return false;
        }
    }

    /**
     * Data balidatu eta salbuespena jaurti baliozkoa ez bada.
     * 
     * @param data Balidatu behar den data
     * @throws IllegalArgumentException Data baliozkoa ez bada
     */
    public static void balidatuDataEtaJaurti(String data) throws IllegalArgumentException {
        if (!balidatuData(data)) {
            String mezu = "Errorea: Data formatua okerra edo baliozkoa ez da. Formatu eskatua: yyyy/MM/dd (adibidez: 2024/12/31)";
            Log.e(ETIKETA, mezu + " - Data jaso: " + data);
            throw new IllegalArgumentException(mezu);
        }
    }

    /**
     * Data formatua bihurtu yyyy-MM-dd formatutik yyyy/MM/dd formatura.
     * 
     * @param data Data yyyy-MM-dd formatuan
     * @return Data yyyy/MM/dd formatuan, null baliozkoa ez bada
     */
    public static String bihurtuFormatua(String data) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }

        String dataTrimm = data.trim();
        
        // Jada yyyy/MM/dd formatuan badago, itzuli
        if (dataTrimm.matches("\\d{4}/\\d{2}/\\d{2}")) {
            return dataTrimm;
        }

        // yyyy-MM-dd formatutik bihurtu
        if (dataTrimm.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return dataTrimm.replace("-", "/");
        }

        // Beste formatu bat bada, parseatu eta bihurtu
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat(DATA_FORMAT_ALTERNATIBOA, Locale.getDefault());
            sdfInput.setLenient(false);
            Date parsedDate = sdfInput.parse(dataTrimm);
            
            SimpleDateFormat sdfOutput = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
            return sdfOutput.format(parsedDate);
        } catch (ParseException e) {
            Log.w(ETIKETA, "bihurtuFormatua: Parse errorea - " + dataTrimm);
            return null;
        }
    }

    /**
     * Gaurko data itzuli yyyy/MM/dd formatuan.
     * 
     * @return Gaurko data formatu zuzenarekin
     */
    public static String gaurkoData() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Data formatua egiaztatu (yyyy/MM/dd) eta errore-mezua itzuli.
     * 
     * @param data Balidatu behar den data
     * @return null baliozkoa bada, errore-mezua bestela
     */
    public static String balidatuDataMezua(String data) {
        if (data == null || data.trim().isEmpty()) {
            return "Data beharrezkoa da";
        }
        
        if (!balidatuData(data)) {
            return "Data formatua okerra edo baliozkoa ez da. Formatu eskatua: yyyy/MM/dd (adibidez: 2024/12/31)";
        }
        
        return null;
    }

    /**
     * Data balidatu eta egiaztatu gaurko data baino lehenagokoa dela (jaiotze data bezala).
     * 
     * @param data Balidatu behar den data (jaiotze data)
     * @return true baliozkoa bada eta gaurko data baino lehenagokoa bada, false bestela
     */
    public static boolean balidatuJaiotzeData(String data) {
        if (!balidatuData(data)) {
            return false;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
            sdf.setLenient(false);
            Date parsedDate = sdf.parse(data.trim());
            
            // Gaurko data lortu (ordua 00:00:00)
            Calendar gaur = Calendar.getInstance();
            gaur.set(Calendar.HOUR_OF_DAY, 0);
            gaur.set(Calendar.MINUTE, 0);
            gaur.set(Calendar.SECOND, 0);
            gaur.set(Calendar.MILLISECOND, 0);
            Date gaurkoData = gaur.getTime();
            
            // Parseatutako data normalizatu (ordua 00:00:00)
            Calendar dataCal = Calendar.getInstance();
            dataCal.setTime(parsedDate);
            dataCal.set(Calendar.HOUR_OF_DAY, 0);
            dataCal.set(Calendar.MINUTE, 0);
            dataCal.set(Calendar.SECOND, 0);
            dataCal.set(Calendar.MILLISECOND, 0);
            Date dataNormalizatua = dataCal.getTime();
            
            // Data gaurko data baino lehenagokoa izan behar du (ez da gaurkoa izan behar)
            if (dataNormalizatua.after(gaurkoData) || dataNormalizatua.equals(gaurkoData)) {
                Log.w(ETIKETA, "balidatuJaiotzeData: Data gaurko data baino berriagoa edo berdina da - " + data);
                return false;
            }
            
            Log.d(ETIKETA, "balidatuJaiotzeData: Jaiotze data baliozkoa da - " + data);
            return true;
            
        } catch (ParseException e) {
            Log.w(ETIKETA, "balidatuJaiotzeData: Parse errorea - " + data);
            return false;
        }
    }

    /**
     * Jaiotze data balidatu eta salbuespena jaurti baliozkoa ez bada.
     * 
     * @param data Balidatu behar den jaiotze data
     * @throws IllegalArgumentException Data baliozkoa ez bada edo etorkizunekoa bada
     */
    public static void balidatuJaiotzeDataEtaJaurti(String data) throws IllegalArgumentException {
        if (!balidatuData(data)) {
            String mezu = "Errorea: Data formatua okerra edo baliozkoa ez da. Formatu eskatua: yyyy/MM/dd (adibidez: 2024/12/31)";
            Log.e(ETIKETA, mezu + " - Data jaso: " + data);
            throw new IllegalArgumentException(mezu);
        }
        
        if (!balidatuJaiotzeData(data)) {
            String mezu = "Errorea: Sartutako data ezin da gaurko eguna baino handiagoa izan.";
            Log.e(ETIKETA, mezu + " - Data jaso: " + data);
            throw new IllegalArgumentException(mezu);
        }
    }

    /**
     * Jaiotze data formatua egiaztatu eta errore-mezua itzuli.
     * 
     * @param data Balidatu behar den jaiotze data
     * @return null baliozkoa bada, errore-mezua bestela
     */
    public static String balidatuJaiotzeDataMezua(String data) {
        if (data == null || data.trim().isEmpty()) {
            return "Jaiotze data beharrezkoa da";
        }
        
        // Lehenik formatua balidatu
        String formatuErrorea = balidatuDataMezua(data);
        if (formatuErrorea != null) {
            return formatuErrorea;
        }
        
        // Gero egiaztatu gaurko data baino lehenagokoa dela
        if (!balidatuJaiotzeData(data)) {
            return "Sartutako data ezin da gaurko eguna baino handiagoa izan.";
        }
        
        return null;
    }
}

