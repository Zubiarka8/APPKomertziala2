package com.example.appkomertziala.db.kontsultak;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.appkomertziala.db.eredua.Bazkidea;

import java.util.List;

/**
 * Bazkideak taularen kontsultak: gehitu, aldatu, ezabatu, bilatu.
 * Partnerrak taularen funtzionalitatea bazkideak taulan integratuta.
 */
@Dao
public interface BazkideaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long txertatu(Bazkidea bazkidea);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> txertatuGuztiak(List<Bazkidea> zerrenda);

    @Update
    int eguneratu(Bazkidea bazkidea);

    @Delete
    int ezabatu(Bazkidea bazkidea);

    @Query("SELECT * FROM bazkideak ORDER BY izena, abizena")
    List<Bazkidea> guztiak();

    @Query("SELECT * FROM bazkideak WHERE id = :id LIMIT 1")
    Bazkidea idzBilatu(long id);

    /** NAN baten arabera bazkidea bilatu. */
    @Query("SELECT * FROM bazkideak WHERE nan = :nan LIMIT 1")
    Bazkidea nanBilatu(String nan);

    /** Bilatzailea: NAN, izena, abizena edo postan testua bilatu. */
    @Query("SELECT * FROM bazkideak WHERE " +
            "(:filter IS NULL OR :filter = '' OR " +
            "nan LIKE '%' || :filter || '%' OR " +
            "izena LIKE '%' || :filter || '%' OR " +
            "abizena LIKE '%' || :filter || '%' OR " +
            "posta LIKE '%' || :filter || '%' OR " +
            "telefonoZenbakia LIKE '%' || :filter || '%') " +
            "ORDER BY izena, abizena")
    List<Bazkidea> bilatu(String filter);

    /** Id horiek ez dituzten bazkideak ezabatu (sinkronizazioa: id zerrenda = mantendu behar diren id-ak). */
    @Query("DELETE FROM bazkideak WHERE id NOT IN (:ids)")
    int ezabatuIdakEzDirenak(List<Long> ids);

    @Query("DELETE FROM bazkideak")
    void ezabatuGuztiak();
}
