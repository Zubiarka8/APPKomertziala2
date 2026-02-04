package com.example.appkomertziala.db.kontsultak;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.appkomertziala.db.eredua.HistorialCompra;

import java.util.List;

/**
 * Historial de compras (erosketa historiala) taularen kontsultak: altak, bajak, aldaketak eta irakurketak.
 * Taula: historial_compras.
 * Bidalketa XML egitura gordetzen du (bidalketa_2_20260130140648.xml).
 */
@Dao
public interface HistorialCompraDao {

    /** Historial de compra bat txertatu (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long txertatu(HistorialCompra historial);

    /** Hainbat historial txertatu (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> txertatuGuztiak(List<HistorialCompra> zerrenda);

    /** Historial bat eguneratu. */
    @Update
    int eguneratu(HistorialCompra historial);

    /** Historial bat ezabatu. */
    @Delete
    int ezabatu(HistorialCompra historial);

    /** Historial guztiak itzuli (data beherakor ordenan). */
    @Query("SELECT * FROM historial_compras ORDER BY data DESC, kodea DESC")
    List<HistorialCompra> guztiak();

    /** Bidalketa kodea baten arabera historialak itzuli. */
    @Query("SELECT * FROM historial_compras WHERE kodea = :kodea ORDER BY data DESC")
    List<HistorialCompra> kodeaBilatu(String kodea);

    /** Bidalketa ID baten arabera historialak itzuli. */
    @Query("SELECT * FROM historial_compras WHERE bidalketaId = :bidalketaId ORDER BY data DESC")
    List<HistorialCompra> bidalketaIdBilatu(int bidalketaId);

    /** Producto ID baten arabera historialak itzuli. */
    @Query("SELECT * FROM historial_compras WHERE productoId = :productoId ORDER BY data DESC")
    List<HistorialCompra> productoIdBilatu(String productoId);

    /** Eguneko historialak itzuli. */
    @Query("SELECT * FROM historial_compras WHERE date(data) = date('now', 'localtime') ORDER BY data DESC")
    List<HistorialCompra> egunekoHistorialak();

    /** Hilabeteko historialak itzuli. */
    @Query("SELECT * FROM historial_compras WHERE strftime('%Y-%m', data) = strftime('%Y-%m', 'now') ORDER BY data DESC")
    List<HistorialCompra> hilabetekoHistorialak();

    /** Historial guztiak ezabatu. */
    @Query("DELETE FROM historial_compras")
    void ezabatuGuztiak();

    /** ID baten arabera historial bilatu. */
    @Query("SELECT * FROM historial_compras WHERE id = :id LIMIT 1")
    HistorialCompra idzBilatu(long id);

    /** Bidalketa ID baten arabera amaituta egoera eguneratu (bidalketa guztiko lerro guztietan). */
    @Query("UPDATE historial_compras SET amaituta = :amaituta WHERE bidalketaId = :bidalketaId")
    int eguneratuAmaitutaBidalketaIdz(int bidalketaId, boolean amaituta);
}


