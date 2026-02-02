package com.example.appkomertziala.db.kontsultak;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.appkomertziala.db.eredua.Partnerra;

import java.util.List;

/**
 * Partnerrak taularen kontsultak: altak, bajak, aldaketak eta irakurketak.
 * Partner bakoitza komertzial bakar bati lotuta dago (komertzialKodea).
 * @Insert(onConflict = REPLACE): ID bera duen erregistro bat badago, informazio zaharra XMLko datu berriekin ordezkatzen da.
 */
@Dao
public interface PartnerraDao {

    /** Partner bat txertatu; gatazka bada ordezkatu (upsert). XMLko datu berriek informazio zaharra ordezkatzen dute. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long txertatu(Partnerra partnerra);

    /** Hainbat partner txertatu (upsert). Gatazka bada ordezkatu. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> txertatuGuztiak(List<Partnerra> zerrenda);

    /** Partner bat eguneratu. */
    @Update
    int eguneratu(Partnerra partnerra);

    /** Partner bat ezabatu. */
    @Delete
    int ezabatu(Partnerra partnerra);

    /** Partner guztiak itzuli. */
    @Query("SELECT * FROM partnerrak ORDER BY izena")
    List<Partnerra> guztiak();

    /**
     * Eguneko alta duten partnerrak itzuli (bazkide berriak esportatzeko — eguneroko txostena).
     * Garrantzitsua: centralera egunero bidaltzeko soilik eguneko erregistro berriak hautatzea.
     */
    @Query("SELECT * FROM partnerrak WHERE date(sortutakoData) = date('now', 'localtime') ORDER BY izena")
    List<Partnerra> egunekoAltaGuztiak();

    /** Komertzial kode baten arabera partnerrak itzuli. */
    @Query("SELECT * FROM partnerrak WHERE komertzialKodea = :komertzialKodea ORDER BY izena")
    List<Partnerra> komertzialarenPartnerrak(String komertzialKodea);

    /** Bazkideak soilik (bazkideak.xml, id >= 1000); ez partnerrak. */
    @Query("SELECT * FROM partnerrak WHERE id >= 1000 ORDER BY izena")
    List<Partnerra> bazkideakGuztiak();

    /** Komertzial baten bazkideak soilik (id >= 1000). */
    @Query("SELECT * FROM partnerrak WHERE id >= 1000 AND komertzialKodea = :komertzialKodea ORDER BY izena")
    List<Partnerra> komertzialarenBazkideak(String komertzialKodea);

    /** ID baten arabera partnerra bilatu. */
    @Query("SELECT * FROM partnerrak WHERE id = :id")
    Partnerra idzBilatu(long id);

    /** Kode baten arabera partnerra bilatu. */
    @Query("SELECT * FROM partnerrak WHERE kodea = :kodea LIMIT 1")
    Partnerra kodeaBilatu(String kodea);

    /** Id horiek ez dituzten partnerrak ezabatu (sinkronizazioa: id zerrenda = mantendu behar diren id-ak). */
    @Query("DELETE FROM partnerrak WHERE id NOT IN (:ids)")
    int ezabatuIdakEzDirenak(List<Long> ids);

    /** Partner guztiak ezabatu. */
    @Query("DELETE FROM partnerrak")
    void ezabatuGuztiak();
}
