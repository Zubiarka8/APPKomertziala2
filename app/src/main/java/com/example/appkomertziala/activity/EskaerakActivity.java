package com.example.appkomertziala.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appkomertziala.adapter.EskaerakAdapter;
import com.example.appkomertziala.R;
import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.EskaeraGoiburua;
import com.example.appkomertziala.db.eredua.EskaeraXehetasuna;

import java.util.ArrayList;
import java.util.List;

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
        recyclerEskaerak.setLayoutManager(new LinearLayoutManager(this));
        recyclerEskaerak.setAdapter(adapter);

        // Eskaerak kargatu - hilo nagusitik kanpo
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

                // Eskaera bakoitzarentzat xehetasunak kargatu eta guztira kalkulatu
                List<EskaerakAdapter.EskaeraElementua> erakusteko = new ArrayList<>();
                for (EskaeraGoiburua goi : goiburuak) {
                    // Xehetasunak kargatu - eskaera zenbakiaren arabera
                    List<EskaeraXehetasuna> xehetasunak = datuBasea.eskaeraXehetasunaDao().eskaerarenXehetasunak(goi.getZenbakia());
                    if (xehetasunak == null) xehetasunak = new ArrayList<>();
                    
                    // Guztira kalkulatu - prezioa * kantitatea bakoitzarentzat
                    double guztira = 0.0;
                    int artikuluKopurua = 0;
                    for (EskaeraXehetasuna x : xehetasunak) {
                        guztira += x.getPrezioa() * x.getKantitatea();
                        artikuluKopurua += x.getKantitatea();
                    }
                    
                    // Elementua sortu eta zerrendara gehitu
                    erakusteko.add(new EskaerakAdapter.EskaeraElementua(
                        goi.getZenbakia(),
                        goi.getData(),
                        artikuluKopurua,
                        guztira
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
}

