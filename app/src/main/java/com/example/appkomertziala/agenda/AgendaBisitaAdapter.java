package com.example.appkomertziala;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView-rako adapter: agenda bisiten zerrenda dotore eta garbi batean erakusten du.
 * Elementu bakoitzak Ikusi, Editatu eta Ezabatu botoiak eskaintzen ditu.
 */
public class AgendaBisitaAdapter extends RecyclerView.Adapter<AgendaBisitaAdapter.BisitaViewHolder> {

    private List<AgendaElementua> zerrenda = new ArrayList<>();
    private OnBisitaEkintzaListener entzulea;

    /** Bisita baten datuak eta bazkidearen izena (erakusteko). */
    public static class AgendaElementua {
        public long id;
        public String bisitaData;
        public String bazkideaIzena;
        public String deskribapena;
        public String egoera;

        public AgendaElementua(long id, String bisitaData, String bazkideaIzena, String deskribapena, String egoera) {
            this.id = id;
            this.bisitaData = bisitaData != null ? bisitaData : "";
            this.bazkideaIzena = bazkideaIzena != null ? bazkideaIzena : "";
            this.deskribapena = deskribapena != null ? deskribapena : "";
            this.egoera = egoera != null ? egoera : "";
        }
    }

    /** Ikusi, Editatu edo Ezabatu sakatzean deitzen den interfazea. */
    public interface OnBisitaEkintzaListener {
        void onIkusi(AgendaElementua elementua);
        void onEditatu(AgendaElementua elementua);
        void onEzabatu(AgendaElementua elementua);
    }

    public AgendaBisitaAdapter(OnBisitaEkintzaListener entzulea) {
        this.entzulea = entzulea;
    }

    /** Zerrenda eguneratu eta notifyDataSetChanged deitu. */
    public void eguneratuZerrenda(List<AgendaElementua> berria) {
        this.zerrenda = berria != null ? berria : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BisitaViewHolder onCreateViewHolder(@NonNull ViewGroup gurasoa, int mota) {
        View erroa = LayoutInflater.from(gurasoa.getContext()).inflate(R.layout.item_agenda_bisita, gurasoa, false);
        return new BisitaViewHolder(erroa);
    }

    @Override
    public void onBindViewHolder(@NonNull BisitaViewHolder holder, int posizioa) {
        AgendaElementua e = zerrenda.get(posizioa);
        holder.itemAgendaData.setText(e.bisitaData != null ? e.bisitaData : "");
        holder.itemAgendaDeskribapena.setText(e.deskribapena != null ? e.deskribapena : "");
        holder.itemAgendaEgoera.setText(e.egoera != null ? e.egoera : "");
        holder.btnIkusi.setOnClickListener(v -> {
            if (entzulea != null) entzulea.onIkusi(e);
        });
        holder.btnEditatu.setOnClickListener(v -> {
            if (entzulea != null) entzulea.onEditatu(e);
        });
        holder.btnEzabatu.setOnClickListener(v -> {
            if (entzulea != null) entzulea.onEzabatu(e);
        });
    }

    @Override
    public int getItemCount() {
        return zerrenda.size();
    }

    static class BisitaViewHolder extends RecyclerView.ViewHolder {
        TextView itemAgendaData;
        TextView itemAgendaDeskribapena;
        TextView itemAgendaEgoera;
        MaterialButton btnIkusi;
        MaterialButton btnEditatu;
        MaterialButton btnEzabatu;

        BisitaViewHolder(View erroa) {
            super(erroa);
            itemAgendaData = erroa.findViewById(R.id.itemAgendaData);
            itemAgendaDeskribapena = erroa.findViewById(R.id.itemAgendaDeskribapena);
            itemAgendaEgoera = erroa.findViewById(R.id.itemAgendaEgoera);
            btnIkusi = erroa.findViewById(R.id.btnAgendaItemIkusi);
            btnEditatu = erroa.findViewById(R.id.btnAgendaItemEditatu);
            btnEzabatu = erroa.findViewById(R.id.btnAgendaItemEzabatu);
        }
    }
}
