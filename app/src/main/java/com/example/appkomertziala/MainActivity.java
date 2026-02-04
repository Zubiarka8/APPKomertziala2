package com.example.appkomertziala;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.core.content.FileProvider;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.appkomertziala.activity.BazkideaFormularioActivity;
import com.example.appkomertziala.activity.BisitaFormularioActivity;
import com.example.appkomertziala.activity.EskaerakActivity;
import com.example.appkomertziala.activity.LoginActivity;
import com.example.appkomertziala.activity.ProduktuDetalaActivity;
import com.example.appkomertziala.adapter.KatalogoaAdapter;
import com.example.appkomertziala.adapter.SaskiaAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Agenda;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.example.appkomertziala.db.eredua.EskaeraGoiburua;
import com.example.appkomertziala.db.eredua.EskaeraXehetasuna;
import com.example.appkomertziala.db.eredua.Katalogoa;
import com.example.appkomertziala.db.eredua.Komertziala;
import com.example.appkomertziala.segurtasuna.DataBalidatzailea;
import com.example.appkomertziala.xml.DatuKudeatzailea;
import com.example.appkomertziala.xml.XmlBilatzailea;
import com.example.appkomertziala.xml.XMLKudeatzailea;
import com.example.appkomertziala.agenda.AgendaEsportatzailea;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParserException;
import java.util.List;
import java.util.Locale;

/**
 * Techno Basque - Main screen with BottomNavigationView and Gipuzkoa contact section.
 * Hasiera tab: SupportMapFragment centered on Donostia + contact card (Map, Call, Email intents).
 * Other tabs: Agenda, Bazkideak, Inventarioa placeholders.
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    /** Donostia center for default map camera. */
    private static final double DONOSTIA_LAT = 43.3180;
    private static final double DONOSTIA_LNG = -1.9812;

    /** Gipuzkoa office contact intents (spec). */
    private static final String MAP_QUERY = "Gran+Via+kalea+1,+Donostia";
    private static final String PHONE_NUMBER = "943123456";
    private static final String EMAIL_ADDRESS = "gipuzkoa@enpresa.eus";

    /** LoginActivity-k bidalitako komertzial hautatua (kredentzialik gabe sartu). */
    public static final String EXTRA_KOMMERTZIALA_KODEA = "komertziala_kodea";
    public static final String EXTRA_KOMMERTZIALA_IZENA = "komertziala_izena";
    
    /** LoginActivity-k bidalitako bazkidea hautatua (bazkide gisa sartu). */
    public static final String EXTRA_BAZKIDEA_NAN = "bazkidea_nan";
    public static final String EXTRA_BAZKIDEA_ID = "bazkidea_id";

    private View contentHasiera;
    private View contentAgenda;
    private View contentBazkideak;
    private View contentInventarioa;
    private BottomNavigationView bottomNav;
    private ExtendedFloatingActionButton fabAgendaZitaGehitu;
    private ExtendedFloatingActionButton fabBazkideaGehitu;
    private ImageButton btnMap;
    private TextInputEditText etAgendaBilatu;

    /** Inbentarioa: katalogo osoa (bilatzaileak iragazteko). */
    private List<Katalogoa> katalogoaOsoa = new ArrayList<>();
    /** Erosketa saskia: artikulu bakoitza eta kopurua. */
    private final List<SaskiaElementua> saskia = new ArrayList<>();
    private ImageButton btnCall;
    private ImageButton btnEmail;

    private final ActivityResultLauncher<Intent> bisitaFormularioLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    kargatuAgendaZitak();
                }
            });

    private final ActivityResultLauncher<Intent> bazkideaFormularioLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    kargatuBazkideakZerrenda();
                }
            });

    private final ActivityResultLauncher<Intent> produktuDetalaLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                String kodea = result.getData().getStringExtra(ProduktuDetalaActivity.EXTRA_EROSI_ARTIKULU_KODEA);
                if (kodea == null || kodea.isEmpty()) return;
                AppDatabase db = AppDatabase.getInstance(this);
                new Thread(() -> {
                    Katalogoa k = db.katalogoaDao().artikuluaBilatu(kodea);
                    if (k != null) {
                        runOnUiThread(() -> {
                            saskiraGehitu(k);
                        });
                    }
                }).start();
            });

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
                        
                        // UI eguneratu inportatutako fitxategiaren arabera
                        String fitxategiIzena = izena.toLowerCase(Locale.ROOT);
                        runOnUiThread(() -> {
                            Toast.makeText(this, R.string.inportatu_ondo, Toast.LENGTH_SHORT).show();
                            
                            // Bazkideak.xml inportatuta bada, bazkide zerrenda eguneratu
                            if (fitxategiIzena.equals("bazkideak.xml")) {
                                if (contentBazkideak != null && contentBazkideak.getVisibility() == View.VISIBLE) {
                                    kargatuBazkideakZerrenda();
                                }
                            }
                            // Agenda.xml inportatuta bada, agenda zitak eguneratu
                            else if (fitxategiIzena.equals("agenda.xml")) {
                                if (contentAgenda != null && contentAgenda.getVisibility() == View.VISIBLE) {
                                    kargatuAgendaZitak();
                                }
                            }
                            // Katalogoa.xml inportatuta bada, inbentarioa eguneratu
                            else if (fitxategiIzena.equals("katalogoa.xml")) {
                                if (contentInventarioa != null && contentInventarioa.getVisibility() == View.VISIBLE) {
                                    erakutsiInbentarioaEdukia();
                                }
                            }
                        });
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
        setContentView(R.layout.activity_main);

        contentHasiera = findViewById(R.id.content_hasiera);
        contentAgenda = findViewById(R.id.content_agenda);
        contentBazkideak = findViewById(R.id.content_bazkideak);
        contentInventarioa = findViewById(R.id.content_inventarioa);
        bottomNav = findViewById(R.id.bottom_nav);
        fabAgendaZitaGehitu = findViewById(R.id.fabAgendaZitaGehitu);
        fabBazkideaGehitu = findViewById(R.id.fabBazkideaGehitu);
        btnMap = findViewById(R.id.btnMap);
        btnCall = findViewById(R.id.btnCall);
        btnEmail = findViewById(R.id.btnEmail);

        // Erabiltzailearen izena kargatu eta erakutsi
        kargatuErabiltzaileIzena();

        setupMap();
        setupBottomNav();
        setupContactButtons();
        setupEsportazioBotoiak();
        setupAgendaCitaGehitu();
        setupBazkideaFab();
        setupAgendaBilaketa();
        erakutsiEsportazioBidea();
    }

    /** Saioa hasi duen erabiltzailearen izena eta abizena kargatu eta erakutsi hasiera atalean. */
    private void kargatuErabiltzaileIzena() {
        TextView tvErabiltzaileIzena = findViewById(R.id.tvErabiltzaileIzena);
        if (tvErabiltzaileIzena == null) return;

        // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
        com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
            new com.example.appkomertziala.segurtasuna.SessionManager(this);
        String komertzialKodea = sessionManager.getKomertzialKodea();
        String komertzialIzena = sessionManager.getKomertzialIzena();
        
        if (komertzialKodea != null && !komertzialKodea.trim().isEmpty()) {
            // Komertziala datu-basean bilatu eta izena + abizena erakutsi
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(this);
                    Komertziala komertziala = db.komertzialaDao().kodeaBilatu(komertzialKodea.trim());
                    
                    runOnUiThread(() -> {
                        if (komertziala != null) {
                            String izena = komertziala.getIzena() != null ? komertziala.getIzena().trim() : "";
                            String abizena = komertziala.getAbizena() != null && !komertziala.getAbizena().trim().isEmpty() 
                                    ? " " + komertziala.getAbizena().trim() : "";
                            String izenOsoa = izena + abizena;
                            
                            if (!izenOsoa.isEmpty()) {
                                tvErabiltzaileIzena.setText(getString(R.string.ongietorri, izenOsoa));
                                tvErabiltzaileIzena.setVisibility(View.VISIBLE);
                            } else {
                                tvErabiltzaileIzena.setVisibility(View.GONE);
                            }
                        } else {
                            tvErabiltzaileIzena.setVisibility(View.GONE);
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> tvErabiltzaileIzena.setVisibility(View.GONE));
                }
            }).start();
            return;
        }

        // Bestela bazkidea bilatu (atzera-egokitasunerako, baina SEGURTASUNA: SessionManager lehenetsia da)
        Intent intent = getIntent();
        if (intent == null) {
            tvErabiltzaileIzena.setVisibility(View.GONE);
            return;
        }
        
        String bazkideaNan = intent.getStringExtra(EXTRA_BAZKIDEA_NAN);
        long bazkideaId = intent.getLongExtra(EXTRA_BAZKIDEA_ID, -1);
        
        if ((bazkideaNan != null && !bazkideaNan.trim().isEmpty()) || bazkideaId > 0) {
            // Bazkidea datu-basean bilatu eta izena + abizena erakutsi
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(this);
                    Bazkidea bazkidea = null;
                    
                    if (bazkideaId > 0) {
                        bazkidea = db.bazkideaDao().idzBilatu(bazkideaId);
                    } else if (bazkideaNan != null && !bazkideaNan.trim().isEmpty()) {
                        bazkidea = db.bazkideaDao().nanBilatu(bazkideaNan.trim());
                    }
                    
                    final Bazkidea finalBazkidea = bazkidea;
                    runOnUiThread(() -> {
                        if (finalBazkidea != null) {
                            String izena = finalBazkidea.getIzena() != null ? finalBazkidea.getIzena().trim() : "";
                            String abizena = finalBazkidea.getAbizena() != null && !finalBazkidea.getAbizena().trim().isEmpty() 
                                    ? " " + finalBazkidea.getAbizena().trim() : "";
                            String izenOsoa = izena + abizena;
                            
                            if (!izenOsoa.isEmpty()) {
                                tvErabiltzaileIzena.setText(getString(R.string.ongietorri, izenOsoa));
                                tvErabiltzaileIzena.setVisibility(View.VISIBLE);
                            } else {
                                // Izena hutsik badago, NAN erabili
                                String nan = finalBazkidea.getNan() != null ? finalBazkidea.getNan().trim() : "";
                                if (!nan.isEmpty()) {
                                    tvErabiltzaileIzena.setText(getString(R.string.ongietorri, nan));
                                    tvErabiltzaileIzena.setVisibility(View.VISIBLE);
                                } else {
                                    tvErabiltzaileIzena.setVisibility(View.GONE);
                                }
                            }
                        } else {
                            tvErabiltzaileIzena.setVisibility(View.GONE);
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> tvErabiltzaileIzena.setVisibility(View.GONE));
                }
            }).start();
            return;
        }

        // Ez bada komertziala ezta bazkidea, ezkutatu
        tvErabiltzaileIzena.setVisibility(View.GONE);
    }

    /** Agenda fitxako Extended FAB: bisita berria gehitzeko pantaila ireki (agenda_bisitak taula). */
    private void setupAgendaCitaGehitu() {
        if (fabAgendaZitaGehitu != null) {
            fabAgendaZitaGehitu.setOnClickListener(v -> {
                // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                    new com.example.appkomertziala.segurtasuna.SessionManager(this);
                String komertzialKode = sessionManager.getKomertzialKodea();
                
                if (komertzialKode == null || komertzialKode.isEmpty()) {
                    Toast.makeText(this, R.string.saioa_ez_dago_hasita, Toast.LENGTH_LONG).show();
                    return;
                }
                
                Intent intent = new Intent(this, BisitaFormularioActivity.class);
                intent.putExtra(BisitaFormularioActivity.EXTRA_BISITA_ID, -1L);
                intent.putExtra(EXTRA_KOMMERTZIALA_KODEA, komertzialKode);
                bisitaFormularioLauncher.launch(intent);
            });
        }
    }

    /** Bazkideak fitxako Extended FAB: bazkide berria gehitzeko pantaila ireki (Material 3, Agenda bera altuera). */
    private void setupBazkideaFab() {
        if (fabBazkideaGehitu != null) {
            fabBazkideaGehitu.setOnClickListener(v -> {
                Intent intent = new Intent(this, BazkideaFormularioActivity.class);
                intent.putExtra(BazkideaFormularioActivity.EXTRA_BAZKIDEA_ID, -1L);
                bazkideaFormularioLauncher.launch(intent);
            });
        }
    }

    /** Agendako bilaketa funtzioa konfiguratu: testua sartzean bilaketa automatikoki exekutatzen da. */
    private void setupAgendaBilaketa() {
        etAgendaBilatu = findViewById(R.id.etAgendaBilatu);
        if (etAgendaBilatu != null) {
            etAgendaBilatu.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String filter = s.toString().trim();
                    if (filter.isEmpty()) {
                        kargatuAgendaZitak(); // Guztiak kargatu
                    } else {
                        bilatuAgendaZitak(filter); // Bilaketa exekutatu
                    }
                }
            });
        }
    }

    /** Agendako bisita zerrenda kargatu eta erakutsi (agenda_bisitak taula, agendaDao). */
    private void kargatuAgendaZitak() {
        LinearLayout listContainer = findViewById(R.id.list_agenda_zitak);
        TextView tvHutsa = findViewById(R.id.tvAgendaZitakHutsa);
        if (listContainer == null || tvHutsa == null) return;

        new Thread(() -> {
            try {
                // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                    new com.example.appkomertziala.segurtasuna.SessionManager(this);
                String komertzialKodea = sessionManager.getKomertzialKodea();
                
                if (komertzialKodea == null || komertzialKodea.isEmpty()) {
                    runOnUiThread(() -> {
                        if (!isDestroyed()) {
                            tvHutsa.setVisibility(View.VISIBLE);
                            tvHutsa.setText(getString(R.string.saioa_ez_dago_hasita));
                            listContainer.removeAllViews();
                        }
                    });
                    return;
                }
                
                AppDatabase db = AppDatabase.getInstance(this);
                // SEGURTASUNA: getVisitsByKomertzial erabili, ez guztiak()
                List<Agenda> bisitak = db.agendaDao().getVisitsByKomertzial(komertzialKodea);
                if (bisitak == null) bisitak = new ArrayList<>();

                ArrayList<Object[]> erakusteko = new ArrayList<>();
                for (Agenda a : bisitak) {
                    String dataStr = a.getBisitaData() != null ? a.getBisitaData() : "";
                    String orduaStr = a.getOrdua() != null ? a.getOrdua().trim() : "";
                    String bazkideaIzena = "";
                    if (a.getBazkideaKodea() != null && !a.getBazkideaKodea().trim().isEmpty()) {
                        Bazkidea b = db.bazkideaDao().nanBilatu(a.getBazkideaKodea().trim());
                        if (b != null) {
                            String izena = (b.getIzena() != null ? b.getIzena().trim() : "") + 
                                           (b.getAbizena() != null && !b.getAbizena().trim().isEmpty() ? " " + b.getAbizena().trim() : "");
                            bazkideaIzena = izena.isEmpty() ? (b.getNan() != null ? b.getNan() : "") : izena;
                        } else {
                            bazkideaIzena = a.getBazkideaKodea();
                        }
                    }
                    String deskribapena = a.getDeskribapena() != null ? a.getDeskribapena().trim() : "";
                    String egoeraStr = a.getEgoera() != null ? a.getEgoera().trim() : "";
                    String zenb = "BIS-" + a.getId();
                    erakusteko.add(new Object[]{dataStr, orduaStr, zenb, bazkideaIzena.isEmpty() ? "—" : bazkideaIzena, deskribapena, egoeraStr, a.getId()});
                }

                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    listContainer.removeAllViews();
                    if (erakusteko.isEmpty()) {
                        tvHutsa.setVisibility(View.VISIBLE);
                        return;
                    }
                    tvHutsa.setVisibility(View.GONE);
                    LayoutInflater inflater = getLayoutInflater();
                    for (Object[] row : erakusteko) {
                        String dataStr = (String) row[0];
                        String orduaStr = (String) row[1];
                        String zenb = (String) row[2];
                        String bazkideaIzena = (String) row[3];
                        String deskribapena = (String) row[4];
                        String egoeraStr = (String) row[5];
                        long agendaId = (Long) row[6];
                        View item = inflater.inflate(R.layout.item_zita, listContainer, false);
                        ((TextView) item.findViewById(R.id.itemZitaData)).setText(dataStr);
                        ((TextView) item.findViewById(R.id.itemZitaZenbakia)).setText(zenb);
                        TextView tvOrdezk = item.findViewById(R.id.itemZitaOrdezkaritza);
                        if (!deskribapena.isEmpty()) {
                            tvOrdezk.setText(deskribapena);
                            tvOrdezk.setVisibility(View.VISIBLE);
                        }
                        TextView tvEgoera = item.findViewById(R.id.itemZitaEgoera);
                        if (tvEgoera != null && !egoeraStr.isEmpty()) {
                            tvEgoera.setText(egoeraStr);
                            tvEgoera.setVisibility(View.VISIBLE);
                        }
                        MaterialButton btnIkusi = item.findViewById(R.id.btnZitaIkusi);
                        MaterialButton btnEditatu = item.findViewById(R.id.btnZitaEditatu);
                        MaterialButton btnEzabatu = item.findViewById(R.id.btnZitaEzabatu);
                        btnIkusi.setOnClickListener(v -> erakutsiZitaXehetasunak(dataStr, orduaStr, zenb, bazkideaIzena, deskribapena, egoeraStr));
                        btnEditatu.setOnClickListener(v -> editatuZita(agendaId));
                        btnEzabatu.setOnClickListener(v -> ezabatuZita(agendaId));
                        listContainer.addView(item);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isDestroyed()) {
                        Toast.makeText(this, getString(R.string.esportatu_errorea_batzuetan), Toast.LENGTH_LONG).show();
                        listContainer.removeAllViews();
                        tvHutsa.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }

    /** Agendako bilaketa exekutatu (data, bazkidea izena/kodea, deskribapena, egoera eremuen artean). */
    private void bilatuAgendaZitak(String filter) {
        LinearLayout listContainer = findViewById(R.id.list_agenda_zitak);
        TextView tvHutsa = findViewById(R.id.tvAgendaZitakHutsa);
        if (listContainer == null || tvHutsa == null) return;

        com.example.appkomertziala.db.AgendaRepository repository = 
            new com.example.appkomertziala.db.AgendaRepository(this);
        
        repository.bilatuOrokorra(filter, bisitak -> {
            if (bisitak == null) bisitak = new ArrayList<>();
            
            AppDatabase db = AppDatabase.getInstance(this);
            ArrayList<Object[]> erakusteko = new ArrayList<>();
            for (Agenda a : bisitak) {
                String dataStr = a.getBisitaData() != null ? a.getBisitaData() : "";
                String orduaStr = a.getOrdua() != null ? a.getOrdua().trim() : "";
                String bazkideaIzena = "";
                if (a.getBazkideaKodea() != null && !a.getBazkideaKodea().trim().isEmpty()) {
                    Bazkidea b = db.bazkideaDao().nanBilatu(a.getBazkideaKodea().trim());
                    if (b != null) {
                        String izena = (b.getIzena() != null ? b.getIzena().trim() : "") + 
                                       (b.getAbizena() != null && !b.getAbizena().trim().isEmpty() ? " " + b.getAbizena().trim() : "");
                        bazkideaIzena = izena.isEmpty() ? (b.getNan() != null ? b.getNan() : "") : izena;
                    } else {
                        bazkideaIzena = a.getBazkideaKodea();
                    }
                }
                String deskribapena = a.getDeskribapena() != null ? a.getDeskribapena().trim() : "";
                String egoeraStr = a.getEgoera() != null ? a.getEgoera().trim() : "";
                String zenb = "BIS-" + a.getId();
                erakusteko.add(new Object[]{dataStr, orduaStr, zenb, bazkideaIzena.isEmpty() ? "—" : bazkideaIzena, deskribapena, egoeraStr, a.getId()});
            }

            runOnUiThread(() -> {
                if (isDestroyed()) return;
                listContainer.removeAllViews();
                if (erakusteko.isEmpty()) {
                    tvHutsa.setVisibility(View.VISIBLE);
                    return;
                }
                tvHutsa.setVisibility(View.GONE);
                LayoutInflater inflater = getLayoutInflater();
                for (Object[] row : erakusteko) {
                    String dataStr = (String) row[0];
                    String orduaStr = (String) row[1];
                    String zenb = (String) row[2];
                    String bazkideaIzena = (String) row[3];
                    String deskribapena = (String) row[4];
                    String egoeraStr = (String) row[5];
                    long agendaId = (Long) row[6];
                    View item = inflater.inflate(R.layout.item_zita, listContainer, false);
                    ((TextView) item.findViewById(R.id.itemZitaData)).setText(dataStr);
                    ((TextView) item.findViewById(R.id.itemZitaZenbakia)).setText(zenb);
                    TextView tvOrdezk = item.findViewById(R.id.itemZitaOrdezkaritza);
                    if (!deskribapena.isEmpty()) {
                        tvOrdezk.setText(deskribapena);
                        tvOrdezk.setVisibility(View.VISIBLE);
                    }
                    TextView tvEgoera = item.findViewById(R.id.itemZitaEgoera);
                    if (tvEgoera != null && !egoeraStr.isEmpty()) {
                        tvEgoera.setText(egoeraStr);
                        tvEgoera.setVisibility(View.VISIBLE);
                    }
                    MaterialButton btnIkusi = item.findViewById(R.id.btnZitaIkusi);
                    MaterialButton btnEditatu = item.findViewById(R.id.btnZitaEditatu);
                    MaterialButton btnEzabatu = item.findViewById(R.id.btnZitaEzabatu);
                    btnIkusi.setOnClickListener(v -> erakutsiZitaXehetasunak(dataStr, orduaStr, zenb, bazkideaIzena, deskribapena, egoeraStr));
                    btnEditatu.setOnClickListener(v -> editatuZita(agendaId));
                    btnEzabatu.setOnClickListener(v -> ezabatuZita(agendaId));
                    listContainer.addView(item);
                }
            });
            
            repository.itxi();
        });
    }

    /** Zitaren xehetasunak dialogoan erakusten du (Ikusi botoia): data, ordua, zenbakia, bazkidea, deskribapena, egoera. */
    private void erakutsiZitaXehetasunak(String dataStr, String orduaStr, String zenbakia, String bazkideaIzena, String deskribapena, String egoera) {
        StringBuilder msg = new StringBuilder();
        msg.append(getString(R.string.zita_data)).append(": ").append(dataStr).append("\n");
        if (orduaStr != null && !orduaStr.isEmpty()) {
            msg.append(getString(R.string.zita_ordua)).append(": ").append(orduaStr).append("\n");
        }
        msg.append(getString(R.string.zita_zenbakia)).append(": ").append(zenbakia).append("\n");
        msg.append(getString(R.string.agenda_bisita_egoera)).append(": ").append(egoera != null && !egoera.isEmpty() ? egoera : "—").append("\n");
        if (deskribapena != null && !deskribapena.isEmpty()) {
            msg.append(getString(R.string.agenda_bisita_deskribapena)).append(": ").append(deskribapena).append("\n");
        }
        String msgStr = msg.toString();
        new AlertDialog.Builder(this)
                .setTitle(R.string.zita_ikusi_izenburua)
                .setMessage(msgStr)
                .setPositiveButton(R.string.ados, null)
                .show();
    }

    /** Bisita editatzeko BisitaFormularioActivity ireki (Editatu botoia). Komertzialaren kodea bidaltzen da (sortzailearen ID erresolbatzeko). */
    private void editatuZita(long agendaId) {
        // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
        com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
            new com.example.appkomertziala.segurtasuna.SessionManager(this);
        String komertzialKode = sessionManager.getKomertzialKodea();
        
        if (komertzialKode == null || komertzialKode.isEmpty()) {
            Toast.makeText(this, R.string.saioa_ez_dago_hasita, Toast.LENGTH_LONG).show();
            return;
        }
        
        Intent intent = new Intent(this, BisitaFormularioActivity.class);
        intent.putExtra(BisitaFormularioActivity.EXTRA_BISITA_ID, agendaId);
        intent.putExtra(EXTRA_KOMMERTZIALA_KODEA, komertzialKode);
        bisitaFormularioLauncher.launch(intent);
    }

    /** Bisita ezabatu baieztapenarekin (Ezabatu botoia). */
    private void ezabatuZita(long agendaId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_ezabatu)
                .setMessage(R.string.zita_ezabatu_baieztatu)
                .setPositiveButton(R.string.bai, (dialog, which) -> {
                    new Thread(() -> {
                        // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                        com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                            new com.example.appkomertziala.segurtasuna.SessionManager(this);
                        String komertzialKodea = sessionManager.getKomertzialKodea();
                        
                        if (komertzialKodea == null || komertzialKodea.isEmpty()) {
                            runOnUiThread(() -> {
                                Toast.makeText(this, R.string.saioa_ez_dago_hasita, Toast.LENGTH_LONG).show();
                            });
                            return;
                        }
                        
                        AppDatabase db = AppDatabase.getInstance(this);
                        // SEGURTASUNA: idzBilatuSegurua erabili, ez idzBilatu
                        Agenda a = db.agendaDao().idzBilatuSegurua(agendaId, komertzialKodea);
                        if (a != null) {
                            // SEGURTASUNA: ezabatuSegurua erabili, ez ezabatu
                            int emaitza = db.agendaDao().ezabatuSegurua(agendaId, komertzialKodea);
                            runOnUiThread(() -> {
                                if (emaitza > 0) {
                                    Toast.makeText(this, R.string.zita_ezabatu_ondo, Toast.LENGTH_SHORT).show();
                                    kargatuAgendaZitak();
                                } else {
                                    Toast.makeText(this, R.string.zita_ezabatu_errorea, Toast.LENGTH_LONG).show();
                                }
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(this, R.string.zita_ezabatu_errorea, Toast.LENGTH_LONG).show();
                            });
                        }
                    }).start();
                })
                .setNegativeButton(R.string.ez, null)
                .show();
    }

    /** Esportazioen karpeta (barne-memoria) pantailan erakusten du. */
    private void erakutsiEsportazioBidea() {
        TextView tvBidea = findViewById(R.id.tvEsportazioBidea);
        String bidea = getFilesDir().getAbsolutePath();
        tvBidea.setText(getString(R.string.esportatu_gordeta_lekua, bidea));
    }

    /** Hasierako esportazio/inportazio botoiak eta Agenda ataleko esportazio botoiak. */
    private void setupEsportazioBotoiak() {
        DatuKudeatzailea datuKudeatzailea = new DatuKudeatzailea(this);
        AgendaEsportatzailea agendaEsportatzailea = new AgendaEsportatzailea(this);
        MaterialButton btnEsportatuKomertzialak = findViewById(R.id.btnEsportatuKomertzialak);
        MaterialButton btnBazkideBerriak = findViewById(R.id.btnEsportatuBazkideBerriak);
        MaterialButton btnEskaeraBerriak = findViewById(R.id.btnEsportatuEskaeraBerriak);
        MaterialButton btnEsportatuBazkideak = findViewById(R.id.btnEsportatuBazkideak);
        MaterialButton btnEsportatuAgendaXml = findViewById(R.id.btnEsportatuAgendaXml);
        MaterialButton btnEsportatuAgendaTxt = findViewById(R.id.btnEsportatuAgendaTxt);
        MaterialButton btnEsportatuKatalogoa = findViewById(R.id.btnEsportatuKatalogoa);
        MaterialButton btnKatalogoaEguneratu = findViewById(R.id.btnKatalogoaEguneratu);
        if (btnEsportatuKomertzialak != null) btnEsportatuKomertzialak.setOnClickListener(v -> esportatuKomertzialak(datuKudeatzailea));
        MaterialButton btnKomertzialakKudeatu = findViewById(R.id.btnKomertzialakKudeatu);
        if (btnKomertzialakKudeatu != null) btnKomertzialakKudeatu.setOnClickListener(v -> erakutsiKomertzialakKudeatuDialogoa());
        btnBazkideBerriak.setOnClickListener(v -> esportatuBazkideBerriak(datuKudeatzailea));
        btnEskaeraBerriak.setOnClickListener(v -> esportatuEskaeraBerriak(datuKudeatzailea));
        btnEsportatuBazkideak.setOnClickListener(v -> esportatuBazkideak(datuKudeatzailea));
        btnEsportatuAgendaXml.setOnClickListener(v -> esportatuAgendaXML(agendaEsportatzailea));
        btnEsportatuAgendaTxt.setOnClickListener(v -> esportatuAgendaTXT(agendaEsportatzailea));
        btnEsportatuKatalogoa.setOnClickListener(v -> esportatuKatalogoa(datuKudeatzailea));
        btnKatalogoaEguneratu.setOnClickListener(v -> katalogoaEguneratu(datuKudeatzailea));
        MaterialButton btnInportatuGailutik = findViewById(R.id.btnInportatuGailutik);
        if (btnInportatuGailutik != null) {
            btnInportatuGailutik.setOnClickListener(v -> inportatuGailutikLauncher.launch(new String[]{"application/xml", "text/xml", "*/*"}));
        }
        MaterialButton btnSesioaItxi = findViewById(R.id.btnSesioaItxi);
        btnSesioaItxi.setOnClickListener(v -> {
            // SEGURTASUNA: SessionManager erabiliz saioa itxi
            com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                new com.example.appkomertziala.segurtasuna.SessionManager(this);
            sessionManager.saioaItxi();
            
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void esportatuKomertzialak(DatuKudeatzailea datuKudeatzailea) {
        new Thread(() -> {
            boolean ondo = datuKudeatzailea.komertzialakEsportatu();
            runOnUiThread(() -> {
                if (ondo) {
                    Toast.makeText(this, R.string.esportatu_ondo, Toast.LENGTH_SHORT).show();
                    bidaliPostaz("komertzialak.xml", getString(R.string.postaz_gaia_komertzialak), "application/xml");
                } else {
                    Toast.makeText(this, R.string.esportatu_errorea_batzuetan, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /** Komertzialak zerrenda datu-basean irakurri eta kudeatu-dialogoa erakusten du. Ezabatu luze-sakatzean; datu-basean idazten da. */
    private void erakutsiKomertzialakKudeatuDialogoa() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<Komertziala> zerrenda = db.komertzialaDao().guztiak();
            runOnUiThread(() -> {
                if (zerrenda.isEmpty()) {
                    Toast.makeText(this, R.string.komertzialak_zerrenda_hutsa_kudeatu, Toast.LENGTH_LONG).show();
                    return;
                }
                final ArrayList<Komertziala> zerrendaMutagarri = new ArrayList<>(zerrenda);
                String[] aukerak = new String[zerrendaMutagarri.size()];
                for (int i = 0; i < zerrendaMutagarri.size(); i++) {
                    Komertziala k = zerrendaMutagarri.get(i);
                    aukerak[i] = (k.getIzena() != null ? k.getIzena().trim() : "") + " (" + (k.getKodea() != null ? k.getKodea() : "") + ")";
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, aukerak);
                ListView listView = new ListView(this);
                listView.setAdapter(adapter);
                listView.setMinimumHeight(400);
                android.widget.LinearLayout wrap = new android.widget.LinearLayout(this);
                wrap.setOrientation(android.widget.LinearLayout.VERTICAL);
                android.widget.TextView oharra = new android.widget.TextView(this);
                oharra.setText(R.string.komertzialak_kudeatu_oharra);
                int pad = (int) (16 * getResources().getDisplayMetrics().density);
                oharra.setPadding(pad, pad * 3 / 4, pad, pad / 2);
                oharra.setTextSize(12);
                oharra.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                wrap.addView(oharra);
                listView.setMinimumHeight(400);
                wrap.addView(listView);
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.komertzialak_kudeatu_titulua)
                        .setView(wrap)
                        .setNegativeButton(R.string.xml_utzi, null)
                        .create();
                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    Komertziala k = zerrendaMutagarri.get(position);
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.btn_ezabatu)
                            .setMessage(R.string.komertzial_ezabatu_baieztatu)
                            .setPositiveButton(R.string.bai, (d, w) -> {
                                new Thread(() -> {
                                    try {
                                        db.komertzialaDao().ezabatu(k);
                                    } catch (Exception e) {
                                        runOnUiThread(() -> Toast.makeText(this, R.string.komertzial_ezabatu_errorea, Toast.LENGTH_LONG).show());
                                        return;
                                    }
                                    List<Komertziala> berria = db.komertzialaDao().guztiak();
                                    runOnUiThread(() -> {
                                        zerrendaMutagarri.clear();
                                        zerrendaMutagarri.addAll(berria);
                                        adapter.clear();
                                        for (Komertziala x : berria) {
                                            adapter.add((x.getIzena() != null ? x.getIzena().trim() : "") + " (" + (x.getKodea() != null ? x.getKodea() : "") + ")");
                                        }
                                        adapter.notifyDataSetChanged();
                                        Toast.makeText(this, R.string.komertzial_ondo_ezabatuta, Toast.LENGTH_SHORT).show();
                                        if (zerrendaMutagarri.isEmpty()) dialog.dismiss();
                                    });
                                }).start();
                            })
                            .setNegativeButton(R.string.ez, null)
                            .show();
                    return true;
                });
                dialog.show();
            });
        }).start();
    }

    private void esportatuBazkideBerriak(DatuKudeatzailea datuKudeatzailea) {
        new Thread(() -> {
            boolean ondo = datuKudeatzailea.bazkideBerriakEsportatu();
            if (ondo) datuKudeatzailea.bazkideBerriakEsportatuTxt();
            runOnUiThread(() -> {
                if (ondo) {
                    Toast.makeText(this, R.string.esportatu_ondo, Toast.LENGTH_SHORT).show();
                    bidaliPostazXmlTxt("bazkide_berriak.xml", "bazkide_berriak.txt", getString(R.string.postaz_gaia_bazkide_berriak));
                } else {
                    Toast.makeText(this, R.string.esportatu_errorea_batzuetan, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void esportatuEskaeraBerriak(DatuKudeatzailea datuKudeatzailea) {
        new Thread(() -> {
            boolean ondo = datuKudeatzailea.eskaeraBerriakEsportatu();
            if (ondo) datuKudeatzailea.eskaeraBerriakEsportatuTxt();
            runOnUiThread(() -> {
                if (ondo) {
                    Toast.makeText(this, R.string.esportatu_ondo, Toast.LENGTH_SHORT).show();
                    bidaliPostazXmlTxt("eskaera_berriak.xml", "eskaera_berriak.txt", getString(R.string.postaz_gaia_eskaera_berriak));
                } else {
                    Toast.makeText(this, R.string.esportatu_errorea_batzuetan, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void esportatuBazkideak(DatuKudeatzailea datuKudeatzailea) {
        new Thread(() -> {
            boolean ondo = datuKudeatzailea.bazkideakEsportatu();
            runOnUiThread(() -> {
                if (ondo) {
                    Toast.makeText(this, R.string.esportatu_ondo, Toast.LENGTH_SHORT).show();
                    bidaliPostaz("bazkideak.xml", getString(R.string.postaz_gaia_bazkideak), "application/xml");
                } else {
                    Toast.makeText(this, R.string.esportatu_errorea_batzuetan, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /** Esportatu XML (ofiziala): agenda.xml — ordezkaritzak datu-basea eguneratzeko. */
    private void esportatuAgendaXML(AgendaEsportatzailea agendaEsportatzailea) {
        new Thread(() -> {
            boolean ondo = agendaEsportatzailea.agendaXMLSortu();
            runOnUiThread(() -> {
                if (ondo) {
                    Toast.makeText(this, R.string.esportatu_agenda_ondo, Toast.LENGTH_SHORT).show();
                    bidaliPostaz(AgendaEsportatzailea.FITXATEGI_XML, getString(R.string.postaz_gaia_agenda_xml), "application/xml");
                } else {
                    Toast.makeText(this, R.string.esportatu_errorea_batzuetan, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /** Esportatu TXT (kopia irakurgarria): agenda.txt — informazioa azkar irakurtzeko. */
    private void esportatuAgendaTXT(AgendaEsportatzailea agendaEsportatzailea) {
        new Thread(() -> {
            boolean ondo = agendaEsportatzailea.agendaTXTSortu();
            runOnUiThread(() -> {
                if (ondo) {
                    Toast.makeText(this, R.string.esportatu_agenda_ondo, Toast.LENGTH_SHORT).show();
                    bidaliPostaz(AgendaEsportatzailea.FITXATEGI_TXT, getString(R.string.postaz_gaia_agenda_txt), "text/plain");
                } else {
                    Toast.makeText(this, R.string.esportatu_errorea_batzuetan, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /**
     * Esportatutako fitxategia Gmail (edo beste posta-app) bidez bidaltzen du (eranskin gisa).
     * FileProvider erabiltzen du barne-fitxategia content:// URI gisa emateko. Helbidea: gipuzkoa@enpresa.eus.
     */
    private void bidaliPostaz(String fitxategiIzena, String gaia) {
        bidaliPostaz(fitxategiIzena, gaia, "application/xml");
    }

    /**
     * XML eta TXT fitxategiak Gmail bidez bidaltzen ditu (eranskin gisa).
     * Fitxategien egiaztapena: existitzen direla eta hutsik ez daudela (Gmail Intent aurretik).
     */
    private void bidaliPostazXmlTxt(String xmlIzena, String txtIzena, String gaia) {
        ArrayList<Uri> uris = new ArrayList<>();
        File xmlFitx = new File(getFilesDir(), xmlIzena);
        File txtFitx = new File(getFilesDir(), txtIzena);
        try {
            if (xmlFitx.exists() && xmlFitx.length() > 0)
                uris.add(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", xmlFitx));
            if (txtFitx.exists() && txtFitx.length() > 0)
                uris.add(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", txtFitx));
            if (uris.isEmpty()) {
                Toast.makeText(this, R.string.postaz_fitxategi_ez, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("*/*");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            intent.putExtra(Intent.EXTRA_SUBJECT, gaia);
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{EMAIL_ADDRESS});
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent gmail = new Intent(intent).setPackage("com.google.android.gm");
            if (gmail.resolveActivity(getPackageManager()) != null) {
                startActivity(gmail);
            } else {
                startActivity(Intent.createChooser(intent, getString(R.string.postaz_hautatu)));
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.postaz_errorea, e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : getString(R.string.errore_ezezaguna)), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Fitxategia Gmail (edo beste posta-app) bidez bidaltzen du. Gmail Intent aurretik egiaztatzen du
     * fitxategia benetan sortu dela eta ez dagoela hutsik (integritasun-mugak).
     */
    private void bidaliPostaz(String fitxategiIzena, String gaia, String mimeMota) {
        File fitxategia = new File(getFilesDir(), fitxategiIzena);
        if (!fitxategia.exists()) {
            Toast.makeText(this, R.string.postaz_fitxategi_ez, Toast.LENGTH_SHORT).show();
            return;
        }
        if (fitxategia.length() <= 0) {
            Toast.makeText(this, R.string.postaz_fitxategia_hutsik, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", fitxategia);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeMota != null ? mimeMota : "application/xml");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, gaia);
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{EMAIL_ADDRESS});
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent gmail = new Intent(intent).setPackage("com.google.android.gm");
            if (gmail.resolveActivity(getPackageManager()) != null) {
                startActivity(gmail);
            } else {
                startActivity(Intent.createChooser(intent, getString(R.string.postaz_hautatu)));
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.postaz_errorea, e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : getString(R.string.errore_ezezaguna)), Toast.LENGTH_LONG).show();
        }
    }

    private void esportatuKatalogoa(DatuKudeatzailea datuKudeatzailea) {
        new Thread(() -> {
            boolean ondo = datuKudeatzailea.katalogoaEsportatu();
            if (ondo) datuKudeatzailea.katalogoaEsportatuTxt();
            runOnUiThread(() -> {
                if (ondo) {
                    Toast.makeText(this, R.string.esportatu_ondo, Toast.LENGTH_SHORT).show();
                    bidaliPostazXmlTxt("katalogoa.xml", "katalogoa.txt", getString(R.string.postaz_gaia_katalogoa));
                } else {
                    Toast.makeText(this, R.string.esportatu_errorea_batzuetan, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void katalogoaEguneratu(DatuKudeatzailea datuKudeatzailea) {
        new Thread(() -> {
            boolean ondo = datuKudeatzailea.katalogoaEguneratu();
            runOnUiThread(() -> Toast.makeText(this, ondo ? R.string.katalogoa_ondo_berritu_datu_zaharrak : R.string.esportatu_errorea_batzuetan, Toast.LENGTH_LONG).show());
        }).start();
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            showContentForNavId(id);
            return true;
        });
        showContentForNavId(R.id.hasiera);
    }

    private void showContentForNavId(int navId) {
        contentHasiera.setVisibility(navId == R.id.hasiera ? View.VISIBLE : View.GONE);
        contentAgenda.setVisibility(navId == R.id.agenda ? View.VISIBLE : View.GONE);
        contentBazkideak.setVisibility(navId == R.id.bazkideak ? View.VISIBLE : View.GONE);
        contentInventarioa.setVisibility(navId == R.id.inventarioa ? View.VISIBLE : View.GONE);
        if (fabAgendaZitaGehitu != null) {
            fabAgendaZitaGehitu.setVisibility(navId == R.id.agenda ? View.VISIBLE : View.GONE);
        }
        if (fabBazkideaGehitu != null) {
            fabBazkideaGehitu.setVisibility(navId == R.id.bazkideak ? View.VISIBLE : View.GONE);
        }
        if (navId == R.id.agenda) {
            kargatuAgendaZitak();
            erakutsiAgendaXmlFalta();
        }
        if (navId == R.id.bazkideak) {
            erakutsiBazkideakEdukia();
        }
        if (navId == R.id.inventarioa) {
            erakutsiInbentarioaEdukia();
        }
    }

    /** Inbentarioa (Katalogoa) atala: bilatzailea, saskia, RecyclerView. */
    private void erakutsiInbentarioaEdukia() {
        TextView tvXmlFalta = findViewById(R.id.tvInventarioaXmlFalta);
        TextView tvHutsa = findViewById(R.id.tvInventarioaHutsa);
        RecyclerView recyclerKatalogoa = findViewById(R.id.recyclerKatalogoa);
        TextInputEditText etBilatu = findViewById(R.id.etInbentarioaBilatu);
        ImageButton btnSaskia = findViewById(R.id.btnInbentarioaSaskia);
        TextView tvSaskiaKopurua = findViewById(R.id.tvSaskiaKopurua);
        if (tvXmlFalta == null || tvHutsa == null || recyclerKatalogoa == null) return;

        if (XmlBilatzailea.katalogoaFaltaDa(this)) {
            tvXmlFalta.setText(getString(R.string.xml_falta_da, "katalogoa.xml"));
            tvXmlFalta.setVisibility(View.VISIBLE);
            tvHutsa.setVisibility(View.GONE);
            recyclerKatalogoa.setVisibility(View.GONE);
            return;
        }
        tvXmlFalta.setVisibility(View.GONE);

        if (recyclerKatalogoa.getLayoutManager() == null) {
            recyclerKatalogoa.setLayoutManager(new LinearLayoutManager(this));
            KatalogoaAdapter adapter = new KatalogoaAdapter(this);
            adapter.setErosiEntzulea(k -> saskiraGehitu(k));
            adapter.setItemEntzulea(k -> produktuDetalaLauncher.launch(ProduktuDetalaActivity.intentProduktuDetala(this, k.getArtikuluKodea())));
            recyclerKatalogoa.setAdapter(adapter);
        }
        KatalogoaAdapter adapter = (KatalogoaAdapter) recyclerKatalogoa.getAdapter();
        if (etBilatu != null && etBilatu.getTag() == null) {
            etBilatu.setTag(true);
            etBilatu.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    inbentarioaBilatzaileaAplikatu(adapter);
                }
            });
        }
        if (btnSaskia != null) {
            btnSaskia.setOnClickListener(v -> erakutsiSaskiaDialogoa());
        }
        ImageButton btnEskaerak = findViewById(R.id.btnInbentarioaEskaerak);
        if (btnEskaerak != null) {
            btnEskaerak.setOnClickListener(v -> {
                Intent intent = new Intent(this, EskaerakActivity.class);
                startActivity(intent);
            });
        }
        saskiaBadgeEguneratu(tvSaskiaKopurua);

        new Thread(() -> {
            List<Katalogoa> zerrenda = AppDatabase.getInstance(this).katalogoaDao().katalogoaIkusi();
            if (zerrenda == null) zerrenda = new ArrayList<>();
            // Katalogoa hutsik badago edo inongo produktuk ez du irudia_izena, assets-eko katalogoa.xml inportatu (irudiak betetzeko).
            boolean irudiakHutsak = zerrenda.stream().allMatch(k -> k.getIrudiaIzena() == null || k.getIrudiaIzena().trim().isEmpty());
            if (zerrenda.isEmpty() || irudiakHutsak) {
                try {
                    XMLKudeatzailea kud = new XMLKudeatzailea(MainActivity.this);
                    kud.katalogoaInportatu();
                    zerrenda = AppDatabase.getInstance(MainActivity.this).katalogoaDao().katalogoaIkusi();
                    if (zerrenda == null) zerrenda = new ArrayList<>();
                } catch (IOException | XmlPullParserException ignored) { }
            }
            final List<Katalogoa> lista = zerrenda;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                katalogoaOsoa = lista;
                if (lista.isEmpty()) {
                    tvHutsa.setVisibility(View.VISIBLE);
                    recyclerKatalogoa.setVisibility(View.GONE);
                } else {
                    tvHutsa.setVisibility(View.GONE);
                    recyclerKatalogoa.setVisibility(View.VISIBLE);
                    inbentarioaBilatzaileaAplikatu(adapter);
                }
            });
        }).start();
    }

    /** Bilatzailearen testua aplikatu: katalogoaOsoa iragazi eta adapter eguneratu. */
    private void inbentarioaBilatzaileaAplikatu(KatalogoaAdapter adapter) {
        if (adapter == null) return;
        TextInputEditText etBilatu = findViewById(R.id.etInbentarioaBilatu);
        String query = (etBilatu != null && etBilatu.getText() != null) ? etBilatu.getText().toString().trim().toLowerCase(Locale.getDefault()) : "";
        List<Katalogoa> iragazia = new ArrayList<>();
        for (Katalogoa k : katalogoaOsoa) {
            String izena = k.getIzena() != null ? k.getIzena().toLowerCase(Locale.getDefault()) : "";
            String kodea = k.getArtikuluKodea() != null ? k.getArtikuluKodea().toLowerCase(Locale.getDefault()) : "";
            if (query.isEmpty() || izena.contains(query) || kodea.contains(query)) {
                iragazia.add(k);
            }
        }
        adapter.eguneratuZerrenda(iragazia);
    }

    /** Produktu bat saskira gehitu (edo kopurua handitu). Stock 0 bada ez da sartzen; kopurua ezin da stock baino handiagoa. */
    private void saskiraGehitu(Katalogoa k) {
        if (k == null) return;
        int stock = k.getStock();
        if (stock <= 0) {
            Toast.makeText(this, R.string.saskia_stock_0, Toast.LENGTH_SHORT).show();
            return;
        }
        String kodea = k.getArtikuluKodea() != null ? k.getArtikuluKodea() : "";
        for (SaskiaElementua e : saskia) {
            if (kodea.equals(e.artikuluKodea)) {
                e.stock = stock;
                if (e.kopurua >= e.stock) {
                    Toast.makeText(this, R.string.saskia_stock_max, Toast.LENGTH_SHORT).show();
                    return;
                }
                e.kopurua++;
                saskiaBadgeEguneratu(findViewById(R.id.tvSaskiaKopurua));
                Toast.makeText(this, getString(R.string.btn_erosi) + " " + (k.getIzena() != null ? k.getIzena() : ""), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        saskia.add(new SaskiaElementua(k.getArtikuluKodea(), k.getIzena(), k.getSalmentaPrezioa(), k.getIrudiaIzena(), 1, stock));
        saskiaBadgeEguneratu(findViewById(R.id.tvSaskiaKopurua));
        Toast.makeText(this, getString(R.string.btn_erosi) + " " + (k.getIzena() != null ? k.getIzena() : ""), Toast.LENGTH_SHORT).show();
    }

    /** Saskia badge (kopurua) eguneratu. */
    private void saskiaBadgeEguneratu(TextView tvSaskiaKopurua) {
        if (tvSaskiaKopurua == null) return;
        int guztira = 0;
        for (SaskiaElementua e : saskia) guztira += e.kopurua;
        if (guztira > 0) {
            tvSaskiaKopurua.setText(String.valueOf(guztira));
            tvSaskiaKopurua.setVisibility(View.VISIBLE);
        } else {
            tvSaskiaKopurua.setVisibility(View.GONE);
        }
    }

    /** Saskia dialogoa erakutsi: produktuak, kopurua (+/-), prezioa eta guztira. Stock DBtik eguneratu; 0 dutenak kendu. */
    private void erakutsiSaskiaDialogoa() {
        if (saskia.isEmpty()) {
            Toast.makeText(this, R.string.saskia_hutsa, Toast.LENGTH_SHORT).show();
            return;
        }
        AppDatabase db = AppDatabase.getInstance(this);
        new Thread(() -> {
            for (int i = saskia.size() - 1; i >= 0; i--) {
                SaskiaElementua e = saskia.get(i);
                Katalogoa k = db.katalogoaDao().artikuluaBilatu(e.artikuluKodea);
                int stock = k != null ? k.getStock() : 0;
                e.stock = stock;
                if (stock <= 0) {
                    saskia.remove(i);
                } else if (e.kopurua > stock) {
                    e.kopurua = stock;
                }
            }
            runOnUiThread(() -> {
                if (saskia.isEmpty()) {
                    saskiaBadgeEguneratu(findViewById(R.id.tvSaskiaKopurua));
                    Toast.makeText(this, R.string.saskia_stock_0_batzuk_kendu, Toast.LENGTH_SHORT).show();
                    return;
                }
                erakutsiSaskiaDialogoaBis(dialogView -> {
                    RecyclerView recyclerSaskia = dialogView.findViewById(R.id.recyclerSaskia);
                    TextView tvGuztira = dialogView.findViewById(R.id.dialogSaskiaGuztira);
                    MaterialButton btnGarbitu = dialogView.findViewById(R.id.btnSaskiaGarbitu);
                    MaterialButton btnItxi = dialogView.findViewById(R.id.btnSaskiaItxi);
                    MaterialButton btnErosi = dialogView.findViewById(R.id.btnSaskiaErosi);

                    SaskiaAdapter adapter = new SaskiaAdapter(this, saskia);
                    AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

                    Runnable eguneratuGuztiraEtaBadge = () -> {
                        double g = 0;
                        for (SaskiaElementua el : saskia) g += el.salmentaPrezioa * el.kopurua;
                        tvGuztira.setText(String.format(Locale.getDefault(), "%.2f €", g));
                        saskiaBadgeEguneratu(findViewById(R.id.tvSaskiaKopurua));
                        if (saskia.isEmpty()) dialog.dismiss();
                    };
                    adapter.setOnSaskiaAldaketa(eguneratuGuztiraEtaBadge);

                    recyclerSaskia.setLayoutManager(new LinearLayoutManager(this));
                    recyclerSaskia.setAdapter(adapter);
                    eguneratuGuztiraEtaBadge.run();

                    btnGarbitu.setOnClickListener(v -> {
                        saskia.clear();
                        adapter.notifyDataSetChanged();
                        saskiaBadgeEguneratu(findViewById(R.id.tvSaskiaKopurua));
                        dialog.dismiss();
                        Toast.makeText(this, R.string.saskia_garbitu, Toast.LENGTH_SHORT).show();
                    });
                    btnItxi.setOnClickListener(v -> dialog.dismiss());

                    btnErosi.setOnClickListener(v -> {
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.saskia_erosi_baieztatu)
                                .setPositiveButton(R.string.bai, (d, w) -> {
                                    new Thread(() -> {
                                        // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                                        com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                                            new com.example.appkomertziala.segurtasuna.SessionManager(MainActivity.this);
                                        String komertzialKode = sessionManager.getKomertzialKodea();
                                        
                                        if (komertzialKode == null || komertzialKode.isEmpty()) {
                                            runOnUiThread(() -> {
                                                Toast.makeText(MainActivity.this, R.string.saioa_ez_dago_hasita, Toast.LENGTH_LONG).show();
                                            });
                                            return;
                                        }
                                        
                                        String zenbakia = "ESK-" + System.currentTimeMillis();
                                        // Data formatua: yyyy/MM/dd HH:mm
                                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault());
                                        String data = sdf.format(new java.util.Date());
                                        EskaeraGoiburua goi = new EskaeraGoiburua();
                                        goi.setZenbakia(zenbakia);
                                        goi.setData(data);
                                        goi.setKomertzialKodea(komertzialKode.trim());
                                        Komertziala kom = db.komertzialaDao().kodeaBilatu(komertzialKode.trim());
                                        if (kom != null) goi.setKomertzialId(kom.getId());
                                        goi.setOrdezkaritza("");
                                        goi.setBazkideaKodea("");
                                        db.eskaeraGoiburuaDao().txertatu(goi);
                                        for (SaskiaElementua e : saskia) {
                                            EskaeraXehetasuna x = new EskaeraXehetasuna();
                                            x.setEskaeraZenbakia(zenbakia);
                                            x.setArtikuluKodea(e.artikuluKodea);
                                            x.setKantitatea(e.kopurua);
                                            x.setPrezioa(e.salmentaPrezioa);
                                            db.eskaeraXehetasunaDao().txertatu(x);
                                        }
                                        for (SaskiaElementua e : saskia) {
                                            Katalogoa k = db.katalogoaDao().artikuluaBilatu(e.artikuluKodea);
                                            if (k != null) {
                                                int stockBerria = Math.max(0, k.getStock() - e.kopurua);
                                                db.katalogoaDao().stockaEguneratu(e.artikuluKodea, stockBerria);
                                            }
                                        }
                                        runOnUiThread(() -> {
                                            saskia.clear();
                                            adapter.notifyDataSetChanged();
                                            saskiaBadgeEguneratu(findViewById(R.id.tvSaskiaKopurua));
                                            dialog.dismiss();
                                            erakutsiInbentarioaEdukia();
                                            Toast.makeText(this, R.string.saskia_erosketa_eginda, Toast.LENGTH_SHORT).show();
                                        });
                                    }).start();
                                })
                                .setNegativeButton(R.string.ez, null)
                                .show();
                    });

                    dialog.show();
                    if (dialog.getWindow() != null) {
                        int screenH = getResources().getDisplayMetrics().heightPixels;
                        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, (int) (screenH * 0.75));
                    }
                });
            });
        }).start();
    }

    /** Dialogoaren vista sortu eta callback-ean adapter/dialog konfiguratu (erakutsiSaskiaDialogoa-k deitua stock eguneratu ondoren). */
    private void erakutsiSaskiaDialogoaBis(java.util.function.Consumer<View> onReady) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_saskia, null);
        onReady.accept(dialogView);
    }

    /** Saskiaren elementu bat: kodea, izena, prezioa, kopurua, irudia, stock (gehienez). */
    public static class SaskiaElementua {
        public final String artikuluKodea;
        public final String izena;
        public final double salmentaPrezioa;
        public final String irudiaIzena;
        public int kopurua;
        /** Stock uneko (DB); kopurua ezin da honetatik handiagoa izan. */
        public int stock;

        public SaskiaElementua(String artikuluKodea, String izena, double salmentaPrezioa, String irudiaIzena, int kopurua, int stock) {
            this.artikuluKodea = artikuluKodea != null ? artikuluKodea : "";
            this.izena = izena != null ? izena : "";
            this.salmentaPrezioa = salmentaPrezioa;
            this.irudiaIzena = irudiaIzena;
            this.kopurua = kopurua;
            this.stock = stock >= 0 ? stock : 0;
        }
    }

    /** Bazkideak atala: XML falta bada mezu hori; bestela datu-baseko bazkide zerrenda taulan erakutsi. */
    private void erakutsiBazkideakEdukia() {
        TextView tvXmlFalta = findViewById(R.id.tvBazkideakXmlFalta);
        TextView tvHutsa = findViewById(R.id.tvBazkideakHutsa);
        View scrollTable = findViewById(R.id.scroll_table_bazkideak);
        TableLayout tableBazkideak = findViewById(R.id.table_bazkideak);
        if (tvXmlFalta == null || tvHutsa == null || scrollTable == null || tableBazkideak == null) return;

        StringBuilder sb = new StringBuilder();
        if (XmlBilatzailea.bazkideakFaltaDa(this)) {
            sb.append(getString(R.string.xml_falta_da, "bazkideak.xml"));
        }
        if (sb.length() > 0) {
            tvXmlFalta.setText(sb.toString());
            tvXmlFalta.setVisibility(View.VISIBLE);
            tvHutsa.setVisibility(View.GONE);
            tableBazkideak.removeAllViews();
            scrollTable.setVisibility(View.GONE);
            return;
        }
        tvXmlFalta.setVisibility(View.GONE);
        scrollTable.setVisibility(View.VISIBLE);
        setupBazkideakBilatuEtaGehitu();
        kargatuBazkideakZerrenda();
    }

    /** Bazkideak atalean bilatzailea konfiguratu (behin bakarrik). */
    private void setupBazkideakBilatuEtaGehitu() {
        TextInputEditText etBilatu = findViewById(R.id.etBazkideakBilatu);
        if (etBilatu != null && etBilatu.getTag() == null) {
            etBilatu.setTag(true);
            etBilatu.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    kargatuBazkideakZerrenda();
                }
            });
        }
    }

    /** Taula bazkideak (DB) kargatu: bilatzailearen testua aplikatu, taula bete, Editatu/Ezabatu botoiak. */
    private void kargatuBazkideakZerrenda() {
        TextView tvHutsa = findViewById(R.id.tvBazkideakHutsa);
        TableLayout tableBazkideak = findViewById(R.id.table_bazkideak);
        TextInputEditText etBilatu = findViewById(R.id.etBazkideakBilatu);
        if (tvHutsa == null || tableBazkideak == null) return;

        final String filter = etBilatu != null && etBilatu.getText() != null ? etBilatu.getText().toString().trim() : "";

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                List<Bazkidea> zerrenda = filter.isEmpty()
                        ? db.bazkideaDao().guztiak()
                        : db.bazkideaDao().bilatu(filter);
                if (zerrenda == null) zerrenda = new ArrayList<>();
                final List<Bazkidea> lista = zerrenda;

                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    tableBazkideak.removeAllViews();
                    if (lista.isEmpty()) {
                        tvHutsa.setVisibility(View.VISIBLE);
                        return;
                    }
                    tvHutsa.setVisibility(View.GONE);
                    int paddingPx = getResources().getDimensionPixelSize(R.dimen.margin_small);

                    // Goiburua: Izena, Abizena, Telefonoa, Akzioak (gainontzekoa Editatu-n ikusiko da)
                    TableRow headerRow = new TableRow(this);
                    headerRow.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_purple));
                    float[] headerWeights = {1f, 1f, 1f, 0.5f};
                    int[] headerResIds = {
                            R.string.table_bazkide_izena,
                            R.string.table_bazkide_abizena,
                            R.string.table_bazkide_telefonoa,
                            R.string.table_bazkide_akzioak
                    };
                    for (int i = 0; i < headerResIds.length; i++) {
                        TextView h = new TextView(this);
                        h.setText(getString(headerResIds[i]));
                        h.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                        h.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
                        h.setTextSize(12);
                        h.setTypeface(null, android.graphics.Typeface.BOLD);
                        TableRow.LayoutParams lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, headerWeights[i]);
                        h.setLayoutParams(lp);
                        headerRow.addView(h);
                    }
                    tableBazkideak.addView(headerRow);

                    int rowBg = ContextCompat.getColor(this, R.color.card_background);
                    int rowBgAlt = ContextCompat.getColor(this, R.color.background_light_gray);
                    int textColor = ContextCompat.getColor(this, R.color.text_primary);
                    int idx = 0;
                    for (Bazkidea b : lista) {
                        TableRow row = new TableRow(this);
                        row.setBackgroundColor(idx % 2 == 0 ? rowBg : rowBgAlt);
                        row.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.button_height) / 2);
                        String izena = b.getIzena() != null ? b.getIzena() : "";
                        String abizena = b.getAbizena() != null ? b.getAbizena() : "";
                        String telefonoa = b.getTelefonoZenbakia() != null ? b.getTelefonoZenbakia() : "";
                        for (String value : new String[]{izena, abizena, telefonoa}) {
                            TextView cell = new TextView(this);
                            cell.setText(value);
                            cell.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
                            cell.setTextSize(11);
                            cell.setTextColor(textColor);
                            TableRow.LayoutParams lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
                            cell.setLayoutParams(lp);
                            row.addView(cell);
                        }
                        LinearLayout akzioak = new LinearLayout(this);
                        akzioak.setOrientation(LinearLayout.HORIZONTAL);
                        akzioak.setGravity(android.view.Gravity.CENTER);
                        int btnMargin = getResources().getDimensionPixelSize(R.dimen.margin_small) / 2;
                        int iconSizePx = getResources().getDimensionPixelSize(R.dimen.icon_size_small);

                        MaterialButton btnEditatu = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                        btnEditatu.setMinimumWidth(0);
                        btnEditatu.setMinimumHeight(0);
                        btnEditatu.setPadding(btnMargin, btnMargin, btnMargin, btnMargin);
                        btnEditatu.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_edit_24));
                        btnEditatu.setIconSize(iconSizePx);
                        btnEditatu.setIconPadding(0);
                        btnEditatu.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_purple)));
                        btnEditatu.setContentDescription(getString(R.string.btn_editatu));
                        btnEditatu.setOnClickListener(v -> {
                            Intent intent = new Intent(this, BazkideaFormularioActivity.class);
                            intent.putExtra(BazkideaFormularioActivity.EXTRA_BAZKIDEA_ID, b.getId());
                            bazkideaFormularioLauncher.launch(intent);
                        });
                        LinearLayout.LayoutParams lpEdit = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lpEdit.setMarginEnd(btnMargin);
                        akzioak.addView(btnEditatu, lpEdit);

                        MaterialButton btnEzabatu = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                        btnEzabatu.setMinimumWidth(0);
                        btnEzabatu.setMinimumHeight(0);
                        btnEzabatu.setPadding(btnMargin, btnMargin, btnMargin, btnMargin);
                        btnEzabatu.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_delete_24));
                        btnEzabatu.setIconSize(iconSizePx);
                        btnEzabatu.setIconPadding(0);
                        btnEzabatu.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error_red)));
                        btnEzabatu.setContentDescription(getString(R.string.btn_ezabatu));
                        btnEzabatu.setOnClickListener(v -> ezabatuBazkidea(b));
                        akzioak.addView(btnEzabatu);

                        TableRow.LayoutParams lpAkzioak = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.5f);
                        row.addView(akzioak, lpAkzioak);
                        tableBazkideak.addView(row);
                        idx++;
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isDestroyed()) {
                        Toast.makeText(this, getString(R.string.esportatu_errorea_batzuetan), Toast.LENGTH_LONG).show();
                        tvHutsa.setVisibility(View.VISIBLE);
                        tableBazkideak.removeAllViews();
                    }
                });
            }
        }).start();
    }

    /** Bazkidea ezabatu baieztapenarekin. */
    private void ezabatuBazkidea(Bazkidea b) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_ezabatu)
                .setMessage(R.string.bazkidea_ezabatu_baieztatu)
                .setPositiveButton(R.string.bai, (dialog, which) -> {
                    new Thread(() -> {
                        AppDatabase.getInstance(this).bazkideaDao().ezabatu(b);
                        runOnUiThread(() -> {
                            Toast.makeText(this, R.string.bazkidea_ondo_ezabatuta, Toast.LENGTH_SHORT).show();
                            kargatuBazkideakZerrenda();
                        });
                    }).start();
                })
                .setNegativeButton(R.string.ez, null)
                .show();
    }

    /** Agenda atalean: beharrezko XMLak falta badira, Bazkideak/Inventarioa moduan mezu bat erakutsi. */
    private void erakutsiAgendaXmlFalta() {
        TextView tvAgendaXmlFalta = findViewById(R.id.tvAgendaXmlFalta);
        if (tvAgendaXmlFalta == null) return;
        StringBuilder sb = new StringBuilder();
        if (XmlBilatzailea.bazkideakFaltaDa(this)) {
            sb.append(getString(R.string.xml_falta_da, "bazkideak.xml"));
        }
        if (sb.length() > 0) {
            tvAgendaXmlFalta.setText(sb.toString());
            tvAgendaXmlFalta.setVisibility(View.VISIBLE);
        } else {
            tvAgendaXmlFalta.setVisibility(View.GONE);
        }
    }

    private void setupContactButtons() {
        btnMap.setOnClickListener(v -> openMap());
        btnCall.setOnClickListener(v -> openPhone());
        btnEmail.setOnClickListener(v -> openEmail());
    }

    private void openMap() {
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + MAP_QUERY);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Uri fallback = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + MAP_QUERY);
            startActivity(new Intent(Intent.ACTION_VIEW, fallback));
        }
    }

    /** Deitu botoia: telefono dialerra ireki zenbaki horrekin (deia egiteko). */
    private void openPhone() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + PHONE_NUMBER));
        try {
            startActivity(callIntent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.deia_errorea, Toast.LENGTH_SHORT).show();
        }
    }

    /** Posta botoia: posta bezeroa ireki helbide horrekin (korreoa bidaltzeko). */
    private void openEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + EMAIL_ADDRESS));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{EMAIL_ADDRESS});
        try {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.button_email)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.posta_errorea, Toast.LENGTH_SHORT).show();
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
}
