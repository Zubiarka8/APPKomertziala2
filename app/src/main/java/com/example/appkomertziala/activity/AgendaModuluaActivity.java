package com.example.appkomertziala.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appkomertziala.MainActivity;
import com.example.appkomertziala.R;
import com.example.appkomertziala.agenda.AgendaBisitaAdapter;
import com.example.appkomertziala.agenda.AgendaHileroEsportatzailea;
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
 * 
 * Hau hemen agenda moduluaren pantaila nagusia da - bisita guztiak erakusten ditu,
 * bilaketa egin daiteke, bisita berria gehitu daiteke, eta esportatu eta postaz bidali.
 * 
 * Repository pattern erabiliz, UI azkarra eta autoritarioa - datu-base kontsultak
 * asinkronoki egiten dira eta UI azkar mantentzen da.
 */
public class AgendaModuluaActivity extends AppCompatActivity implements AgendaBisitaAdapter.OnBisitaEkintzaListener {

    private static final String ETIKETA = "AgendaModuluaActivity";
    
    /** Posta helbidea esportazio fitxategiak bidaltzeko. */
    private static final String HELBIDE_POSTA = "gipuzkoa@enpresa.eus";

    /** RecyclerView bisitak erakusteko. */
    private RecyclerView errecyclerAgendaBisitak;
    
    /** Testu erakusteko bisitak hutsik badira. */
    private TextView tvAgendaModuluaHutsa;
    
    /** Bilaketa eremua (TextInputEditText). */
    private TextInputEditText etBilatu;
    
    /** Adapter bisita zerrenda erakusteko. */
    private AgendaBisitaAdapter adapter;
    
    /** Datu-basea (Room). */
    private AppDatabase datuBasea;
    
    /** Repository bisita kontsultak egiteko (Repository pattern). */
    private AgendaRepository repository;

    /** Bisita formularioa itzultzean: zerrenda eguneratu (bisita berria edo editatua). */
    private final ActivityResultLauncher<Intent> bisitaFormularioLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Bisita gorde bada (RESULT_OK), zerrenda eguneratu
                if (result.getResultCode() == RESULT_OK) {
                    kargatuZerrenda();
                }
            });

    /**
     * Activity sortzean: UI elementuak kargatu, adapter konfiguratu,
     * bilaketa funtzioa konfiguratu, eta bisita zerrenda kargatu.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda_modulua);

        // Izenburua jarri
        setTitle(getString(R.string.agenda_modulua_izenburua));
        
        // Datu-basea eta repository sortu - hau hemen badago, dena ondo doa
        datuBasea = AppDatabase.getInstance(this);
        repository = new AgendaRepository(this);

        // UI elementuak kargatu
        errecyclerAgendaBisitak = findViewById(R.id.errecyclerAgendaBisitak);
        tvAgendaModuluaHutsa = findViewById(R.id.tvAgendaModuluaHutsa);
        etBilatu = findViewById(R.id.etBilatuBisitak);
        ExtendedFloatingActionButton fabBisitaBerria = findViewById(R.id.fabAgendaBisitaBerria);

        // Adapter sortu eta konfiguratu - RecyclerView-ri lotu
        adapter = new AgendaBisitaAdapter(this);
        errecyclerAgendaBisitak.setLayoutManager(new LinearLayoutManager(this));
        errecyclerAgendaBisitak.setAdapter(adapter);

        // Bilaketa funtzioa konfiguratu - testua sartzean automatikoki bilatu
        konfiguratuBilaketa();

        // Botoien listener-ak konfiguratu
        fabBisitaBerria.setOnClickListener(v -> irekiFormularioa(-1)); // Bisita berria gehitu
        findViewById(R.id.btnAgendaEsportatuTxt).setOnClickListener(v -> esportatuEtaBidali()); // Esportatu TXT

        // Bisita zerrenda kargatu
        kargatuZerrenda();
    }

    /**
     * Activity destruituzean: repository itxi (errendimendua).
     * 
     * Repository-k executor service bat erabiltzen du - itxi behar da
     * memoria ihesak saihesteko.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.itxi();
        }
    }

    /**
     * Bilaketa funtzioa konfiguratu: testua sartzean bilaketa automatikoki exekutatzen da.
     * 
     * TextWatcher erabiliz, erabiltzaileak testua sartzen duenean automatikoki
     * bilaketa exekutatzen da. Testua hutsik badago, zerrenda osoa kargatzen da.
     */
    private void konfiguratuBilaketa() {
        if (etBilatu != null) {
            etBilatu.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Ez dugu ezer egin behar
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Ez dugu ezer egin behar
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // Testua sartu ondoren: bilaketa exekutatu
                    String filter = s.toString().trim();
                    if (filter.isEmpty()) {
                        // Testua hutsik badago, zerrenda osoa kargatu
                        kargatuZerrenda();
                    } else {
                        // Testua badago, bilaketa exekutatu
                        bilatu(filter);
                    }
                }
            });
        }
    }

    /**
     * Activity berriro agertzean: zerrenda eguneratu.
     * 
     * onResume()-an zerrenda eguneratzen da, beste Activity batetik itzultzean
     * datu berriak erakusteko.
     */
    @Override
    protected void onResume() {
        super.onResume();
        kargatuZerrenda();
    }

    /**
     * Zerrenda datu-baseatik kargatu eta adapter eguneratu (Repository pattern erabiliz).
     * 
     * Repository-k bisitak kargatzen ditu asinkronoki, eta callback bidez
     * emaitzak jasotzen dira. Bisita bakoitzarentzat bazkidearen izena lortzen
     * da kodea erabiliz, eta data eta ordua batera erakusten dira.
     */
    private void kargatuZerrenda() {
        // Repository-k bisitak kargatzen ditu asinkronoki
        repository.kargatuBisitak(bisitak -> {
            // Bisitak null bada, zerrenda hutsa erabili
            if (bisitak == null) bisitak = new ArrayList<>();
            
            // Bisita bakoitzarentzat elementua sortu
            List<AgendaBisitaAdapter.AgendaElementua> erakusteko = new ArrayList<>();
            for (Agenda a : bisitak) {
                // Bazkidearen izena lortu kodea erabiliz - begiratu hemen ea bazkidea badago
                String bazkideaIzena = bazkidearenIzenaLortu(a.getBazkideaKodea());
                
                // Ordua lortu eta data-rekin batera erakutsi
                String ordua = a.getOrdua() != null && !a.getOrdua().trim().isEmpty() ? a.getOrdua() : "";
                String dataEtaOrdua = a.getBisitaData() != null ? a.getBisitaData() : "";
                if (!ordua.isEmpty()) {
                    dataEtaOrdua += " " + ordua;
                }
                
                // Elementua sortu eta zerrendara gehitu
                erakusteko.add(new AgendaBisitaAdapter.AgendaElementua(
                        a.getId(),
                        dataEtaOrdua,
                        bazkideaIzena,
                        a.getDeskribapena(),
                        a.getEgoera()));
            }
            
            // UI eguneratu - hilo nagusian
            runOnUiThread(() -> {
                // Activity destruitu bada, ezer ez egin
                if (isDestroyed()) return;
                
                // Adapter eguneratu
                adapter.eguneratuZerrenda(erakusteko);
                
                // Hutsik badago, mezua erakutsi; bestela zerrenda
                boolean hutsa = erakusteko.isEmpty();
                tvAgendaModuluaHutsa.setVisibility(hutsa ? View.VISIBLE : View.GONE);
                errecyclerAgendaBisitak.setVisibility(hutsa ? View.GONE : View.VISIBLE);
            });
        });
    }

    /**
     * Bilaketa exekutatu (data, bazkidea izena/kodea, deskribapena, egoera eremuen artean).
     * 
     * Repository-k bilaketa orokorra egiten du - data, bazkidea izena/kodea,
     * deskribapena eta egoera eremuen artean bilatzen du.
     * 
     * @param filter Bilaketa testua
     */
    private void bilatu(String filter) {
        // Filter hutsik badago, zerrenda osoa kargatu
        if (filter == null || filter.trim().isEmpty()) {
            kargatuZerrenda();
            return;
        }
        
        // Bilaketa orokorra erabili: data, bazkidea izena/kodea, deskribapena, egoera
        // Honek mapan jartzen gaitu - bilaketa emaitzak erakusteko
        repository.bilatuOrokorra(filter.trim(), bisitak -> {
            erakutsiBilaketaEmaitzak(bisitak);
        });
    }

    /**
     * Bilaketa emaitzak erakutsi: bisitak elementu bihurtu eta adapter eguneratu.
     * 
     * Bilaketa emaitzak jaso eta elementu bihurtzen ditu, gero adapter eguneratzen du.
     * 
     * @param bisitak Bilaketa emaitzak (Agenda zerrenda)
     */
    private void erakutsiBilaketaEmaitzak(List<Agenda> bisitak) {
        // Bisitak null bada, zerrenda hutsa erabili
        if (bisitak == null) bisitak = new ArrayList<>();
        
        // Bisita bakoitzarentzat elementua sortu
        List<AgendaBisitaAdapter.AgendaElementua> erakusteko = new ArrayList<>();
        for (Agenda a : bisitak) {
            // Bazkidearen izena lortu kodea erabiliz
            String bazkideaIzena = bazkidearenIzenaLortu(a.getBazkideaKodea());
            
            // Ordua lortu eta data-rekin batera erakutsi
            String ordua = a.getOrdua() != null && !a.getOrdua().trim().isEmpty() ? a.getOrdua() : "";
            String dataEtaOrdua = a.getBisitaData() != null ? a.getBisitaData() : "";
            if (!ordua.isEmpty()) {
                dataEtaOrdua += " " + ordua;
            }
            
            // Elementua sortu eta zerrendara gehitu
            erakusteko.add(new AgendaBisitaAdapter.AgendaElementua(
                    a.getId(),
                    dataEtaOrdua,
                    bazkideaIzena,
                    a.getDeskribapena(),
                    a.getEgoera()));
        }
        
        // UI eguneratu - hilo nagusian
        runOnUiThread(() -> {
            // Activity destruitu bada, ezer ez egin
            if (isDestroyed()) return;
            
            // Adapter eguneratu
            adapter.eguneratuZerrenda(erakusteko);
            
            // Hutsik badago, mezua erakutsi; bestela zerrenda
            boolean hutsa = erakusteko.isEmpty();
            tvAgendaModuluaHutsa.setVisibility(hutsa ? View.VISIBLE : View.GONE);
            errecyclerAgendaBisitak.setVisibility(hutsa ? View.GONE : View.VISIBLE);
        });
    }

    /**
     * Bazkidearen izena lortu kodea erabiliz.
     * 
     * Bazkidea kodea (NAN) erabiliz datu-basean bilatu eta izen osoa lortzen du.
     * Izena eta abizena batera erakusten ditu, hutsik badago kodea erakusten du.
     * 
     * @param bazkideaKodea Bazkidearen kodea (NAN)
     * @return Bazkidearen izen osoa edo kodea ez badago aurkitu
     */
    private String bazkidearenIzenaLortu(String bazkideaKodea) {
        // Kodea hutsik badago, hutsik itzuli
        if (bazkideaKodea == null || bazkideaKodea.trim().isEmpty()) {
            return "";
        }
        
        try {
            // Bazkidea datu-baseatik bilatu kodea erabiliz
            Bazkidea b = datuBasea.bazkideaDao().nanBilatu(bazkideaKodea.trim());
            if (b != null) {
                // Izena eta abizena batera erakutsi
                String izena = (b.getIzena() != null ? b.getIzena().trim() : "") + 
                               (b.getAbizena() != null && !b.getAbizena().trim().isEmpty() ? " " + b.getAbizena().trim() : "");
                // Izena hutsik badago, kodea erabili
                return izena.isEmpty() ? (b.getNan() != null ? b.getNan() : "") : izena;
            }
        } catch (Exception e) {
            // Errorea log-ean erregistratu baina ez erakutsi erabiltzaileari
            Log.w(ETIKETA, "Errorea bazkidearen izena lortzean: " + bazkideaKodea, e);
        }
        
        // Bazkidea ez badago aurkitu, kodea itzuli
        return bazkideaKodea;
    }

    /**
     * Bisita ikusi: bisita xehetasunak erakutsi AlertDialog batean.
     * 
     * Adapter-eko "Ikusi" botoia sakatzean deitzen da. Bisita datu guztiak
     * erakusten ditu: data, ordua, komertziala, bazkidea, deskribapena, egoera.
     * 
     * @param elementua Bisita elementua (adapter-etik)
     */
    @Override
    public void onIkusi(AgendaBisitaAdapter.AgendaElementua elementua) {
        // Repository-k bisita bilatzen du ID-ren arabera
        repository.bilatuBisitaIdz(elementua.id, bisita -> {
            // Bisita null bada, errorea erakutsi
            if (bisita == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.bisita_ez_da_aurkitu, Toast.LENGTH_SHORT).show();
                });
                return;
            }
            
            // Bazkidearen izena lortu kodea erabiliz
            String bazkideaIzena = bazkidearenIzenaLortu(bisita.getBazkideaKodea());
            
            // Komertzialaren izena lortu kodea erabiliz
            String komertzialaIzena = "";
            if (bisita.getKomertzialKodea() != null && !bisita.getKomertzialKodea().trim().isEmpty()) {
                try {
                    com.example.appkomertziala.db.eredua.Komertziala k = datuBasea.komertzialaDao().kodeaBilatu(bisita.getKomertzialKodea().trim());
                    if (k != null) {
                        komertzialaIzena = k.getIzena() != null ? k.getIzena().trim() : "";
                    }
                } catch (Exception e) {
                    // Errorea log-ean erregistratu
                    Log.w(ETIKETA, "Errorea komertzialaren izena lortzean", e);
                }
            }
            
            // Mezua sortu - bisita datu guztiak
            StringBuilder mezua = new StringBuilder();
            mezua.append(getString(R.string.agenda_bisita_data)).append(": ").append(bisita.getBisitaData() != null ? bisita.getBisitaData() : "").append("\n");
            if (bisita.getOrdua() != null && !bisita.getOrdua().trim().isEmpty()) {
                mezua.append(getString(R.string.zita_ordua)).append(": ").append(bisita.getOrdua()).append("\n");
            }
            if (!komertzialaIzena.isEmpty()) {
                mezua.append(getString(R.string.komertziala)).append(": ").append(komertzialaIzena).append("\n");
            }
            mezua.append(getString(R.string.agenda_bisita_bazkidea)).append(": ").append(bazkideaIzena).append("\n");
            mezua.append(getString(R.string.agenda_bisita_deskribapena)).append(": ").append(bisita.getDeskribapena() != null ? bisita.getDeskribapena() : "").append("\n");
            mezua.append(getString(R.string.agenda_bisita_egoera)).append(": ").append(bisita.getEgoera() != null ? bisita.getEgoera() : "");
            
            // AlertDialog erakutsi - hilo nagusian
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

    /**
     * Bisita editatu: bisita formularioa ireki editatzeko.
     * 
     * Adapter-eko "Editatu" botoia sakatzean deitzen da.
     * 
     * @param elementua Bisita elementua (adapter-etik)
     */
    @Override
    public void onEditatu(AgendaBisitaAdapter.AgendaElementua elementua) {
        irekiFormularioa(elementua.id);
    }

    /**
     * Bisita ezabatu: baieztapen dialogo erakutsi, gero ezabatu.
     * 
     * Adapter-eko "Ezabatu" botoia sakatzean deitzen da.
     * SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu,
     * eta bakarrik bere bisitak ezabatu ditzake (idzBilatuSegurua, ezabatuSegurua).
     * 
     * @param elementua Bisita elementua (adapter-etik)
     */
    @Override
    public void onEzabatu(AgendaBisitaAdapter.AgendaElementua elementua) {
        // Baieztapen dialogo erakutsi - segurtasuna bermatzeko
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_ezabatu)
                .setMessage(R.string.bisita_ezabatu_baieztatu)
                .setPositiveButton(R.string.bai, (dialog, which) -> {
                    // Ezabatu - hilo nagusitik kanpo
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
                                    Toast.makeText(this, R.string.saioa_ez_dago_hasita, Toast.LENGTH_LONG).show();
                                });
                                return;
                            }
                            
                            // SEGURTASUNA: idzBilatuSegurua eta ezabatuSegurua erabili
                            // Hau hemen badago, dena ondo doa - bakarrik uneko komertzialaren bisitak ezabatu
                            Agenda a = datuBasea.agendaDao().idzBilatuSegurua(elementua.id, komertzialKodea);
                            if (a != null) {
                                // Bisita aurkitu da eta uneko komertzialarena da - ezabatu
                                int emaitza = datuBasea.agendaDao().ezabatuSegurua(elementua.id, komertzialKodea);
                                runOnUiThread(() -> {
                                    if (emaitza > 0) {
                                        // Arrakasta - zerrenda eguneratu
                                        Toast.makeText(this, R.string.bisita_ezabatu_ondo, Toast.LENGTH_SHORT).show();
                                        kargatuZerrenda();
                                    } else {
                                        // Errorea ezabatzean
                                        Toast.makeText(this, R.string.bisita_ezabatu_errorea, Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                // Bisita ez da aurkitu edo ez duzu sarbiderik
                                runOnUiThread(() -> {
                                    Toast.makeText(this, R.string.bisita_ez_da_aurkitu_sarbiderik, Toast.LENGTH_LONG).show();
                                });
                            }
                        } catch (Exception e) {
                            Log.e(ETIKETA, "Errorea bisita ezabatzean", e);
                            runOnUiThread(() -> {
                                Toast.makeText(this, R.string.bisita_ezabatu_errorea, Toast.LENGTH_LONG).show();
                            });
                        }
                    }).start();
                })
                .setNegativeButton(R.string.ez, null)
                .show();
    }

    /**
     * Bisita formularioa ireki (id >= 0 editatzeko, -1 berria gehitzeko).
     * 
     * Komertzialaren kodea bidaltzen da (sortzailearen ID erresolbatzeko).
     * Gorde ondoren zerrenda eguneratzen da (bisitaFormularioLauncher callback).
     * 
     * @param bisitaId Bisita ID (>= 0 editatzeko, -1 berria gehitzeko)
     */
    private void irekiFormularioa(long bisitaId) {
        Intent intent = new Intent(this, BisitaFormularioActivity.class);
        intent.putExtra(BisitaFormularioActivity.EXTRA_BISITA_ID, bisitaId);
        
        // Komertzial kodea bidali (sortzailearen ID erresolbatzeko)
        String komertzialKode = getIntent() != null ? getIntent().getStringExtra(MainActivity.EXTRA_KOMMERTZIALA_KODEA) : null;
        if (komertzialKode != null) intent.putExtra(MainActivity.EXTRA_KOMMERTZIALA_KODEA, komertzialKode);
        
        // Formularioa ireki - callback bidez zerrenda eguneratuko da
        bisitaFormularioLauncher.launch(intent);
    }

    /**
     * Bi fitxategiak (XML eta TXT) barne-memorian sortu eta Gmail bidez bidali.
     * 
     * Goazen esportazio honekin egurra ematera - uneko hilabeteko bisitak esportatzen
     * ditu XML, TXT eta CSV formatuetan, eta gero postaz bidaltzen ditu.
     * 
     * Gaia: [Techno Basque] Hileroko Agenda - HILABETEA.
     * FileProvider erabiltzen du barne-memoriako fitxategiak content:// URI gisa emateko.
     */
    private void esportatuEtaBidali() {
        new Thread(() -> {
            try {
                // AgendaHileroEsportatzailea sortu eta uneko hilabetea esportatu
                AgendaHileroEsportatzailea esportatzailea = new AgendaHileroEsportatzailea(this);
                boolean ondo = esportatzailea.esportatuUnekoHilabetea();
                
                runOnUiThread(() -> {
                    if (ondo) {
                        // Esportazioa ondo - postaz bidali
                        Toast.makeText(this, R.string.esportatu_agenda_ondo, Toast.LENGTH_SHORT).show();
                        bidaliPostazBiEranskin();
                    } else {
                        // Esportazioan akatsa
                        Toast.makeText(this, R.string.esportatu_errorea_batzuetan, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                // Errorea log-ean erregistratu eta erabiltzaileari erakutsi
                Log.e(ETIKETA, "Errorea esportatzean", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.esportatu_errorea_batzuetan, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Barne-memorian dauden agenda.txt eta agenda.csv fitxategiak
     * eranskin gisa bidaltzen ditu Gmail (edo beste posta-app) bidez.
     * 
     * Goazen esportazio honekin egurra ematera - fitxategiak postaz bidaltzen ditu.
     * FileProvider erabiltzen du barne-memoriako fitxategiak content:// URI gisa emateko.
     * 
     * Helmuga: gipuzkoa@enpresa.eus.
     * Gaia: [Techno Basque] Hileroko Agenda - HILABETEA.
     */
    private void bidaliPostazBiEranskin() {
        // Fitxategiak barne-memorian bilatu
        File karpeta = getFilesDir();
        File txtFitx = new File(karpeta, AgendaHileroEsportatzailea.FITXATEGI_TXT);
        File csvFitx = new File(karpeta, AgendaHileroEsportatzailea.FITXATEGI_CSV);
        
        // Fitxategirik ez badago, errorea erakutsi
        if (!txtFitx.exists() && !csvFitx.exists()) {
            Toast.makeText(this, R.string.postaz_fitxategi_ez, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // URI-ak sortu FileProvider erabiliz
        ArrayList<Uri> uriak = new ArrayList<>();
        try {
            String pakeIzena = getPackageName();
            
            // TXT fitxategia existitzen bada eta hutsik ez bada, URI gehitu
            if (txtFitx.exists() && txtFitx.length() > 0) {
                uriak.add(androidx.core.content.FileProvider.getUriForFile(this, pakeIzena + ".fileprovider", txtFitx));
            }
            
            // CSV fitxategia existitzen bada eta hutsik ez bada, URI gehitu
            if (csvFitx.exists() && csvFitx.length() > 0) {
                uriak.add(androidx.core.content.FileProvider.getUriForFile(this, pakeIzena + ".fileprovider", csvFitx));
            }
            
            // URI-rik ez badago, errorea erakutsi
            if (uriak.isEmpty()) {
                Toast.makeText(this, R.string.postaz_fitxategi_ez, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Hilabetearen izena lortu - gaia sortzeko
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            String[] hilabeteIzenak = {"Urtarrila", "Otsaila", "Martxoa", "Apirila", "Maiatza", "Ekaina",
                    "Uztaila", "Abuztua", "Iraila", "Urria", "Azaroa", "Abendua"};
            String hilabetea = hilabeteIzenak[calendar.get(java.util.Calendar.MONTH)];
            String gaia = getString(R.string.postaz_gaia_hilero, hilabetea);
            
            // Intent sortu postaz bidaltzeko - ACTION_SEND_MULTIPLE erabiliz
            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("*/*");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriak);
            intent.putExtra(Intent.EXTRA_SUBJECT, gaia);
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{HELBIDE_POSTA});
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Gmail aplikazioa existitzen bada, hori erabili; bestela aukeratzailea erakutsi
            Intent gmail = new Intent(intent).setPackage("com.google.android.gm");
            if (gmail.resolveActivity(getPackageManager()) != null) {
                startActivity(gmail);
            } else {
                startActivity(Intent.createChooser(intent, getString(R.string.postaz_hautatu)));
            }
        } catch (Exception e) {
            // Errorea log-ean erregistratu eta erabiltzaileari erakutsi
            Log.e(ETIKETA, "Errorea postaz bidaltzean", e);
            Toast.makeText(this, getString(R.string.postaz_errorea, e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : getString(R.string.errore_ezezaguna)), Toast.LENGTH_LONG).show();
        }
    }
}
