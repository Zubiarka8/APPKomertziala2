package com.example.appkomertziala.activity;

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

import com.example.appkomertziala.MainActivity;
import com.example.appkomertziala.R;
import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.example.appkomertziala.db.eredua.Komertziala;
import com.example.appkomertziala.db.eredua.Logina;
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
    
    /** Importazioa exekutatzen ari den egiaztatzeko (toast gehiegi saihesteko). */
    private volatile boolean importazioaExekutatzen = false;

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
        
        // KRITIKOA: Hardware acceleration optimizazioa - rendering pipeline ordena bermatzeko
        // Window-aren hardware acceleration mantendu, baina MapFragment software rendering erabiliko du
        
        setContentView(R.layout.activity_login);

        tilUser = findViewById(R.id.tilUser);
        tilPassword = findViewById(R.id.tilPassword);
        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);

        MaterialButton btnLoadXml = findViewById(R.id.btnLoadXml);
        MaterialButton btnSelectCommercial = findViewById(R.id.btnSelectCommercial);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        MaterialButton btnSartuKomertzialGisa = findViewById(R.id.btnSartuKomertzialGisa);
        MaterialButton btnSartuBazkideGisa = findViewById(R.id.btnSartuBazkideGisa);

        btnLoadXml.setOnClickListener(v -> erakutsiXmlHautaketaDialogoa());
        MaterialButton btnInportatuGailutik = findViewById(R.id.btnInportatuGailutik);
        btnInportatuGailutik.setOnClickListener(v -> inportatuGailutikLauncher.launch(new String[]{"application/xml", "text/xml", "*/*"}));
        btnSelectCommercial.setOnClickListener(v -> erakutsiKomertzialHautaketaDialogoa());
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnSartuKomertzialGisa.setOnClickListener(v -> sartuKomertzialGisa());
        btnSartuBazkideGisa.setOnClickListener(v -> sartuBazkideGisa());

        // Logo irudia optimizatu - cache eta rendering optimizazioa
        android.widget.ImageView ivLogo = findViewById(R.id.ivLogo);
        if (ivLogo != null) {
            // Software rendering erabili logoarentzat buffer ordena bermatzeko
            ivLogo.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null);
            // Cache optimizazioa
            ivLogo.setDrawingCacheEnabled(true);
        }

        // Hasieran: komertzial guztiak kargatu taula hutsik badago (assets edo barne-memoria)
        kargatuKomertzialakHasieran();

        // MapFragment kargatu - post() erabiliz UI thread-ean exekutatzeko
        findViewById(android.R.id.content).post(() -> konfiguratuMapFragment());
    }

    /**
     * MapFragment konfiguratu eta kargatu.
     */
    private void konfiguratuMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragmentLogin);
        if (mapFragment != null) {
            // MapFragment view-a lortu eta hardware acceleration mantendu
            android.view.View mapView = mapFragment.getView();
            if (mapView != null) {
                // Hardware acceleration erabili (software rendering ez erabili)
                mapView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);
                // Clip optimizazioa
                if (mapView instanceof android.view.ViewGroup) {
                    android.view.ViewGroup viewGroup = (android.view.ViewGroup) mapView;
                    viewGroup.setClipToPadding(true);
                    viewGroup.setClipChildren(true);
                }
            }
            // Map-a kargatu
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // Map konfigurazioa
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        
        // UI settings
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(true);
        
        // Camera eta marker konfiguratu
        LatLng donostia = new LatLng(DONOSTIA_LAT, DONOSTIA_LNG);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(donostia, 14f));
        
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
        // Importazio bat exekutatzen ari bada, ez egin ezer (toast gehiegi saihesteko)
        if (importazioaExekutatzen) {
            return;
        }
        importazioaExekutatzen = true;
        new Thread(() -> {
            try {
                XMLKudeatzailea kud = new XMLKudeatzailea(this);
                kud.inportatuFitxategia(fitxategiIzena);
                // KRITIKOA: UI eguneraketa bideratu - invalidate ordena bermatzeko
                android.view.View rootView = findViewById(android.R.id.content);
                if (rootView != null) {
                    rootView.post(() -> runOnUiThread(() -> Toast.makeText(this, R.string.datuak_ondo_eguneratu, Toast.LENGTH_LONG).show()));
                } else {
                    runOnUiThread(() -> Toast.makeText(this, R.string.datuak_ondo_eguneratu, Toast.LENGTH_LONG).show());
                }
            } catch (IllegalArgumentException e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.xml_ezin_inportatu, Toast.LENGTH_LONG).show());
            } catch (java.io.IOException e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.errorea_fitxategia_irakurtzean, Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                String mezu = e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : getString(R.string.errore_ezezaguna);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.xml_errorea, mezu), Toast.LENGTH_LONG).show());
            } finally {
                importazioaExekutatzen = false;
            }
        }).start();
    }

    /** Al abrir la pantalla de login: si no hay komertzialak en la base de datos, cargar komertzialak.xml (assets o barne-memoria). Solo carga si la tabla sigue vacía justo antes de escribir, para no sobrescribir importaciones del usuario. */
    private void kargatuKomertzialakHasieran() {
        new Thread(() -> {
            try {
                XMLKudeatzailea kud = new XMLKudeatzailea(this);
                kud.komertzialakInportatuBakarrikHutsikBada();
            } catch (Exception ignored) {
                // komertzialak.xml ez badago, zerrenda hutsik egongo da XML kargatu arte
            }
        }).start();
    }

    /** XML guztiak ordena egokian kargatzen du (komertzialak, bazkideak, loginak, katalogoa). Hila nagusitik kanpo. */
    private void kargatuXmlGuztiak() {
        // Importazio bat exekutatzen ari bada, ez egin ezer (toast gehiegi saihesteko)
        if (importazioaExekutatzen) {
            return;
        }
        importazioaExekutatzen = true;
        new Thread(() -> {
            try {
                XMLKudeatzailea kud = new XMLKudeatzailea(this);
                kud.guztiakInportatu();
                // KRITIKOA: UI eguneraketa bideratu - invalidate ordena bermatzeko
                android.view.View rootView = findViewById(android.R.id.content);
                if (rootView != null) {
                    rootView.post(() -> runOnUiThread(() -> Toast.makeText(this, R.string.datuak_ondo_eguneratu, Toast.LENGTH_LONG).show()));
                } else {
                    runOnUiThread(() -> Toast.makeText(this, R.string.datuak_ondo_eguneratu, Toast.LENGTH_LONG).show());
                }
            } catch (java.io.IOException e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.errorea_fitxategia_irakurtzean, Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                String mezu = e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : getString(R.string.errore_ezezaguna);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.xml_errorea, mezu), Toast.LENGTH_LONG).show());
            } finally {
                importazioaExekutatzen = false;
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
                    // Lehenengo saiakera huts egin badu, komertzialak.xml bakarrik saiatu (toast-ik gabe)
                    try {
                        XMLKudeatzailea kud = new XMLKudeatzailea(this);
                        kud.inportatuFitxategia("komertzialak.xml");
                        zerrenda = db.komertzialaDao().guztiak();
                    } catch (Exception ignored) {
                        // Utzi zerrenda hutsik - toast-ik ez erakutsi, dialogoa hutsik erakutsiko du
                    }
                }
            }
            final List<Komertziala> finalZerrenda = zerrenda;
            // KRITIKOA: UI eguneraketa bideratu - invalidate ordena bermatzeko
            // post() erabili runOnUiThread() baino lehen, frame buffer ordena bermatzeko
            android.view.View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                rootView.post(() -> {
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
                                    Komertziala hautatua = finalZerrenda.get(which);
                                    
                                    // SEGURTASUNA: SessionManager erabiliz saioa hasi
                                    com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                                        new com.example.appkomertziala.segurtasuna.SessionManager(this);
                                    sessionManager.saioaHasi(hautatua.getKodea(), hautatua.getIzena());
                                    
                                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .setNegativeButton(R.string.xml_utzi, null)
                                .show();
                    });
                });
            } else {
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
                                Komertziala hautatua = finalZerrenda.get(which);
                                
                                // SEGURTASUNA: SessionManager erabiliz saioa hasi
                                com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                                    new com.example.appkomertziala.segurtasuna.SessionManager(this);
                                sessionManager.saioaHasi(hautatua.getKodea(), hautatua.getIzena());
                                
                                Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            })
                            .setNegativeButton(R.string.xml_utzi, null)
                            .show();
                });
            }
        }).start();
    }

    /** Komertzialen zerrenda erakusten du (hautatu komertziala botoirako). */
    private void erakutsiKomertzialHautaketaDialogoa() {
        sartuKomertzialGisa();
    }

    /** Bazkideen zerrenda datu-basean bilatu eta hautaketa-dialogoa erakusten du. Hutsik badago, XML guztiak kargatzen ditu (datu-basea betetzeko) eta gero zerrenda osoa erakusten du. */
    private void sartuBazkideGisa() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<Bazkidea> zerrenda = db.bazkideaDao().guztiak();
            if (zerrenda.isEmpty()) {
                try {
                    XMLKudeatzailea kud = new XMLKudeatzailea(this);
                    kud.guztiakInportatu();
                    zerrenda = db.bazkideaDao().guztiak();
                } catch (Exception e) {
                    // Lehenengo saiakera huts egin badu, bazkideak.xml bakarrik saiatu (toast-ik gabe)
                    try {
                        XMLKudeatzailea kud = new XMLKudeatzailea(this);
                        kud.inportatuFitxategia("bazkideak.xml");
                        zerrenda = db.bazkideaDao().guztiak();
                    } catch (Exception ignored) {
                        // Utzi zerrenda hutsik - toast-ik ez erakutsi, dialogoa hutsik erakutsiko du
                    }
                }
            }
            final List<Bazkidea> finalZerrenda = zerrenda;
            // KRITIKOA: UI eguneraketa bideratu - invalidate ordena bermatzeko
            android.view.View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                rootView.post(() -> {
                    runOnUiThread(() -> {
                        if (finalZerrenda.isEmpty()) {
                            Toast.makeText(this, R.string.bazkide_zerrenda_hutsa, Toast.LENGTH_LONG).show();
                            return;
                        }
                        String[] aukerak = new String[finalZerrenda.size()];
                        for (int i = 0; i < finalZerrenda.size(); i++) {
                            Bazkidea b = finalZerrenda.get(i);
                            String izena = (b.getIzena() != null ? b.getIzena().trim() : "") + 
                                           (b.getAbizena() != null && !b.getAbizena().trim().isEmpty() ? " " + b.getAbizena().trim() : "");
                            String nan = b.getNan() != null ? b.getNan() : "";
                            aukerak[i] = izena.isEmpty() ? nan : izena + (nan.isEmpty() ? "" : " (" + nan + ")");
                        }
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.bazkide_hautatu_titulua)
                                .setItems(aukerak, (dialog, which) -> {
                                    Bazkidea hautatutakoBazkidea = finalZerrenda.get(which);
                                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(this, MainActivity.class);
                                    if (hautatutakoBazkidea != null) {
                                        intent.putExtra(MainActivity.EXTRA_BAZKIDEA_NAN, hautatutakoBazkidea.getNan());
                                        intent.putExtra(MainActivity.EXTRA_BAZKIDEA_ID, hautatutakoBazkidea.getId());
                                    }
                                    startActivity(intent);
                                    finish();
                                })
                                .setNegativeButton(R.string.xml_utzi, null)
                                .show();
                    });
                });
            } else {
                runOnUiThread(() -> {
                    if (finalZerrenda.isEmpty()) {
                        Toast.makeText(this, R.string.bazkide_zerrenda_hutsa, Toast.LENGTH_LONG).show();
                        return;
                    }
                    String[] aukerak = new String[finalZerrenda.size()];
                    for (int i = 0; i < finalZerrenda.size(); i++) {
                        Bazkidea b = finalZerrenda.get(i);
                        String izena = (b.getIzena() != null ? b.getIzena().trim() : "") + 
                                       (b.getAbizena() != null && !b.getAbizena().trim().isEmpty() ? " " + b.getAbizena().trim() : "");
                        String nan = b.getNan() != null ? b.getNan() : "";
                        aukerak[i] = izena.isEmpty() ? nan : izena + (nan.isEmpty() ? "" : " (" + nan + ")");
                    }
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.bazkide_hautatu_titulua)
                            .setItems(aukerak, (dialog, which) -> {
                                Bazkidea hautatutakoBazkidea = finalZerrenda.get(which);
                                Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, MainActivity.class);
                                if (hautatutakoBazkidea != null) {
                                    intent.putExtra(MainActivity.EXTRA_BAZKIDEA_NAN, hautatutakoBazkidea.getNan());
                                    intent.putExtra(MainActivity.EXTRA_BAZKIDEA_ID, hautatutakoBazkidea.getId());
                                }
                                startActivity(intent);
                                finish();
                            })
                            .setNegativeButton(R.string.xml_utzi, null)
                            .show();
                });
            }
        }).start();
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
            final String userFinal = user;
            final String passwordFinal = password;
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(this);
                Logina logina = db.loginaDao().sarbideaBalidatu(userFinal, passwordFinal);
                // KRITIKOA: UI eguneraketa bideratu - frame buffer ordena bermatzeko
                android.view.View rootView = findViewById(android.R.id.content);
                if (rootView != null) {
                    rootView.post(() -> {
                        runOnUiThread(() -> {
                            if (logina == null) {
                                Toast.makeText(this, R.string.login_error_erabiltzaile_pasahitz, Toast.LENGTH_LONG).show();
                                return;
                            }
                            String komertzialKode = logina.getKomertzialKodea();
                            Komertziala komertziala = (komertzialKode != null && !komertzialKode.isEmpty())
                                    ? db.komertzialaDao().kodeaBilatu(komertzialKode) : null;
                            
                            // SEGURTASUNA: SessionManager erabiliz saioa hasi
                            if (komertziala != null) {
                                com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                                    new com.example.appkomertziala.segurtasuna.SessionManager(this);
                                sessionManager.saioaHasi(komertziala.getKodea(), komertziala.getIzena());
                            }
                            
                            Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        });
                    });
                } else {
                    runOnUiThread(() -> {
                        if (logina == null) {
                            Toast.makeText(this, R.string.login_error_erabiltzaile_pasahitz, Toast.LENGTH_LONG).show();
                            return;
                        }
                        String komertzialKode = logina.getKomertzialKodea();
                        Komertziala komertziala = (komertzialKode != null && !komertzialKode.isEmpty())
                                ? db.komertzialaDao().kodeaBilatu(komertzialKode) : null;
                        
                        // SEGURTASUNA: SessionManager erabiliz saioa hasi
                        if (komertziala != null) {
                            com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                                new com.example.appkomertziala.segurtasuna.SessionManager(this);
                            sessionManager.saioaHasi(komertziala.getKodea(), komertziala.getIzena());
                        }
                        
                        Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }
            }).start();
        }
    }
}
