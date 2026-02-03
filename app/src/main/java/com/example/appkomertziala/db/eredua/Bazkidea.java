package com.example.appkomertziala.db.eredua;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Bazkidea entitatea: bazkideak.xml egitura garbia.
 * 
 * XML-etik datozen eremuak bakarrik (bazkideak.xml):
 * - NAN: Bazkidearen NAN identifikatzailea
 * - izena: Izena
 * - abizena: Abizena
 * - telefonoZenbakia: Telefono zenbakia
 * - posta: Posta elektronikoa
 * - jaiotzeData: Jaiotze data (yyyy/MM/dd formatuan)
 * - argazkia: Argazki fitxategiaren izena
 * 
 * Taula: bazkideak
 * Indizeak: nan, izena
 * 
 * Eskaerak: Bazkidea bakoitzak hainbat Eskaera izan ditzake (One-to-Many).
 */
@Entity(
    tableName = "bazkideak",
    indices = {@Index("nan"), @Index("izena")}
)
public class Bazkidea {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Bazkidearen NAN identifikatzailea. */
    private String nan;

    private String izena;
    private String abizena;
    private String telefonoZenbakia;
    private String posta;
    private String jaiotzeData;
    private String argazkia;

    public Bazkidea() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getNan() { return nan; }
    public void setNan(String nan) { this.nan = nan; }
    public String getIzena() { return izena; }
    public void setIzena(String izena) { this.izena = izena; }
    public String getAbizena() { return abizena; }
    public void setAbizena(String abizena) { this.abizena = abizena; }
    public String getTelefonoZenbakia() { return telefonoZenbakia; }
    public void setTelefonoZenbakia(String telefonoZenbakia) { this.telefonoZenbakia = telefonoZenbakia; }
    public String getPosta() { return posta; }
    public void setPosta(String posta) { this.posta = posta; }
    public String getJaiotzeData() { return jaiotzeData; }
    public void setJaiotzeData(String jaiotzeData) { this.jaiotzeData = jaiotzeData; }
    public String getArgazkia() { return argazkia; }
    public void setArgazkia(String argazkia) { this.argazkia = argazkia; }
}
