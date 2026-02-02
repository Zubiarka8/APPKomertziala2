package com.example.appkomertziala;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appkomertziala.db.eredua.Katalogoa;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Katalogoa (inbentarioa) zerrenda erakusteko RecyclerView adapterra.
 * Elementu bakoitzak: irudia, izena, kodea, prezioa, stock eta Erosi botoia.
 */
public class KatalogoaAdapter extends RecyclerView.Adapter<KatalogoaAdapter.KatalogoaViewHolder> {

    private final Context context;
    private List<Katalogoa> zerrenda = new ArrayList<>();

    /** Erosi botoian sakatzean deitzen den entzulea. */
    private OnErosiClickListener erosiEntzulea;

    /** Irudi baliabidearen izena ezezaguna edo hutsa denean erabiltzen den leihoko drawable. */
    private static final int LEIHOKO_IRUDIA = R.drawable.ic_logo_generico;

    /** Erosi sakatzean: saskira gehitzeko. */
    public interface OnErosiClickListener {
        void onErosi(Katalogoa produktua);
    }

    public KatalogoaAdapter(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setErosiEntzulea(OnErosiClickListener entzulea) {
        this.erosiEntzulea = entzulea;
    }

    /** Zerrenda eguneratu eta notifyDataSetChanged. */
    public void eguneratuZerrenda(List<Katalogoa> berria) {
        this.zerrenda = berria != null ? berria : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public KatalogoaViewHolder onCreateViewHolder(@NonNull ViewGroup gurasoa, int mota) {
        View vista = LayoutInflater.from(gurasoa.getContext()).inflate(R.layout.item_katalogoa, gurasoa, false);
        return new KatalogoaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull KatalogoaViewHolder holder, int posizioa) {
        Katalogoa k = zerrenda.get(posizioa);
        int irudiId = irudiIdAurkitu(k.getIrudiaIzena());
        holder.irudia.setImageResource(irudiId);
        holder.irudia.setContentDescription(irudiId == LEIHOKO_IRUDIA
                ? context.getString(R.string.irudirik_gabe)
                : context.getString(R.string.cd_katalogoa_irudia));
        holder.izena.setText(k.getIzena() != null ? k.getIzena() : "");
        holder.kodea.setText(context.getString(R.string.katalogoa_artikulu_kodea_etiketa, k.getArtikuluKodea() != null ? k.getArtikuluKodea() : ""));
        holder.prezioa.setText(formatuaPrezioa(k.getSalmentaPrezioa()));
        holder.stock.setText(context.getString(R.string.katalogoa_stock_etiketa, k.getStock()));
        holder.btnErosi.setOnClickListener(v -> {
            if (erosiEntzulea != null) erosiEntzulea.onErosi(k);
        });
    }

    @Override
    public int getItemCount() {
        return zerrenda.size();
    }

    /**
     * irudia_izena (adib. macbook.jpg) drawable baliabide ID bihurtu.
     * Luzapena kendu eta getIdentifier erabiliz; ez badago, leihoko irudia.
     */
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

    static class KatalogoaViewHolder extends RecyclerView.ViewHolder {
        final ImageView irudia;
        final TextView izena;
        final TextView kodea;
        final TextView prezioa;
        final TextView stock;
        final MaterialButton btnErosi;

        KatalogoaViewHolder(View itemView) {
            super(itemView);
            irudia = itemView.findViewById(R.id.itemKatalogoaIrudia);
            izena = itemView.findViewById(R.id.itemKatalogoaIzena);
            kodea = itemView.findViewById(R.id.itemKatalogoaKodea);
            prezioa = itemView.findViewById(R.id.itemKatalogoaPrezioa);
            stock = itemView.findViewById(R.id.itemKatalogoaStock);
            btnErosi = itemView.findViewById(R.id.btnKatalogoaErosi);
        }
    }
}
