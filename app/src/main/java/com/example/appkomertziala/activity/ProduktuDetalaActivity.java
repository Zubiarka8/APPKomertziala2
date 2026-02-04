package com.example.appkomertziala;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Katalogoa;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

/**
 * Produktu baten informazioa erakusten duen orria.
 * Intent extra: EXTRA_ARTIKULU_KODEA. Erosi sakatzean RESULT_OK eta artikuluKodea bidaltzen du.
 */
public class ProduktuDetalaActivity extends AppCompatActivity {

    public static final String EXTRA_ARTIKULU_KODEA = "artikulu_kodea";
    /** Erosi sakatzean bidalitako erantzun extra (artikulu_kodea). */
    public static final String EXTRA_EROSI_ARTIKULU_KODEA = "erosi_artikulu_kodea";

    private String artikuluKodea;
    private Katalogoa produktua;
    private ImageView irudia;
    private TextView izena, kodea, prezioa, stock;
    private MaterialButton btnErosi;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_produktu_detala);

        artikuluKodea = getIntent() != null ? getIntent().getStringExtra(EXTRA_ARTIKULU_KODEA) : null;
        if (artikuluKodea == null || artikuluKodea.trim().isEmpty()) {
            Toast.makeText(this, R.string.errore_ezezaguna, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        irudia = findViewById(R.id.produktuDetalaIrudia);
        izena = findViewById(R.id.produktuDetalaIzena);
        kodea = findViewById(R.id.produktuDetalaKodea);
        prezioa = findViewById(R.id.produktuDetalaPrezioa);
        stock = findViewById(R.id.produktuDetalaStock);
        btnErosi = findViewById(R.id.btnProduktuDetalaErosi);

        btnErosi.setOnClickListener(v -> erosiSaskira());

        new Thread(() -> {
            produktua = AppDatabase.getInstance(this).katalogoaDao().artikuluaBilatu(artikuluKodea);
            runOnUiThread(() -> {
                if (produktua == null) {
                    Toast.makeText(this, R.string.errore_ezezaguna, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                beteEdukia();
            });
        }).start();
    }

    private void beteEdukia() {
        if (produktua == null) return;
        setTitle(produktua.getIzena() != null ? produktua.getIzena() : getString(R.string.produktu_detala_izenburua));

        int irudiId = irudiIdAurkitu(produktua.getIrudiaIzena());
        irudia.setImageResource(irudiId);
        irudia.setContentDescription(getString(R.string.cd_katalogoa_irudia));

        izena.setText(produktua.getIzena() != null ? produktua.getIzena() : "");
        kodea.setText(getString(R.string.katalogoa_artikulu_kodea_etiketa, produktua.getArtikuluKodea() != null ? produktua.getArtikuluKodea() : ""));
        prezioa.setText(formatuaPrezioa(produktua.getSalmentaPrezioa()));
        stock.setText(getString(R.string.katalogoa_stock_etiketa, produktua.getStock()));

        if (produktua.getStock() <= 0) {
            btnErosi.setEnabled(false);
        }
    }

    private void erosiSaskira() {
        if (produktua == null) return;
        if (produktua.getStock() <= 0) {
            Toast.makeText(this, R.string.saskia_stock_0, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent data = new Intent();
        data.putExtra(EXTRA_EROSI_ARTIKULU_KODEA, produktua.getArtikuluKodea());
        setResult(RESULT_OK, data);
        finish();
    }

    private int irudiIdAurkitu(String irudiaIzena) {
        if (irudiaIzena == null || irudiaIzena.trim().isEmpty()) {
            return R.drawable.ic_logo_generico;
        }
        String izena = irudiaIzena.trim();
        int puntua = izena.lastIndexOf('.');
        if (puntua > 0) izena = izena.substring(0, puntua);
        izena = izena.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase(Locale.ROOT);
        Context ctx = getApplicationContext();
        int id = ctx.getResources().getIdentifier(izena, "drawable", ctx.getPackageName());
        return id != 0 ? id : R.drawable.ic_logo_generico;
    }

    private static String formatuaPrezioa(double p) {
        return String.format(Locale.getDefault(), "%.2f €", p);
    }

    /** Produktu detala irekitzeko Intent sortu. */
    public static Intent intentProduktuDetala(Context context, String artikuluKodea) {
        Intent i = new Intent(context, ProduktuDetalaActivity.class);
        i.putExtra(EXTRA_ARTIKULU_KODEA, artikuluKodea);
        return i;
    }
}
