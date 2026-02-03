package com.example.appkomertziala;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Eskaerak adapter: RecyclerView-arentzat eskaera zerrenda erakusteko.
 */
public class EskaerakAdapter extends RecyclerView.Adapter<EskaerakAdapter.EskaeraViewHolder> {

    private final List<EskaeraElementua> zerrenda;
    private final LayoutInflater inflater;

    public EskaerakAdapter(android.content.Context context) {
        this.zerrenda = new ArrayList<>();
        this.inflater = LayoutInflater.from(context);
    }

    /** Zerrenda eguneratu eta notifyDataSetChanged deitu. */
    public void eguneratuZerrenda(List<EskaeraElementua> berria) {
        this.zerrenda.clear();
        if (berria != null) {
            this.zerrenda.addAll(berria);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EskaeraViewHolder onCreateViewHolder(@NonNull ViewGroup gurasoa, int mota) {
        View erroa = inflater.inflate(R.layout.item_eskaera, gurasoa, false);
        return new EskaeraViewHolder(erroa);
    }

    @Override
    public void onBindViewHolder(@NonNull EskaeraViewHolder holder, int posizioa) {
        EskaeraElementua e = zerrenda.get(posizioa);
        holder.itemEskaeraZenbakia.setText(e.zenbakia != null ? e.zenbakia : "");
        holder.itemEskaeraData.setText(e.data != null ? e.data : "");
        holder.itemEskaeraArtikuluak.setText(String.valueOf(e.artikuluKopurua));
        holder.itemEskaeraGuztira.setText(String.format(Locale.getDefault(), "%.2f €", e.guztira));
    }

    @Override
    public int getItemCount() {
        return zerrenda.size();
    }

    static class EskaeraViewHolder extends RecyclerView.ViewHolder {
        final TextView itemEskaeraZenbakia;
        final TextView itemEskaeraData;
        final TextView itemEskaeraArtikuluak;
        final TextView itemEskaeraGuztira;

        EskaeraViewHolder(@NonNull View itemView) {
            super(itemView);
            itemEskaeraZenbakia = itemView.findViewById(R.id.itemEskaeraZenbakia);
            itemEskaeraData = itemView.findViewById(R.id.itemEskaeraData);
            itemEskaeraArtikuluak = itemView.findViewById(R.id.itemEskaeraArtikuluak);
            itemEskaeraGuztira = itemView.findViewById(R.id.itemEskaeraGuztira);
        }
    }

    /** Eskaera elementu bat: zenbakia, data, artikulu kopurua, guztira. */
    static class EskaeraElementua {
        final String zenbakia;
        final String data;
        final int artikuluKopurua;
        final double guztira;

        EskaeraElementua(String zenbakia, String data, int artikuluKopurua, double guztira) {
            this.zenbakia = zenbakia;
            this.data = data;
            this.artikuluKopurua = artikuluKopurua;
            this.guztira = guztira;
        }
    }
}

