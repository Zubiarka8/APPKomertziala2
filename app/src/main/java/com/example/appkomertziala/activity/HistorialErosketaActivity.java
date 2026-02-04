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

import com.example.appkomertziala.adapter.HistorialErosketaAdapter;
import com.example.appkomertziala.R;
import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.HistorialCompra;

import java.util.Locale;

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
        adapter.setOnAmaitutaAldaketaListener((historialId, amaituta) -> {
            // Amaituta egoera eguneratu datu-basean
            eguneratuAmaitutaEgoera(historialId, amaituta);
        });
        adapter.setOnIkusiHistorialListener((historialId) -> {
            // Historial xehetasunak erakutsi
            erakutsiHistorialXehetasunak(historialId);
        });
        recyclerHistorial.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistorial.setAdapter(adapter);

        // Historiala kargatu - hilo nagusitik kanpo
        kargatuHistoriala();
    }

    /**
     * Activity berriro erakustean: historiala berriro kargatu (erosketa berri bat egitean
     * beste pantailatik itzultzean datuak eguneratzeko).
     */
    @Override
    protected void onResume() {
        super.onResume();
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
                    // Datuak bildu
                    String kodea = historial.getKodea();
                    String helmuga = historial.getHelmuga();
                    String data = historial.getData();
                    
                    // Produktuaren izena: productoIzena erabili, hutsik badago productoId
                    String produktua = historial.getProductoIzena();
                    if (produktua == null || produktua.trim().isEmpty()) {
                        produktua = historial.getProductoId();
                    }
                    if (produktua == null) {
                        produktua = "";
                    }
                    
                    String productoId = historial.getProductoId();
                    int kantitatea = historial.getEskatuta();
                    int bidalita = historial.getBidalita();
                    double prezioUnit = historial.getPrezioUnit();
                    double prezioTotala = kantitatea * prezioUnit;
                    long historialId = historial.getId();
                    boolean amaituta = historial.isAmaituta();
                    
                    // Elementua sortu eta zerrendara gehitu
                    erakusteko.add(new HistorialErosketaAdapter.HistorialElementua(
                        kodea,
                        helmuga,
                        data,
                        produktua,
                        productoId,
                        kantitatea,
                        bidalita,
                        prezioUnit,
                        prezioTotala,
                        historialId,
                        amaituta
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

    /**
     * Amaituta egoera eguneratu datu-basean.
     * 
     * XML egituraren arabera, Amaituta bidalketa mailan dago (ez lerro mailan),
     * beraz bidalketa ID berdina duten lerro guztiak eguneratu behar dira.
     * 
     * @param historialId HistorialCompra-ren ID
     * @param amaituta Egoera berria (true = iritsi da, false = ez da iritsi)
     */
    private void eguneratuAmaitutaEgoera(long historialId, boolean amaituta) {
        new Thread(() -> {
            try {
                HistorialCompra historial = datuBasea.historialCompraDao().idzBilatu(historialId);
                if (historial != null) {
                    int bidalketaId = historial.getBidalketaId();
                    // Bidalketa ID berdina duten lerro guztiak eguneratu (XML egituraren arabera)
                    int eguneratutakoKopurua = datuBasea.historialCompraDao().eguneratuAmaitutaBidalketaIdz(bidalketaId, amaituta);
                    Log.d(ETIKETA, "eguneratuAmaitutaEgoera: " + eguneratutakoKopurua + " lerro eguneratuta bidalketaId=" + bidalketaId + ", amaituta=" + amaituta);
                    
                    // Historiala berriro kargatu UI eguneratzeko
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.historial_amaituta_eguneratu, Toast.LENGTH_SHORT).show();
                        // Historiala berriro kargatu bidalketa guztiko lerro guztietan aldaketa erakusteko
                        kargatuHistoriala();
                    });
                }
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea amaituta egoera eguneratzean", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.errorea_historiala_kargatzean), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Historial xehetasunak erakutsi dialog batean.
     * 
     * @param historialId HistorialCompra-ren ID
     */
    private void erakutsiHistorialXehetasunak(long historialId) {
        new Thread(() -> {
            try {
                HistorialCompra historial = datuBasea.historialCompraDao().idzBilatu(historialId);
                if (historial == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.errorea_historiala_kargatzean), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // Datuak bildu
                String kodea = historial.getKodea() != null ? historial.getKodea() : "—";
                String helmuga = historial.getHelmuga() != null ? historial.getHelmuga() : "—";
                String data = historial.getData() != null ? historial.getData() : "—";
                String produktua = historial.getProductoIzena();
                if (produktua == null || produktua.trim().isEmpty()) {
                    produktua = historial.getProductoId();
                }
                if (produktua == null || produktua.trim().isEmpty()) {
                    produktua = "—";
                }
                String productoId = historial.getProductoId() != null ? historial.getProductoId() : "—";
                int eskatuta = historial.getEskatuta();
                int bidalita = historial.getBidalita();
                double prezioUnit = historial.getPrezioUnit();
                double prezioTotala = eskatuta * prezioUnit;
                boolean amaituta = historial.isAmaituta();
                String amaitutaStr = amaituta ? getString(R.string.historial_amaituta_true) : getString(R.string.historial_amaituta_false);
                
                // Mezua sortu
                StringBuilder msg = new StringBuilder();
                msg.append(getString(R.string.historial_kodea)).append(": ").append(kodea).append("\n");
                msg.append(getString(R.string.historial_helmuga)).append(": ").append(helmuga).append("\n");
                msg.append(getString(R.string.historial_data)).append(": ").append(data).append("\n");
                msg.append(getString(R.string.historial_produktua)).append(": ").append(produktua).append("\n");
                msg.append(getString(R.string.historial_producto_id)).append(": ").append(productoId).append("\n");
                msg.append(getString(R.string.kantitatea)).append(": ").append(eskatuta).append("\n");
                msg.append(getString(R.string.historial_bidalita)).append(": ").append(bidalita).append("\n");
                msg.append(getString(R.string.prezio_unitarioa)).append(": ").append(String.format(Locale.getDefault(), "%.2f €", prezioUnit)).append("\n");
                msg.append(getString(R.string.prezio_totala)).append(": ").append(String.format(Locale.getDefault(), "%.2f €", prezioTotala)).append("\n");
                msg.append(getString(R.string.historial_amaituta)).append(": ").append(amaitutaStr);
                
                String msgStr = msg.toString();
                
                // Dialog erakutsi hilo nagusian
                runOnUiThread(() -> {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.historial_xehetasunak_izenburua)
                            .setMessage(msgStr)
                            .setPositiveButton(R.string.ados, null)
                            .show();
                });
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea historial xehetasunak erakustean", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.errorea_historiala_kargatzean), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}

