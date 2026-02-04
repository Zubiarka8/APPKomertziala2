package com.example.appkomertziala.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appkomertziala.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * HistorialCompra adapter: RecyclerView-arentzat erosketa historial zerrenda erakusteko.
 * 
 * Hau hemen bidalketen historiala erakusteko adapterra da. HistorialCompraActivity-k
 * erabiltzen du erosketa historial zerrenda bat erakusteko. Elementu bakoitzak produktua,
 * kantitatea, prezio unitarioa eta prezio totala erakusten ditu.
 * 
 * Nola funtzionatzen duen:
 * 1. HistorialCompraActivity-k datu basean HistorialCompra entitateak irakurtzen ditu
 * 2. Bakoitzarentzat produktu izena, kantitatea, prezio unitarioa eta totala kalkulatzen du
 * 3. HistorialElementua objektuak sortzen ditu eta adapter honi ematen dio
 * 4. Adapter-ak RecyclerView-n erakusten ditu item_historial_compra layout-a erabiliz
 */
public class HistorialErosketaAdapter extends RecyclerView.Adapter<HistorialErosketaAdapter.HistorialViewHolder> {

    /** Historial elementuen zerrenda. Hau hemen badago, dena ondo doa. */
    private final List<HistorialElementua> zerrenda;
    
    /** LayoutInflater: XML layout-ak View objektu bihurtzeko. RecyclerView-ek erabiltzen du. */
    private final LayoutInflater inflater;

    /**
     * Eraikitzailea: adapter-a sortzen du context batetik.
     * 
     * @param context Android context-a (normalean Activity bat). LayoutInflater sortzeko erabiltzen da.
     */
    public HistorialErosketaAdapter(android.content.Context context) {
        // Zerrenda hutsa hasieratu - gero eguneratuko dugu datuekin
        this.zerrenda = new ArrayList<>();
        // LayoutInflater sortu context-etik - hau hemen beharrezkoa da View-ak sortzeko
        this.inflater = LayoutInflater.from(context);
    }

    /**
     * Zerrenda eguneratu eta notifyDataSetChanged deitu.
     * 
     * HistorialCompraActivity-k datu basean historialak irakurri eta gero deitzen du metodo hau.
     * Zerrenda zaharra garbitu eta berria sartzen du, gero RecyclerView-ri esaten dio
     * eguneratu behar dela.
     * 
     * @param berria Historial elementuen zerrenda berria. Null bada, zerrenda hutsik geratuko da.
     */
    public void eguneratuZerrenda(List<HistorialElementua> berria) {
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
     * item_historial_compra.xml layout-a kargatzen du eta ViewHolder bat sortzen du.
     * 
     * @param gurasoa ViewGroup gurasoa (RecyclerView bera)
     * @param mota View mota (normalean 0, baina mota anitzeko adapter-etan erabilgarria)
     * @return HistorialViewHolder berria, item_historial_compra layout-arekin
     */
    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(@NonNull ViewGroup gurasoa, int mota) {
        // XML layout-a View objektu bihurtu - hau hemen magia gertatzen da
        View erroa = inflater.inflate(R.layout.item_historial_compra, gurasoa, false);
        // ViewHolder sortu eta itzuli
        return new HistorialViewHolder(erroa);
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
    public void onBindViewHolder(@NonNull HistorialViewHolder holder, int posizioa) {
        // Zerrendatik elementua hartu - begiratu hemen ea posizioa baliozkoa den
        HistorialElementua e = zerrenda.get(posizioa);
        
        // Bidalketa kodea
        holder.itemHistorialKodea.setText(e.kodea != null ? e.kodea : "—");
        
        // Helmuga
        holder.itemHistorialHelmuga.setText(e.helmuga != null ? e.helmuga : "—");
        
        // Data
        holder.itemHistorialData.setText(e.data != null ? e.data : "—");
        
        // Produktu izena jartu - null bada, kate hutsa
        holder.itemHistorialProduktua.setText(e.produktua != null ? e.produktua : "");
        
        // Producto ID
        holder.itemHistorialProductoId.setText(e.productoId != null ? e.productoId : "—");
        
        // Kantitatea jartu - hau hemen zenbaki bat da, String bihurtu
        holder.itemHistorialKantitatea.setText(String.valueOf(e.kantitatea));
        
        // Bidalita
        holder.itemHistorialBidalita.setText(String.valueOf(e.bidalita));
        
        // Prezio unitarioa jartu - formatu ederra: "12.50 €"
        holder.itemHistorialPrezioUnit.setText(String.format(Locale.getDefault(), "%.2f €", e.prezioUnit));
        
        // Prezio totala jartu - formatu ederra: "125.00 €"
        holder.itemHistorialPrezioTotala.setText(String.format(Locale.getDefault(), "%.2f €", e.prezioTotala));
        
        // Amaituta switch konfiguratu
        holder.itemHistorialAmaituta.setChecked(e.amaituta);
        holder.itemHistorialAmaitutaText.setText(e.amaituta ? 
            holder.itemView.getContext().getString(R.string.historial_amaituta_true) : 
            holder.itemView.getContext().getString(R.string.historial_amaituta_false));
        holder.itemHistorialAmaituta.setOnCheckedChangeListener(null); // Listener temporala kendu aldaketa automatikoa saihesteko
        holder.itemHistorialAmaituta.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (amaitutaAldaketaListener != null) {
                e.amaituta = isChecked;
                holder.itemHistorialAmaitutaText.setText(isChecked ? 
                    holder.itemView.getContext().getString(R.string.historial_amaituta_true) : 
                    holder.itemView.getContext().getString(R.string.historial_amaituta_false));
                amaitutaAldaketaListener.onAmaitutaAldaketa(e.historialId, isChecked);
            }
        });
        
        // Ikusi historiala botoia konfiguratu
        if (holder.btnHistorialIkusi != null) {
            holder.btnHistorialIkusi.setVisibility(View.VISIBLE);
            holder.btnHistorialIkusi.setEnabled(true);
            holder.btnHistorialIkusi.setAlpha(1.0f); // Asegurar que no esté transparente
            holder.btnHistorialIkusi.setOnClickListener(null); // Listener zaharra kendu
            holder.btnHistorialIkusi.setOnClickListener(v -> {
                if (ikusiHistorialListener != null) {
                    ikusiHistorialListener.onIkusiHistorial(e.historialId);
                }
            });
            Log.d("HistorialAdapter", "Ikusi historial button configured for historialId: " + e.historialId);
        } else {
            Log.e("HistorialAdapter", "ERROR: btnHistorialIkusi es null!");
        }
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
    static class HistorialViewHolder extends RecyclerView.ViewHolder {
        /** Bidalketa kodea erakusteko TextView. */
        final TextView itemHistorialKodea;
        
        /** Helmuga erakusteko TextView. */
        final TextView itemHistorialHelmuga;
        
        /** Data erakusteko TextView. */
        final TextView itemHistorialData;
        
        /** Produktuaren izena erakusteko TextView. */
        final TextView itemHistorialProduktua;
        
        /** Producto ID erakusteko TextView. */
        final TextView itemHistorialProductoId;
        
        /** Kantitatea erakusteko TextView. */
        final TextView itemHistorialKantitatea;
        
        /** Bidalita erakusteko TextView. */
        final TextView itemHistorialBidalita;
        
        /** Prezio unitarioa erakusteko TextView. */
        final TextView itemHistorialPrezioUnit;
        
        /** Prezio totala erakusteko TextView. */
        final TextView itemHistorialPrezioTotala;
        
        /** Amaituta egoera aldatzeko Switch. */
        final SwitchMaterial itemHistorialAmaituta;
        
        /** Amaituta egoera erakusteko TextView. */
        final TextView itemHistorialAmaitutaText;
        
        /** Ikusi historiala botoia. */
        final MaterialButton btnHistorialIkusi;

        /**
         * Eraikitzailea: View-ak kargatzen ditu findViewById-ekin.
         * 
         * @param itemView item_historial_compra.xml layout-aren View-a
         */
        HistorialViewHolder(@NonNull View itemView) {
            super(itemView);
            // TextView guztiak kargatu - hau hemen behin bakarrik egiten da, optimizazio ona!
            itemHistorialKodea = itemView.findViewById(R.id.itemHistorialKodea);
            itemHistorialHelmuga = itemView.findViewById(R.id.itemHistorialHelmuga);
            itemHistorialData = itemView.findViewById(R.id.itemHistorialData);
            itemHistorialProduktua = itemView.findViewById(R.id.itemHistorialProduktua);
            itemHistorialProductoId = itemView.findViewById(R.id.itemHistorialProductoId);
            itemHistorialKantitatea = itemView.findViewById(R.id.itemHistorialKantitatea);
            itemHistorialBidalita = itemView.findViewById(R.id.itemHistorialBidalita);
            itemHistorialPrezioUnit = itemView.findViewById(R.id.itemHistorialPrezioUnit);
            itemHistorialPrezioTotala = itemView.findViewById(R.id.itemHistorialPrezioTotala);
            itemHistorialAmaituta = itemView.findViewById(R.id.itemHistorialAmaituta);
            itemHistorialAmaitutaText = itemView.findViewById(R.id.itemHistorialAmaitutaText);
            btnHistorialIkusi = itemView.findViewById(R.id.btnHistorialIkusi);
            if (btnHistorialIkusi == null) {
                Log.e("HistorialAdapter", "ERROR: No se encontró btnHistorialIkusi en el layout!");
            } else {
                Log.d("HistorialAdapter", "btnHistorialIkusi encontrado correctamente");
            }
        }
    }

    /**
     * Historial elementu bat: informazio osoa bidalketa eta produktuaren datuekin.
     * 
     * Hau hemen datu kontainer bat da. HistorialCompraActivity-k HistorialCompra entitateak
     * irakurri eta gero, datu hauek biltzen ditu eta adapter-ari ematen dio.
     */
    public static class HistorialElementua {
        /** Bidalketaren kodea. */
        final String kodea;
        
        /** Bidalketaren helmuga. */
        final String helmuga;
        
        /** Bidalketaren data. */
        final String data;
        
        /** Produktuaren izena. Null izan daiteke. */
        final String produktua;
        
        /** Produktuaren ID. */
        final String productoId;
        
        /** Kantitatea (unitate kopurua, eskatuta). */
        final int kantitatea;
        
        /** Bidaltutako kantitatea. */
        final int bidalita;
        
        /** Prezio unitarioa (eurotan). */
        final double prezioUnit;
        
        /** Prezio totala (eurotan). */
        final double prezioTotala;
        
        /** HistorialCompra-ren ID (eguneratu ahal izateko). */
        final long historialId;
        
        /** Bidalketa amaituta dagoen ala ez (true = iritsi da, false = ez da iritsi). */
        boolean amaituta;

        /**
         * Eraikitzailea: historial elementu bat sortzen du.
         */
        public HistorialElementua(String kodea, String helmuga, String data, String produktua, 
                String productoId, int kantitatea, int bidalita, double prezioUnit, 
                double prezioTotala, long historialId, boolean amaituta) {
            this.kodea = kodea;
            this.helmuga = helmuga;
            this.data = data;
            this.produktua = produktua;
            this.productoId = productoId;
            this.kantitatea = kantitatea;
            this.bidalita = bidalita;
            this.prezioUnit = prezioUnit;
            this.prezioTotala = prezioTotala;
            this.historialId = historialId;
            this.amaituta = amaituta;
        }
    }
    
    /** Listener amaituta egoera aldatzeko. */
    private OnAmaitutaAldaketaListener amaitutaAldaketaListener;
    
    /**
     * Listener amaituta egoera aldatzeko.
     */
    public interface OnAmaitutaAldaketaListener {
        /**
         * Amaituta egoera aldatu denean deitu behar da.
         * 
         * @param historialId HistorialCompra-ren ID
         * @param amaituta Egoera berria (true = iritsi da, false = ez da iritsi)
         */
        void onAmaitutaAldaketa(long historialId, boolean amaituta);
    }
    
    /**
     * Listener ezarri amaituta egoera aldatzeko.
     * 
     * @param listener Listener-a
     */
    public void setOnAmaitutaAldaketaListener(OnAmaitutaAldaketaListener listener) {
        this.amaitutaAldaketaListener = listener;
    }
    
    /** Listener ikusi historiala botoia sakatzean. */
    private OnIkusiHistorialListener ikusiHistorialListener;
    
    /**
     * Listener ikusi historiala botoia sakatzean.
     */
    public interface OnIkusiHistorialListener {
        /**
         * Ikusi historiala botoia sakatzean deitu behar da.
         * 
         * @param historialId HistorialCompra-ren ID
         */
        void onIkusiHistorial(long historialId);
    }
    
    /**
     * Listener ezarri ikusi historiala botoia sakatzean.
     * 
     * @param listener Listener-a
     */
    public void setOnIkusiHistorialListener(OnIkusiHistorialListener listener) {
        this.ikusiHistorialListener = listener;
    }
}

