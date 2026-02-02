package com.example.appkomertziala.xml;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Assets-en dauden XML fitxategiak bilatzen ditu.
 * Falta diren fitxategiak zehazteko erabiltzen da; inportak ez dauden bitartean
 * sezio bakoitzean "xml-a 'fichero.xml' falta da" erakusteko.
 */
public final class XmlBilatzailea {

    /** Inportatu behar diren XML fitxategien izenak (orden egokian). */
    public static final List<String> XML_BEHARREZKOAK = Arrays.asList(
            "komertzialak.xml",
            "loginak.xml",
            "bazkideak.xml",
            "partnerrak.xml",
            "katalogoa.xml"
    );

    private XmlBilatzailea() {}

    /**
     * Assets-en fitxategi hori badagoen ala ez.
     */
    public static boolean faltaDa(Context context, String fitxategiIzena) {
        if (context == null || fitxategiIzena == null || fitxategiIzena.trim().isEmpty()) {
            return true;
        }
        try {
            context.getAssets().open(fitxategiIzena.trim()).close();
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * Assets-en falta diren XML fitxategien zerrenda itzultzen du.
     */
    public static List<String> faltatzenDiren(Context context) {
        List<String> falta = new ArrayList<>();
        if (context == null) return falta;
        for (String izena : XML_BEHARREZKOAK) {
            if (faltaDa(context, izena)) {
                falta.add(izena);
            }
        }
        return falta;
    }

    /** Login atalerako beharrezkoa: loginak.xml */
    public static boolean loginakFaltaDa(Context context) {
        return faltaDa(context, "loginak.xml");
    }

    /** Komertziala atalerako beharrezkoa: komertzialak.xml */
    public static boolean komertzialakFaltaDa(Context context) {
        return faltaDa(context, "komertzialak.xml");
    }

    /** Bazkideak atalerako beharrezkoa: bazkideak.xml */
    public static boolean bazkideakFaltaDa(Context context) {
        return faltaDa(context, "bazkideak.xml");
    }

    /** Partnerra atalerako beharrezkoa: partnerrak.xml */
    public static boolean partnerrakFaltaDa(Context context) {
        return faltaDa(context, "partnerrak.xml");
    }

    /** Katalogoa atalerako beharrezkoa: katalogoa.xml */
    public static boolean katalogoaFaltaDa(Context context) {
        return faltaDa(context, "katalogoa.xml");
    }
}
