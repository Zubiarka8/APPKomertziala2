package com.example.appkomertziala.db.eredua;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Partnerra entitatea: ID, Kodea, Izena, Helbidea, Probintzia, Komertzial_Kodea (kanpo-gakoa).
 * Partner bakoitza komertzial bakar bati lotuta dago.
 */
@Entity(
    tableName = "partnerrak",
    indices = {@Index("komertzialKodea")},
    foreignKeys = @ForeignKey(
        entity = Komertziala.class,
        parentColumns = "kodea",
        childColumns = "komertzialKodea",
        onDelete = ForeignKey.CASCADE
    )
)
public class Partnerra {

    @PrimaryKey
    private long id;

    /** Partnerraren kode identifikatzailea. */
    private String kodea;

    private String izena;
    private String helbidea;

    /** Probintzia (aukerakoa). */
    private String probintzia;

    /** Lotutako komertzialaren kodea (kanpo-gakoa). */
    private String komertzialKodea;

    /**
     * Alta-data (yyyy-MM-dd). Eguneroko txostenean eguneko bazkide berriak iragazteko erabiltzen da.
     * Room 2. migrazioa gehitu zuen.
     */
    private String sortutakoData;

    public Partnerra() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getKodea() { return kodea; }
    public void setKodea(String kodea) { this.kodea = kodea; }
    public String getIzena() { return izena; }
    public void setIzena(String izena) { this.izena = izena; }
    public String getHelbidea() { return helbidea; }
    public void setHelbidea(String helbidea) { this.helbidea = helbidea; }
    public String getProbintzia() { return probintzia; }
    public void setProbintzia(String probintzia) { this.probintzia = probintzia; }
    public String getKomertzialKodea() { return komertzialKodea; }
    public void setKomertzialKodea(String komertzialKodea) { this.komertzialKodea = komertzialKodea; }
    public String getSortutakoData() { return sortutakoData; }
    public void setSortutakoData(String sortutakoData) { this.sortutakoData = sortutakoData; }
}
