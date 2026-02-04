package com.example.appkomertziala.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appkomertziala.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Eskaerak adapter: RecyclerView-arentzat eskaera zerrenda erakusteko.
 * 
 * Hau hemen eskaera goiburuak erakusteko adapterra da. EskaerakActivity-k erabiltzen du
 * eskaera zerrenda bat erakusteko. Elementu bakoitzak zenbakia, data, artikulu kopurua
 * eta guztira prezioa erakusten ditu.
 * 
 * Nola funtzionatzen duen:
 * 1. EskaerakActivity-k datu basean eskaera goiburuak irakurtzen ditu
 * 2. Bakoitzarentzat xehetasunak kargatzen ditu eta guztira kalkulatzen du
 * 3. EskaeraElementua objektuak sortzen ditu eta adapter honi ematen dio
 * 4. Adapter-ak RecyclerView-n erakusten ditu item_eskaera layout-a erabiliz
 */
public class EskaerakAdapter extends RecyclerView.Adapter<EskaerakAdapter.EskaeraViewHolder> {

    /** Eskaera elementuen zerrenda. Hau hemen badago, dena ondo doa. */
    private final List<EskaeraElementua> zerrenda;
    
    /** LayoutInflater: XML layout-ak View objektu bihurtzeko. RecyclerView-ek erabiltzen du. */
    private final LayoutInflater inflater;
    
    /** Listener eskaera xehetasunak ikusteko botoia sakatzean. */
    private OnXehetasunakClickListener xehetasunakClickListener;

    /**
     * Eraikitzailea: adapter-a sortzen du context batetik.
     * 
     * @param context Android context-a (normalean Activity bat). LayoutInflater sortzeko erabiltzen da.
     */
    public EskaerakAdapter(android.content.Context context) {
        // Zerrenda hutsa hasieratu - gero eguneratuko dugu datuekin
        this.zerrenda = new ArrayList<>();
        // LayoutInflater sortu context-etik - hau hemen beharrezkoa da View-ak sortzeko
        this.inflater = LayoutInflater.from(context);
    }

    /**
     * Listener ezarri xehetasunak botoia sakatzean.
     */
    public void setOnXehetasunakClickListener(OnXehetasunakClickListener listener) {
        this.xehetasunakClickListener = listener;
    }

    /**
     * Zerrenda eguneratu eta notifyDataSetChanged deitu.
     * 
     * EskaerakActivity-k datu basean eskaerak irakurri eta gero deitzen du metodo hau.
     * Zerrenda zaharra garbitu eta berria sartzen du, gero RecyclerView-ri esaten dio
     * eguneratu behar dela.
     * 
     * @param berria Eskaera elementuen zerrenda berria. Null bada, zerrenda hutsik geratuko da.
     */
    public void eguneratuZerrenda(List<EskaeraElementua> berria) {
        // Lehenik zerrenda zaharra garbitu
        this.zerrenda.clear();
        // Begiratu hemen ea zerrenda berria null ez den
        if (berria != null) {
            // Zerrenda berria gehitu
            this.zerrenda.addAll(berria);
        }
        // RecyclerView-ri esan eguneratu behar duela - hau hemen garrantzitsua da!
        notifyDataSetChanged();
    }

    /**
     * ViewHolder bat sortzen du elementu bakoitzarentzat.
     * 
     * RecyclerView-k deitzen du elementu berri bat sortu behar denean.
     * item_eskaera.xml layout-a kargatzen du eta ViewHolder bat sortzen du.
     * 
     * @param gurasoa ViewGroup gurasoa (RecyclerView bera)
     * @param mota View mota (normalean 0, baina mota anitzeko adapter-etan erabilgarria)
     * @return EskaeraViewHolder berria, item_eskaera layout-arekin
     */
    @NonNull
    @Override
    public EskaeraViewHolder onCreateViewHolder(@NonNull ViewGroup gurasoa, int mota) {
        // XML layout-a View objektu bihurtu - hau hemen magia gertatzen da
        View erroa = inflater.inflate(R.layout.item_eskaera, gurasoa, false);
        // ViewHolder sortu eta itzuli
        return new EskaeraViewHolder(erroa);
    }

    /**
     * ViewHolder bat datuekin betetzen du.
     * 
     * RecyclerView-k deitzen du elementu bat erakusteko behar denean.
     * Zerrendako posizio baten datuak hartzen ditu eta ViewHolder-eko TextView-etan jartzen ditu.
     * 
     * @param holder Eguneratu behar den ViewHolder
     * @param posizioa Zerrendako posizioa (0-tik hasita)
     */
    @Override
    public void onBindViewHolder(@NonNull EskaeraViewHolder holder, int posizioa) {
        EskaeraElementua e = zerrenda.get(posizioa);
        
        holder.itemEskaeraZenbakia.setText(e.zenbakia != null ? e.zenbakia : "");
        holder.itemEskaeraData.setText(e.data != null ? e.data : "");
        holder.itemEskaeraBazkidea.setText(e.bazkideIzena != null ? e.bazkideIzena : "—");
        holder.itemEskaeraArtikuluak.setText(String.valueOf(e.artikuluKopurua));
        holder.itemEskaeraGuztira.setText(String.format(Locale.getDefault(), "%.2f €", e.guztira));
        
        holder.btnXehetasunak.setOnClickListener(v -> {
            if (xehetasunakClickListener != null) {
                xehetasunakClickListener.onXehetasunakClick(e);
            }
        });
    }

    /**
     * Zerrendako elementu kopurua itzultzen du.
     * 
     * RecyclerView-k deitzen du zenbat elementu erakusteko dauden jakiteko.
     * 
     * @return Zerrendako elementu kopurua
     */
    @Override
    public int getItemCount() {
        return zerrenda.size();
    }

    /**
     * ViewHolder: RecyclerView elementu bakoitzaren View-ak gordetzen ditu.
     * 
     * Hau hemen optimizazio bat da: findViewById behin bakarrik deitzen da,
     * ez elementu bakoitza erakusteko behin eta berriro. Honek mapan jartzen gaitu
     * performance aldetik.
     */
    static class EskaeraViewHolder extends RecyclerView.ViewHolder {
        final TextView itemEskaeraZenbakia;
        final TextView itemEskaeraData;
        final TextView itemEskaeraBazkidea;
        final TextView itemEskaeraArtikuluak;
        final TextView itemEskaeraGuztira;
        final MaterialButton btnXehetasunak;

        EskaeraViewHolder(@NonNull View itemView) {
            super(itemView);
            itemEskaeraZenbakia = itemView.findViewById(R.id.itemEskaeraZenbakia);
            itemEskaeraData = itemView.findViewById(R.id.itemEskaeraData);
            itemEskaeraBazkidea = itemView.findViewById(R.id.itemEskaeraBazkidea);
            itemEskaeraArtikuluak = itemView.findViewById(R.id.itemEskaeraArtikuluak);
            itemEskaeraGuztira = itemView.findViewById(R.id.itemEskaeraGuztira);
            btnXehetasunak = itemView.findViewById(R.id.btnEskaeraXehetasunak);
        }
    }
    
    /** Listener eskaera xehetasunak ikusteko. */
    public interface OnXehetasunakClickListener {
        void onXehetasunakClick(EskaeraElementua e);
    }

    /**
     * Eskaera elementu bat: zenbakia, data, artikulu kopurua, guztira.
     * 
     * Hau hemen datu kontainer bat da. EskaerakActivity-k eskaera goiburuak eta
     * xehetasunak irakurri eta gero, datu hauek biltzen ditu eta adapter-ari ematen dio.
     * 
     * Datuak:
     * - zenbakia: Eskaeraren zenbakia (adib. "ESK-001")
     * - data: Eskaeraren data (adib. "2024-01-15")
     * - artikuluKopurua: Zenbat artikulu dauden eskaeran (guztira)
     * - guztira: Prezio totala (prezioa * kantitatea bakoitzarentzat batuta)
     */
    public static class EskaeraElementua {
        public final String zenbakia;
        public final String data;
        public final String bazkideIzena;
        public final int artikuluKopurua;
        public final double guztira;
        /** Produktu xehetasunak (izena, kantitatea, prezioa) xehetasunak dialogorako. */
        public final List<ProduktuXehetasuna> produktuXehetasunak;

        public EskaeraElementua(String zenbakia, String data, String bazkideIzena, int artikuluKopurua, double guztira, List<ProduktuXehetasuna> produktuXehetasunak) {
            this.zenbakia = zenbakia;
            this.data = data;
            this.bazkideIzena = bazkideIzena;
            this.artikuluKopurua = artikuluKopurua;
            this.guztira = guztira;
            this.produktuXehetasunak = produktuXehetasunak != null ? produktuXehetasunak : new ArrayList<>();
        }
    }
    
    /** Produktu xehetasuna: izena, kantitatea, prezioa. */
    public static class ProduktuXehetasuna {
        public final String produktuIzena;
        public final int kantitatea;
        public final double prezioa;
        public final double guztira;
        
        public ProduktuXehetasuna(String produktuIzena, int kantitatea, double prezioa) {
            this.produktuIzena = produktuIzena != null ? produktuIzena : "";
            this.kantitatea = kantitatea;
            this.prezioa = prezioa;
            this.guztira = kantitatea * prezioa;
        }
    }
}

