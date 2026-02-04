package com.example.appkomertziala.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appkomertziala.R;
import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Katalogoa;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

/**
 * Produktu baten informazioa erakusten duen pantaila.
 * 
 * Hau hemen produktu baten xehetasun guztiak erakusten ditu: izena, kodea, prezioa, stock-a,
 * eta irudia. Erosi botoia sakatzean, produktua saskira gehitzen da eta MainActivity-ra
 * itzultzen da artikulu kodea bidaliz.
 * 
 * Intent extra: EXTRA_ARTIKULU_KODEA (produktu kodea).
 * Erosi sakatzean: RESULT_OK eta artikuluKodea bidaltzen du EXTRA_EROSI_ARTIKULU_KODEA bezala.
 */
public class ProduktuDetalaActivity extends AppCompatActivity {

    private static final String ETIKETA = "ProduktuDetala";

    /** Intent extra: produktu kodea (sarrera). */
    public static final String EXTRA_ARTIKULU_KODEA = "artikulu_kodea";
    
    /** Intent extra: produktu kodea (irteera, erosi sakatzean). */
    public static final String EXTRA_EROSI_ARTIKULU_KODEA = "erosi_artikulu_kodea";

    /** Produktu kodea (Intent-etik dator). */
    private String artikuluKodea;
    
    /** Produktu objektua (datu-baseatik kargatua). */
    private Katalogoa produktua;
    
    /** UI elementuak: irudia, izena, kodea, prezioa, stock eta erosi botoia. */
    private ImageView irudia;
    private TextView izena, kodea, prezioa, stock;
    private MaterialButton btnErosi;

    /**
     * Activity sortzean: Intent-etik kodea atera, UI elementuak kargatu,
     * eta produktua datu-baseatik bilatu (hilo nagusitik kanpo).
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_produktu_detala);

        // Intent-etik kodea atera - hau hemen badago, dena ondo doa
        artikuluKodea = getIntent() != null ? getIntent().getStringExtra(EXTRA_ARTIKULU_KODEA) : null;
        if (artikuluKodea == null || artikuluKodea.trim().isEmpty()) {
            // Kodea hutsik badago, ezin dugu produktua erakutsi - itxi
            Toast.makeText(this, R.string.errore_ezezaguna, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // UI elementuak kargatu - findViewById guztiak hemen
        irudia = findViewById(R.id.produktuDetalaIrudia);
        izena = findViewById(R.id.produktuDetalaIzena);
        kodea = findViewById(R.id.produktuDetalaKodea);
        prezioa = findViewById(R.id.produktuDetalaPrezioa);
        stock = findViewById(R.id.produktuDetalaStock);
        btnErosi = findViewById(R.id.btnProduktuDetalaErosi);

        // Erosi botoiaren listener-a - sakatzean saskira gehitzen du
        btnErosi.setOnClickListener(v -> erosiSaskira());

        // Produktua datu-baseatik kargatu (hilo nagusitik kanpo - Room-en muga)
        new Thread(() -> {
            try {
                produktua = AppDatabase.getInstance(this).katalogoaDao().artikuluaBilatu(artikuluKodea);
                runOnUiThread(() -> {
                    // Produktua null bada, ezin dugu erakutsi - itxi
                    if (produktua == null) {
                        Log.w(ETIKETA, "Produktua ez da aurkitu kodea: " + artikuluKodea);
                        Toast.makeText(this, R.string.errore_ezezaguna, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    // Produktua aurkitu da - pantaila bete
                    beteEdukia();
                });
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea produktua kargatzean", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.errore_ezezaguna, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    /**
     * Pantaila bete: produktuaren datu guztiak erakutsi (izena, kodea, prezioa, stock, irudia).
     * Stock 0 bada edo gutxiago, erosi botoia desgaitzen da.
     */
    private void beteEdukia() {
        // Segurtasuna: produktua null bada, ezer ez egin
        if (produktua == null) return;
        
        // Izenburua produktuaren izenarekin
        setTitle(produktua.getIzena() != null ? produktua.getIzena() : getString(R.string.produktu_detala_izenburua));

        // Irudia kargatu - drawable ID bilatu izenaren arabera
        int irudiId = irudiIdAurkitu(produktua.getIrudiaIzena());
        irudia.setImageResource(irudiId);
        irudia.setContentDescription(getString(R.string.cd_katalogoa_irudia));

        // Datuak erakutsi: izena, kodea, prezioa, stock
        izena.setText(produktua.getIzena() != null ? produktua.getIzena() : "");
        kodea.setText(getString(R.string.katalogoa_artikulu_kodea_etiketa, produktua.getArtikuluKodea() != null ? produktua.getArtikuluKodea() : ""));
        prezioa.setText(formatuaPrezioa(produktua.getSalmentaPrezioa()));
        stock.setText(getString(R.string.katalogoa_stock_etiketa, produktua.getStock()));

        // Stock 0 bada edo gutxiago, erosi botoia desgaitzen da
        if (produktua.getStock() <= 0) {
            btnErosi.setEnabled(false);
        }
    }

    /**
     * Erosi botoia sakatzean: produktua saskira gehitzen du.
     * Stock balidatu eta gero Intent bidez kodea bidaltzen du MainActivity-ra.
     */
    private void erosiSaskira() {
        // Segurtasuna: produktua null bada, ezer ez egin
        if (produktua == null) return;
        
        // Stock balidatu - 0 bada edo gutxiago, ezin da erosi
        if (produktua.getStock() <= 0) {
            Toast.makeText(this, R.string.saskia_stock_0, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Intent sortu eta kodea bidali - MainActivity-k jasoko du
        Intent data = new Intent();
        data.putExtra(EXTRA_EROSI_ARTIKULU_KODEA, produktua.getArtikuluKodea());
        setResult(RESULT_OK, data);
        finish();
    }

    /**
     * Irudi ID bilatu izenaren arabera (drawable baliabideak).
     * 
     * Fitxategi izena hartu, luzapena kendu, karaktere bereziak garbitu,
     * eta drawable ID bilatu. Ez badago, generikoa erabili.
     * 
     * @param irudiaIzena Irudi fitxategiaren izena (adib. "macbook.jpg")
     * @return Drawable ID edo generikoa ez badago
     */
    private int irudiIdAurkitu(String irudiaIzena) {
        // Izena hutsik badago, generikoa erabili
        if (irudiaIzena == null || irudiaIzena.trim().isEmpty()) {
            return R.drawable.ic_logo_generico;
        }
        
        // Izena garbitu: trim, luzapena kendu, karaktere bereziak ordezkatu
        String izena = irudiaIzena.trim();
        int puntua = izena.lastIndexOf('.');
        if (puntua > 0) izena = izena.substring(0, puntua); // Luzapena kendu
        izena = izena.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase(Locale.ROOT); // Karaktere bereziak garbitu
        
        // Drawable ID bilatu
        Context ctx = getApplicationContext();
        int id = ctx.getResources().getIdentifier(izena, "drawable", ctx.getPackageName());
        
        // ID aurkitu bada, hori erabili; bestela generikoa
        return id != 0 ? id : R.drawable.ic_logo_generico;
    }

    /**
     * Prezioa formatu egokian erakutsi (adib. "25.50 €").
     * 
     * @param p Prezioa (double)
     * @return Formatutako string-a
     */
    private static String formatuaPrezioa(double p) {
        return String.format(Locale.getDefault(), "%.2f €", p);
    }

    /**
     * Produktu detala irekitzeko Intent sortu (helper metodoa).
     * 
     * Beste Activity batetik deitzeko erabilgarria - kodea Intent-ean jartzen du.
     * 
     * @param context Aplikazioaren context-a
     * @param artikuluKodea Produktu kodea
     * @return Intent produktu detala irekitzeko
     */
    public static Intent intentProduktuDetala(Context context, String artikuluKodea) {
        Intent i = new Intent(context, ProduktuDetalaActivity.class);
        i.putExtra(EXTRA_ARTIKULU_KODEA, artikuluKodea);
        return i;
    }
}
