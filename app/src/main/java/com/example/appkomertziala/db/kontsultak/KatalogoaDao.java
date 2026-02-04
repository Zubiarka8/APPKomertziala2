package com.example.appkomertziala.db.kontsultak;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.appkomertziala.db.eredua.Katalogoa;

import java.util.List;

/**
 * Katalogoa taularen kontsultak: asteko inportazioan wipe-and-load (ezabatuGuztiak + txertatuGuztiak),
 * katalogoa ikusi, eta stock-a eskaera egitean eguneratu.
 * Taula: katalogoa.
 * OnConflictStrategy.REPLACE: artikulu kodea lehendik badago, zaharrak ordezkatzen ditu.
 */
@Dao
public interface KatalogoaDao {

    /** Artikuluak txertatu; gatazka bada ordezkatu (OnConflictStrategy.REPLACE). Asteko wipe-and-load inportazioan erabiltzen da. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> artikuluakKargatu(List<Katalogoa> zerrenda);

    /** Katalogo guztia ikusi (produktu zerrenda UI-rako). */
    @Query("SELECT * FROM katalogoa ORDER BY izena")
    List<Katalogoa> katalogoaIkusi();

    /** Eskaera bat egiten denean stock-a aldatzeko: artikulu baten stock_a eguneratu. */
    @Query("UPDATE katalogoa SET stock = :stockBerria WHERE artikuluKodea = :artikuluKodea")
    int stockaEguneratu(String artikuluKodea, int stockBerria);

    /** Katalogo-artikulu bat txertatu; gatazka bada ordezkatu (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long txertatu(Katalogoa katalogoa);

    /** Hainbat artikulu txertatu (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> txertatuGuztiak(List<Katalogoa> zerrenda);

    /** Artikulu bat eguneratu. */
    @Update
    int eguneratu(Katalogoa katalogoa);

    /** Artikulu bat ezabatu. */
    @Delete
    int ezabatu(Katalogoa katalogoa);

    /** Katalogo guztia itzuli (katalogoaIkusi() bera). */
    @Query("SELECT * FROM katalogoa ORDER BY izena")
    List<Katalogoa> guztiak();

    /** Artikulu-kode baten arabera bilatu. */
    @Query("SELECT * FROM katalogoa WHERE artikuluKodea = :artikuluKodea LIMIT 1")
    Katalogoa artikuluaBilatu(String artikuluKodea);

    /** XML-etik ez dauden artikulu-kodeak ezabatu (sinkronizazioa). */
    @Query("DELETE FROM katalogoa WHERE artikuluKodea NOT IN (:artikuluKodeak)")
    int ezabatuArtikuluKodeakEzDirenak(List<String> artikuluKodeak);

    /** Katalogo guztia ezabatu. */
    @Query("DELETE FROM katalogoa")
    void ezabatuGuztiak();
}
