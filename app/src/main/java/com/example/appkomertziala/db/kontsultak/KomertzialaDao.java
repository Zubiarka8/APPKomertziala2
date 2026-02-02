package com.example.appkomertziala.db.kontsultak;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.appkomertziala.db.eredua.Komertziala;

import java.util.List;

/**
 * Komertzialak taularen kontsultak: altak, bajak, aldaketak eta irakurketak.
 */
@Dao
public interface KomertzialaDao {

    /** Komertzial bat edo gehiago txertatu; gatazka bada ordezkatu (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long txertatu(Komertziala komertziala);

    /** Hainbat komertzial txertatu (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> txertatuGuztiak(List<Komertziala> zerrenda);

    /** Komertzial bat eguneratu. */
    @Update
    int eguneratu(Komertziala komertziala);

    /** Komertzial bat ezabatu. */
    @Delete
    int ezabatu(Komertziala komertziala);

    /** Komertzial guztiak itzuli. */
    @Query("SELECT * FROM komertzialak ORDER BY izena")
    List<Komertziala> guztiak();

    /** ID baten arabera komertziala bilatu. */
    @Query("SELECT * FROM komertzialak WHERE id = :id")
    Komertziala idzBilatu(long id);

    /** Kode baten arabera komertziala bilatu. */
    @Query("SELECT * FROM komertzialak WHERE kodea = :kodea")
    Komertziala kodeaBilatu(String kodea);

    /** Komertzial kopurua itzuli. */
    @Query("SELECT COUNT(*) FROM komertzialak")
    int kopurua();

    /** XML-etik ez dauden kodeak dituzten komertzialak ezabatu (sinkronizazioa). */
    @Query("DELETE FROM komertzialak WHERE kodea NOT IN (:codeak)")
    int ezabatuKodeakEzDirenak(List<String> codeak);

    /** Komertzial guztiak ezabatu. */
    @Query("DELETE FROM komertzialak")
    void ezabatuGuztiak();
}
