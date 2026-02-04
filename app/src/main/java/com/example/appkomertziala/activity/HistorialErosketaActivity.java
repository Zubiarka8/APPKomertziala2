package com.example.appkomertziala.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appkomertziala.adapter.HistorialErosketaAdapter;
import com.example.appkomertziala.R;
import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.HistorialCompra;

import java.util.ArrayList;
import java.util.List;

/**
 * Erosketen historiala pantaila: erosketa historial guztiak erakusten ditu.
 * 
 * Hau hemen erosketa historial guztiak erakusten dira: produktua, kantitatea,
 * prezio unitarioa eta prezio totala. HistorialCompra entitateak datu-basean
 * gordetzen dira bidalketa XML-etik inportatutako datuekin.
 * 
 * Produktua, kantitatea, prezio unitarioa eta prezio totala erakusten ditu.
 */
public class HistorialErosketaActivity extends AppCompatActivity {

    private static final String ETIKETA = "HistorialCompraActivity";

    /** RecyclerView historial erakusteko. */
    private RecyclerView recyclerHistorial;
    
    /** Testu erakusteko historial hutsik badago. */
    private TextView tvHistorialHutsa;
    
    /** Adapter historial zerrenda erakusteko. */
    private HistorialErosketaAdapter adapter;
    
    /** Datu-basea (Room). */
    private AppDatabase datuBasea;

    /**
     * Activity sortzean: UI elementuak kargatu, adapter konfiguratu,
     * eta historiala kargatu (hilo nagusitik kanpo).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_compra);

        // Izenburua jarri
        setTitle(getString(R.string.erosketa_historiala));
        
        // ActionBar-en atzera botoia - hau hemen badago, dena ondo doa
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Datu-basea eta UI elementuak kargatu
        datuBasea = AppDatabase.getInstance(this);
        recyclerHistorial = findViewById(R.id.recyclerHistorial);
        tvHistorialHutsa = findViewById(R.id.tvHistorialHutsa);

        // Adapter sortu eta konfiguratu - RecyclerView-ri lotu
        adapter = new HistorialErosketaAdapter(this);
        recyclerHistorial.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistorial.setAdapter(adapter);

        // Historiala kargatu - hilo nagusitik kanpo
        kargatuHistoriala();
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
     * Erosketen historiala kargatu: datu-baseatik historial guztiak lortu,
     * produktu izena, kantitatea, prezio unitarioa eta prezio totala kalkulatu.
     * 
     * HistorialCompra entitateak bidalketa XML-etik inportatutako datuekin
     * gordetzen dira. Produktu izena productoIzena erabiltzen du, hutsik badago
     * productoId erabiltzen du.
     */
    private void kargatuHistoriala() {
        new Thread(() -> {
            try {
                // Historial guztiak datu-baseatik lortu
                List<HistorialCompra> historialak = datuBasea.historialCompraDao().guztiak();
                if (historialak == null) historialak = new ArrayList<>();

                // Historial bakoitzarentzat elementua sortu
                List<HistorialErosketaAdapter.HistorialElementua> erakusteko = new ArrayList<>();
                for (HistorialCompra historial : historialak) {
                    // Produktuaren izena: productoIzena erabili, hutsik badago productoId
                    // Begiratu hemen ea produktu izena badaukagun
                    String produktua = historial.getProductoIzena();
                    if (produktua == null || produktua.trim().isEmpty()) {
                        produktua = historial.getProductoId();
                    }
                    if (produktua == null) {
                        produktua = "";
                    }
                    
                    // Kantitatea: eskatuta erabili
                    int kantitatea = historial.getEskatuta();
                    
                    // Prezio unitarioa
                    double prezioUnit = historial.getPrezioUnit();
                    
                    // Prezio totala: kantitatea * prezio unitarioa
                    double prezioTotala = kantitatea * prezioUnit;
                    
                    // Elementua sortu eta zerrendara gehitu
                    erakusteko.add(new HistorialErosketaAdapter.HistorialElementua(
                        produktua,
                        kantitatea,
                        prezioUnit,
                        prezioTotala
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
                        tvHistorialHutsa.setVisibility(View.VISIBLE);
                        tvHistorialHutsa.setText(getString(R.string.historial_zerrenda_hutsa));
                    } else {
                        tvHistorialHutsa.setVisibility(View.GONE);
                    }
                    recyclerHistorial.setVisibility(hutsa ? View.GONE : View.VISIBLE);
                });
            } catch (Exception e) {
                // Errorea log-ean erregistratu eta erabiltzaileari erakutsi
                Log.e(ETIKETA, "Errorea historiala kargatzean", e);
                runOnUiThread(() -> {
                    if (!isDestroyed()) {
                        Toast.makeText(this, getString(R.string.errorea_historiala_kargatzean), Toast.LENGTH_LONG).show();
                        tvHistorialHutsa.setVisibility(View.VISIBLE);
                        recyclerHistorial.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }
}

