package com.example.appkomertziala.db.eredua;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Komertziala entitatea: ID, Izena, Kodea (NAN), abizena, posta, jaiotzeData, argazkia.
 * Komertzialak taularen erlazio-diagrama eta komertzialak.xml egitura (migrazio 9–11) bateratuta.
 */
@Entity(tableName = "komertzialak", indices = {@Index(value = "kodea", unique = true)})
public class Komertziala {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Komertzialaren izen osoa edo izena. */
    private String izena;

    /** Komertzialaren kode bakarra (NAN, erreferentzia gisa). */
    private String kodea;

    private String abizena;
    private String posta;
    private String jaiotzeData;
    private String argazkia;

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
    public String getAbizena() { return abizena; }
    public void setAbizena(String abizena) { this.abizena = abizena; }
    public String getPosta() { return posta; }
    public void setPosta(String posta) { this.posta = posta; }
    public String getJaiotzeData() { return jaiotzeData; }
    public void setJaiotzeData(String jaiotzeData) { this.jaiotzeData = jaiotzeData; }
    public String getArgazkia() { return argazkia; }
    public void setArgazkia(String argazkia) { this.argazkia = argazkia; }
}
