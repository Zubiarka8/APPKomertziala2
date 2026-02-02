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

    @NonNull
    @PrimaryKey
    /** Erabiltzaile identifikatzailea (posta helbidea). */
    private String erabiltzailea;

    private String pasahitza;
    /** Lotutako komertzialaren kodea. */
    private String komertzialKodea;

    public Logina() {}

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
