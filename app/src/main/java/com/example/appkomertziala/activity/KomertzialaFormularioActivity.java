package com.example.appkomertziala.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appkomertziala.R;
import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Komertziala;
import com.example.appkomertziala.segurtasuna.DataBalidatzailea;
import com.example.appkomertziala.segurtasuna.PostaBalidatzailea;
import com.example.appkomertziala.xml.DatuKudeatzailea;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Komertzial berria gehitzeko edo lehendik dagoen komertziala editatzeko formularioa.
 * 
 * Hau hemen komertzial berria gehitu edo lehendik dagoen komertziala editatu dezakegu.
 * Formularioa bete behar da: izena, kodea, abizena, posta, jaiotze data, argazkia.
 * Gorde sakatzean, lehen komertzialak.xml eguneratzen da eta gero datu-basea.
 * 
 * Egitura: komertzialak.xml (izena, kodea, abizena, posta, jaiotzeData, argazkia).
 * Datuen balidazioa: izena eta kodea beharrezkoak; erroreak eremuetan eta Toast bidez.
 */
public class KomertzialaFormularioActivity extends AppCompatActivity {

    private static final String ETIKETA = "KomertzialaFormulario";

    /** Editatzeko: komertzialaren id (Intent extra). Zero baino txikiagoa bada berria da. */
    public static final String EXTRA_KOMERTZIALA_ID = "komertziala_id";
    
    /** Data formatua (yyyy/MM/dd). */
    private static final String DATA_FORMAT = "yyyy/MM/dd";

    /** TextInputLayout-ak erroreak erakusteko (eremu guztiak). */
    private TextInputLayout tilIzena;
    private TextInputLayout tilAbizena;
    private TextInputLayout tilKodea;
    private TextInputLayout tilPosta;
    private TextInputLayout tilJaiotzeData;
    private TextInputLayout tilArgazkia;
    
    /** TextInputEditText-ak formulario eremuak (izena, abizena, kodea, posta, jaiotze data, argazkia). */
    private TextInputEditText etIzena, etAbizena, etKodea, etPosta, etJaiotzeData, etArgazkia;
    
    /** Datu-basea (Room). */
    private AppDatabase datuBasea;
    
    /** Editatzeko komertzialaren ID. -1 bada berria da, >= 0 bada editatzen ari gara. */
    private long editatuId = -1;

    /**
     * Activity sortzean: UI elementuak kargatu, editatu ID lortu,
     * eta editatzen badago komertziala kargatu (hilo nagusitik kanpo).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_komertziala_formulario);

        // Datu-basea eta editatu ID lortu
        datuBasea = AppDatabase.getInstance(this);
        editatuId = getIntent() != null ? getIntent().getLongExtra(EXTRA_KOMERTZIALA_ID, -1) : -1;
        
        // Izenburua jarri - editatzen badago "Editatu", bestela "Berria"
        setTitle(editatuId >= 0 ? getString(R.string.komertzial_editatu) : getString(R.string.komertzial_berria));

        // UI elementuak kargatu
        tilIzena = findViewById(R.id.tilKomertzialaIzena);
        tilAbizena = findViewById(R.id.tilKomertzialaAbizena);
        tilKodea = findViewById(R.id.tilKomertzialaKodea);
        tilPosta = findViewById(R.id.tilKomertzialaPosta);
        tilJaiotzeData = findViewById(R.id.tilKomertzialaJaiotzeData);
        tilArgazkia = findViewById(R.id.tilKomertzialaArgazkia);
        etIzena = findViewById(R.id.etKomertzialaIzena);
        etAbizena = findViewById(R.id.etKomertzialaAbizena);
        etKodea = findViewById(R.id.etKomertzialaKodea);
        etPosta = findViewById(R.id.etKomertzialaPosta);
        etJaiotzeData = findViewById(R.id.etKomertzialaJaiotzeData);
        etArgazkia = findViewById(R.id.etKomertzialaArgazkia);
        MaterialButton btnGorde = findViewById(R.id.btnKomertzialaGorde);
        MaterialButton btnEzabatu = findViewById(R.id.btnKomertzialaEzabatu);

        // Botoien listener-ak konfiguratu
        btnGorde.setOnClickListener(v -> erakutsiGordeBaieztapena());
        btnEzabatu.setOnClickListener(v -> erakutsiEzabatuBaieztapena());
        
        // Jaiotze data eremua: MaterialDatePicker erakutsi klik egitean
        etJaiotzeData.setOnClickListener(v -> erakutsiJaiotzeDataHautatzailea());
        etJaiotzeData.setFocusable(false);
        etJaiotzeData.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                erakutsiJaiotzeDataHautatzailea();
            }
        });
        
        // Editatzen badago, komertziala kargatu eta formularioa bete
        if (editatuId >= 0) {
            // Ezabatu botoia erakutsi - editatzen badago bakarrik
            btnEzabatu.setVisibility(View.VISIBLE);
            
            // Komertziala datu-baseatik kargatu (hilo nagusitik kanpo)
            new Thread(() -> {
                try {
                    Komertziala k = datuBasea.komertzialaDao().idzBilatu(editatuId);
                    if (k != null) {
                        // Formularioa bete - hilo nagusian
                        runOnUiThread(() -> {
                            etIzena.setText(k.getIzena());
                            etAbizena.setText(k.getAbizena());
                            etKodea.setText(k.getKodea());
                            etPosta.setText(k.getPosta());
                            etJaiotzeData.setText(k.getJaiotzeData());
                            etArgazkia.setText(k.getArgazkia());
                        });
                    } else {
                        Log.w(ETIKETA, "Komertziala ez da aurkitu id=" + editatuId);
                    }
                } catch (Exception e) {
                    Log.e(ETIKETA, "Errorea komertziala kargatzean", e);
                }
            }).start();
        }
    }

    /**
     * MaterialDatePicker erakusten du — jaiotze data hautatzeko.
     * Material Design 3 date picker erabiltzen du.
     * Muga: gaurko data baino lehenagoko data bakarrik hautatu daiteke.
     */
    private void erakutsiJaiotzeDataHautatzailea() {
        long gaurkoDataMillis = MaterialDatePicker.todayInUtcMilliseconds();
        long selection = gaurkoDataMillis;
        
        String dataStr = etJaiotzeData.getText() != null ? etJaiotzeData.getText().toString().trim() : "";
        if (!dataStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(dataStr);
                if (d != null) {
                    long dataMillis = d.getTime();
                    if (dataMillis < gaurkoDataMillis) {
                        selection = dataMillis;
                    }
                }
            } catch (Exception ignored) {
                selection = gaurkoDataMillis;
            }
        }
        
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.data_hautatu))
                .setSelection(selection)
                .setInputMode(com.google.android.material.datepicker.MaterialDatePicker.INPUT_MODE_CALENDAR);
        
        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selectionMillis -> {
            if (selectionMillis >= gaurkoDataMillis) {
                Toast.makeText(this, "Errorea: Sartutako data ezin da gaurko eguna edo gaurko eguna baino handiagoa izan.", Toast.LENGTH_LONG).show();
                return;
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            etJaiotzeData.setText(sdf.format(new Date(selectionMillis)));
        });
        picker.show(getSupportFragmentManager(), "JAIOTZE_DATA_PICKER");
    }

    /**
     * Gorde botoia sakatzean: datuak balidatu, erroreak erakutsi;
     * balidoak badira baieztapen dialogo erakutsi, gero gorde.
     */
    private void erakutsiGordeBaieztapena() {
        // Erroreak garbitu
        if (tilIzena != null) tilIzena.setError(null);
        if (tilAbizena != null) tilAbizena.setError(null);
        if (tilKodea != null) tilKodea.setError(null);
        if (tilPosta != null) tilPosta.setError(null);
        if (tilJaiotzeData != null) tilJaiotzeData.setError(null);
        if (tilArgazkia != null) tilArgazkia.setError(null);
        
        // Datuak atera eta trim egin
        String izena = etIzena.getText() != null ? etIzena.getText().toString().trim() : "";
        String abizena = etAbizena.getText() != null ? etAbizena.getText().toString().trim() : "";
        String kodea = etKodea.getText() != null ? etKodea.getText().toString().trim() : "";
        String posta = etPosta.getText() != null ? etPosta.getText().toString().trim() : "";
        String jaiotzeData = etJaiotzeData.getText() != null ? etJaiotzeData.getText().toString().trim() : "";
        String argazkia = etArgazkia.getText() != null ? etArgazkia.getText().toString().trim() : "";
        
        // Balidazioa
        boolean baliogabea = false;
        
        if (editatuId < 0) {
            // Komertziala berria: izena eta kodea beharrezkoak
            if (izena.isEmpty()) {
                if (tilIzena != null) tilIzena.setError(getString(R.string.komertzial_errorea_izena_beharrezkoa));
                baliogabea = true;
            }
            
            if (kodea.isEmpty()) {
                if (tilKodea != null) tilKodea.setError(getString(R.string.komertzial_errorea_kodea_beharrezkoa));
                baliogabea = true;
            }
            
            // Posta balidatu (aukerakoa baina formatua zuzena izan behar du)
            if (!posta.isEmpty()) {
                String postaErroreMezua = PostaBalidatzailea.balidatuPostaMezua(posta);
                if (postaErroreMezua != null) {
                    if (tilPosta != null) tilPosta.setError(postaErroreMezua);
                    baliogabea = true;
                }
            }
            
            // Jaiotze data balidatu (aukerakoa baina formatua zuzena izan behar du)
            if (!jaiotzeData.isEmpty()) {
                String dataErroreMezua = DataBalidatzailea.balidatuJaiotzeDataMezua(jaiotzeData);
                if (dataErroreMezua != null) {
                    if (tilJaiotzeData != null) tilJaiotzeData.setError(dataErroreMezua);
                    baliogabea = true;
                }
            }
        } else {
            // Editatzen: izena eta kodea beharrezkoak
            if (izena.isEmpty()) {
                if (tilIzena != null) tilIzena.setError(getString(R.string.komertzial_errorea_izena_beharrezkoa));
                baliogabea = true;
            }
            
            if (kodea.isEmpty()) {
                if (tilKodea != null) tilKodea.setError(getString(R.string.komertzial_errorea_kodea_beharrezkoa));
                baliogabea = true;
            }
            
            // Posta balidatu (aukerakoa baina formatua zuzena izan behar du)
            if (!posta.isEmpty()) {
                String postaErroreMezua = PostaBalidatzailea.balidatuPostaMezua(posta);
                if (postaErroreMezua != null) {
                    if (tilPosta != null) tilPosta.setError(postaErroreMezua);
                    baliogabea = true;
                }
            }
            
            // Jaiotze data balidatu (aukerakoa baina formatua zuzena izan behar du)
            if (!jaiotzeData.isEmpty()) {
                String dataErroreMezua = DataBalidatzailea.balidatuJaiotzeDataMezua(jaiotzeData);
                if (dataErroreMezua != null) {
                    if (tilJaiotzeData != null) tilJaiotzeData.setError(dataErroreMezua);
                    baliogabea = true;
                }
            }
        }
        
        // Balidazioak huts egin badu, errorea erakutsi eta itzuli
        if (baliogabea) {
            Toast.makeText(this, getString(R.string.komertzial_errorea_izena_beharrezkoa), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Datuak balidoak dira - baieztapen dialogo erakutsi
        new AlertDialog.Builder(this)
                .setMessage(R.string.komertzial_gorde_baieztatu)
                .setPositiveButton(R.string.bai, (dialog, which) -> gordeKomertziala())
                .setNegativeButton(R.string.ez, null)
                .show();
    }

    /**
     * Ezabatu botoia sakatzean: baieztapen dialogo erakutsi, gero ezabatu.
     */
    private void erakutsiEzabatuBaieztapena() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.komertzial_ezabatu_baieztatu)
                .setPositiveButton(R.string.bai, (dialog, which) -> ezabatuKomertziala())
                .setNegativeButton(R.string.ez, null)
                .show();
    }

    /**
     * Komertziala ezabatu: lehen komertzialak.xml eguneratu, gero datu-basea.
     */
    private void ezabatuKomertziala() {
        new Thread(() -> {
            try {
                Komertziala k = datuBasea.komertzialaDao().idzBilatu(editatuId);
                if (k != null) {
                    DatuKudeatzailea dk = new DatuKudeatzailea(this);
                    
                    List<Komertziala> zerrenda = datuBasea.komertzialaDao().guztiak();
                    if (zerrenda == null) zerrenda = new ArrayList<>();
                    
                    List<Komertziala> berria = new ArrayList<>();
                    for (Komertziala x : zerrenda) {
                        if (x.getId() != editatuId) berria.add(x);
                    }
                    
                    runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_idazten, Toast.LENGTH_LONG).show());
                    Log.d(ETIKETA, "Ezabatu: XML idazten, zerrenda tamaina=" + berria.size());
                    
                    if (dk.komertzialakEsportatuZerrenda(berria)) {
                        runOnUiThread(() -> Toast.makeText(this, R.string.debug_ezabatu_xml_ondo, Toast.LENGTH_LONG).show());
                        Log.d(ETIKETA, "Ezabatu: XML ondo. Datu-baseatik ezabatzen id=" + editatuId);
                        datuBasea.komertzialaDao().ezabatu(k);
                        
                        runOnUiThread(() -> {
                            Toast.makeText(this, R.string.komertzial_ondo_ezabatuta, Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                    } else {
                        Log.e(ETIKETA, "Ezabatu: XML eguneratzean akatsa.");
                        runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_akatsa, Toast.LENGTH_LONG).show());
                    }
                } else {
                    Log.e(ETIKETA, "Ezabatu: Ez da komertziala aurkitu id=" + editatuId);
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.errorea_gordetzean, "Komertziala ez da aurkitu"), Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea komertziala ezabatzean", e);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.errorea_gordetzean, e.getMessage() != null ? e.getMessage() : getString(R.string.errore_ezezaguna)), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /**
     * Formularioa gorde: lehen komertzialak.xml eguneratu, gero datu-basea.
     */
    private void gordeKomertziala() {
        String izena = etIzena.getText() != null ? etIzena.getText().toString().trim() : "";
        String abizena = etAbizena.getText() != null ? etAbizena.getText().toString().trim() : "";
        String kodea = etKodea.getText() != null ? etKodea.getText().toString().trim() : "";
        String posta = etPosta.getText() != null ? etPosta.getText().toString().trim() : "";
        String jaiotzeData = etJaiotzeData.getText() != null ? etJaiotzeData.getText().toString().trim() : "";
        String argazkia = etArgazkia.getText() != null ? etArgazkia.getText().toString().trim() : "";

        Komertziala k = new Komertziala();
        k.setIzena(izena);
        k.setAbizena(abizena);
        k.setKodea(kodea);
        k.setPosta(posta);
        k.setJaiotzeData(jaiotzeData);
        k.setArgazkia(argazkia);

        final boolean editatzen = editatuId >= 0;
        
        new Thread(() -> {
            try {
                runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_idazten, Toast.LENGTH_LONG).show());
                
                DatuKudeatzailea dk = new DatuKudeatzailea(this);
                
                List<Komertziala> zerrenda = datuBasea.komertzialaDao().guztiak();
                if (zerrenda == null) zerrenda = new ArrayList<>();
                
                Log.d(ETIKETA, "Gorde: DB zerrenda tamaina=" + zerrenda.size() + ", editatzen=" + editatzen);
                
                List<Komertziala> xmlZerrenda = new ArrayList<>();
                if (editatzen) {
                    k.setId(editatuId);
                    for (Komertziala x : zerrenda) {
                        xmlZerrenda.add(x.getId() == editatuId ? k : x);
                    }
                } else {
                    xmlZerrenda.addAll(zerrenda);
                    xmlZerrenda.add(k);
                }
                
                if (!dk.komertzialakEsportatuZerrenda(xmlZerrenda)) {
                    Log.e(ETIKETA, "Gorde: komertzialakEsportatuZerrenda false itzuli du.");
                    runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_akatsa, Toast.LENGTH_LONG).show());
                    return;
                }
                
                runOnUiThread(() -> Toast.makeText(this, R.string.debug_xml_ondo, Toast.LENGTH_LONG).show());
                Log.d(ETIKETA, "Gorde: XML ondo. Datu-basea eguneratzen.");
                
                if (editatzen) {
                    runOnUiThread(() -> Toast.makeText(this, R.string.debug_datu_basea_eguneratzen, Toast.LENGTH_LONG).show());
                    int errenkadak = datuBasea.komertzialaDao().eguneratu(k);
                    Log.d(ETIKETA, "Gorde: eguneratu errenkadak=" + errenkadak);
                } else {
                    runOnUiThread(() -> Toast.makeText(this, R.string.debug_datu_basea_txertatzen, Toast.LENGTH_LONG).show());
                    long id = datuBasea.komertzialaDao().txertatu(k);
                    Log.d(ETIKETA, "Gorde: txertatu id=" + id);
                }
                
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.debug_datu_basea_ondo, Toast.LENGTH_LONG).show();
                    Toast.makeText(this, R.string.komertzial_ondo_gordeta, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                Log.e(ETIKETA, "Gorde: salbuespena", e);
                String mezu = e.getMessage() != null ? e.getMessage() : "";
                if (mezu.isEmpty()) mezu = getString(R.string.errore_ezezaguna);
                final String mezuFinal = mezu;
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.errorea_gordetzean, mezuFinal), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}

