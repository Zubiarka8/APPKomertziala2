package com.example.appkomertziala.db.eredua;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Eskaera goiburua: Zenbakia, Data, Komertzial_Kodea/Id, Ordezkaritza, Partner_Kodea/Id.
 * Loturak: partnerId → Partnerra.id, komertzialId → Komertziala.id (aplikazioan erresolbatzen dira; migrazioek ez dute FK sortzen).
 */
@Entity(
    tableName = "eskaera_goiburuak",
    indices = {@Index("komertzialKodea"), @Index("partnerKodea"), @Index("komertzialId"), @Index("partnerId")}
)
public class EskaeraGoiburua {

    @NonNull
    @PrimaryKey
    private String zenbakia;

    private String data;
    /** Lotutako komertzialaren kodea (XML / pantaila). */
    private String komertzialKodea;
    /** Lotutako komertzialaren ID (kanpo-gakoa: nor kudeatu duen). */
    private Long komertzialId;
    private String ordezkaritza;
    /** Lotutako partnerraren kodea (XML / pantaila). */
    private String partnerKodea;
    /** Lotutako partnerraren ID (kanpo-gakoa: nor erosi duen). */
    private Long partnerId;

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
    public String getPartnerKodea() { return partnerKodea; }
    public void setPartnerKodea(String partnerKodea) { this.partnerKodea = partnerKodea; }
    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }
}
