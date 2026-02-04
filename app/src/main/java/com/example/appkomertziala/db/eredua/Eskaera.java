package com.example.appkomertziala.db.eredua;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Eskaera entitatea: bazkideak.xml-eko <eskaerak> blokea.
 * 
 * XML-etik datozen eremuak:
 * - eskaeraID: Eskaeraren identifikatzailea
 * - prodIzena: Produktuaren izena
 * - data: Eskaeraren data
 * - kopurua: Produktu kopurua
 * - prodArgazkia: Produktu argazkiaren izena
 * 
 * Taula: eskaerak
 * Foreign Key: bazkideaId -> bazkideak.id (ON DELETE CASCADE)
 * Indizeak: bazkideaId, eskaeraID
 */
@Entity(
    tableName = "eskaerak",
    indices = {@Index("bazkideaId"), @Index("eskaeraID")},
    foreignKeys = @ForeignKey(
        entity = Bazkidea.class,
        parentColumns = "id",
        childColumns = "bazkideaId",
        onDelete = ForeignKey.CASCADE
    )
)
public class Eskaera {

    /** Gako nagusia (auto-sortua). */
    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Eskaeraren identifikatzailea (XML-etik). */
    private String eskaeraID;

    /** Lotutako bazkidearen ID (Foreign Key). */
    private long bazkideaId;

    /** Produktuaren izena. */
    private String prodIzena;

    /** Eskaeraren data. */
    private String data;

    /** Produktu kopurua. */
    private int kopurua;

    /** Produktu argazkiaren izena. */
    private String prodArgazkia;

    /** Eraikitzaile hutsa (Room-entzat). */
    public Eskaera() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getEskaeraID() { return eskaeraID; }
    public void setEskaeraID(String eskaeraID) { this.eskaeraID = eskaeraID; }
    public long getBazkideaId() { return bazkideaId; }
    public void setBazkideaId(long bazkideaId) { this.bazkideaId = bazkideaId; }
    public String getProdIzena() { return prodIzena; }
    public void setProdIzena(String prodIzena) { this.prodIzena = prodIzena; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public int getKopurua() { return kopurua; }
    public void setKopurua(int kopurua) { this.kopurua = kopurua; }
    public String getProdArgazkia() { return prodArgazkia; }
    public void setProdArgazkia(String prodArgazkia) { this.prodArgazkia = prodArgazkia; }
}

