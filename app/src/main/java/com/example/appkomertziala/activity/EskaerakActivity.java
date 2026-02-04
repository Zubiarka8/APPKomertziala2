package com.example.appkomertziala;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.EskaeraGoiburua;
import com.example.appkomertziala.db.eredua.EskaeraXehetasuna;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Eskaerak pantaila: uneko komertzialaren eskaera guztiak erakusten ditu.
 * SEGURTASUNA: SessionManager erabiliz bakarrik uneko komertzialaren eskaerak erakusten dira.
 */
public class EskaerakActivity extends AppCompatActivity {

    private RecyclerView recyclerEskaerak;
    private TextView tvEskaerakHutsa;
    private EskaerakAdapter adapter;
    private AppDatabase datuBasea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eskaerak);

        setTitle(getString(R.string.eskaerak_izenburua));
        
        // ActionBar-en atzera botoia
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        datuBasea = AppDatabase.getInstance(this);
        recyclerEskaerak = findViewById(R.id.recyclerEskaerak);
        tvEskaerakHutsa = findViewById(R.id.tvEskaerakHutsa);

        adapter = new EskaerakAdapter(this);
        recyclerEskaerak.setLayoutManager(new LinearLayoutManager(this));
        recyclerEskaerak.setAdapter(adapter);

        kargatuEskaerak();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /** Eskaerak kargatu: SEGURTASUNA - bakarrik uneko komertzialarenak. */
    private void kargatuEskaerak() {
        new Thread(() -> {
            try {
                // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                    new com.example.appkomertziala.segurtasuna.SessionManager(this);
                String komertzialKodea = sessionManager.getKomertzialKodea();
                
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
                
                // SEGURTASUNA: bakarrik uneko komertzialaren eskaerak
                List<EskaeraGoiburua> goiburuak = datuBasea.eskaeraGoiburuaDao().komertzialarenEskaerak(komertzialKodea.trim());
                if (goiburuak == null) goiburuak = new ArrayList<>();

                List<EskaerakAdapter.EskaeraElementua> erakusteko = new ArrayList<>();
                for (EskaeraGoiburua goi : goiburuak) {
                    // Xehetasunak kargatu
                    List<EskaeraXehetasuna> xehetasunak = datuBasea.eskaeraXehetasunaDao().eskaerarenXehetasunak(goi.getZenbakia());
                    if (xehetasunak == null) xehetasunak = new ArrayList<>();
                    
                    // Guztira kalkulatu
                    double guztira = 0.0;
                    int artikuluKopurua = 0;
                    for (EskaeraXehetasuna x : xehetasunak) {
                        guztira += x.getPrezioa() * x.getKantitatea();
                        artikuluKopurua += x.getKantitatea();
                    }
                    
                    erakusteko.add(new EskaerakAdapter.EskaeraElementua(
                        goi.getZenbakia(),
                        goi.getData(),
                        artikuluKopurua,
                        guztira
                    ));
                }

                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    adapter.eguneratuZerrenda(erakusteko);
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
                android.util.Log.e("EskaerakActivity", "Errorea eskaerak kargatzean", e);
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

