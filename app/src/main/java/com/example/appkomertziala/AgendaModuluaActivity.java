package com.example.appkomertziala;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Agenda;
import com.example.appkomertziala.db.eredua.Partnerra;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Agenda moduluaren pantaila nagusia: bisiten zerrenda (RecyclerView), bisita berria FAB,
 * eta Esportatu XML / Esportatu TXT botoiak. Gmail bidez bi eranskin bidaltzeko aukera.
 */
public class AgendaModuluaActivity extends AppCompatActivity implements AgendaBisitaAdapter.OnBisitaEkintzaListener {

    private static final String HELBIDE_POSTA = "gipuzkoa@enpresa.eus";

    private RecyclerView errecyclerAgendaBisitak;
    private TextView tvAgendaModuluaHutsa;
    private AgendaBisitaAdapter adapter;
    private AppDatabase datuBasea;

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

        errecyclerAgendaBisitak = findViewById(R.id.errecyclerAgendaBisitak);
        tvAgendaModuluaHutsa = findViewById(R.id.tvAgendaModuluaHutsa);
        ExtendedFloatingActionButton fabBisitaBerria = findViewById(R.id.fabAgendaBisitaBerria);

        adapter = new AgendaBisitaAdapter(this);
        errecyclerAgendaBisitak.setLayoutManager(new LinearLayoutManager(this));
        errecyclerAgendaBisitak.setAdapter(adapter);

        fabBisitaBerria.setOnClickListener(v -> irekiFormularioa(-1));
        findViewById(R.id.btnAgendaEsportatuXml).setOnClickListener(v -> esportatuEtaBidali());
        findViewById(R.id.btnAgendaEsportatuTxt).setOnClickListener(v -> esportatuEtaBidali());

        kargatuZerrenda();
    }

    @Override
    protected void onResume() {
        super.onResume();
        kargatuZerrenda();
    }

    /** Zerrenda datu-baseatik kargatu eta adapter eguneratu. */
    private void kargatuZerrenda() {
        new Thread(() -> {
            try {
                List<Agenda> guztiak = datuBasea.agendaDao().guztiak();
                if (guztiak == null) guztiak = new ArrayList<>();
                List<AgendaBisitaAdapter.AgendaElementua> erakusteko = new ArrayList<>();
                for (Agenda a : guztiak) {
                    String partnerIzena = "";
                    if (a.getPartnerKodea() != null && !a.getPartnerKodea().trim().isEmpty()) {
                        Partnerra p = datuBasea.partnerraDao().kodeaBilatu(a.getPartnerKodea().trim());
                        partnerIzena = p != null && p.getIzena() != null ? p.getIzena() : a.getPartnerKodea();
                    }
                    erakusteko.add(new AgendaBisitaAdapter.AgendaElementua(
                            a.getId(),
                            a.getBisitaData(),
                            partnerIzena,
                            a.getDeskribapena(),
                            a.getEgoera()));
                }
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    adapter.eguneratuZerrenda(erakusteko);
                    boolean hutsa = erakusteko.isEmpty();
                    tvAgendaModuluaHutsa.setVisibility(hutsa ? View.VISIBLE : View.GONE);
                    errecyclerAgendaBisitak.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isDestroyed()) {
                        android.widget.Toast.makeText(this, getString(R.string.esportatu_errorea_batzuetan), android.widget.Toast.LENGTH_LONG).show();
                        tvAgendaModuluaHutsa.setVisibility(View.VISIBLE);
                        adapter.eguneratuZerrenda(new ArrayList<>());
                    }
                });
            }
        }).start();
    }

    @Override
    public void onIkusi(AgendaBisitaAdapter.AgendaElementua elementua) {
        StringBuilder mezua = new StringBuilder();
        mezua.append(getString(R.string.agenda_bisita_data)).append(": ").append(elementua.bisitaData).append("\n");
        mezua.append(getString(R.string.agenda_bisita_partnerra)).append(": ").append(elementua.partnerIzena).append("\n");
        mezua.append(getString(R.string.agenda_bisita_deskribapena)).append(": ").append(elementua.deskribapena).append("\n");
        mezua.append(getString(R.string.agenda_bisita_egoera)).append(": ").append(elementua.egoera);
        new AlertDialog.Builder(this)
                .setTitle(R.string.bisita_ikusi_izenburua)
                .setMessage(mezua.toString())
                .setPositiveButton(R.string.ados, null)
                .show();
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
                        Agenda a = datuBasea.agendaDao().idzBilatu(elementua.id);
                        if (a != null) {
                            datuBasea.agendaDao().ezabatu(a);
                            runOnUiThread(() -> {
                                Toast.makeText(this, R.string.bisita_ezabatu_ondo, Toast.LENGTH_SHORT).show();
                                kargatuZerrenda();
                            });
                        }
                    }).start();
                })
                .setNegativeButton(R.string.ez, null)
                .show();
    }

    /** Bisita formularioa ireki (id >= 0 editatzeko, -1 berria gehitzeko). Gorde ondoren zerrenda eguneratzen da. */
    private void irekiFormularioa(long bisitaId) {
        Intent intent = new Intent(this, BisitaFormularioActivity.class);
        intent.putExtra(BisitaFormularioActivity.EXTRA_BISITA_ID, bisitaId);
        bisitaFormularioLauncher.launch(intent);
    }

    /**
     * Bi fitxategiak (XML eta TXT) barne-memorian sortu eta Gmail bidez bidali.
     * Gaia: [Techno Basque] Hileroko Agenda - HILABETEA.
     * FileProvider erabiltzen du barne-memoriako fitxategiak content:// URI gisa emateko.
     */
    private void esportatuEtaBidali() {
        AgendaHileroEsportatzailea esportatzailea = new AgendaHileroEsportatzailea(this);
        boolean xmlOndo = esportatzailea.agendaXMLSortu();
        boolean txtOndo = esportatzailea.agendaTXTSortu();
        if (!xmlOndo || !txtOndo) {
            Toast.makeText(this, R.string.esportatu_errorea_batzuetan, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.esportatu_agenda_ondo, Toast.LENGTH_SHORT).show();
        bidaliPostazBiEranskin();
    }

    /**
     * Barne-memorian dauden agenda.xml eta agenda.txt fitxategiak
     * eranskin gisa bidaltzen ditu Gmail (edo beste posta-app) bidez.
     * Helmuga: gipuzkoa@enpresa.eus. Gaia: [Techno Basque] Hileroko Agenda - HILABETEA.
     */
    private void bidaliPostazBiEranskin() {
        File karpeta = getFilesDir();
        File xmlFitx = new File(karpeta, AgendaHileroEsportatzailea.FITXATEGI_XML);
        File txtFitx = new File(karpeta, AgendaHileroEsportatzailea.FITXATEGI_TXT);
        if (!xmlFitx.exists() && !txtFitx.exists()) {
            Toast.makeText(this, R.string.postaz_fitxategi_ez, Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<Uri> uriak = new ArrayList<>();
        try {
            String pakeIzena = getPackageName();
            if (xmlFitx.exists()) {
                uriak.add(androidx.core.content.FileProvider.getUriForFile(this, pakeIzena + ".fileprovider", xmlFitx));
            }
            if (txtFitx.exists()) {
                uriak.add(androidx.core.content.FileProvider.getUriForFile(this, pakeIzena + ".fileprovider", txtFitx));
            }
            if (uriak.isEmpty()) {
                Toast.makeText(this, R.string.postaz_fitxategi_ez, Toast.LENGTH_SHORT).show();
                return;
            }
            String hilabetea = AgendaHileroEsportatzailea.unekoHilabetearenIzena();
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
