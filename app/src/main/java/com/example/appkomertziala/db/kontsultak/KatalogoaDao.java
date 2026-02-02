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
 * Katalogoa taularen kontsultak: altak, bajak, aldaketak eta irakurketak.
 */
@Dao
public interface KatalogoaDao {

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

    /** Katalogo guztia itzuli. */
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
