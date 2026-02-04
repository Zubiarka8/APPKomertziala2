package com.example.appkomertziala.db.eredua;

import androidx.room.Entity;
import androidx.room.ForeignKey;
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
 * Foreign Keys:
 * - eskaeraZenbakia → EskaeraGoiburua.zenbakia (ON DELETE CASCADE)
 * - komertzialId → Komertziala.id (ON DELETE SET NULL)
 * - bazkideaId → Bazkidea.id (ON DELETE SET NULL)
 * - productoId → Katalogoa.artikuluKodea (ON DELETE RESTRICT - produktua ezabatu ezin da historiala badaude)
 * Indizeak: kodea, data, productoId, bidalketaId, eskaeraZenbakia, komertzialId, bazkideaId
 */
@Entity(
    tableName = "historial_compras",
    indices = {
        @Index("kodea"),
        @Index("data"),
        @Index("productoId"),
        @Index("bidalketaId"),
        @Index("eskaeraZenbakia"),
        @Index("komertzialId"),
        @Index("bazkideaId")
    },
    foreignKeys = {
        @ForeignKey(
            entity = EskaeraGoiburua.class,
            parentColumns = "zenbakia",
            childColumns = "eskaeraZenbakia",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Komertziala.class,
            parentColumns = "id",
            childColumns = "komertzialId",
            onDelete = ForeignKey.SET_NULL
        ),
        @ForeignKey(
            entity = Bazkidea.class,
            parentColumns = "id",
            childColumns = "bazkideaId",
            onDelete = ForeignKey.SET_NULL
        ),
        @ForeignKey(
            entity = Katalogoa.class,
            parentColumns = "artikuluKodea",
            childColumns = "productoId",
            onDelete = ForeignKey.RESTRICT
        )
    }
)
public class HistorialCompra {

    /** Gako nagusia (auto-sortua). */
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

    /** Lotutako eskaera goiburuaren zenbakia (Foreign Key → EskaeraGoiburua.zenbakia). */
    private String eskaeraZenbakia;

    /** Lotutako komertzialaren ID (Foreign Key → Komertziala.id). */
    private Long komertzialId;

    /** Lotutako bazkidearen ID (Foreign Key → Bazkidea.id). */
    private Long bazkideaId;

    /** Eraikitzaile hutsa (Room-entzat). */
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

    public String getEskaeraZenbakia() {
        return eskaeraZenbakia;
    }

    public void setEskaeraZenbakia(String eskaeraZenbakia) {
        this.eskaeraZenbakia = eskaeraZenbakia;
    }

    public Long getKomertzialId() {
        return komertzialId;
    }

    public void setKomertzialId(Long komertzialId) {
        this.komertzialId = komertzialId;
    }

    public Long getBazkideaId() {
        return bazkideaId;
    }

    public void setBazkideaId(Long bazkideaId) {
        this.bazkideaId = bazkideaId;
    }
}

