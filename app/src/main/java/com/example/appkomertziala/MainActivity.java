package com.example.appkomertziala;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.content.FileProvider;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.EskaeraGoiburua;
import com.example.appkomertziala.db.eredua.Partnerra;
import com.example.appkomertziala.xml.DatuKudeatzailea;
import com.example.appkomertziala.xml.XmlBilatzailea;
import com.example.appkomertziala.AgendaEsportatzailea;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    private View contentHasiera;
    private View contentAgenda;
    private View contentBazkideak;
    private View contentInventarioa;
    private BottomNavigationView bottomNav;
    private ExtendedFloatingActionButton fabAgendaCitaGehitu;
    private ImageButton btnMap;
    private ImageButton btnCall;
    private ImageButton btnEmail;

    private final ActivityResultLauncher<Intent> citaGehituLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    kargatuAgendaZitak();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentHasiera = findViewById(R.id.content_hasiera);
        contentAgenda = findViewById(R.id.content_agenda);
        contentBazkideak = findViewById(R.id.content_bazkideak);
        contentInventarioa = findViewById(R.id.content_inventarioa);
        bottomNav = findViewById(R.id.bottom_nav);
        fabAgendaCitaGehitu = findViewById(R.id.fabAgendaCitaGehitu);
        btnMap = findViewById(R.id.btnMap);
        btnCall = findViewById(R.id.btnCall);
        btnEmail = findViewById(R.id.btnEmail);

        setupMap();
        setupBottomNav();
        setupContactButtons();
        setupEsportazioBotoiak();
        setupAgendaCitaGehitu();
        erakutsiEsportazioBidea();
    }

    /** Agenda fitxako Extended FAB: zita berria gehitzeko pantaila ireki. */
    private void setupAgendaCitaGehitu() {
        if (fabAgendaCitaGehitu != null) {
            fabAgendaCitaGehitu.setOnClickListener(v -> {
                String kode = getIntent() != null ? getIntent().getStringExtra(EXTRA_KOMMERTZIALA_KODEA) : null;
                Intent intent = new Intent(this, CitaGehituActivity.class);
                intent.putExtra(CitaGehituActivity.EXTRA_KOMMERTZIALA_KODEA, kode != null ? kode : "");
                citaGehituLauncher.launch(intent);
            });
        }
    }

    /** Agendako zita zerrenda kargatu eta erakutsi (komertzialaren arabera). */
    private void kargatuAgendaZitak() {
        String k = getIntent() != null ? getIntent().getStringExtra(EXTRA_KOMMERTZIALA_KODEA) : null;
        final String komertzialKode = (k == null || k.isEmpty()) ? "" : k;
        LinearLayout listContainer = findViewById(R.id.list_agenda_zitak);
        TextView tvHutsa = findViewById(R.id.tvAgendaZitakHutsa);
        if (listContainer == null || tvHutsa == null) return;

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<EskaeraGoiburua> zitak = db.eskaeraGoiburuaDao().komertzialarenEskaerak(komertzialKode);
            if (zitak == null) zitak = new ArrayList<>();

            ArrayList<String[]> erakusteko = new ArrayList<>();
            for (EskaeraGoiburua goi : zitak) {
                String dataStr = goi.getData() != null ? goi.getData() : "";
                String partnerIzena = "";
                if (goi.getPartnerKodea() != null && !goi.getPartnerKodea().trim().isEmpty()) {
                    Partnerra p = db.partnerraDao().kodeaBilatu(goi.getPartnerKodea().trim());
                    partnerIzena = p != null && p.getIzena() != null ? p.getIzena() : goi.getPartnerKodea();
                }
                String ordezk = goi.getOrdezkaritza() != null ? goi.getOrdezkaritza().trim() : "";
                String zenb = goi.getZenbakia() != null ? goi.getZenbakia() : "";
                erakusteko.add(new String[]{dataStr, zenb, partnerIzena.isEmpty() ? "—" : partnerIzena, ordezk});
            }

            runOnUiThread(() -> {
                listContainer.removeAllViews();
                if (erakusteko.isEmpty()) {
                    tvHutsa.setVisibility(View.VISIBLE);
                    return;
                }
                tvHutsa.setVisibility(View.GONE);
                LayoutInflater inflater = getLayoutInflater();
                for (String[] row : erakusteko) {
                    View item = inflater.inflate(R.layout.item_zita, listContainer, false);
                    ((TextView) item.findViewById(R.id.itemZitaData)).setText(row[0]);
                    ((TextView) item.findViewById(R.id.itemZitaZenbakia)).setText(row[1]);
                    ((TextView) item.findViewById(R.id.itemZitaPartnerra)).setText(row[2]);
                    TextView tvOrdezk = item.findViewById(R.id.itemZitaOrdezkaritza);
                    if (!row[3].isEmpty()) {
                        tvOrdezk.setText(row[3]);
                        tvOrdezk.setVisibility(View.VISIBLE);
                    }
                    final String zenbakia = row[1];
                    final String dataStr = row[0];
                    final String partnerIzena = row[2];
                    final String ordezk = row[3];
                    MaterialButton btnIkusi = item.findViewById(R.id.btnZitaIkusi);
                    MaterialButton btnEditatu = item.findViewById(R.id.btnZitaEditatu);
                    MaterialButton btnEzabatu = item.findViewById(R.id.btnZitaEzabatu);
                    btnIkusi.setOnClickListener(v -> erakutsiZitaXehetasunak(dataStr, zenbakia, partnerIzena, ordezk));
                    btnEditatu.setOnClickListener(v -> editatuZita(zenbakia));
                    btnEzabatu.setOnClickListener(v -> ezabatuZita(zenbakia));
                    listContainer.addView(item);
                }
            });
        }).start();
    }

    /** Zitaren xehetasunak dialogoan erakusten du (Ikusi botoia). */
    private void erakutsiZitaXehetasunak(String dataStr, String zenbakia, String partnerIzena, String ordezk) {
        StringBuilder msg = new StringBuilder();
        msg.append(getString(R.string.cita_data)).append(": ").append(dataStr).append("\n");
        msg.append(getString(R.string.cita_zenbakia)).append(": ").append(zenbakia).append("\n");
        msg.append(getString(R.string.cita_partnerra)).append(": ").append(partnerIzena).append("\n");
        if (ordezk != null && !ordezk.isEmpty()) {
            msg.append(getString(R.string.cita_ordezkaritza)).append(": ").append(ordezk);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.zita_ikusi_izenburua)
                .setMessage(msg.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /** Zita editatzeko CitaGehituActivity ireki (Editatu botoia). */
    private void editatuZita(String zenbakia) {
        String kode = getIntent() != null ? getIntent().getStringExtra(EXTRA_KOMMERTZIALA_KODEA) : null;
        Intent intent = new Intent(this, CitaGehituActivity.class);
        intent.putExtra(CitaGehituActivity.EXTRA_KOMMERTZIALA_KODEA, kode != null ? kode : "");
        intent.putExtra(CitaGehituActivity.EXTRA_ESKAERA_ZENBAKIA, zenbakia);
        citaGehituLauncher.launch(intent);
    }

    /** Zita ezabatu baieztapenarekin (Ezabatu botoia). */
    private void ezabatuZita(String zenbakia) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_ezabatu)
                .setMessage(R.string.zita_ezabatu_baieztatu)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    new Thread(() -> {
                        AppDatabase db = AppDatabase.getInstance(this);
                        EskaeraGoiburua goi = db.eskaeraGoiburuaDao().zenbakiaBilatu(zenbakia);
                        if (goi != null) {
                            db.eskaeraGoiburuaDao().ezabatu(goi);
                            runOnUiThread(() -> {
                                Toast.makeText(this, R.string.zita_ezabatu_ondo, Toast.LENGTH_SHORT).show();
                                kargatuAgendaZitak();
                            });
                        }
                    }).start();
                })
                .setNegativeButton(android.R.string.no, null)
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
        MaterialButton btnBazkideBerriak = findViewById(R.id.btnEsportatuBazkideBerriak);
        MaterialButton btnEskaeraBerriak = findViewById(R.id.btnEsportatuEskaeraBerriak);
        MaterialButton btnEsportatuAgendaXml = findViewById(R.id.btnEsportatuAgendaXml);
        MaterialButton btnEsportatuAgendaTxt = findViewById(R.id.btnEsportatuAgendaTxt);
        MaterialButton btnEsportatuKatalogoa = findViewById(R.id.btnEsportatuKatalogoa);
        MaterialButton btnKatalogoaEguneratu = findViewById(R.id.btnKatalogoaEguneratu);
        btnBazkideBerriak.setOnClickListener(v -> esportatuBazkideBerriak(datuKudeatzailea));
        btnEskaeraBerriak.setOnClickListener(v -> esportatuEskaeraBerriak(datuKudeatzailea));
        btnEsportatuAgendaXml.setOnClickListener(v -> esportatuAgendaXML(agendaEsportatzailea));
        btnEsportatuAgendaTxt.setOnClickListener(v -> esportatuAgendaTXT(agendaEsportatzailea));
        btnEsportatuKatalogoa.setOnClickListener(v -> esportatuKatalogoa(datuKudeatzailea));
        btnKatalogoaEguneratu.setOnClickListener(v -> katalogoaEguneratu(datuKudeatzailea));
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

    /** Esportatu XML (ofiziala): agenda_hilero.xml — ordezkaritzak datu-basea eguneratzeko. */
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

    /** Esportatu TXT (kopia irakurgarria): agenda_oharra.txt — informazioa azkar irakurtzeko. */
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

    /** XML eta TXT fitxategiak Gmail bidez bidaltzen ditu (bi eranskin). */
    private void bidaliPostazXmlTxt(String xmlIzena, String txtIzena, String gaia) {
        ArrayList<Uri> uris = new ArrayList<>();
        File xmlFitx = new File(getFilesDir(), xmlIzena);
        File txtFitx = new File(getFilesDir(), txtIzena);
        try {
            if (xmlFitx.exists()) uris.add(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", xmlFitx));
            if (txtFitx.exists()) uris.add(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", txtFitx));
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
            Toast.makeText(this, getString(R.string.postaz_errorea, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void bidaliPostaz(String fitxategiIzena, String gaia, String mimeMota) {
        File fitxategia = new File(getFilesDir(), fitxategiIzena);
        if (!fitxategia.exists()) {
            Toast.makeText(this, R.string.postaz_fitxategi_ez, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, getString(R.string.postaz_errorea, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void esportatuKatalogoa(DatuKudeatzailea datuKudeatzailea) {
        new Thread(() -> {
            boolean ondo = datuKudeatzailea.katalogoaEsportatu();
            if (ondo) datuKudeatzailea.katalogoaEsportatuTxt();
            runOnUiThread(() -> {
                if (ondo) {
                    Toast.makeText(this, R.string.esportatu_ondo, Toast.LENGTH_SHORT).show();
                    bidaliPostazXmlTxt("katalogoa_esportazioa.xml", "katalogoa_esportazioa.txt", getString(R.string.postaz_gaia_katalogoa));
                } else {
                    Toast.makeText(this, R.string.esportatu_errorea_batzuetan, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void katalogoaEguneratu(DatuKudeatzailea datuKudeatzailea) {
        new Thread(() -> {
            boolean ondo = datuKudeatzailea.katalogoaEguneratu();
            runOnUiThread(() -> Toast.makeText(this, ondo ? R.string.katalogoa_eguneratu_ondo : R.string.esportatu_errorea_batzuetan, Toast.LENGTH_SHORT).show());
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
        if (fabAgendaCitaGehitu != null) {
            fabAgendaCitaGehitu.setVisibility(navId == R.id.agenda ? View.VISIBLE : View.GONE);
        }
        if (navId == R.id.agenda) {
            kargatuAgendaZitak();
        }
        // Bazkideak / Inventarioa: falta den XML bat bada, mezu hori erakutsi
        if (navId == R.id.bazkideak && contentBazkideak instanceof android.widget.TextView) {
            StringBuilder sb = new StringBuilder();
            if (XmlBilatzailea.bazkideakFaltaDa(this)) {
                sb.append(getString(R.string.xml_falta_da, "bazkideak.xml"));
            }
            if (XmlBilatzailea.partnerrakFaltaDa(this)) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(getString(R.string.xml_falta_da, "partnerrak.xml"));
            }
            ((android.widget.TextView) contentBazkideak).setText(
                    sb.length() > 0 ? sb.toString() : getString(R.string.tab_bazkideak_placeholder));
        }
        if (navId == R.id.inventarioa && contentInventarioa instanceof android.widget.TextView) {
            String text = XmlBilatzailea.katalogoaFaltaDa(this)
                    ? getString(R.string.xml_falta_da, "katalogoa.xml")
                    : getString(R.string.tab_inventarioa_placeholder);
            ((android.widget.TextView) contentInventarioa).setText(text);
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
        // Checkpoint (marker) Gipuzkoa egoitzan
        googleMap.addMarker(new MarkerOptions()
                .position(donostia)
                .title(getString(R.string.contact_title)));
    }
}
