package com.example.appkomertziala.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appkomertziala.MainActivity;
import com.example.appkomertziala.R;

import java.util.List;
import java.util.Locale;

/**
 * Erosketa saskiko zerrenda erakusteko adapterra.
 * 
 * Hau hemen saskiko produktuak erakusteko adapterra da. MainActivity-k erabiltzen du
 * saskiko produktu zerrenda bat erakusteko. Elementu bakoitzak irudia, izena, prezioa
 * unitateko, kopurua (+/- botoiekin) eta lerro guztira erakusten ditu.
 * 
 * Nola funtzionatzen duen:
 * 1. MainActivity-k saskiko produktuak gordetzen ditu MainActivity.SaskiaElementua objektuetan
 * 2. Adapter honi ematen dio zerrenda bat
 * 3. Adapter-ak RecyclerView-n erakusten ditu item_saskia layout-a erabiliz
 * 4. Gehitu botoian sakatzean, kopurua handitzen da (stock maximo arte)
 * 5. Kendu botoian sakatzean, kopurua gutxitzen da (1 baino gutxiago bada, elementua kentzen da)
 * 6. Aldaketa bakoitzaren ondoren, onSaskiaAldaketa entzulea deitzen da (badge eta guztira eguneratzeko)
 */
public class SaskiaAdapter extends RecyclerView.Adapter<SaskiaAdapter.SaskiaViewHolder> {

    /** Irudi baliabidearen izena ezezaguna edo hutsa denean erabiltzen den leihoko drawable. */
    private static final int LEIHOKO_IRUDIA = R.drawable.ic_logo_generico;

    /** Android context-a. Irudiak kargatzeko eta string baliabideak erabiltzeko beharrezkoa. */
    private final Context context;
    
    /** Saskia elementuen zerrenda. Hau hemen badago, dena ondo doa. */
    private final List<MainActivity.SaskiaElementua> zerrenda;
    
    /** Kopurua aldatu edo elementu bat kendu denean deitzen da (badge eta guztira eguneratzeko). */
    private Runnable onSaskiaAldaketa;

    /**
     * Eraikitzailea: adapter-a sortzen du context eta zerrenda batetik.
     * 
     * ApplicationContext erabiltzen du memory leak-ak saihesteko (Activity context baten ordez).
     * 
     * @param context Android context-a (normalean Activity bat)
     * @param zerrenda Saskia elementuen zerrenda. Null bada, zerrenda hutsa sortzen da.
     */
    public SaskiaAdapter(Context context, List<MainActivity.SaskiaElementua> zerrenda) {
        // ApplicationContext erabili - hau hemen garrantzitsua da memory leak-ak saihesteko
        this.context = context.getApplicationContext();
        // Zerrenda ezarri - null bada, zerrenda hutsa
        this.zerrenda = zerrenda != null ? zerrenda : new java.util.ArrayList<>();
    }

    /**
     * Saskia aldaketa entzulea ezartzen du.
     * 
     * Hau hemen callback bat da. Kopurua aldatu edo elementu bat kendu denean deitzen da,
     * badge eta guztira eguneratzeko.
     * 
     * @param runnable Saskia aldatu denean deitzen den runnable
     */
    public void setOnSaskiaAldaketa(Runnable runnable) {
        this.onSaskiaAldaketa = runnable;
    }

    /**
     * ViewHolder bat sortzen du elementu bakoitzarentzat.
     * 
     * RecyclerView-k deitzen du elementu berri bat sortu behar denean.
     * item_saskia.xml layout-a kargatzen du eta ViewHolder bat sortzen du.
     * 
     * @param parent ViewGroup gurasoa (RecyclerView bera)
     * @param viewType View mota (normalean 0, baina mota anitzeko adapter-etan erabilgarria)
     * @return SaskiaViewHolder berria, item_saskia layout-arekin
     */
    @NonNull
    @Override
    public SaskiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // XML layout-a View objektu bihurtu - hau hemen magia gertatzen da
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saskia, parent, false);
        // ViewHolder sortu eta itzuli
        return new SaskiaViewHolder(v);
    }

    /**
     * ViewHolder bat datuekin betetzen du.
     * 
     * RecyclerView-k deitzen du elementu bat erakusteko behar denean.
     * Zerrendako posizio baten datuak hartzen ditu eta ViewHolder-eko View-etan jartzen ditu.
     * Gehitu eta Kendu botoien entzuleak ere ezartzen ditu.
     * 
     * @param holder Eguneratu behar den ViewHolder
     * @param position Zerrendako posizioa (0-tik hasita)
     */
    @Override
    public void onBindViewHolder(@NonNull SaskiaViewHolder holder, int position) {
        // Zerrendatik elementua hartu - begiratu hemen ea posizioa baliozkoa den
        MainActivity.SaskiaElementua e = zerrenda.get(position);
        
        // Irudi ID aurkitu eta irudia jartzu - hau hemen produktuaren irudi izena drawable ID bihurtzen du
        holder.irudia.setImageResource(irudiIdAurkitu(e.irudiaIzena));
        
        // Produktu izena jartzu - null bada, artikulu kodea erabili
        holder.izena.setText(e.izena != null ? e.izena : e.artikuluKodea);
        
        // Prezio unitarioa jartzu - formatu ederra: "Prezioa unitateko: 12.50 €"
        holder.prezioUnitatea.setText(context.getString(R.string.saskia_prezio_unitatea, formatuaPrezioa(e.salmentaPrezioa)));
        
        // Kopurua jartzu - hau hemen zenbaki bat da, String bihurtu
        holder.kopurua.setText(String.valueOf(e.kopurua));
        
        // Lerro guztira jartzu - formatu ederra: "125.00 €" (prezioa * kopurua)
        holder.lerroa.setText(formatuaPrezioa(e.salmentaPrezioa * e.kopurua));

        // Kendu botoiaren entzulea ezarri - hau hemen kopurua gutxitzen du
        holder.btnKendu.setOnClickListener(v -> {
            // Posizioa lortu - hau hemen garrantzitsua da, adapter eguneratu denean posizioa aldatu daitekeelako
            int currentPos = holder.getBindingAdapterPosition();
            // Begiratu hemen ea posizioa baliozkoa den
            if (currentPos == RecyclerView.NO_POSITION) {
                // Posizioa baliozkoa ez bada, ezer ez egin
                return;
            }
            
            // Kopurua 1 edo gutxiago bada, elementua kendu
            if (e.kopurua <= 1) {
                // Zerrendatik kendu
                zerrenda.remove(currentPos);
                // RecyclerView-ri esan elementu bat kendu dela - hau hemen animazioa aktibatzen du
                notifyItemRemoved(currentPos);
            } else {
                // Bestela, kopurua gutxitu
                e.kopurua--;
                // RecyclerView-ri esan elementua aldatu dela - hau hemen kopurua eguneratzen du
                notifyItemChanged(currentPos);
            }
            
            // Saskia aldaketa entzulea deitu - hau hemen badge eta guztira eguneratzen ditu
            if (onSaskiaAldaketa != null) {
                onSaskiaAldaketa.run();
            }
        });

        // Gehitu botoiaren entzulea ezarri - hau hemen kopurua handitzen du
        holder.btnGehitu.setOnClickListener(v -> {
            // Posizioa lortu - hau hemen garrantzitsua da, adapter eguneratu denean posizioa aldatu daitekeelako
            int currentPos = holder.getBindingAdapterPosition();
            // Begiratu hemen ea posizioa baliozkoa den
            if (currentPos == RecyclerView.NO_POSITION) {
                // Posizioa baliozkoa ez bada, ezer ez egin
                return;
            }
            
            // Kopurua stock maximo baino handiagoa edo berdina bada, ezer ez egin
            if (e.kopurua >= e.stock) {
                // Toast mezua erakutsi - hau hemen erabiltzaileari jakinarazten dio stock maximoa iritsi dela
                android.widget.Toast.makeText(context, context.getString(R.string.saskia_stock_max), android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Kopurua handitu
            e.kopurua++;
            // RecyclerView-ri esan elementua aldatu dela - hau hemen kopurua eguneratzen du
            notifyItemChanged(currentPos);
            
            // Saskia aldaketa entzulea deitu - hau hemen badge eta guztira eguneratzen ditu
            if (onSaskiaAldaketa != null) {
                onSaskiaAldaketa.run();
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
    static class SaskiaViewHolder extends RecyclerView.ViewHolder {
        /** Produktuaren irudia erakusteko ImageView. */
        final ImageView irudia;
        
        /** Produktuaren izena erakusteko TextView. */
        final TextView izena;
        
        /** Prezio unitarioa erakusteko TextView. */
        final TextView prezioUnitatea;
        
        /** Kopurua erakusteko TextView. */
        final TextView kopurua;
        
        /** Lerro guztira erakusteko TextView. */
        final TextView lerroa;
        
        /** Kopurua gutxitzeko botoia (ImageButton). */
        final ImageButton btnKendu;
        
        /** Kopurua handitzeko botoia (ImageButton). */
        final ImageButton btnGehitu;

        /**
         * Eraikitzailea: View-ak kargatzen ditu findViewById-ekin.
         * 
         * @param itemView item_saskia.xml layout-aren View-a
         */
        SaskiaViewHolder(View itemView) {
            super(itemView);
            // View guztiak kargatu - hau hemen behin bakarrik egiten da, optimizazio ona!
            irudia = itemView.findViewById(R.id.itemSaskiaIrudia);
            izena = itemView.findViewById(R.id.itemSaskiaIzena);
            prezioUnitatea = itemView.findViewById(R.id.itemSaskiaPrezioUnitatea);
            kopurua = itemView.findViewById(R.id.itemSaskiaKopurua);
            lerroa = itemView.findViewById(R.id.itemSaskiaLerroa);
            btnKendu = itemView.findViewById(R.id.btnSaskiaKendu);
            btnGehitu = itemView.findViewById(R.id.btnSaskiaGehitu);
        }
    }
}
