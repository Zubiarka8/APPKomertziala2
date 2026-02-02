package com.example.appkomertziala.db.kontsultak;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.appkomertziala.db.eredua.EskaeraGoiburua;

import java.util.List;

/**
 * Eskaera goiburuak taularen kontsultak: altak, bajak, aldaketak eta irakurketak.
 * Eskaera bakoitza komertzial bakar bati lotuta dago.
 */
@Dao
public interface EskaeraGoiburuaDao {

    /** Eskaera goiburu bat txertatu (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long txertatu(EskaeraGoiburua goiburua);

    /** Hainbat goiburu txertatu (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> txertatuGuztiak(List<EskaeraGoiburua> zerrenda);

    /** Goiburu bat eguneratu. */
    @Update
    int eguneratu(EskaeraGoiburua goiburua);

    /** Goiburu bat ezabatu. */
    @Delete
    int ezabatu(EskaeraGoiburua goiburua);

    /** Eskaera goiburu guztiak itzuli. */
    @Query("SELECT * FROM eskaera_goiburuak ORDER BY data DESC")
    List<EskaeraGoiburua> guztiak();

    /**
     * Eguneko eskaera goiburu guztiak itzuli (eskaera berriak esportatzeko — eguneroko txostena).
     * Garrantzitsua: egunero eskaera berrien laburpena centralera bidaltzeko.
     */
    @Query("SELECT * FROM eskaera_goiburuak WHERE date(data) = date('now', 'localtime') ORDER BY data DESC")
    List<EskaeraGoiburua> egunekoEskaerak();

    /** Komertzial kode baten arabera eskaerak itzuli. */
    @Query("SELECT * FROM eskaera_goiburuak WHERE komertzialKodea = :komertzialKodea ORDER BY data DESC")
    List<EskaeraGoiburua> komertzialarenEskaerak(String komertzialKodea);

    /** Zenbaki baten arabera goiburua bilatu. */
    @Query("SELECT * FROM eskaera_goiburuak WHERE zenbakia = :zenbakia LIMIT 1")
    EskaeraGoiburua zenbakiaBilatu(String zenbakia);

    /**
     * Uneko hilabeteko eskaera goiburu guztiak itzuli (agenda esportazioa — hileroko laburpena).
     * Garrantzitsua: centralera hilero bidaltzeko agendan erregistratutako bisita guztiak hautatzea.
     */
    @Query("SELECT * FROM eskaera_goiburuak WHERE strftime('%Y-%m', data) = strftime('%Y-%m', 'now') ORDER BY data DESC")
    List<EskaeraGoiburua> hilabetekoEskaerak();
}
