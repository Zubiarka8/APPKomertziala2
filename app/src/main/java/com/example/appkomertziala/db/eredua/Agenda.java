package com.example.appkomertziala.db.eredua;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Agenda entitatea: bisita bakoitzaren erregistroa.
 * Eremuak: gako nagusia (id), bisita_data, partner_kodea (kanpo-gakoa), deskribapena, egoera (Egina, Zain, Deuseztatua).
 */
@Entity(
    tableName = "agenda_bisitak",
    indices = {@Index("partnerKodea"), @Index("bisitaData")}
)
public class Agenda {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Bisitaren data (yyyy-MM-dd edo yyyy-MM-dd HH:mm). */
    private String bisitaData;

    /** Lotutako partnerraren kodea (kanpo-gakoa; partnerrak taulako kodea eremua). */
    private String partnerKodea;

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
    public String getDeskribapena() { return deskribapena; }
    public void setDeskribapena(String deskribapena) { this.deskribapena = deskribapena; }
    public String getEgoera() { return egoera; }
    public void setEgoera(String egoera) { this.egoera = egoera; }
}
