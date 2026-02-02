package com.example.appkomertziala;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
 * Egitura: bazkideak.xml (NAN, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia).
 * Datuen balidazioa: NAN eta izena beharrezkoak; erroreak eremuetan eta Toast bidez.
 */
public class BazkideaFormularioActivity extends AppCompatActivity {

    private static final String ETIKETA = "BazkideaFormulario";

    /** Editatzeko: bazkidearen id. Zero baino txikiagoa bada berria da. */
    public static final String EXTRA_BAZKIDEA_ID = "bazkidea_id";

    private TextInputLayout tilNan;
    private TextInputLayout tilIzena;
    private TextInputEditText etNan, etIzena, etAbizena, etTelefonoa, etPosta, etJaiotzeData, etArgazkia;
    private AppDatabase datuBasea;
    private long editatuId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bazkidea_formulario);

        datuBasea = AppDatabase.getInstance(this);
        editatuId = getIntent() != null ? getIntent().getLongExtra(EXTRA_BAZKIDEA_ID, -1) : -1;
        setTitle(editatuId >= 0 ? getString(R.string.bazkidea_editatu) : getString(R.string.bazkidea_berria));

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

        btnGorde.setOnClickListener(v -> erakutsiGordeBaieztapena());
        btnEzabatu.setOnClickListener(v -> erakutsiEzabatuBaieztapena());

        if (editatuId >= 0) {
            btnEzabatu.setVisibility(View.VISIBLE);
            new Thread(() -> {
                Bazkidea b = datuBasea.bazkideaDao().idzBilatu(editatuId);
                if (b != null) {
                    runOnUiThread(() -> {
                        etNan.setText(b.getNan());
                        etIzena.setText(b.getIzena());
                        etAbizena.setText(b.getAbizena());
                        etTelefonoa.setText(b.getTelefonoZenbakia());
                        etPosta.setText(b.getPosta());
                        etJaiotzeData.setText(b.getJaiotzeData());
                        etArgazkia.setText(b.getArgazkia());
                    });
                }
            }).start();
        }
    }

    /** Gorde botoian: datuak balidatu, erroreak erakutsi; balidoak badira baieztapen dialogo, gero gorde. */
    private void erakutsiGordeBaieztapena() {
        if (tilNan != null) tilNan.setError(null);
        if (tilIzena != null) tilIzena.setError(null);
        String nan = etNan.getText() != null ? etNan.getText().toString().trim() : "";
        String izena = etIzena.getText() != null ? etIzena.getText().toString().trim() : "";
        boolean baliogabea = false;
        if (nan.isEmpty()) {
            if (tilNan != null) tilNan.setError(getString(R.string.bazkidea_errorea_nan_beharrezkoa));
            baliogabea = true;
        }
        if (izena.isEmpty()) {
            if (tilIzena != null) tilIzena.setError(getString(R.string.bazkidea_errorea_izena_beharrezkoa));
            baliogabea = true;
        }
        if (baliogabea) {
            Toast.makeText(this, getString(R.string.bazkidea_errorea_nan_beharrezkoa), Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage(R.string.bazkidea_gorde_baieztatu)
                .setPositiveButton(R.string.bai, (dialog, which) -> gordeBazkidea())
                .setNegativeButton(R.string.ez, null)
                .show();
    }

    /** Ezabatu botoian: baieztapen dialogo erakutsi, gero ezabatu. */
    private void erakutsiEzabatuBaieztapena() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.bazkidea_ezabatu_baieztatu)
                .setPositiveButton(R.string.bai, (dialog, which) -> ezabatuBazkidea())
                .setNegativeButton(R.string.ez, null)
                .show();
    }

    /** Bazkidea ezabatu: lehen bazkideak.xml eguneratu (bazkidea zerrendatik kendu), gero datu-basea. */
    private void ezabatuBazkidea() {
        new Thread(() -> {
            Bazkidea b = datuBasea.bazkideaDao().idzBilatu(editatuId);
            if (b != null) {
                DatuKudeatzailea dk = new DatuKudeatzailea(this);
                List<Bazkidea> zerrenda = datuBasea.bazkideaDao().guztiak();
                List<Bazkidea> berria = new ArrayList<>();
                for (Bazkidea x : zerrenda) {
                    if (x.getId() != editatuId) berria.add(x);
                }
                runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_idazten, Toast.LENGTH_LONG).show());
                Log.d(ETIKETA, "Ezabatu: XML idazten, zerrenda tamaina=" + berria.size());
                if (dk.bazkideakEsportatuZerrenda(berria)) {
                    runOnUiThread(() -> Toast.makeText(this, R.string.debug_ezabatu_xml_ondo, Toast.LENGTH_LONG).show());
                    Log.d(ETIKETA, "Ezabatu: XML ondo. Datu-baseatik ezabatzen id=" + editatuId);
                    datuBasea.bazkideaDao().ezabatu(b);
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.bazkidea_ondo_ezabatuta, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                } else {
                    Log.e(ETIKETA, "Ezabatu: XML eguneratzean akatsa.");
                    runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_akatsa, Toast.LENGTH_LONG).show());
                }
            } else {
                Log.e(ETIKETA, "Ezabatu: Ez da bazkidea aurkitu id=" + editatuId);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.errorea_gordetzean, getString(R.string.bazkidea_ez_da_aurkitu)), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /** Formularioa gorde: lehen bazkideak.xml eguneratu (zerrenda + berria edo ordezkatua), gero datu-basea. */
    private void gordeBazkidea() {
        String nan = etNan.getText() != null ? etNan.getText().toString().trim() : "";
        String izena = etIzena.getText() != null ? etIzena.getText().toString().trim() : "";
        String abizena = etAbizena.getText() != null ? etAbizena.getText().toString().trim() : "";
        String telefonoa = etTelefonoa.getText() != null ? etTelefonoa.getText().toString().trim() : "";
        String posta = etPosta.getText() != null ? etPosta.getText().toString().trim() : "";
        String jaiotzeData = etJaiotzeData.getText() != null ? etJaiotzeData.getText().toString().trim() : "";
        String argazkia = etArgazkia.getText() != null ? etArgazkia.getText().toString().trim() : "";

        Bazkidea b = new Bazkidea();
        b.setNan(nan);
        b.setIzena(izena);
        b.setAbizena(abizena);
        b.setTelefonoZenbakia(telefonoa);
        b.setPosta(posta);
        b.setJaiotzeData(jaiotzeData);
        b.setArgazkia(argazkia);

        final boolean editatzen = editatuId >= 0;
        new Thread(() -> {
            try {
                runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_idazten, Toast.LENGTH_LONG).show());
                DatuKudeatzailea dk = new DatuKudeatzailea(this);
                List<Bazkidea> zerrenda = datuBasea.bazkideaDao().guztiak();
                Log.d(ETIKETA, "Gorde: DB zerrenda tamaina=" + zerrenda.size() + ", editatzen=" + editatzen);
                List<Bazkidea> xmlZerrenda = new ArrayList<>();
                if (editatzen) {
                    b.setId(editatuId);
                    for (Bazkidea x : zerrenda) {
                        xmlZerrenda.add(x.getId() == editatuId ? b : x);
                    }
                } else {
                    xmlZerrenda.addAll(zerrenda);
                    xmlZerrenda.add(b);
                }
                if (!dk.bazkideakEsportatuZerrenda(xmlZerrenda)) {
                    Log.e(ETIKETA, "Gorde: bazkideakEsportatuZerrenda false itzuli du.");
                    runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_akatsa, Toast.LENGTH_LONG).show());
                    return;
                }
                runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_ondo, Toast.LENGTH_LONG).show());
                Log.d(ETIKETA, "Gorde: XML ondo. Datu-basea eguneratzen.");
                if (editatzen) {
                    runOnUiThread(() -> Toast.makeText(this, R.string.debug_datu_basea_eguneratzen, Toast.LENGTH_LONG).show());
                    int errenkadak = datuBasea.bazkideaDao().eguneratu(b);
                    Log.d(ETIKETA, "Gorde: eguneratu errenkadak=" + errenkadak);
                } else {
                    runOnUiThread(() -> Toast.makeText(this, R.string.debug_datu_basea_txertatzen, Toast.LENGTH_LONG).show());
                    long id = datuBasea.bazkideaDao().txertatu(b);
                    Log.d(ETIKETA, "Gorde: txertatu id=" + id);
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.debug_datu_basea_ondo, Toast.LENGTH_LONG).show();
                    Toast.makeText(this, editatzen ? R.string.ondo_gorde_dira_aldaketak : R.string.ondo_gorde_da, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                Log.e(ETIKETA, "Gorde: salbuespena", e);
                String mezu = e.getMessage() != null ? e.getMessage() : "";
                if (mezu.isEmpty()) mezu = getString(R.string.errore_ezezaguna);
                final String mezuFinal = mezu;
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.debug_datu_basea_akatsa, mezuFinal), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
