package com.example.appkomertziala.db.kontsultak;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.appkomertziala.db.eredua.EskaeraXehetasuna;

import java.util.List;

/**
 * Eskaera xehetasunak taularen kontsultak: altak, bajak, aldaketak eta irakurketak.
 */
@Dao
public interface EskaeraXehetasunaDao {

    /** Xehetasun bat txertatu. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long txertatu(EskaeraXehetasuna xehetasuna);

    /** Hainbat xehetasun txertatu. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> txertatuGuztiak(List<EskaeraXehetasuna> zerrenda);

    /** Xehetasun bat eguneratu. */
    @Update
    int eguneratu(EskaeraXehetasuna xehetasuna);

    /** Xehetasun bat ezabatu. */
    @Delete
    int ezabatu(EskaeraXehetasuna xehetasuna);

    /** Eskaera zenbaki baten arabera xehetasun guztiak itzuli. */
    @Query("SELECT * FROM eskaera_xehetasunak WHERE eskaeraZenbakia = :eskaeraZenbakia")
    List<EskaeraXehetasuna> eskaerarenXehetasunak(String eskaeraZenbakia);

    /** ID baten arabera xehetasuna bilatu. */
    @Query("SELECT * FROM eskaera_xehetasunak WHERE id = :id LIMIT 1")
    EskaeraXehetasuna idzBilatu(long id);
}
