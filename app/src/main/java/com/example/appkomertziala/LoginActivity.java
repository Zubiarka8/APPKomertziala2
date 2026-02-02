package com.example.appkomertziala;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Komertziala;
import com.example.appkomertziala.xml.XMLKudeatzailea;
import com.example.appkomertziala.xml.XmlBilatzailea;
import com.google.android.gms.maps.CameraUpdateFactory;

import java.util.List;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Techno Basque - Saioa hasi pantaila.
 * Goiko atala: logoa eta izenburua. Kargatu atala: XML kargatu eta komertzial hautaketa.
 * Formularioa: Erabiltzailea eta Pasahitza balidazioarekin.
 * Google Maps behean Donostian zentratua. Arrakastan MainActivity-ra nabigatzen du.
 */
public class LoginActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final double DONOSTIA_LAT = 43.3180;
    private static final double DONOSTIA_LNG = -1.9812;

    private TextInputLayout tilUser;
    private TextInputLayout tilPassword;
    private TextInputEditText etUser;
    private TextInputEditText etPassword;

    private final ActivityResultLauncher<String[]> inportatuGailutikLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri == null) return;
                new Thread(() -> {
                    try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
                        if (is == null) {
                            runOnUiThread(() -> Toast.makeText(this, R.string.inportatu_fitxategi_ezin_irakurri, Toast.LENGTH_LONG).show());
                            return;
                        }
                        String izena = fitxategiIzenaUri(uri);
                        if (izena == null || izena.isEmpty()) izena = "katalogoa.xml";
                        XMLKudeatzailea kud = new XMLKudeatzailea(this);
                        kud.inportatuSarreraFluxutik(is, izena);
                        runOnUiThread(() -> Toast.makeText(this, R.string.inportatu_ondo, Toast.LENGTH_SHORT).show());
                    } catch (Exception e) {
                        String mezu = e.getMessage() != null ? e.getMessage() : "";
                        runOnUiThread(() -> Toast.makeText(this, getString(R.string.inportatu_errorea, mezu != null && !mezu.isEmpty() ? mezu : getString(R.string.errore_ezezaguna)), Toast.LENGTH_LONG).show());
                    }
                }).start();
            });

    private String fitxategiIzenaUri(Uri uri) {
        String izena = null;
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) izena = c.getString(0);
        }
        if (izena == null || izena.isEmpty()) {
            String path = uri.getLastPathSegment();
            if (path != null && path.contains("/")) izena = path.substring(path.lastIndexOf('/') + 1);
            else izena = path;
        }
        return izena;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tilUser = findViewById(R.id.tilUser);
        tilPassword = findViewById(R.id.tilPassword);
        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);

        MaterialButton btnLoadXml = findViewById(R.id.btnLoadXml);
        MaterialButton btnSelectCommercial = findViewById(R.id.btnSelectCommercial);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        MaterialButton btnSartuKomertzialGisa = findViewById(R.id.btnSartuKomertzialGisa);

        btnLoadXml.setOnClickListener(v -> erakutsiXmlHautaketaDialogoa());
        MaterialButton btnInportatuGailutik = findViewById(R.id.btnInportatuGailutik);
        btnInportatuGailutik.setOnClickListener(v -> inportatuGailutikLauncher.launch(new String[]{"application/xml", "text/xml", "*/*"}));
        btnSelectCommercial.setOnClickListener(v -> erakutsiKomertzialHautaketaDialogoa());
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnSartuKomertzialGisa.setOnClickListener(v -> sartuKomertzialGisa());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragmentLogin);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        LatLng donostia = new LatLng(DONOSTIA_LAT, DONOSTIA_LNG);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(donostia, 14f));
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        // Markatzailea (marker) Gipuzkoa egoitzan
        googleMap.addMarker(new MarkerOptions()
                .position(donostia)
                .title(getString(R.string.contact_title)));
    }

    /** Assets-eko XML fitxategien zerrenda erakusten du; hautatutakoa (edo guztiak) kargatzen du. */
    private void erakutsiXmlHautaketaDialogoa() {
        String[] xmlak;
        try {
            XMLKudeatzailea kud = new XMLKudeatzailea(this);
            xmlak = kud.assetsXmlFitxategiak();
        } catch (Exception e) {
            String mezu = e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : getString(R.string.errore_ezezaguna);
            Toast.makeText(this, getString(R.string.xml_errorea, mezu), Toast.LENGTH_LONG).show();
            return;
        }
        // Lehen aukera: «Guztiak inportatu»; gero assets-eko fitxategi bakoitza
        String[] aukerak = new String[1 + xmlak.length];
        aukerak[0] = getString(R.string.xml_guztiak_kargatu);
        for (int i = 0; i < xmlak.length; i++) {
            aukerak[1 + i] = xmlak[i];
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.xml_hautatu_titulua)
                .setItems(aukerak, (dialog, which) -> {
                    if (which == 0) {
                        kargatuXmlGuztiak();
                    } else {
                        kargatuXmlFitxategia(aukerak[which]);
                    }
                })
                .setNegativeButton(R.string.xml_utzi, null)
                .show();
    }

    /** Hautatutako XML fitxategi bakarra kargatzen du (barne-memoriatik edo assets-etik). Hila nagusitik kanpo. */
    private void kargatuXmlFitxategia(String fitxategiIzena) {
        new Thread(() -> {
            try {
                XMLKudeatzailea kud = new XMLKudeatzailea(this);
                kud.inportatuFitxategia(fitxategiIzena);
                runOnUiThread(() -> Toast.makeText(this, R.string.datuak_ondo_eguneratu, Toast.LENGTH_LONG).show());
            } catch (IllegalArgumentException e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.xml_ezin_inportatu, Toast.LENGTH_LONG).show());
            } catch (java.io.IOException e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.errorea_fitxategia_irakurtzean, Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                String mezu = e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : getString(R.string.errore_ezezaguna);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.xml_errorea, mezu), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /** XML guztiak ordena egokian kargatzen du (komertzialak, partnerrak, bazkideak, loginak, katalogoa). Hila nagusitik kanpo. */
    private void kargatuXmlGuztiak() {
        new Thread(() -> {
            try {
                XMLKudeatzailea kud = new XMLKudeatzailea(this);
                kud.guztiakInportatu();
                runOnUiThread(() -> Toast.makeText(this, R.string.datuak_ondo_eguneratu, Toast.LENGTH_LONG).show());
            } catch (java.io.IOException e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.errorea_fitxategia_irakurtzean, Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                String mezu = e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : getString(R.string.errore_ezezaguna);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.xml_errorea, mezu), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /** Komertzialen zerrenda datu-basean bilatu eta hautaketa-dialogoa erakusten du. Hutsik badago, XML guztiak kargatzen ditu (datu-basea betetzeko) eta gero zerrenda osoa erakusten du. */
    private void sartuKomertzialGisa() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<Komertziala> zerrenda = db.komertzialaDao().guztiak();
            if (zerrenda.isEmpty()) {
                try {
                    XMLKudeatzailea kud = new XMLKudeatzailea(this);
                    kud.guztiakInportatu();
                    zerrenda = db.komertzialaDao().guztiak();
                } catch (Exception e) {
                    try {
                        XMLKudeatzailea kud = new XMLKudeatzailea(this);
                        kud.inportatuFitxategia("komertzialak.xml");
                        zerrenda = db.komertzialaDao().guztiak();
                    } catch (Exception ignored) {
                        // Utzi zerrenda hutsik
                    }
                }
            }
            final List<Komertziala> finalZerrenda = zerrenda;
            runOnUiThread(() -> {
                if (finalZerrenda.isEmpty()) {
                    Toast.makeText(this, R.string.komertzial_zerrenda_hutsa, Toast.LENGTH_LONG).show();
                    return;
                }
                String[] aukerak = new String[finalZerrenda.size()];
                for (int i = 0; i < finalZerrenda.size(); i++) {
                    Komertziala k = finalZerrenda.get(i);
                    aukerak[i] = (k.getIzena() != null ? k.getIzena().trim() : "") + " (" + (k.getKodea() != null ? k.getKodea() : "") + ")";
                }
                new AlertDialog.Builder(this)
                        .setTitle(R.string.komertzial_hautatu_titulua)
                        .setItems(aukerak, (dialog, which) -> {
                            Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.putExtra(MainActivity.EXTRA_KOMMERTZIALA_KODEA, finalZerrenda.get(which).getKodea());
                            intent.putExtra(MainActivity.EXTRA_KOMMERTZIALA_IZENA, finalZerrenda.get(which).getIzena());
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton(R.string.xml_utzi, null)
                        .show();
            });
        }).start();
    }

    /** Komertzialen zerrenda erakusten du (hautatu komertziala botoirako). */
    private void erakutsiKomertzialHautaketaDialogoa() {
        sartuKomertzialGisa();
    }

    private void attemptLogin() {
        String user = etUser.getText() != null ? etUser.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        boolean valid = true;

        if (user.isEmpty()) {
            tilUser.setError(getString(R.string.error_user_required));
            valid = false;
        } else {
            tilUser.setError(null);
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_password_required));
            valid = false;
        } else {
            tilPassword.setError(null);
        }

        if (valid) {
            if (XmlBilatzailea.loginakFaltaDa(this)) {
                Toast.makeText(this, getString(R.string.xml_falta_da, "loginak.xml"), Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}
