package com.example.appkomertziala.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appkomertziala.R;

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
        // Zerrendatik elementua hartu - begiratu hemen ea posizioa baliozkoa den
        EskaeraElementua e = zerrenda.get(posizioa);
        
        // Zenbakia jartu - null bada, kate hutsa
        holder.itemEskaeraZenbakia.setText(e.zenbakia != null ? e.zenbakia : "");
        
        // Data jartu - null bada, kate hutsa
        holder.itemEskaeraData.setText(e.data != null ? e.data : "");
        
        // Artikulu kopurua jartu - hau hemen zenbaki bat da, String bihurtu
        holder.itemEskaeraArtikuluak.setText(String.valueOf(e.artikuluKopurua));
        
        // Guztira prezioa jartu - formatu ederra: "123.45 €"
        holder.itemEskaeraGuztira.setText(String.format(Locale.getDefault(), "%.2f €", e.guztira));
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
        /** Eskaeraren zenbakia erakusteko TextView. */
        final TextView itemEskaeraZenbakia;
        
        /** Eskaeraren data erakusteko TextView. */
        final TextView itemEskaeraData;
        
        /** Artikulu kopurua erakusteko TextView. */
        final TextView itemEskaeraArtikuluak;
        
        /** Guztira prezioa erakusteko TextView. */
        final TextView itemEskaeraGuztira;

        /**
         * Eraikitzailea: View-ak kargatzen ditu findViewById-ekin.
         * 
         * @param itemView item_eskaera.xml layout-aren View-a
         */
        EskaeraViewHolder(@NonNull View itemView) {
            super(itemView);
            // TextView guztiak kargatu - hau hemen behin bakarrik egiten da, optimizazio ona!
            itemEskaeraZenbakia = itemView.findViewById(R.id.itemEskaeraZenbakia);
            itemEskaeraData = itemView.findViewById(R.id.itemEskaeraData);
            itemEskaeraArtikuluak = itemView.findViewById(R.id.itemEskaeraArtikuluak);
            itemEskaeraGuztira = itemView.findViewById(R.id.itemEskaeraGuztira);
        }
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
        /** Eskaeraren zenbakia. Null izan daiteke. */
        final String zenbakia;
        
        /** Eskaeraren data. Null izan daiteke. */
        final String data;
        
        /** Artikulu kopurua (guztira). */
        final int artikuluKopurua;
        
        /** Prezio totala (eurotan). */
        final double guztira;

        /**
         * Eraikitzailea: eskaera elementu bat sortzen du.
         * 
         * @param zenbakia Eskaeraren zenbakia
         * @param data Eskaeraren data
         * @param artikuluKopurua Artikulu kopurua
         * @param guztira Prezio totala
         */
        public EskaeraElementua(String zenbakia, String data, int artikuluKopurua, double guztira) {
            this.zenbakia = zenbakia;
            this.data = data;
            this.artikuluKopurua = artikuluKopurua;
            this.guztira = guztira;
        }
    }
}

