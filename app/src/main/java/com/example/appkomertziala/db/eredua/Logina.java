package com.example.appkomertziala.db.eredua;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Logina entitatea: Erabiltzailea (email), Pasahitza, Komertzial_Kodea.
 * Sarbide kredentzialak.
 */
@Entity(tableName = "loginak", indices = {@Index("komertzialKodea")})
public class Logina {

    /** Erabiltzaile identifikatzailea (posta helbidea, gako nagusia). */
    @NonNull
    @PrimaryKey
    private String erabiltzailea;

    /** Erabiltzailearen pasahitza. */
    private String pasahitza;
    /** Lotutako komertzialaren kodea (sarbidea balioztatu ondoren zein komertzial den jakiteko). */
    private String komertzialKodea;

    /** Eraikitzaile hutsa (Room-entzat). */
    public Logina() {}

    /**
     * Eraikitzaile osoa (loginak.xml inportazioetarako).
     *
     * @param erabiltzailea Posta helbidea
     * @param pasahitza Pasahitza
     * @param komertzialKodea Lotutako komertzialaren kodea
     */
    @Ignore
    public Logina(String erabiltzailea, String pasahitza, String komertzialKodea) {
        this.erabiltzailea = erabiltzailea;
        this.pasahitza = pasahitza;
        this.komertzialKodea = komertzialKodea;
    }

    public String getErabiltzailea() { return erabiltzailea; }
    public void setErabiltzailea(String erabiltzailea) { this.erabiltzailea = erabiltzailea; }
    public String getPasahitza() { return pasahitza; }
    public void setPasahitza(String pasahitza) { this.pasahitza = pasahitza; }
    public String getKomertzialKodea() { return komertzialKodea; }
    public void setKomertzialKodea(String komertzialKodea) { this.komertzialKodea = komertzialKodea; }
}
