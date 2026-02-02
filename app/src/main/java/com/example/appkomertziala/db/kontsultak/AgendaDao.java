package com.example.appkomertziala.db.kontsultak;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.appkomertziala.db.eredua.Agenda;

import java.util.List;

/**
 * Agenda (bisitak) taularen kontsultak: altak, bajak, aldaketak eta irakurketak.
 * Hilabetearen arabera iragazteko kontsulta espezifikoa eskaintzen du.
 */
@Dao
public interface AgendaDao {

    /** Bisita bat txertatu; gatazka bada ordezkatu. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long txertatu(Agenda agenda);

    /** Hainbat bisita txertatu. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> txertatuGuztiak(List<Agenda> zerrenda);

    /** Bisita bat eguneratu. */
    @Update
    int eguneratu(Agenda agenda);

    /** Bisita bat ezabatu. */
    @Delete
    int ezabatu(Agenda agenda);

    /** Bisita guztiak itzuli, data arabera ordenatuta (berrienak lehenik). */
    @Query("SELECT * FROM agenda_bisitak ORDER BY bisitaData DESC")
    List<Agenda> guztiak();

    /** Gako nagusiaren arabera bisita bat bilatu. */
    @Query("SELECT * FROM agenda_bisitak WHERE id = :id LIMIT 1")
    Agenda idzBilatu(long id);

    /**
     * Hilabetearen arabera bisitak itzuli.
     * Uneko hilabeteko bisita guztiak (hileroko agenda esportatzeko).
     */
    @Query("SELECT * FROM agenda_bisitak WHERE strftime('%Y-%m', bisitaData) = strftime('%Y-%m', 'now') ORDER BY bisitaData DESC")
    List<Agenda> hilabetearenBisitak();

    /**
     * Urte eta hilabete zehatzaren arabera bisitak itzuli.
     * @param urtea "yyyy" formatua
     * @param hilabetea "MM" formatua (01-12)
     */
    @Query("SELECT * FROM agenda_bisitak WHERE strftime('%Y', bisitaData) = :urtea AND strftime('%m', bisitaData) = :hilabetea ORDER BY bisitaData DESC")
    List<Agenda> hilabetearenBisitak(String urtea, String hilabetea);
}
