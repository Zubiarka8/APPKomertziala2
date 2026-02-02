package com.example.appkomertziala;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Bazkide berria gehitzeko edo lehendik dagoen bazkidea editatzeko formularioa.
 * Egitura: bazkideak.xml (NAN, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia).
 */
public class BazkideaFormularioActivity extends AppCompatActivity {

    /** Editatzeko: bazkidearen id. &lt; 0 bada berria. */
    public static final String EXTRA_BAZKIDEA_ID = "bazkidea_id";

    private TextInputEditText etNan, etIzena, etAbizena, etTelefonoa, etPosta, etJaiotzeData, etArgazkia;
    private AppDatabase datuBasea;
    private long editatuId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bazkidea_formulario);

        datuBasea = AppDatabase.getInstance(this);
        editatuId = getIntent().getLongExtra(EXTRA_BAZKIDEA_ID, -1);
        setTitle(editatuId >= 0 ? getString(R.string.bazkidea_editatu) : getString(R.string.bazkidea_berria));

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

    /** Gorde botoian: baieztapen dialogo erakutsi, gero gorde. */
    private void erakutsiGordeBaieztapena() {
        String nan = etNan.getText() != null ? etNan.getText().toString().trim() : "";
        if (nan.isEmpty()) {
            Toast.makeText(this, getString(R.string.table_bazkide_nan) + " beharrezkoa", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage(R.string.bazkidea_gorde_baieztatu)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> gordeBazkidea())
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    /** Ezabatu botoian: baieztapen dialogo erakutsi, gero ezabatu. */
    private void erakutsiEzabatuBaieztapena() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.bazkidea_ezabatu_baieztatu)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> ezabatuBazkidea())
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void ezabatuBazkidea() {
        new Thread(() -> {
            Bazkidea b = datuBasea.bazkideaDao().idzBilatu(editatuId);
            if (b != null) {
                datuBasea.bazkideaDao().ezabatu(b);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.btn_ezabatu), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }
        }).start();
    }

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

        new Thread(() -> {
            if (editatuId >= 0) {
                b.setId(editatuId);
                datuBasea.bazkideaDao().eguneratu(b);
            } else {
                datuBasea.bazkideaDao().txertatu(b);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, getString(R.string.bazkidea_gorde), Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        }).start();
    }
}
