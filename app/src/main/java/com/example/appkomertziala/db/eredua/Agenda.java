package com.example.appkomertziala.db.eredua;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Agenda entitatea: bisita bakoitzaren erregistroa.
 * Eremuak: gako nagusia (id), bisita_data, bazkidea_kodea, bazkideaId (kanpo-gakoa Bazkidea.id),
 * komertzialaId (kanpo-gakoa Komertziala.id, bisita sortu duen komertziala), deskribapena, egoera.
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
    // Métodos de compatibilidad temporal (deprecated)
    @Deprecated
    public String getPartnerKodea() { return bazkideaKodea; }
    @Deprecated
    public void setPartnerKodea(String partnerKodea) { this.bazkideaKodea = partnerKodea; }
    @Deprecated
    public Long getPartnerId() { return bazkideaId; }
    @Deprecated
    public void setPartnerId(Long partnerId) { this.bazkideaId = partnerId; }
    public Long getKomertzialaId() { return komertzialaId; }
    public void setKomertzialaId(Long komertzialaId) { this.komertzialaId = komertzialaId; }
    public String getDeskribapena() { return deskribapena; }
    public void setDeskribapena(String deskribapena) { this.deskribapena = deskribapena; }
    public String getEgoera() { return egoera; }
    public void setEgoera(String egoera) { this.egoera = egoera; }
}
