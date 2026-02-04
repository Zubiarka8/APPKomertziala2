package com.example.appkomertziala.db.eredua;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Eskaera goiburua: Zenbakia, Data, Komertzial_Kodea/Id, Ordezkaritza, Bazkidea_Kodea/Id.
 * Loturak: bazkideaId → Bazkidea.id, komertzialId → Komertziala.id (aplikazioan erresolbatzen dira; migrazioek ez dute FK sortzen).
 */
@Entity(
    tableName = "eskaera_goiburuak",
    indices = {@Index("komertzialKodea"), @Index("bazkideaKodea"), @Index("komertzialId"), @Index("bazkideaId")}
)
public class EskaeraGoiburua {

    /** Eskaeraren zenbaki bakarra (gako nagusia). */
    @NonNull
    @PrimaryKey
    private String zenbakia;

    /** Eskaeraren data (yyyy/MM/dd edo yyyy/MM/dd HH:mm). */
    private String data;
    /** Lotutako komertzialaren kodea (XML / pantaila erakusteko). */
    private String komertzialKodea;
    /** Lotutako komertzialaren ID (kanpo-gakoa: nor kudeatu duen). */
    private Long komertzialId;
    /** Ordezkaritzaren izena (bidalketa helmuga). */
    private String ordezkaritza;
    /** Lotutako bazkidearen kodea (XML / pantaila erakusteko). */
    private String bazkideaKodea;
    /** Lotutako bazkidearen ID (kanpo-gakoa: nor erosi duen). */
    private Long bazkideaId;

    /** Eraikitzaile hutsa (Room-entzat). */
    public EskaeraGoiburua() {}

    public String getZenbakia() { return zenbakia; }
    public void setZenbakia(String zenbakia) { this.zenbakia = zenbakia; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public String getKomertzialKodea() { return komertzialKodea; }
    public void setKomertzialKodea(String komertzialKodea) { this.komertzialKodea = komertzialKodea; }
    public Long getKomertzialId() { return komertzialId; }
    public void setKomertzialId(Long komertzialId) { this.komertzialId = komertzialId; }
    public String getOrdezkaritza() { return ordezkaritza; }
    public void setOrdezkaritza(String ordezkaritza) { this.ordezkaritza = ordezkaritza; }
    public String getBazkideaKodea() { return bazkideaKodea; }
    public void setBazkideaKodea(String bazkideaKodea) { this.bazkideaKodea = bazkideaKodea; }
    public Long getBazkideaId() { return bazkideaId; }
    public void setBazkideaId(Long bazkideaId) { this.bazkideaId = bazkideaId; }
}
