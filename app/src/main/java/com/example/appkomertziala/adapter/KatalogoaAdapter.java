package com.example.appkomertziala.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appkomertziala.R;
import com.example.appkomertziala.db.eredua.Katalogoa;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Katalogoa (inbentarioa) zerrenda erakusteko RecyclerView adapterra.
 * 
 * Hau hemen produktu katalogoa erakusteko adapterra da. KatalogoaActivity-k erabiltzen du
 * produktu zerrenda bat erakusteko. Elementu bakoitzak irudia, izena, kodea, prezioa,
 * stock eta Erosi botoia erakusten ditu.
 * 
 * Nola funtzionatzen duen:
 * 1. KatalogoaActivity-k datu basean Katalogoa entitateak irakurtzen ditu
 * 2. Adapter honi ematen dio zerrenda bat
 * 3. Adapter-ak RecyclerView-n erakusten ditu item_katalogoa layout-a erabiliz
 * 4. Erosi botoian sakatzean, entzulea deitzen da produktua saskira gehitzeko
 * 5. Item-ean sakatzean, entzulea deitzen da produktuaren detale orria irekitzeko
 */
public class KatalogoaAdapter extends RecyclerView.Adapter<KatalogoaAdapter.KatalogoaViewHolder> {

    /** Android context-a. Irudiak kargatzeko eta string baliabideak erabiltzeko beharrezkoa. */
    private final Context context;
    
    /** Katalogoa entitateen zerrenda. Hau hemen badago, dena ondo doa. */
    private List<Katalogoa> zerrenda = new ArrayList<>();

    /** Erosi botoian sakatzean deitzen den entzulea. Hau hemen produktua saskira gehitzeko erabiltzen da. */
    private OnErosiClickListener erosiEntzulea;

    /** Item (produktua) sakatzean detale orria irekitzeko entzulea. Hau hemen ProduktuDetalaActivity irekitzeko erabiltzen da. */
    private OnItemClickListener itemEntzulea;

    /** Irudi baliabidearen izena ezezaguna edo hutsa denean erabiltzen den leihoko drawable. */
    private static final int LEIHOKO_IRUDIA = R.drawable.ic_logo_generico;

    /**
     * Erosi sakatzean: saskira gehitzeko entzulea.
     * 
     * Hau hemen callback bat da. Erosi botoian sakatzean, produktua saskira gehitzen da.
     */
    public interface OnErosiClickListener {
        /**
         * Erosi botoian sakatzean deitzen da.
         * 
         * @param produktua Saskira gehitu behar den produktua
         */
        void onErosi(Katalogoa produktua);
    }

    /**
     * Item sakatzean: produktuaren detale orria irekitzeko entzulea.
     * 
     * Hau hemen callback bat da. Item-ean sakatzean, produktuaren detale orria irekitzen da.
     */
    public interface OnItemClickListener {
        /**
         * Item-ean sakatzean deitzen da.
         * 
         * @param produktua Detale orria ireki behar den produktua
         */
        void onItemClick(Katalogoa produktua);
    }

    /**
     * Eraikitzailea: adapter-a sortzen du context batetik.
     * 
     * ApplicationContext erabiltzen du memory leak-ak saihesteko (Activity context baten ordez).
     * 
     * @param context Android context-a (normalean Activity bat)
     */
    public KatalogoaAdapter(Context context) {
        // ApplicationContext erabili - hau hemen garrantzitsua da memory leak-ak saihesteko
        this.context = context.getApplicationContext();
    }

    /**
     * Erosi entzulea ezartzen du.
     * 
     * @param entzulea Erosi botoian sakatzean deitzen den entzulea
     */
    public void setErosiEntzulea(OnErosiClickListener entzulea) {
        this.erosiEntzulea = entzulea;
    }

    /**
     * Item entzulea ezartzen du.
     * 
     * @param entzulea Item-ean sakatzean deitzen den entzulea
     */
    public void setItemEntzulea(OnItemClickListener entzulea) {
        this.itemEntzulea = entzulea;
    }

    /**
     * Zerrenda eguneratu eta notifyDataSetChanged.
     * 
     * KatalogoaActivity-k datu basean produktuak irakurri eta gero deitzen du metodo hau.
     * Zerrenda berria sartzen du, gero RecyclerView-ri esaten dio eguneratu behar dela.
     * 
     * @param berria Katalogoa entitateen zerrenda berria. Null bada, zerrenda hutsik geratuko da.
     */
    public void eguneratuZerrenda(List<Katalogoa> berria) {
        // Zerrenda berria ezarri - null bada, zerrenda hutsa
        this.zerrenda = berria != null ? berria : new ArrayList<>();
        // RecyclerView-ri esan eguneratu behar duela - hau hemen garrantzitsua da!
        notifyDataSetChanged();
    }

    /**
     * ViewHolder bat sortzen du elementu bakoitzarentzat.
     * 
     * RecyclerView-k deitzen du elementu berri bat sortu behar denean.
     * item_katalogoa.xml layout-a kargatzen du eta ViewHolder bat sortzen du.
     * 
     * @param gurasoa ViewGroup gurasoa (RecyclerView bera)
     * @param mota View mota (normalean 0, baina mota anitzeko adapter-etan erabilgarria)
     * @return KatalogoaViewHolder berria, item_katalogoa layout-arekin
     */
    @NonNull
    @Override
    public KatalogoaViewHolder onCreateViewHolder(@NonNull ViewGroup gurasoa, int mota) {
        // XML layout-a View objektu bihurtu - hau hemen magia gertatzen da
        View vista = LayoutInflater.from(gurasoa.getContext()).inflate(R.layout.item_katalogoa, gurasoa, false);
        // ViewHolder sortu eta itzuli
        return new KatalogoaViewHolder(vista);
    }

    /**
     * ViewHolder bat datuekin betetzen du.
     * 
     * RecyclerView-k deitzen du elementu bat erakusteko behar denean.
     * Zerrendako posizio baten datuak hartzen ditu eta ViewHolder-eko View-etan jartzen ditu.
     * Erosi botoiaren eta item-aren entzuleak ere ezartzen ditu.
     * 
     * @param holder Eguneratu behar den ViewHolder
     * @param posizioa Zerrendako posizioa (0-tik hasita)
     */
    @Override
    public void onBindViewHolder(@NonNull KatalogoaViewHolder holder, int posizioa) {
        // Zerrendatik produktua hartu - begiratu hemen ea posizioa baliozkoa den
        Katalogoa k = zerrenda.get(posizioa);
        
        // Irudi ID aurkitu - hau hemen produktuaren irudi izena drawable ID bihurtzen du
        int irudiId = irudiIdAurkitu(k.getIrudiaIzena());
        // Irudia jartzu
        holder.irudia.setImageResource(irudiId);
        // Content description jartzu - accessibility-rentzat garrantzitsua
        holder.irudia.setContentDescription(irudiId == LEIHOKO_IRUDIA
                ? context.getString(R.string.irudirik_gabe)
                : context.getString(R.string.cd_katalogoa_irudia));
        
        // Produktu izena jartzu - null bada, kate hutsa
        holder.izena.setText(k.getIzena() != null ? k.getIzena() : "");
        
        // Artikulu kodea jartzu - formatu ederra: "Kodea: ART-001"
        holder.kodea.setText(context.getString(R.string.katalogoa_artikulu_kodea_etiketa, k.getArtikuluKodea() != null ? k.getArtikuluKodea() : ""));
        
        // Prezioa jartzu - formatu ederra: "123.45 €"
        holder.prezioa.setText(formatuaPrezioa(k.getSalmentaPrezioa()));
        
        // Stock jartzu - formatu ederra: "Stock: 10"
        holder.stock.setText(context.getString(R.string.katalogoa_stock_etiketa, k.getStock()));
        
        // Erosi botoiaren entzulea ezarri - hau hemen produktua saskira gehitzen du
        holder.btnErosi.setOnClickListener(v -> {
            // Begiratu hemen ea entzulea badago
            if (erosiEntzulea != null) {
                // Entzulea deitu - goazen esportazio honekin egurra ematera!
                erosiEntzulea.onErosi(k);
            }
        });
        
        // Item-aren entzulea ezarri - hau hemen produktuaren detale orria irekitzen du
        holder.itemView.setOnClickListener(v -> {
            // Begiratu hemen ea entzulea badago
            if (itemEntzulea != null) {
                // Entzulea deitu
                itemEntzulea.onItemClick(k);
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
     * irudia_izena (adib. macbook.jpg) drawable baliabide ID bihurtu.
     * 
     * Hau hemen produktuaren irudi izena (adib. "macbook.jpg") drawable baliabide ID
     * bihurtzen du. Prozesua:
     * 1. Null edo hutsa bada, leihoko irudia itzultzen du
     * 2. Luzapena kentzen du (adib. ".jpg" -> "")
     * 3. Karaktere bereziak "_" bihurtzen ditu eta minuskulak egiten ditu
     * 4. getIdentifier erabiliz drawable ID bilatzen du
     * 5. Ez badago, leihoko irudia itzultzen du
     * 
     * @param irudiaIzena Produktuaren irudi izena (adib. "macbook.jpg")
     * @return Drawable baliabide ID-a, edo LEIHOKO_IRUDIA ez badago
     */
    private int irudiIdAurkitu(String irudiaIzena) {
        // Begiratu hemen ea irudi izena null edo hutsa den
        if (irudiaIzena == null || irudiaIzena.trim().isEmpty()) {
            // Leihoko irudia itzuli
            return LEIHOKO_IRUDIA;
        }
        
        // Izena garbitu - trim egin
        String izena = irudiaIzena.trim();
        
        // Luzapena kendu - azken puntua aurkitu
        int puntua = izena.lastIndexOf('.');
        if (puntua > 0) {
            // Puntua baino lehenagoko zatia hartu
            izena = izena.substring(0, puntua);
        }
        
        // Karaktere bereziak "_" bihurtu eta minuskulak egin - hau hemen Android drawable izenak onartzen ditu
        izena = izena.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase(Locale.ROOT);
        
        // Drawable ID bilatu - hau hemen getIdentifier erabiltzen du
        int id = context.getResources().getIdentifier(izena, "drawable", context.getPackageName());
        
        // ID baliozkoa bada, itzuli; bestela leihoko irudia
        return id != 0 ? id : LEIHOKO_IRUDIA;
    }

    /**
     * Prezioa formatu eder batean itzultzen du.
     * 
     * @param prezioa Prezioa (eurotan)
     * @return Formatu eder batean: "123.45 €"
     */
    private static String formatuaPrezioa(double prezioa) {
        return String.format(Locale.getDefault(), "%.2f €", prezioa);
    }

    /**
     * ViewHolder: RecyclerView elementu bakoitzaren View-ak gordetzen ditu.
     * 
     * Hau hemen optimizazio bat da: findViewById behin bakarrik deitzen da,
     * ez elementu bakoitza erakusteko behin eta berriro. Honek mapan jartzen gaitu
     * performance aldetik.
     */
    static class KatalogoaViewHolder extends RecyclerView.ViewHolder {
        /** Produktuaren irudia erakusteko ImageView. */
        final ImageView irudia;
        
        /** Produktuaren izena erakusteko TextView. */
        final TextView izena;
        
        /** Produktuaren kodea erakusteko TextView. */
        final TextView kodea;
        
        /** Produktuaren prezioa erakusteko TextView. */
        final TextView prezioa;
        
        /** Produktuaren stock erakusteko TextView. */
        final TextView stock;
        
        /** Erosi botoia (MaterialButton). */
        final MaterialButton btnErosi;

        /**
         * Eraikitzailea: View-ak kargatzen ditu findViewById-ekin.
         * 
         * @param itemView item_katalogoa.xml layout-aren View-a
         */
        KatalogoaViewHolder(View itemView) {
            super(itemView);
            // View guztiak kargatu - hau hemen behin bakarrik egiten da, optimizazio ona!
            irudia = itemView.findViewById(R.id.itemKatalogoaIrudia);
            izena = itemView.findViewById(R.id.itemKatalogoaIzena);
            kodea = itemView.findViewById(R.id.itemKatalogoaKodea);
            prezioa = itemView.findViewById(R.id.itemKatalogoaPrezioa);
            stock = itemView.findViewById(R.id.itemKatalogoaStock);
            btnErosi = itemView.findViewById(R.id.btnKatalogoaErosi);
        }
    }
}
