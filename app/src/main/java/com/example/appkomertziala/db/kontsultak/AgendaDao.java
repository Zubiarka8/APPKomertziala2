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
 * SEGURTASUNA: Sarbide kontrol zorrotza aplikatzen da. Query guztiek komertzialKodea filtroa dute.
 * Ez erabili getAllVisits() - erabili getVisitsByKomertzial() ordez.
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

    /**
     * SEGURTASUNA: Metodo hau DEPRECATED da. Erabili getVisitsByKomertzial() ordez.
     * @deprecated Sarbide kontrol zorrotzaren ondorioz, erabili getVisitsByKomertzial(String kodea) ordez
     */
    @Deprecated
    @Query("SELECT * FROM agenda_bisitak ORDER BY bisitaData DESC")
    List<Agenda> guztiak();

    /**
     * SEGURTASUNA: Komertzial baten bisitak bakarrik itzuli.
     * Sarbide kontrol zorrotza: komertzial bakoitzak bere bisitak bakarrik ikus ditzake.
     * 
     * @param komertzialKodea Komertzialaren kodea (NAN edo identifikatzailea)
     * @return Komertzialaren bisitak, data arabera ordenatuta (berrienak lehenik)
     */
    @Query("SELECT * FROM agenda_bisitak WHERE komertzialKodea = :komertzialKodea ORDER BY bisitaData DESC")
    List<Agenda> getVisitsByKomertzial(String komertzialKodea);

    /**
     * SEGURTASUNA: Gako nagusiaren arabera bisita bat bilatu, baina bakarrik uneko komertzialarena bada.
     * @param id Bisitaren ID
     * @param komertzialKodea Komertzialaren kodea (segurtasuna bermatzeko)
     */
    @Query("SELECT * FROM agenda_bisitak WHERE id = :id AND komertzialKodea = :komertzialKodea LIMIT 1")
    Agenda idzBilatuSegurua(long id, String komertzialKodea);

    /** Gako nagusiaren arabera bisita bat bilatu (DEPRECATED - erabili idzBilatuSegurua ordez). */
    @Deprecated
    @Query("SELECT * FROM agenda_bisitak WHERE id = :id LIMIT 1")
    Agenda idzBilatu(long id);

    /**
     * SEGURTASUNA: Hilabetearen arabera bisitak itzuli, uneko komertzialarenak bakarrik.
     * @param komertzialKodea Komertzialaren kodea
     */
    @Query("SELECT * FROM agenda_bisitak WHERE komertzialKodea = :komertzialKodea AND strftime('%Y-%m', bisitaData) = strftime('%Y-%m', 'now') ORDER BY bisitaData DESC")
    List<Agenda> hilabetearenBisitak(String komertzialKodea);

    /**
     * SEGURTASUNA: Urte eta hilabete zehatzaren arabera bisitak itzuli, uneko komertzialarenak bakarrik.
     * @param komertzialKodea Komertzialaren kodea
     * @param urtea "yyyy" formatua
     * @param hilabetea "MM" formatua (01-12)
     */
    @Query("SELECT * FROM agenda_bisitak WHERE komertzialKodea = :komertzialKodea AND strftime('%Y', bisitaData) = :urtea AND strftime('%m', bisitaData) = :hilabetea ORDER BY bisitaData DESC")
    List<Agenda> hilabetearenBisitak(String komertzialKodea, String urtea, String hilabetea);

    /**
     * DEPRECATED: Erabili hilabetearenBisitak(String komertzialKodea, String urtea, String hilabetea) ordez.
     */
    @Deprecated
    @Query("SELECT * FROM agenda_bisitak WHERE strftime('%Y', bisitaData) = :urtea AND strftime('%m', bisitaData) = :hilabetea ORDER BY bisitaData DESC")
    List<Agenda> hilabetearenBisitak(String urtea, String hilabetea);

    /**
     * DEPRECATED: Erabili hilabetearenBisitak(String komertzialKodea) ordez.
     */
    @Deprecated
    @Query("SELECT * FROM agenda_bisitak WHERE strftime('%Y-%m', bisitaData) = strftime('%Y-%m', 'now') ORDER BY bisitaData DESC")
    List<Agenda> hilabetearenBisitak();

    /**
     * SEGURTASUNA: Data zehatzaren arabera bisitak bilatu, uneko komertzialarenak bakarrik.
     * @param data Bilaketa data (yyyy-MM-dd)
     * @param komertzialKodea Komertzialaren kodea
     */
    @Query("SELECT * FROM agenda_bisitak WHERE bisitaData = :data AND komertzialKodea = :komertzialKodea ORDER BY ordua ASC")
    List<Agenda> bilatuDataz(String data, String komertzialKodea);

    /**
     * DEPRECATED: Erabili bilatuDataz(String data, String komertzialKodea) ordez.
     */
    @Deprecated
    @Query("SELECT * FROM agenda_bisitak WHERE bisitaData = :data ORDER BY ordua ASC")
    List<Agenda> bilatuDataz(String data);

    /**
     * SEGURTASUNA: Bazkide kodea edo izenaren arabera bilatu, uneko komertzialaren bisitak bakarrik.
     * @param filter Bilaketa testua
     * @param komertzialKodea Komertzialaren kodea
     */
    @Query("SELECT * FROM agenda_bisitak WHERE komertzialKodea = :komertzialKodea AND (bazkideaKodea LIKE '%' || :filter || '%' OR " +
           "bazkideaId IN (SELECT id FROM bazkideak WHERE izena LIKE '%' || :filter || '%' OR abizena LIKE '%' || :filter || '%' OR nan LIKE '%' || :filter || '%')) " +
           "ORDER BY bisitaData DESC")
    List<Agenda> bilatuBezeroaz(String filter, String komertzialKodea);

    /**
     * DEPRECATED: Erabili bilatuBezeroaz(String filter, String komertzialKodea) ordez.
     */
    @Deprecated
    @Query("SELECT * FROM agenda_bisitak WHERE bazkideaKodea LIKE '%' || :filter || '%' OR " +
           "bazkideaId IN (SELECT id FROM bazkideak WHERE izena LIKE '%' || :filter || '%' OR abizena LIKE '%' || :filter || '%' OR nan LIKE '%' || :filter || '%') " +
           "ORDER BY bisitaData DESC")
    List<Agenda> bilatuBezeroaz(String filter);

    /**
     * SEGURTASUNA: Data tartearen arabera bilatu, uneko komertzialaren bisitak bakarrik.
     * @param hasieraData Hasiera data (yyyy-MM-dd)
     * @param amaieraData Amaiera data (yyyy-MM-dd)
     * @param komertzialKodea Komertzialaren kodea
     */
    @Query("SELECT * FROM agenda_bisitak WHERE komertzialKodea = :komertzialKodea AND bisitaData >= :hasieraData AND bisitaData <= :amaieraData ORDER BY bisitaData DESC, ordua ASC")
    List<Agenda> bilatuDataTarteaz(String hasieraData, String amaieraData, String komertzialKodea);

    /**
     * SEGURTASUNA: Bilaketa orokorra: bilatu data, bazkidea izena/kodea, deskribapena eta egoera eremuen artean.
     * Uneko komertzialaren bisitak bakarrik bilatzen dira.
     * @param filter Bilaketa testua (data, izena, deskribapena, egoera...)
     * @param komertzialKodea Komertzialaren kodea
     */
    @Query("SELECT DISTINCT a.* FROM agenda_bisitak a " +
           "LEFT JOIN bazkideak b ON a.bazkideaId = b.id " +
           "WHERE a.komertzialKodea = :komertzialKodea AND (" +
           "a.bisitaData LIKE '%' || :filter || '%' OR " +
           "a.ordua LIKE '%' || :filter || '%' OR " +
           "a.bazkideaKodea LIKE '%' || :filter || '%' OR " +
           "a.deskribapena LIKE '%' || :filter || '%' OR " +
           "a.egoera LIKE '%' || :filter || '%' OR " +
           "b.izena LIKE '%' || :filter || '%' OR " +
           "b.abizena LIKE '%' || :filter || '%' OR " +
           "b.nan LIKE '%' || :filter || '%') " +
           "ORDER BY a.bisitaData DESC, a.ordua ASC")
    List<Agenda> bilatuOrokorra(String filter, String komertzialKodea);

    /**
     * DEPRECATED: Erabili bilatuDataTarteaz(String hasieraData, String amaieraData, String komertzialKodea) ordez.
     */
    @Deprecated
    @Query("SELECT * FROM agenda_bisitak WHERE bisitaData >= :hasieraData AND bisitaData <= :amaieraData ORDER BY bisitaData DESC, ordua ASC")
    List<Agenda> bilatuDataTarteaz(String hasieraData, String amaieraData);

    /**
     * SEGURTASUNA: Bisita bat ezabatu, bakarrik uneko komertzialarena bada.
     * @param id Bisitaren ID
     * @param komertzialKodea Komertzialaren kodea (segurtasuna bermatzeko)
     */
    @Query("DELETE FROM agenda_bisitak WHERE id = :id AND komertzialKodea = :komertzialKodea")
    int ezabatuSegurua(long id, String komertzialKodea);

    /**
     * SEGURTASUNA: Bisita bat eguneratu, bakarrik uneko komertzialarena bada.
     * ONDO: Erabili @Update eguneratu() lehenik, gero egiaztatu komertzialKodea.
     * HOBE: Erabili transakzio bat idzBilatuSegurua() eta eguneratu() erabiliz.
     * 
     * @param agenda Eguneratu behar den bisita (komertzialKodea eremua baliozkoa izan behar du)
     * @param komertzialKodea Komertzialaren kodea (segurtasuna bermatzeko - egiaztatu agenda.komertzialKodea == komertzialKodea)
     * @return Eguneratutako erregistro kopurua (1 baliozkoa bada, 0 bestela)
     */
    @Query("UPDATE agenda_bisitak SET bisitaData = :bisitaData, ordua = :ordua, bazkideaKodea = :bazkideaKodea, bazkideaId = :bazkideaId, deskribapena = :deskribapena, egoera = :egoera " +
           "WHERE id = :id AND komertzialKodea = :komertzialKodea")
    int eguneratuSegurua(long id, String bisitaData, String ordua, String bazkideaKodea, Long bazkideaId, String deskribapena, String egoera, String komertzialKodea);

    /** Komertzial ID baten arabera bisitak bilatu (DEPRECATED - erabili getVisitsByKomertzial ordez). */
    @Deprecated
    @Query("SELECT * FROM agenda_bisitak WHERE komertzialaId = :komertzialaId ORDER BY bisitaData DESC, ordua ASC")
    List<Agenda> bilatuKomertzialIdz(Long komertzialaId);
}
