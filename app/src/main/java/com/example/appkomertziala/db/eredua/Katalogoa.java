package com.example.appkomertziala.db.eredua;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Katalogoa entitatea: Artikulu_Kodea, Izena, Salmenta_Prezioa, Stock-a.
 * Produktuen katalogoa.
 */
@Entity(tableName = "katalogoa")
public class Katalogoa {

    @NonNull
    @PrimaryKey
    private String artikuluKodea;

    private String izena;
    /** Salmenta-prezioa. */
    private double salmentaPrezioa;
    /** Stock kopurua. */
    private int stock;

    public Katalogoa() {}

    @Ignore
    public Katalogoa(String artikuluKodea, String izena, double salmentaPrezioa, int stock) {
        this.artikuluKodea = artikuluKodea;
        this.izena = izena;
        this.salmentaPrezioa = salmentaPrezioa;
        this.stock = stock;
    }

    public String getArtikuluKodea() { return artikuluKodea; }
    public void setArtikuluKodea(String artikuluKodea) { this.artikuluKodea = artikuluKodea; }
    public String getIzena() { return izena; }
    public void setIzena(String izena) { this.izena = izena; }
    public double getSalmentaPrezioa() { return salmentaPrezioa; }
    public void setSalmentaPrezioa(double salmentaPrezioa) { this.salmentaPrezioa = salmentaPrezioa; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}
