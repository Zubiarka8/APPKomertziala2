package com.example.appkomertziala.db.eredua;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Agenda entitatea: bisita bakoitzaren erregistroa.
 * Eremuak: gako nagusia (id), bisita_data, partner_kodea, partnerId (kanpo-gakoa Partnerra.id),
 * komertzialaId (kanpo-gakoa Komertziala.id, bisita sortu duen komertziala), deskribapena, egoera.
 */
@Entity(
    tableName = "agenda_bisitak",
    indices = {
        @Index("partnerKodea"),
        @Index("bisitaData"),
        @Index("partnerId"),
        @Index("komertzialaId")
    }
)
public class Agenda {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Bisitaren data (yyyy-MM-dd edo yyyy-MM-dd HH:mm). */
    private String bisitaData;

    /** Lotutako partnerraren/bazkidearen kodea (partnerrak taulako kodea eremua; erakusteko). */
    private String partnerKodea;

    /**
     * Lotutako partnerraren edo bazkidearen gako nagusia (kanpo-gakoa: Partnerra taulako id).
     * Bisitatzen den pertsona (partnerra edo bazkidea).
     */
    private Long partnerId;

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
    public String getPartnerKodea() { return partnerKodea; }
    public void setPartnerKodea(String partnerKodea) { this.partnerKodea = partnerKodea; }
    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }
    public Long getKomertzialaId() { return komertzialaId; }
    public void setKomertzialaId(Long komertzialaId) { this.komertzialaId = komertzialaId; }
    public String getDeskribapena() { return deskribapena; }
    public void setDeskribapena(String deskribapena) { this.deskribapena = deskribapena; }
    public String getEgoera() { return egoera; }
    public void setEgoera(String egoera) { this.egoera = egoera; }
}
