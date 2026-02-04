package com.example.appkomertziala.db.eredua;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Erosketen historiala (bidalketa) entitatea: bidalketa XML egitura gordetzeko.
 * 
 * XML-etik datozen eremuak (bidalketa_2_20260130140648.xml):
 * - BidalketaId: Bidalketaren IDa
 * - Kodea: Bidalketaren kodea (adib. BDK-002)
 * - Helmuga: Bidalketaren helmuga
 * - Data: Bidalketaren data (yyyy/MM/dd)
 * - Amaituta: Amaituta dagoen ala ez (true/false)
 * - Lerroak > Lerro:
 *   - ProductoId: Produktuaren IDa
 *   - Izena: Produktuaren izena
 *   - Eskatuta: Eskatutako kantitatea
 *   - Bidalita: Bidaltutako kantitatea
 *   - PrezioUnit: Prezio unitarioa
 *   - Argazkia: Produktuaren irudiaren izena (String/Testua)
 * 
 * Taula: historial_compras
 * Indizeak: kodea, data, productoId
 */
@Entity(
    tableName = "historial_compras",
    indices = {
        @Index("kodea"),
        @Index("data"),
        @Index("productoId"),
        @Index("bidalketaId")
    }
)
public class HistorialCompra {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Bidalketaren IDa (XML-eko BidalketaId). */
    private int bidalketaId;

    /** Bidalketaren kodea (XML-eko Kodea, adib. BDK-002). */
    private String kodea;

    /** Bidalketaren helmuga (XML-eko Helmuga). */
    private String helmuga;

    /** Bidalketaren data (XML-eko Data, formatua: yyyy/MM/dd edo yyyy-MM-dd). */
    private String data;

    /** Bidalketa amaituta dagoen ala ez (XML-eko Amaituta: true/false). */
    private boolean amaituta;

    /** Produktuaren IDa (XML-eko ProductoId). */
    private String productoId;

    /** Produktuaren izena (XML-eko Izena). */
    private String productoIzena;

    /** Eskatutako kantitatea (XML-eko Eskatuta). */
    private int eskatuta;

    /** Bidaltutako kantitatea (XML-eko Bidalita). */
    private int bidalita;

    /** Prezio unitarioa (XML-eko PrezioUnit). */
    private double prezioUnit;

    /** Produktuaren irudiaren izena (XML-eko Argazkia, String/Testua). */
    private String argazkia;

    public HistorialCompra() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getBidalketaId() {
        return bidalketaId;
    }

    public void setBidalketaId(int bidalketaId) {
        this.bidalketaId = bidalketaId;
    }

    public String getKodea() {
        return kodea;
    }

    public void setKodea(String kodea) {
        this.kodea = kodea;
    }

    public String getHelmuga() {
        return helmuga;
    }

    public void setHelmuga(String helmuga) {
        this.helmuga = helmuga;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isAmaituta() {
        return amaituta;
    }

    public void setAmaituta(boolean amaituta) {
        this.amaituta = amaituta;
    }

    public String getProductoId() {
        return productoId;
    }

    public void setProductoId(String productoId) {
        this.productoId = productoId;
    }

    public String getProductoIzena() {
        return productoIzena;
    }

    public void setProductoIzena(String productoIzena) {
        this.productoIzena = productoIzena;
    }

    public int getEskatuta() {
        return eskatuta;
    }

    public void setEskatuta(int eskatuta) {
        this.eskatuta = eskatuta;
    }

    public int getBidalita() {
        return bidalita;
    }

    public void setBidalita(int bidalita) {
        this.bidalita = bidalita;
    }

    public double getPrezioUnit() {
        return prezioUnit;
    }

    public void setPrezioUnit(double prezioUnit) {
        this.prezioUnit = prezioUnit;
    }

    public String getArgazkia() {
        return argazkia;
    }

    public void setArgazkia(String argazkia) {
        this.argazkia = argazkia;
    }
}

