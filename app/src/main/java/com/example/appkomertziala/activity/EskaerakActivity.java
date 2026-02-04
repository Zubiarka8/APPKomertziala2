package com.example.appkomertziala.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appkomertziala.adapter.EskaerakAdapter;
import com.example.appkomertziala.R;
import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.example.appkomertziala.db.eredua.EskaeraGoiburua;
import com.example.appkomertziala.db.eredua.EskaeraXehetasuna;
import com.example.appkomertziala.db.eredua.Katalogoa;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Eskaerak pantaila: uneko komertzialaren eskaera guztiak erakusten ditu.
 * 
 * Hau hemen komertzial bakoitzak bere eskaerak bakarrik ikus ditzake - segurtasuna
 * bermatzeko SessionManager erabiliz kodea lortzen du eta bakarrik bere eskaerak
 * erakusten ditu. Eskaera bakoitzak zenbakia, data, artikulu kopurua eta guztira
 * erakusten ditu.
 * 
 * SEGURTASUNA: SessionManager erabiliz bakarrik uneko komertzialaren eskaerak erakusten dira.
 */
public class EskaerakActivity extends AppCompatActivity {

    private static final String ETIKETA = "EskaerakActivity";

    /** RecyclerView eskaerak erakusteko. */
    private RecyclerView recyclerEskaerak;
    
    /** Testu erakusteko eskaerak hutsik badira. */
    private TextView tvEskaerakHutsa;
    
    /** Adapter eskaerak zerrenda erakusteko. */
    private EskaerakAdapter adapter;
    
    /** Datu-basea (Room). */
    private AppDatabase datuBasea;

    /**
     * Activity sortzean: UI elementuak kargatu, adapter konfiguratu,
     * eta eskaerak kargatu (hilo nagusitik kanpo).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eskaerak);

        // Izenburua jarri
        setTitle(getString(R.string.eskaerak_izenburua));
        
        // ActionBar-en atzera botoia - hau hemen badago, dena ondo doa
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Datu-basea eta UI elementuak kargatu
        datuBasea = AppDatabase.getInstance(this);
        recyclerEskaerak = findViewById(R.id.recyclerEskaerak);
        tvEskaerakHutsa = findViewById(R.id.tvEskaerakHutsa);

        // Adapter sortu eta konfiguratu - RecyclerView-ri lotu
        adapter = new EskaerakAdapter(this);
        adapter.setOnXehetasunakClickListener(this::erakutsiEskaeraXehetasunak);
        recyclerEskaerak.setLayoutManager(new LinearLayoutManager(this));
        recyclerEskaerak.setAdapter(adapter);

        // Eskaerak kargatu - hilo nagusitik kanpo
        kargatuEskaerak();
    }

    @Override
    protected void onResume() {
        super.onResume();
        kargatuEskaerak();
    }

    /**
     * Atzera botoia sakatzean: Activity itxi.
     * 
     * @return true (ekintza kudeatua)
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Eskaerak kargatu: SEGURTASUNA - bakarrik uneko komertzialarenak.
     * 
     * SessionManager erabiliz uneko komertzialaren kodea lortzen du,
     * eta bakarrik bere eskaerak erakusten ditu. Eskaera bakoitzaren
     * xehetasunak kargatu eta guztira kalkulatzen du (prezioa * kantitatea).
     */
    private void kargatuEskaerak() {
        new Thread(() -> {
            try {
                // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                // Begiratu hemen ea kodea badaukagun - saioa hasita dagoen egiaztatu
                com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                    new com.example.appkomertziala.segurtasuna.SessionManager(this);
                String komertzialKodea = sessionManager.getKomertzialKodea();
                
                // Kodea hutsik badago, saioa ez dago hasita - errorea erakutsi
                if (komertzialKodea == null || komertzialKodea.isEmpty()) {
                    runOnUiThread(() -> {
                        if (!isDestroyed()) {
                            tvEskaerakHutsa.setVisibility(View.VISIBLE);
                            tvEskaerakHutsa.setText(getString(R.string.saioa_ez_dago_hasita));
                            recyclerEskaerak.setVisibility(View.GONE);
                        }
                    });
                    return;
                }
                
                // SEGURTASUNA: bakarrik uneko komertzialaren eskaerak - kodea erabiliz
                List<EskaeraGoiburua> goiburuak = datuBasea.eskaeraGoiburuaDao().komertzialarenEskaerak(komertzialKodea.trim());
                if (goiburuak == null) goiburuak = new ArrayList<>();

                List<EskaerakAdapter.EskaeraElementua> erakusteko = new ArrayList<>();
                for (EskaeraGoiburua goi : goiburuak) {
                    List<EskaeraXehetasuna> xehetasunak = datuBasea.eskaeraXehetasunaDao().eskaerarenXehetasunak(goi.getZenbakia());
                    if (xehetasunak == null) xehetasunak = new ArrayList<>();
                    
                    double guztira = 0.0;
                    int artikuluKopurua = 0;
                    List<EskaerakAdapter.ProduktuXehetasuna> produktuXehetasunak = new ArrayList<>();
                    
                    for (EskaeraXehetasuna x : xehetasunak) {
                        guztira += x.getPrezioa() * x.getKantitatea();
                        artikuluKopurua += x.getKantitatea();
                        String produktuIzena = null;
                        Katalogoa k = datuBasea.katalogoaDao().artikuluaBilatu(x.getArtikuluKodea());
                        if (k != null && k.getIzena() != null) {
                            produktuIzena = k.getIzena().trim();
                        }
                        if (produktuIzena == null || produktuIzena.isEmpty()) {
                            produktuIzena = x.getArtikuluKodea() != null ? x.getArtikuluKodea() : "";
                        }
                        produktuXehetasunak.add(new EskaerakAdapter.ProduktuXehetasuna(produktuIzena, x.getKantitatea(), x.getPrezioa()));
                    }
                    
                    String bazkideIzena = "—";
                    if (goi.getBazkideaId() != null) {
                        Bazkidea b = datuBasea.bazkideaDao().idzBilatu(goi.getBazkideaId());
                        if (b != null) {
                            String izena = (b.getIzena() != null ? b.getIzena().trim() : "") + 
                                (b.getAbizena() != null && !b.getAbizena().trim().isEmpty() ? " " + b.getAbizena().trim() : "");
                            bazkideIzena = izena.isEmpty() ? (b.getNan() != null ? b.getNan() : "—") : izena;
                        }
                    } else if (goi.getBazkideaKodea() != null && !goi.getBazkideaKodea().trim().isEmpty()) {
                        bazkideIzena = goi.getBazkideaKodea().trim();
                    }
                    
                    erakusteko.add(new EskaerakAdapter.EskaeraElementua(
                        goi.getZenbakia(),
                        goi.getData(),
                        bazkideIzena,
                        artikuluKopurua,
                        guztira,
                        produktuXehetasunak
                    ));
                }

                // UI eguneratu - hilo nagusian
                runOnUiThread(() -> {
                    // Activity destruitu bada, ezer ez egin
                    if (isDestroyed()) return;
                    
                    // Adapter eguneratu
                    adapter.eguneratuZerrenda(erakusteko);
                    
                    // Hutsik badago, mezua erakutsi; bestela zerrenda
                    boolean hutsa = erakusteko.isEmpty();
                    if (hutsa) {
                        tvEskaerakHutsa.setVisibility(View.VISIBLE);
                        tvEskaerakHutsa.setText(getString(R.string.eskaerak_zerrenda_hutsa));
                    } else {
                        tvEskaerakHutsa.setVisibility(View.GONE);
                    }
                    recyclerEskaerak.setVisibility(hutsa ? View.GONE : View.VISIBLE);
                });
            } catch (Exception e) {
                // Errorea log-ean erregistratu eta erabiltzaileari erakutsi
                Log.e(ETIKETA, "Errorea eskaerak kargatzean", e);
                runOnUiThread(() -> {
                    if (!isDestroyed()) {
                        Toast.makeText(this, getString(R.string.esportatu_errorea_batzuetan), Toast.LENGTH_LONG).show();
                        tvEskaerakHutsa.setVisibility(View.VISIBLE);
                        recyclerEskaerak.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    /**
     * Eskaeraren xehetasunak erakutsi dialog batean: produktu zerrenda, kantitatea eta prezioa.
     */
    private void erakutsiEskaeraXehetasunak(EskaerakAdapter.EskaeraElementua e) {
        StringBuilder msg = new StringBuilder();
        msg.append(getString(R.string.eskaera_zenbakia)).append(": ").append(e.zenbakia != null ? e.zenbakia : "—").append("\n");
        msg.append(getString(R.string.eskaera_data)).append(": ").append(e.data != null ? e.data : "—").append("\n");
        msg.append(getString(R.string.eskaera_bazkidea)).append(": ").append(e.bazkideIzena != null ? e.bazkideIzena : "—").append("\n\n");
        msg.append(getString(R.string.eskaera_artikuluak)).append(":\n");
        
        for (EskaerakAdapter.ProduktuXehetasuna p : e.produktuXehetasunak) {
            String lerroa = getString(R.string.eskaera_produktua_kantitatea, 
                p.produktuIzena, p.kantitatea, String.format(Locale.getDefault(), "%.2f €", p.guztira));
            msg.append("  • ").append(lerroa).append("\n");
        }
        
        msg.append("\n").append(getString(R.string.eskaera_guztira)).append(": ")
           .append(String.format(Locale.getDefault(), "%.2f €", e.guztira));
        
        new AlertDialog.Builder(this)
                .setTitle(R.string.eskaera_xehetasunak_izenburua)
                .setMessage(msg.toString())
                .setPositiveButton(R.string.ados, null)
                .show();
    }
}

