package com.example.appkomertziala.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appkomertziala.R;
import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.example.appkomertziala.xml.DatuKudeatzailea;

import java.util.ArrayList;
import java.util.List;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Bazkide berria gehitzeko edo lehendik dagoen bazkidea editatzeko formularioa.
 * 
 * Hau hemen bazkide berria gehitu edo lehendik dagoen bazkidea editatu dezakegu.
 * Formularioa bete behar da: NAN, izena, abizena, telefonoa, posta, jaiotze data, argazkia.
 * Gorde sakatzean, lehen bazkideak.xml eguneratzen da eta gero datu-basea.
 * 
 * Egitura: bazkideak.xml (NAN, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia).
 * Datuen balidazioa: NAN eta izena beharrezkoak; erroreak eremuetan eta Toast bidez.
 */
public class BazkideaFormularioActivity extends AppCompatActivity {

    private static final String ETIKETA = "BazkideaFormulario";

    /** Editatzeko: bazkidearen id (Intent extra). Zero baino txikiagoa bada berria da. */
    public static final String EXTRA_BAZKIDEA_ID = "bazkidea_id";

    /** TextInputLayout-ak erroreak erakusteko (NAN eta izena). */
    private TextInputLayout tilNan;
    private TextInputLayout tilIzena;
    
    /** TextInputEditText-ak formulario eremuak (NAN, izena, abizena, telefonoa, posta, jaiotze data, argazkia). */
    private TextInputEditText etNan, etIzena, etAbizena, etTelefonoa, etPosta, etJaiotzeData, etArgazkia;
    
    /** Datu-basea (Room). */
    private AppDatabase datuBasea;
    
    /** Editatzeko bazkidearen ID. -1 bada berria da, >= 0 bada editatzen ari gara. */
    private long editatuId = -1;

    /**
     * Activity sortzean: UI elementuak kargatu, editatu ID lortu,
     * eta editatzen badago bazkidea kargatu (hilo nagusitik kanpo).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bazkidea_formulario);

        // Datu-basea eta editatu ID lortu - hau hemen badago, dena ondo doa
        datuBasea = AppDatabase.getInstance(this);
        editatuId = getIntent() != null ? getIntent().getLongExtra(EXTRA_BAZKIDEA_ID, -1) : -1;
        
        // Izenburua jarri - editatzen badago "Editatu", bestela "Berria"
        setTitle(editatuId >= 0 ? getString(R.string.bazkidea_editatu) : getString(R.string.bazkidea_berria));

        // UI elementuak kargatu - findViewById guztiak hemen
        tilNan = findViewById(R.id.tilBazkideaNan);
        tilIzena = findViewById(R.id.tilBazkideaIzena);
        etNan = findViewById(R.id.etBazkideaNan);
        etIzena = findViewById(R.id.etBazkideaIzena);
        etAbizena = findViewById(R.id.etBazkideaAbizena);
        etTelefonoa = findViewById(R.id.etBazkideaTelefonoa);
        etPosta = findViewById(R.id.etBazkideaPosta);
        etJaiotzeData = findViewById(R.id.etBazkideaJaiotzeData);
        etArgazkia = findViewById(R.id.etBazkideaArgazkia);
        MaterialButton btnGorde = findViewById(R.id.btnBazkideaGorde);
        MaterialButton btnEzabatu = findViewById(R.id.btnBazkideaEzabatu);

        // Botoien listener-ak konfiguratu
        btnGorde.setOnClickListener(v -> erakutsiGordeBaieztapena());
        btnEzabatu.setOnClickListener(v -> erakutsiEzabatuBaieztapena());

        // Editatzen badago, bazkidea kargatu eta formularioa bete
        if (editatuId >= 0) {
            // Ezabatu botoia erakutsi - editatzen badago bakarrik
            btnEzabatu.setVisibility(View.VISIBLE);
            
            // Bazkidea datu-baseatik kargatu (hilo nagusitik kanpo)
            new Thread(() -> {
                try {
                    Bazkidea b = datuBasea.bazkideaDao().idzBilatu(editatuId);
                    if (b != null) {
                        // Formularioa bete - hilo nagusian
                        runOnUiThread(() -> {
                            etNan.setText(b.getNan());
                            etIzena.setText(b.getIzena());
                            etAbizena.setText(b.getAbizena());
                            etTelefonoa.setText(b.getTelefonoZenbakia());
                            etPosta.setText(b.getPosta());
                            etJaiotzeData.setText(b.getJaiotzeData());
                            etArgazkia.setText(b.getArgazkia());
                        });
                    } else {
                        Log.w(ETIKETA, "Bazkidea ez da aurkitu id=" + editatuId);
                    }
                } catch (Exception e) {
                    Log.e(ETIKETA, "Errorea bazkidea kargatzean", e);
                }
            }).start();
        }
    }

    /**
     * Gorde botoia sakatzean: datuak balidatu, erroreak erakutsi;
     * balidoak badira baieztapen dialogo erakutsi, gero gorde.
     * 
     * Balidazioa: NAN eta izena beharrezkoak dira. Hutsik badira,
     * erroreak eremuetan erakusten dira (TextInputLayout.setError).
     */
    private void erakutsiGordeBaieztapena() {
        // Erroreak garbitu - balidazio berri baino lehen
        if (tilNan != null) tilNan.setError(null);
        if (tilIzena != null) tilIzena.setError(null);
        
        // Datuak atera eta trim egin
        String nan = etNan.getText() != null ? etNan.getText().toString().trim() : "";
        String izena = etIzena.getText() != null ? etIzena.getText().toString().trim() : "";
        
        // Balidazioa: NAN eta izena beharrezkoak dira
        boolean baliogabea = false;
        if (nan.isEmpty()) {
            if (tilNan != null) tilNan.setError(getString(R.string.bazkidea_errorea_nan_beharrezkoa));
            baliogabea = true;
        }
        if (izena.isEmpty()) {
            if (tilIzena != null) tilIzena.setError(getString(R.string.bazkidea_errorea_izena_beharrezkoa));
            baliogabea = true;
        }
        
        // Balidazioak huts egin badu, errorea erakutsi eta itzuli
        if (baliogabea) {
            Toast.makeText(this, getString(R.string.bazkidea_errorea_nan_beharrezkoa), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Datuak balidoak dira - baieztapen dialogo erakutsi
        new AlertDialog.Builder(this)
                .setMessage(R.string.bazkidea_gorde_baieztatu)
                .setPositiveButton(R.string.bai, (dialog, which) -> gordeBazkidea())
                .setNegativeButton(R.string.ez, null)
                .show();
    }

    /**
     * Ezabatu botoia sakatzean: baieztapen dialogo erakutsi, gero ezabatu.
     * 
     * Segurtasuna: baieztapen dialogo erakusten du ezabatu baino lehen,
     * erabiltzaileak baieztatu behar du.
     */
    private void erakutsiEzabatuBaieztapena() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.bazkidea_ezabatu_baieztatu)
                .setPositiveButton(R.string.bai, (dialog, which) -> ezabatuBazkidea())
                .setNegativeButton(R.string.ez, null)
                .show();
    }

    /**
     * Bazkidea ezabatu: lehen bazkideak.xml eguneratu (bazkidea zerrendatik kendu), gero datu-basea.
     * 
     * Prozesua:
     * 1. Bazkidea datu-baseatik bilatu
     * 2. Bazkide zerrenda lortu (guztiak)
     * 3. Ezabatu behar den bazkidea zerrendatik kendu
     * 4. XML esportatu zerrenda berriarekin
     * 5. Datu-baseatik ezabatu
     * 
     * Goazen esportazio honekin egurra ematera - XML eguneratu eta gero datu-basea.
     */
    private void ezabatuBazkidea() {
        new Thread(() -> {
            try {
                // Bazkidea datu-baseatik bilatu - begiratu hemen ea bazkidea badago
                Bazkidea b = datuBasea.bazkideaDao().idzBilatu(editatuId);
                if (b != null) {
                    // DatuKudeatzailea sortu XML esportatzeko
                    DatuKudeatzailea dk = new DatuKudeatzailea(this);
                    
                    // Bazkide zerrenda lortu (guztiak)
                    List<Bazkidea> zerrenda = datuBasea.bazkideaDao().guztiak();
                    if (zerrenda == null) zerrenda = new ArrayList<>();
                    
                    // Ezabatu behar den bazkidea zerrendatik kendu
                    List<Bazkidea> berria = new ArrayList<>();
                    for (Bazkidea x : zerrenda) {
                        if (x.getId() != editatuId) berria.add(x);
                    }
                    
                    // XML esportatu zerrenda berriarekin
                    runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_idazten, Toast.LENGTH_LONG).show());
                    Log.d(ETIKETA, "Ezabatu: XML idazten, zerrenda tamaina=" + berria.size());
                    
                    if (dk.bazkideakEsportatuZerrenda(berria)) {
                        // XML ondo esportatu da - datu-baseatik ezabatu
                        runOnUiThread(() -> Toast.makeText(this, R.string.debug_ezabatu_xml_ondo, Toast.LENGTH_LONG).show());
                        Log.d(ETIKETA, "Ezabatu: XML ondo. Datu-baseatik ezabatzen id=" + editatuId);
                        datuBasea.bazkideaDao().ezabatu(b);
                        
                        // Arrakasta - Activity itxi
                        runOnUiThread(() -> {
                            Toast.makeText(this, R.string.bazkidea_ondo_ezabatuta, Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                    } else {
                        // XML esportazioan akatsa
                        Log.e(ETIKETA, "Ezabatu: XML eguneratzean akatsa.");
                        runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_akatsa, Toast.LENGTH_LONG).show());
                    }
                } else {
                    // Bazkidea ez da aurkitu
                    Log.e(ETIKETA, "Ezabatu: Ez da bazkidea aurkitu id=" + editatuId);
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.errorea_gordetzean, getString(R.string.bazkidea_ez_da_aurkitu)), Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea bazkidea ezabatzean", e);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.errorea_gordetzean, e.getMessage() != null ? e.getMessage() : getString(R.string.errore_ezezaguna)), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /**
     * Formularioa gorde: lehen bazkideak.xml eguneratu (zerrenda + berria edo ordezkatua), gero datu-basea.
     * 
     * Prozesua:
     * 1. Formulario datuak atera (NAN, izena, abizena, telefonoa, posta, jaiotze data, argazkia)
     * 2. Bazkidea objektua sortu
     * 3. Bazkide zerrenda lortu (guztiak)
     * 4. Editatzen badago: zerrendan ordezkatu; bestela: zerrendara gehitu
     * 5. XML esportatu zerrenda berriarekin
     * 6. Datu-basea eguneratu (eguneratu edo txertatu)
     * 
     * Goazen esportazio honekin egurra ematera - XML eguneratu eta gero datu-basea.
     */
    private void gordeBazkidea() {
        // Formulario datuak atera eta trim egin - hau hemen badago, dena ondo doa
        String nan = etNan.getText() != null ? etNan.getText().toString().trim() : "";
        String izena = etIzena.getText() != null ? etIzena.getText().toString().trim() : "";
        String abizena = etAbizena.getText() != null ? etAbizena.getText().toString().trim() : "";
        String telefonoa = etTelefonoa.getText() != null ? etTelefonoa.getText().toString().trim() : "";
        String posta = etPosta.getText() != null ? etPosta.getText().toString().trim() : "";
        String jaiotzeData = etJaiotzeData.getText() != null ? etJaiotzeData.getText().toString().trim() : "";
        String argazkia = etArgazkia.getText() != null ? etArgazkia.getText().toString().trim() : "";

        // Bazkidea objektua sortu eta datuak ezarri
        Bazkidea b = new Bazkidea();
        b.setNan(nan);
        b.setIzena(izena);
        b.setAbizena(abizena);
        b.setTelefonoZenbakia(telefonoa);
        b.setPosta(posta);
        b.setJaiotzeData(jaiotzeData);
        b.setArgazkia(argazkia);

        // Editatzen al ari gara? (editatuId >= 0 bada bai)
        final boolean editatzen = editatuId >= 0;
        
        // Datu-base kontsultak hilo nagusitik kanpo
        new Thread(() -> {
            try {
                // XML idazten hasi - erabiltzaileari jakinarazi
                runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_idazten, Toast.LENGTH_LONG).show());
                
                // DatuKudeatzailea sortu XML esportatzeko
                DatuKudeatzailea dk = new DatuKudeatzailea(this);
                
                // Bazkide zerrenda lortu (guztiak)
                List<Bazkidea> zerrenda = datuBasea.bazkideaDao().guztiak();
                if (zerrenda == null) zerrenda = new ArrayList<>();
                
                Log.d(ETIKETA, "Gorde: DB zerrenda tamaina=" + zerrenda.size() + ", editatzen=" + editatzen);
                
                // XML zerrenda sortu - editatzen badago ordezkatu, bestela gehitu
                List<Bazkidea> xmlZerrenda = new ArrayList<>();
                if (editatzen) {
                    // Editatzen: ID ezarri eta zerrendan ordezkatu
                    b.setId(editatuId);
                    for (Bazkidea x : zerrenda) {
                        xmlZerrenda.add(x.getId() == editatuId ? b : x);
                    }
                } else {
                    // Berria: zerrendara gehitu
                    xmlZerrenda.addAll(zerrenda);
                    xmlZerrenda.add(b);
                }
                
                // XML esportatu zerrenda berriarekin
                if (!dk.bazkideakEsportatuZerrenda(xmlZerrenda)) {
                    // XML esportazioan akatsa
                    Log.e(ETIKETA, "Gorde: bazkideakEsportatuZerrenda false itzuli du.");
                    runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_akatsa, Toast.LENGTH_LONG).show());
                    return;
                }
                
                // XML ondo esportatu da - datu-basea eguneratu
                runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_ondo, Toast.LENGTH_LONG).show());
                Log.d(ETIKETA, "Gorde: XML ondo. Datu-basea eguneratzen.");
                
                if (editatzen) {
                    // Editatzen: eguneratu
                    runOnUiThread(() -> Toast.makeText(this, R.string.debug_datu_basea_eguneratzen, Toast.LENGTH_LONG).show());
                    int errenkadak = datuBasea.bazkideaDao().eguneratu(b);
                    Log.d(ETIKETA, "Gorde: eguneratu errenkadak=" + errenkadak);
                } else {
                    // Berria: txertatu
                    runOnUiThread(() -> Toast.makeText(this, R.string.debug_datu_basea_txertatzen, Toast.LENGTH_LONG).show());
                    long id = datuBasea.bazkideaDao().txertatu(b);
                    Log.d(ETIKETA, "Gorde: txertatu id=" + id);
                }
                
                // Arrakasta - Activity itxi
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.debug_datu_basea_ondo, Toast.LENGTH_LONG).show();
                    Toast.makeText(this, editatzen ? R.string.ondo_gorde_dira_aldaketak : R.string.ondo_gorde_da, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                // Errorea log-ean erregistratu eta erabiltzaileari erakutsi
                Log.e(ETIKETA, "Gorde: salbuespena", e);
                String mezu = e.getMessage() != null ? e.getMessage() : "";
                if (mezu.isEmpty()) mezu = getString(R.string.errore_ezezaguna);
                final String mezuFinal = mezu;
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.debug_datu_basea_akatsa, mezuFinal), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
