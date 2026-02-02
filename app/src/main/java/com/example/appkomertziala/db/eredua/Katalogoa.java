package com.example.appkomertziala.db.eredua;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Katalogoa entitatea: artikulu_kodea (PK), izena, salmenta_prezioa, stock_a, irudia_izena.
 * irudia_izena: res/drawable baliabidearen izena (adib. macbook.jpg → drawable macbook).
 * Produktuen katalogoa; asteko inportazioarekin eguneratzen da.
 */
@Entity(tableName = "katalogoa")
public class Katalogoa {

    @NonNull
    @PrimaryKey
    private String artikuluKodea;

    private String izena;

    /** Salmenta-prezioa (ordezkaritzatik jasotakoa). */
    private double salmentaPrezioa;

    /** Stock kopurua; eskaera bat egiten denean stockaEguneratu() deitzen da. */
    private int stock;

    /** Irudi baliabidearen izena (drawable), adib. macbook.jpg. */
    @ColumnInfo(name = "irudia_izena")
    private String irudiaIzena;

    public Katalogoa() {}

    @Ignore
    public Katalogoa(String artikuluKodea, String izena, double salmentaPrezioa, int stock, String irudiaIzena) {
        this.artikuluKodea = artikuluKodea;
        this.izena = izena;
        this.salmentaPrezioa = salmentaPrezioa;
        this.stock = stock;
        this.irudiaIzena = irudiaIzena;
    }

    /** Atzera-bateragarritasunerako: irudia_izena gabe (null). */
    @Ignore
    public Katalogoa(String artikuluKodea, String izena, double salmentaPrezioa, int stock) {
        this(artikuluKodea, izena, salmentaPrezioa, stock, null);
    }

    public String getArtikuluKodea() { return artikuluKodea; }
    public void setArtikuluKodea(String artikuluKodea) { this.artikuluKodea = artikuluKodea; }
    public String getIzena() { return izena; }
    public void setIzena(String izena) { this.izena = izena; }
    public double getSalmentaPrezioa() { return salmentaPrezioa; }
    public void setSalmentaPrezioa(double salmentaPrezioa) { this.salmentaPrezioa = salmentaPrezioa; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getIrudiaIzena() { return irudiaIzena; }
    public void setIrudiaIzena(String irudiaIzena) { this.irudiaIzena = irudiaIzena; }
}
