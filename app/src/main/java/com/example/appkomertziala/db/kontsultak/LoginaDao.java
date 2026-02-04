package com.example.appkomertziala.db.kontsultak;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.appkomertziala.db.eredua.Logina;

import java.util.List;

/**
 * Loginak taularen kontsultak: sarbide kredentzialen altak, bajak, aldaketak eta irakurketak.
 * Taula: loginak.
 * Erabiltzailea (posta) gako nagusia da; komertzialKodea lotura du Komertziala entitatearekin.
 */
@Dao
public interface LoginaDao {

    /** Logina bat txertatu (upsert): erabiltzailea bera bada ordezkatu. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long txertatu(Logina logina);

    /** Hainbat logina txertatu (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> txertatuGuztiak(List<Logina> zerrenda);

    /** Logina bat eguneratu. */
    @Update
    int eguneratu(Logina logina);

    /** Logina bat ezabatu. */
    @Delete
    int ezabatu(Logina logina);

    /** Erabiltzaile (email) eta pasahitzaren arabera logina bilatu, sarbidea balioztatzeko. */
    @Query("SELECT * FROM loginak WHERE erabiltzailea = :erabiltzailea AND pasahitza = :pasahitza LIMIT 1")
    Logina sarbideaBalidatu(String erabiltzailea, String pasahitza);

    /** Erabiltzaile baten arabera logina bilatu. */
    @Query("SELECT * FROM loginak WHERE erabiltzailea = :erabiltzailea LIMIT 1")
    Logina erabiltzaileaBilatu(String erabiltzailea);

    /** Login guztiak itzuli. */
    @Query("SELECT * FROM loginak ORDER BY erabiltzailea")
    List<Logina> guztiak();

    /** XML-etik ez dauden erabiltzaileak ezabatu (sinkronizazioa). */
    @Query("DELETE FROM loginak WHERE erabiltzailea NOT IN (:erabiltzaileak)")
    int ezabatuErabiltzaileakEzDirenak(List<String> erabiltzaileak);

    /** Login guztiak ezabatu. */
    @Query("DELETE FROM loginak")
    void ezabatuGuztiak();
}
