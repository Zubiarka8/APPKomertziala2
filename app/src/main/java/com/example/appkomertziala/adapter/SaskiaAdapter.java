package com.example.appkomertziala;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * Erosketa saskiko zerrenda erakusteko adapterra.
 * Elementu bakoitzak: irudia, izena, prezioa unitateko, kopurua (+/-), lerro guztira.
 */
public class SaskiaAdapter extends RecyclerView.Adapter<SaskiaAdapter.SaskiaViewHolder> {

    private static final int LEIHOKO_IRUDIA = R.drawable.ic_logo_generico;

    private final Context context;
    private final List<MainActivity.SaskiaElementua> zerrenda;
    /** Kopurua aldatu edo elementu bat kendu denean deitzen da (badge eta guztira eguneratzeko). */
    private Runnable onSaskiaAldaketa;

    public SaskiaAdapter(Context context, List<MainActivity.SaskiaElementua> zerrenda) {
        this.context = context.getApplicationContext();
        this.zerrenda = zerrenda != null ? zerrenda : new java.util.ArrayList<>();
    }

    public void setOnSaskiaAldaketa(Runnable runnable) {
        this.onSaskiaAldaketa = runnable;
    }

    @NonNull
    @Override
    public SaskiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saskia, parent, false);
        return new SaskiaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SaskiaViewHolder holder, int position) {
        MainActivity.SaskiaElementua e = zerrenda.get(position);
        holder.irudia.setImageResource(irudiIdAurkitu(e.irudiaIzena));
        holder.izena.setText(e.izena != null ? e.izena : e.artikuluKodea);
        holder.prezioUnitatea.setText(context.getString(R.string.saskia_prezio_unitatea, formatuaPrezioa(e.salmentaPrezioa)));
        holder.kopurua.setText(String.valueOf(e.kopurua));
        holder.lerroa.setText(formatuaPrezioa(e.salmentaPrezioa * e.kopurua));

        holder.btnKendu.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;
            if (e.kopurua <= 1) {
                zerrenda.remove(currentPos);
                notifyItemRemoved(currentPos);
            } else {
                e.kopurua--;
                notifyItemChanged(currentPos);
            }
            if (onSaskiaAldaketa != null) onSaskiaAldaketa.run();
        });

        holder.btnGehitu.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;
            if (e.kopurua >= e.stock) {
                android.widget.Toast.makeText(context, context.getString(R.string.saskia_stock_max), android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            e.kopurua++;
            notifyItemChanged(currentPos);
            if (onSaskiaAldaketa != null) onSaskiaAldaketa.run();
        });
    }

    @Override
    public int getItemCount() {
        return zerrenda.size();
    }

    private int irudiIdAurkitu(String irudiaIzena) {
        if (irudiaIzena == null || irudiaIzena.trim().isEmpty()) {
            return LEIHOKO_IRUDIA;
        }
        String izena = irudiaIzena.trim();
        int puntua = izena.lastIndexOf('.');
        if (puntua > 0) {
            izena = izena.substring(0, puntua);
        }
        izena = izena.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase(Locale.ROOT);
        int id = context.getResources().getIdentifier(izena, "drawable", context.getPackageName());
        return id != 0 ? id : LEIHOKO_IRUDIA;
    }

    private static String formatuaPrezioa(double prezioa) {
        return String.format(Locale.getDefault(), "%.2f €", prezioa);
    }

    static class SaskiaViewHolder extends RecyclerView.ViewHolder {
        final ImageView irudia;
        final TextView izena;
        final TextView prezioUnitatea;
        final TextView kopurua;
        final TextView lerroa;
        final ImageButton btnKendu;
        final ImageButton btnGehitu;

        SaskiaViewHolder(View itemView) {
            super(itemView);
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
