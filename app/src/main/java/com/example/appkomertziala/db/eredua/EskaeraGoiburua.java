package com.example.appkomertziala.db.eredua;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Eskaera goiburua: Zenbakia, Data, Komertzial_Kodea, Ordezkaritza, Partner_Kodea.
 * Eskaera bakoitza komertzial bakar bati lotuta.
 */
@Entity(tableName = "eskaera_goiburuak", indices = {@Index("komertzialKodea"), @Index("partnerKodea")})
public class EskaeraGoiburua {

    @NonNull
    @PrimaryKey
    private String zenbakia;

    private String data;
    /** Lotutako komertzialaren kodea. */
    private String komertzialKodea;
    private String ordezkaritza;
    /** Lotutako partnerraren kodea. */
    private String partnerKodea;

    public EskaeraGoiburua() {}

    public String getZenbakia() { return zenbakia; }
    public void setZenbakia(String zenbakia) { this.zenbakia = zenbakia; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public String getKomertzialKodea() { return komertzialKodea; }
    public void setKomertzialKodea(String komertzialKodea) { this.komertzialKodea = komertzialKodea; }
    public String getOrdezkaritza() { return ordezkaritza; }
    public void setOrdezkaritza(String ordezkaritza) { this.ordezkaritza = ordezkaritza; }
    public String getPartnerKodea() { return partnerKodea; }
    public void setPartnerKodea(String partnerKodea) { this.partnerKodea = partnerKodea; }
}
