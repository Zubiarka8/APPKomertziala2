package com.example.appkomertziala.db.eredua;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Eskaera xehetasuna: ID, Eskaera_Zenbakia, Artikulu_Kodea, Kantitatea, Prezioa.
 * Loturak: eskaeraZenbakia → EskaeraGoiburua.zenbakia, artikuluKodea → Katalogoa (aplikazioan; migrazioek ez dute FK sortzen).
 */
@Entity(
    tableName = "eskaera_xehetasunak",
    indices = {@Index("eskaeraZenbakia"), @Index("artikuluKodea")}
)
public class EskaeraXehetasuna {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Lotutako eskaera goiburuaren zenbakia. */
    private String eskaeraZenbakia;
    /** Katalogoko artikuluaren kodea. */
    private String artikuluKodea;
    private int kantitatea;
    private double prezioa;

    public EskaeraXehetasuna() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getEskaeraZenbakia() { return eskaeraZenbakia; }
    public void setEskaeraZenbakia(String eskaeraZenbakia) { this.eskaeraZenbakia = eskaeraZenbakia; }
    public String getArtikuluKodea() { return artikuluKodea; }
    public void setArtikuluKodea(String artikuluKodea) { this.artikuluKodea = artikuluKodea; }
    public int getKantitatea() { return kantitatea; }
    public void setKantitatea(int kantitatea) { this.kantitatea = kantitatea; }
    public double getPrezioa() { return prezioa; }
    public void setPrezioa(double prezioa) { this.prezioa = prezioa; }
}
