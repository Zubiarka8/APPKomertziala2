package com.example.appkomertziala.db.eredua;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Komertziala entitatea: ID, Izena, Kodea (NAN).
 * Komertzialak taularen erlazio-diagrama jarraituz.
 */
@Entity(tableName = "komertzialak", indices = {@Index(value = "kodea", unique = true)})
public class Komertziala {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Komertzialaren izen osoa (izen + abizen). */
    private String izena;

    /** Komertzialaren kode bakarra (NAN, erreferentzia gisa). */
    private String kodea;

    public Komertziala() {}

    @Ignore
    public Komertziala(String izena, String kodea) {
        this.izena = izena;
        this.kodea = kodea;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getIzena() { return izena; }
    public void setIzena(String izena) { this.izena = izena; }
    public String getKodea() { return kodea; }
    public void setKodea(String kodea) { this.kodea = kodea; }
}
