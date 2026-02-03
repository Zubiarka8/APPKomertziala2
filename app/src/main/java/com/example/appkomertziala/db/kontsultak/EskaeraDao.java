package com.example.appkomertziala.db.kontsultak;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.appkomertziala.db.eredua.Eskaera;

import java.util.List;

/**
 * Eskaerak taularen kontsultak: gehitu, aldatu, ezabatu, bilatu.
 */
@Dao
public interface EskaeraDao {

    /** Eskaera bat txertatu (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long txertatu(Eskaera eskaera);

    /** Hainbat eskaera txertatu (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> txertatuGuztiak(List<Eskaera> zerrenda);

    /** Eskaera bat eguneratu. */
    @Update
    int eguneratu(Eskaera eskaera);

    /** Eskaera bat ezabatu. */
    @Delete
    int ezabatu(Eskaera eskaera);

    /** Eskaera guztiak itzuli. */
    @Query("SELECT * FROM eskaerak ORDER BY data DESC")
    List<Eskaera> guztiak();

    /** Bazkidea ID baten arabera eskaerak itzuli. */
    @Query("SELECT * FROM eskaerak WHERE bazkideaId = :bazkideaId ORDER BY data DESC")
    List<Eskaera> bazkidearenEskaerak(long bazkideaId);

    /** Eskaera ID baten arabera bilatu. */
    @Query("SELECT * FROM eskaerak WHERE eskaeraID = :eskaeraID LIMIT 1")
    Eskaera eskaeraIDBilatu(String eskaeraID);

    /** Bazkidea ID baten arabera eskaerak ezabatu. */
    @Query("DELETE FROM eskaerak WHERE bazkideaId = :bazkideaId")
    int ezabatuBazkidearenEskaerak(long bazkideaId);

    /** Eskaera guztiak ezabatu. */
    @Query("DELETE FROM eskaerak")
    void ezabatuGuztiak();
}

