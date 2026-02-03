package com.example.appkomertziala;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appkomertziala.db.AgendaRepository;
import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Agenda;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Agenda moduluaren pantaila nagusia: bisiten zerrenda (RecyclerView), bisita berria FAB,
 * bilaketa funtzioa (data edo bezeroaren arabera), eta Esportatu XML / TXT / CSV botoiak.
 * Repository pattern erabiliz, UI azkarra eta autoritarioa.
 */
public class AgendaModuluaActivity extends AppCompatActivity implements AgendaBisitaAdapter.OnBisitaEkintzaListener {

    private static final String HELBIDE_POSTA = "gipuzkoa@enpresa.eus";

    private RecyclerView errecyclerAgendaBisitak;
    private TextView tvAgendaModuluaHutsa;
    private TextInputEditText etBilatu;
    private AgendaBisitaAdapter adapter;
    private AppDatabase datuBasea;
    private AgendaRepository repository;

    private final ActivityResultLauncher<Intent> bisitaFormularioLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    kargatuZerrenda();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda_modulua);

        setTitle(getString(R.string.agenda_modulua_izenburua));
        datuBasea = AppDatabase.getInstance(this);
        repository = new AgendaRepository(this);

        errecyclerAgendaBisitak = findViewById(R.id.errecyclerAgendaBisitak);
        tvAgendaModuluaHutsa = findViewById(R.id.tvAgendaModuluaHutsa);
        etBilatu = findViewById(R.id.etBilatuBisitak);
        ExtendedFloatingActionButton fabBisitaBerria = findViewById(R.id.fabAgendaBisitaBerria);

        adapter = new AgendaBisitaAdapter(this);
        errecyclerAgendaBisitak.setLayoutManager(new LinearLayoutManager(this));
        errecyclerAgendaBisitak.setAdapter(adapter);

        // Bilaketa funtzioa konfiguratu
        konfiguratuBilaketa();

        fabBisitaBerria.setOnClickListener(v -> irekiFormularioa(-1));
        findViewById(R.id.btnAgendaEsportatuXml).setOnClickListener(v -> esportatuEtaBidali());
        findViewById(R.id.btnAgendaEsportatuTxt).setOnClickListener(v -> esportatuEtaBidali());

        kargatuZerrenda();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.itxi();
        }
    }

    /** Bilaketa funtzioa konfiguratu: testua sartzean bilaketa automatikoki exekutatzen da. */
    private void konfiguratuBilaketa() {
        if (etBilatu != null) {
            etBilatu.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String filter = s.toString().trim();
                    if (filter.isEmpty()) {
                        kargatuZerrenda(); // Guztiak kargatu
                    } else {
                        bilatu(filter); // Bilaketa exekutatu
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        kargatuZerrenda();
    }

    /** Zerrenda datu-baseatik kargatu eta adapter eguneratu (Repository pattern erabiliz). */
    private void kargatuZerrenda() {
        repository.kargatuBisitak(bisitak -> {
            if (bisitak == null) bisitak = new ArrayList<>();
            List<AgendaBisitaAdapter.AgendaElementua> erakusteko = new ArrayList<>();
            for (Agenda a : bisitak) {
                String bazkideaIzena = bazkidearenIzenaLortu(a.getBazkideaKodea());
                String ordua = a.getOrdua() != null && !a.getOrdua().trim().isEmpty() ? a.getOrdua() : "";
                String dataEtaOrdua = a.getBisitaData() != null ? a.getBisitaData() : "";
                if (!ordua.isEmpty()) {
                    dataEtaOrdua += " " + ordua;
                }
                erakusteko.add(new AgendaBisitaAdapter.AgendaElementua(
                        a.getId(),
                        dataEtaOrdua,
                        bazkideaIzena,
                        a.getDeskribapena(),
                        a.getEgoera()));
            }
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                adapter.eguneratuZerrenda(erakusteko);
                boolean hutsa = erakusteko.isEmpty();
                tvAgendaModuluaHutsa.setVisibility(hutsa ? View.VISIBLE : View.GONE);
                errecyclerAgendaBisitak.setVisibility(hutsa ? View.GONE : View.VISIBLE);
            });
        });
    }

    /** Bilaketa exekutatu (data, bazkidea izena/kodea, deskribapena, egoera eremuen artean). */
    private void bilatu(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            kargatuZerrenda();
            return;
        }
        
        // Bilaketa orokorra erabili: data, bazkidea izena/kodea, deskribapena, egoera
        repository.bilatuOrokorra(filter.trim(), bisitak -> {
            erakutsiBilaketaEmaitzak(bisitak);
        });
    }

    /** Bilaketa emaitzak erakutsi. */
    private void erakutsiBilaketaEmaitzak(List<Agenda> bisitak) {
        if (bisitak == null) bisitak = new ArrayList<>();
        List<AgendaBisitaAdapter.AgendaElementua> erakusteko = new ArrayList<>();
        for (Agenda a : bisitak) {
            String bazkideaIzena = bazkidearenIzenaLortu(a.getBazkideaKodea());
            String ordua = a.getOrdua() != null && !a.getOrdua().trim().isEmpty() ? a.getOrdua() : "";
            String dataEtaOrdua = a.getBisitaData() != null ? a.getBisitaData() : "";
            if (!ordua.isEmpty()) {
                dataEtaOrdua += " " + ordua;
            }
            erakusteko.add(new AgendaBisitaAdapter.AgendaElementua(
                    a.getId(),
                    dataEtaOrdua,
                    bazkideaIzena,
                    a.getDeskribapena(),
                    a.getEgoera()));
        }
        runOnUiThread(() -> {
            if (isDestroyed()) return;
            adapter.eguneratuZerrenda(erakusteko);
            boolean hutsa = erakusteko.isEmpty();
            tvAgendaModuluaHutsa.setVisibility(hutsa ? View.VISIBLE : View.GONE);
            errecyclerAgendaBisitak.setVisibility(hutsa ? View.GONE : View.VISIBLE);
        });
    }

    /** Bazkidearen izena lortu kodea erabiliz. */
    private String bazkidearenIzenaLortu(String bazkideaKodea) {
        if (bazkideaKodea == null || bazkideaKodea.trim().isEmpty()) {
            return "";
        }
        try {
            Bazkidea b = datuBasea.bazkideaDao().nanBilatu(bazkideaKodea.trim());
            if (b != null) {
                String izena = (b.getIzena() != null ? b.getIzena().trim() : "") + 
                               (b.getAbizena() != null && !b.getAbizena().trim().isEmpty() ? " " + b.getAbizena().trim() : "");
                return izena.isEmpty() ? (b.getNan() != null ? b.getNan() : "") : izena;
            }
        } catch (Exception e) {
            // Errorea log-ean erregistratu baina ez erakutsi erabiltzaileari
        }
        return bazkideaKodea;
    }

    @Override
    public void onIkusi(AgendaBisitaAdapter.AgendaElementua elementua) {
        repository.bilatuBisitaIdz(elementua.id, bisita -> {
            if (bisita == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.bisita_ez_da_aurkitu, Toast.LENGTH_SHORT).show();
                });
                return;
            }
            
            String bazkideaIzena = bazkidearenIzenaLortu(bisita.getBazkideaKodea());
            String komertzialaIzena = "";
            if (bisita.getKomertzialKodea() != null && !bisita.getKomertzialKodea().trim().isEmpty()) {
                try {
                    com.example.appkomertziala.db.eredua.Komertziala k = datuBasea.komertzialaDao().kodeaBilatu(bisita.getKomertzialKodea().trim());
                    if (k != null) {
                        komertzialaIzena = k.getIzena() != null ? k.getIzena().trim() : "";
                    }
                } catch (Exception e) {
                    // Errorea log-ean erregistratu
                }
            }
            
            StringBuilder mezua = new StringBuilder();
            mezua.append(getString(R.string.agenda_bisita_data)).append(": ").append(bisita.getBisitaData() != null ? bisita.getBisitaData() : "").append("\n");
            if (bisita.getOrdua() != null && !bisita.getOrdua().trim().isEmpty()) {
                mezua.append(getString(R.string.zita_ordua)).append(": ").append(bisita.getOrdua()).append("\n");
            }
            if (!komertzialaIzena.isEmpty()) {
                mezua.append(getString(R.string.komertziala)).append(": ").append(komertzialaIzena).append("\n");
            }
            mezua.append(getString(R.string.agenda_bisita_partnerra)).append(": ").append(bazkideaIzena).append("\n");
            mezua.append(getString(R.string.agenda_bisita_deskribapena)).append(": ").append(bisita.getDeskribapena() != null ? bisita.getDeskribapena() : "").append("\n");
            mezua.append(getString(R.string.agenda_bisita_egoera)).append(": ").append(bisita.getEgoera() != null ? bisita.getEgoera() : "");
            
            String mezuaFinal = mezua.toString();
            runOnUiThread(() -> {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.bisita_ikusi_izenburua)
                        .setMessage(mezuaFinal)
                        .setPositiveButton(R.string.ados, null)
                        .show();
            });
        });
    }

    @Override
    public void onEditatu(AgendaBisitaAdapter.AgendaElementua elementua) {
        irekiFormularioa(elementua.id);
    }

    @Override
    public void onEzabatu(AgendaBisitaAdapter.AgendaElementua elementua) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_ezabatu)
                .setMessage(R.string.bisita_ezabatu_baieztatu)
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
                        
                        // SEGURTASUNA: idzBilatuSegurua eta ezabatuSegurua erabili
                        Agenda a = datuBasea.agendaDao().idzBilatuSegurua(elementua.id, komertzialKodea);
                        if (a != null) {
                            int emaitza = datuBasea.agendaDao().ezabatuSegurua(elementua.id, komertzialKodea);
                            runOnUiThread(() -> {
                                if (emaitza > 0) {
                                    Toast.makeText(this, R.string.bisita_ezabatu_ondo, Toast.LENGTH_SHORT).show();
                                    kargatuZerrenda();
                                } else {
                                    Toast.makeText(this, R.string.bisita_ezabatu_errorea, Toast.LENGTH_LONG).show();
                                }
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(this, R.string.bisita_ez_da_aurkitu_sarbiderik, Toast.LENGTH_LONG).show();
                            });
                        }
                    }).start();
                })
                .setNegativeButton(R.string.ez, null)
                .show();
    }

    /** Bisita formularioa ireki (id >= 0 editatzeko, -1 berria gehitzeko). Komertzialaren kodea bidaltzen da (sortzailearen ID erresolbatzeko). Gorde ondoren zerrenda eguneratzen da. */
    private void irekiFormularioa(long bisitaId) {
        Intent intent = new Intent(this, BisitaFormularioActivity.class);
        intent.putExtra(BisitaFormularioActivity.EXTRA_BISITA_ID, bisitaId);
        String komertzialKode = getIntent() != null ? getIntent().getStringExtra(MainActivity.EXTRA_KOMMERTZIALA_KODEA) : null;
        if (komertzialKode != null) intent.putExtra(MainActivity.EXTRA_KOMMERTZIALA_KODEA, komertzialKode);
        bisitaFormularioLauncher.launch(intent);
    }

    /**
     * Bi fitxategiak (XML eta TXT) barne-memorian sortu eta Gmail bidez bidali.
     * Gaia: [Techno Basque] Hileroko Agenda - HILABETEA.
     * FileProvider erabiltzen du barne-memoriako fitxategiak content:// URI gisa emateko.
     */
    private void esportatuEtaBidali() {
        new Thread(() -> {
            try {
                AgendaHileroEsportatzailea esportatzailea = new AgendaHileroEsportatzailea(this);
                boolean ondo = esportatzailea.esportatuUnekoHilabetea();
                runOnUiThread(() -> {
                    if (ondo) {
                        Toast.makeText(this, R.string.esportatu_agenda_ondo, Toast.LENGTH_SHORT).show();
                        bidaliPostazBiEranskin();
                    } else {
                        Toast.makeText(this, R.string.esportatu_errorea_batzuetan, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.esportatu_errorea_batzuetan, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Barne-memorian dauden agenda.xml, agenda.txt eta agenda.csv fitxategiak
     * eranskin gisa bidaltzen ditu Gmail (edo beste posta-app) bidez.
     * Helmuga: gipuzkoa@enpresa.eus. Gaia: [Techno Basque] Hileroko Agenda - HILABETEA.
     */
    private void bidaliPostazBiEranskin() {
        File karpeta = getFilesDir();
        File xmlFitx = new File(karpeta, AgendaHileroEsportatzailea.FITXATEGI_XML);
        File txtFitx = new File(karpeta, AgendaHileroEsportatzailea.FITXATEGI_TXT);
        File csvFitx = new File(karpeta, AgendaHileroEsportatzailea.FITXATEGI_CSV);
        
        if (!xmlFitx.exists() && !txtFitx.exists() && !csvFitx.exists()) {
            Toast.makeText(this, R.string.postaz_fitxategi_ez, Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<Uri> uriak = new ArrayList<>();
        try {
            String pakeIzena = getPackageName();
            if (xmlFitx.exists() && xmlFitx.length() > 0) {
                uriak.add(androidx.core.content.FileProvider.getUriForFile(this, pakeIzena + ".fileprovider", xmlFitx));
            }
            if (txtFitx.exists() && txtFitx.length() > 0) {
                uriak.add(androidx.core.content.FileProvider.getUriForFile(this, pakeIzena + ".fileprovider", txtFitx));
            }
            if (csvFitx.exists() && csvFitx.length() > 0) {
                uriak.add(androidx.core.content.FileProvider.getUriForFile(this, pakeIzena + ".fileprovider", csvFitx));
            }
            if (uriak.isEmpty()) {
                Toast.makeText(this, R.string.postaz_fitxategi_ez, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Hilabetearen izena lortu
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            String[] hilabeteIzenak = {"Urtarrila", "Otsaila", "Martxoa", "Apirila", "Maiatza", "Ekaina",
                    "Uztaila", "Abuztua", "Iraila", "Urria", "Azaroa", "Abendua"};
            String hilabetea = hilabeteIzenak[calendar.get(java.util.Calendar.MONTH)];
            String gaia = getString(R.string.postaz_gaia_hilero, hilabetea);
            
            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("*/*");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriak);
            intent.putExtra(Intent.EXTRA_SUBJECT, gaia);
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{HELBIDE_POSTA});
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
}
