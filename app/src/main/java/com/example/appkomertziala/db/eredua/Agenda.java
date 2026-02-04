package com.example.appkomertziala.db.eredua;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Agenda entitatea: bisita bakoitzaren erregistroa.
 * Eremuak: gako nagusia (id), bisita_data, bazkidea_kodea, bazkideaId (kanpo-gakoa Bazkidea.id),
 * komertzialaId (kanpo-gakoa Komertziala.id, bisita sortu duen komertziala), deskribapena, egoera.
 * 
 * Foreign Keys:
 * - bazkideaId → Bazkidea.id (ON DELETE SET NULL - bazkidea ezabatzen bada, bisita mantendu baina bazkideaId null)
 * - komertzialaId → Komertziala.id (ON DELETE SET NULL - komertziala ezabatzen bada, bisita mantendu baina komertzialaId null)
 */
@Entity(
    tableName = "agenda_bisitak",
    indices = {
        @Index("bazkideaKodea"),
        @Index("bisitaData"),
        @Index("bazkideaId"),
        @Index("komertzialaId"),
        @Index("komertzialKodea"),
        @Index(value = {"komertzialKodea", "bazkideaKodea", "bisitaData"}, unique = false)
    },
    foreignKeys = {
        @ForeignKey(
            entity = Bazkidea.class,
            parentColumns = "id",
            childColumns = "bazkideaId",
            onDelete = ForeignKey.SET_NULL
        ),
        @ForeignKey(
            entity = Komertziala.class,
            parentColumns = "id",
            childColumns = "komertzialaId",
            onDelete = ForeignKey.SET_NULL
        )
    }
)
public class Agenda {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Bisitaren data (yyyy-MM-dd). */
    private String bisitaData;

    /** Bisitaren ordua (HH:mm formatua). */
    private String ordua;

    /** Lotutako komertzialaren kodea (komertzialak taulako kodea; erakusteko eta sinkronizaziorako). */
    private String komertzialKodea;

    /** Lotutako bazkidearen kodea (bazkideak taulako kodea eremua; erakusteko). */
    private String bazkideaKodea;

    /**
     * Lotutako bazkidearen gako nagusia (kanpo-gakoa: Bazkidea taulako id).
     * Bisitatzen den pertsona (bazkidea).
     */
    private Long bazkideaId;

    /**
     * Bisita sortu duen komertzialaren gako nagusia (kanpo-gakoa: Komertzialak taulako id).
     * Agenda sartu duen pertsonaren IDa.
     */
    private Long komertzialaId;

    /** Bisitaren deskribapena edo oharra. */
    private String deskribapena;

    /**
     * Egoera: Egina, Zain, Deuseztatua.
     * Egina = burututa; Zain = zain; Deuseztatua = bertan behera utzia.
     */
    private String egoera;

    public Agenda() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getBisitaData() { return bisitaData; }
    public void setBisitaData(String bisitaData) { this.bisitaData = bisitaData; }
    public String getOrdua() { return ordua; }
    public void setOrdua(String ordua) { this.ordua = ordua; }
    public String getKomertzialKodea() { return komertzialKodea; }
    public void setKomertzialKodea(String komertzialKodea) { this.komertzialKodea = komertzialKodea; }
    public String getBazkideaKodea() { return bazkideaKodea; }
    public void setBazkideaKodea(String bazkideaKodea) { this.bazkideaKodea = bazkideaKodea; }
    public Long getBazkideaId() { return bazkideaId; }
    public void setBazkideaId(Long bazkideaId) { this.bazkideaId = bazkideaId; }
    public Long getKomertzialaId() { return komertzialaId; }
    public void setKomertzialaId(Long komertzialaId) { this.komertzialaId = komertzialaId; }
    public String getDeskribapena() { return deskribapena; }
    public void setDeskribapena(String deskribapena) { this.deskribapena = deskribapena; }
    public String getEgoera() { return egoera; }
    public void setEgoera(String egoera) { this.egoera = egoera; }
}
