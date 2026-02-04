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
import com.example.appkomertziala.segurtasuna.DataBalidatzailea;
import com.example.appkomertziala.segurtasuna.NanBalidatzailea;
import com.example.appkomertziala.segurtasuna.PostaBalidatzailea;
import com.example.appkomertziala.segurtasuna.TelefonoBalidatzailea;
import com.example.appkomertziala.xml.DatuKudeatzailea;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
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
    
    /** Data formatua (yyyy/MM/dd). */
    private static final String DATA_FORMAT = "yyyy/MM/dd";

    /** TextInputLayout-ak erroreak erakusteko (eremu guztiak). */
    private TextInputLayout tilNan;
    private TextInputLayout tilIzena;
    private TextInputLayout tilAbizena;
    private TextInputLayout tilTelefonoa;
    private TextInputLayout tilPosta;
    private TextInputLayout tilJaiotzeData;
    private TextInputLayout tilArgazkia;
    
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
        tilAbizena = findViewById(R.id.tilBazkideaAbizena);
        tilTelefonoa = findViewById(R.id.tilBazkideaTelefonoa);
        tilPosta = findViewById(R.id.tilBazkideaPosta);
        tilJaiotzeData = findViewById(R.id.tilBazkideaJaiotzeData);
        tilArgazkia = findViewById(R.id.tilBazkideaArgazkia);
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
        
        // Jaiotze data eremua: MaterialDatePicker erakutsi klik egitean
        etJaiotzeData.setOnClickListener(v -> erakutsiJaiotzeDataHautatzailea());
        etJaiotzeData.setFocusable(false); // Teklatua ez erakutsi, date picker bakarrik
        etJaiotzeData.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                erakutsiJaiotzeDataHautatzailea();
            }
        });

        // Bazkidea berria bada, gaurko data lehenetsi (ez, jaiotze data hutsik utzi)
        // Jaiotze data erabiltzaileak hautatu behar du
        
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
     * MaterialDatePicker erakusten du — jaiotze data hautatzeko.
     * Material Design 3 date picker erabiltzen du.
     * Muga: gaurko data baino lehenagoko data bakarrik hautatu daiteke.
     * 
     * @see <a href="https://m3.material.io/components/date-pickers/overview">Material Design 3 Date Pickers</a>
     */
    private void erakutsiJaiotzeDataHautatzailea() {
        // Gaurko data muga gisa erabili (etorkizuneko datak ezin dira hautatu)
        long gaurkoDataMillis = MaterialDatePicker.todayInUtcMilliseconds();
        long selection = gaurkoDataMillis;
        
        String dataStr = etJaiotzeData.getText() != null ? etJaiotzeData.getText().toString().trim() : "";
        if (!dataStr.isEmpty()) {
            try {
                // yyyy/MM/dd formatutik parseatu
                SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(dataStr);
                if (d != null) {
                    long dataMillis = d.getTime();
                    // Gaurko data baino lehenagokoa bada bakarrik erabili
                    if (dataMillis < gaurkoDataMillis) {
                        selection = dataMillis;
                    }
                }
            } catch (Exception ignored) {
                // Parse errorea bada, gaurko data erabili (hautatzaileak erabiltzaileak aukeratuko du)
                selection = gaurkoDataMillis;
            }
        }
        // Datuak hutsik badago, gaurko data erabili (hautatzaileak erabiltzaileak aukeratuko du)
        
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.data_hautatu))
                .setSelection(selection)
                .setInputMode(com.google.android.material.datepicker.MaterialDatePicker.INPUT_MODE_CALENDAR);
        
        // Muga: gaurko data baino lehenagoko data bakarrik hautatu daiteke
        // MaterialDatePicker-ek ez du muga zuzenean onartzen, baina validazioa gero egingo dugu
        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selectionMillis -> {
            // Egiaztatu hautatutako data gaurko data baino lehenagokoa dela
            if (selectionMillis >= gaurkoDataMillis) {
                Toast.makeText(this, "Errorea: Sartutako data ezin da gaurko eguna edo gaurko eguna baino handiagoa izan.", Toast.LENGTH_LONG).show();
                return;
            }
            
            // yyyy/MM/dd formatuan jarri
            SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            etJaiotzeData.setText(sdf.format(new Date(selectionMillis)));
        });
        picker.show(getSupportFragmentManager(), "JAIOTZE_DATA_PICKER");
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
        if (tilAbizena != null) tilAbizena.setError(null);
        if (tilTelefonoa != null) tilTelefonoa.setError(null);
        if (tilPosta != null) tilPosta.setError(null);
        if (tilJaiotzeData != null) tilJaiotzeData.setError(null);
        if (tilArgazkia != null) tilArgazkia.setError(null);
        
        // Datuak atera eta trim egin
        String nan = etNan.getText() != null ? etNan.getText().toString().trim() : "";
        String izena = etIzena.getText() != null ? etIzena.getText().toString().trim() : "";
        String abizena = etAbizena.getText() != null ? etAbizena.getText().toString().trim() : "";
        String telefonoa = etTelefonoa.getText() != null ? etTelefonoa.getText().toString().trim() : "";
        String posta = etPosta.getText() != null ? etPosta.getText().toString().trim() : "";
        String jaiotzeData = etJaiotzeData.getText() != null ? etJaiotzeData.getText().toString().trim() : "";
        String argazkia = etArgazkia.getText() != null ? etArgazkia.getText().toString().trim() : "";
        
        // Balidazioa: Bazkidea berria bada, eremu GUZTIAK beharrezkoak dira
        boolean baliogabea = false;
        
        if (editatuId < 0) {
            // Bazkidea berria: eremu guztiak beharrezkoak
            if (nan.isEmpty()) {
                if (tilNan != null) tilNan.setError(getString(R.string.bazkidea_errorea_nan_beharrezkoa));
                baliogabea = true;
            } else {
                // NAN formatua balidatu
                String nanErroreMezua = NanBalidatzailea.balidatuNanMezua(nan);
                if (nanErroreMezua != null) {
                    if (tilNan != null) tilNan.setError(nanErroreMezua);
                    baliogabea = true;
                }
            }
            
            if (izena.isEmpty()) {
                if (tilIzena != null) tilIzena.setError(getString(R.string.bazkidea_errorea_izena_beharrezkoa));
                baliogabea = true;
            }
            
            if (abizena.isEmpty()) {
                if (tilAbizena != null) tilAbizena.setError("Abizena beharrezkoa da");
                baliogabea = true;
            }
            
            if (telefonoa.isEmpty()) {
                if (tilTelefonoa != null) tilTelefonoa.setError("Telefonoa beharrezkoa da");
                baliogabea = true;
            } else {
                // Telefono formatua balidatu
                String telefonoErroreMezua = TelefonoBalidatzailea.balidatuTelefonoaMezua(telefonoa);
                if (telefonoErroreMezua != null) {
                    if (tilTelefonoa != null) tilTelefonoa.setError(telefonoErroreMezua);
                    baliogabea = true;
                }
            }
            
            if (posta.isEmpty()) {
                if (tilPosta != null) tilPosta.setError("Posta elektronikoa beharrezkoa da");
                baliogabea = true;
            } else {
                // Posta formatua balidatu
                String postaErroreMezua = PostaBalidatzailea.balidatuPostaMezua(posta);
                if (postaErroreMezua != null) {
                    if (tilPosta != null) tilPosta.setError(postaErroreMezua);
                    baliogabea = true;
                }
            }
            
            if (jaiotzeData.isEmpty()) {
                if (tilJaiotzeData != null) tilJaiotzeData.setError("Jaiotze data beharrezkoa da");
                baliogabea = true;
            } else {
                // Jaiotze data formatua balidatu (formatu eta gaurko data baino lehenagokoa)
                String dataErroreMezua = DataBalidatzailea.balidatuJaiotzeDataMezua(jaiotzeData);
                if (dataErroreMezua != null) {
                    if (tilJaiotzeData != null) tilJaiotzeData.setError(dataErroreMezua);
                    baliogabea = true;
                }
            }
            
            if (argazkia.isEmpty()) {
                if (tilArgazkia != null) tilArgazkia.setError("Argazkia beharrezkoa da");
                baliogabea = true;
            }
        } else {
            // Editatzen: NAN eta izena beharrezkoak (beste eremuak aukerakoak)
            if (nan.isEmpty()) {
                if (tilNan != null) tilNan.setError(getString(R.string.bazkidea_errorea_nan_beharrezkoa));
                baliogabea = true;
            } else {
                // NAN formatua balidatu
                String nanErroreMezua = NanBalidatzailea.balidatuNanMezua(nan);
                if (nanErroreMezua != null) {
                    if (tilNan != null) tilNan.setError(nanErroreMezua);
                    baliogabea = true;
                }
            }
            
            if (izena.isEmpty()) {
                if (tilIzena != null) tilIzena.setError(getString(R.string.bazkidea_errorea_izena_beharrezkoa));
                baliogabea = true;
            }
            
            // Telefono balidatu (aukerakoa baina formatua zuzena izan behar du)
            if (!telefonoa.isEmpty()) {
                String telefonoErroreMezua = TelefonoBalidatzailea.balidatuTelefonoaMezua(telefonoa);
                if (telefonoErroreMezua != null) {
                    if (tilTelefonoa != null) tilTelefonoa.setError(telefonoErroreMezua);
                    baliogabea = true;
                }
            }
            
            // Posta balidatu (aukerakoa baina formatua zuzena izan behar du)
            if (!posta.isEmpty()) {
                String postaErroreMezua = PostaBalidatzailea.balidatuPostaMezua(posta);
                if (postaErroreMezua != null) {
                    if (tilPosta != null) tilPosta.setError(postaErroreMezua);
                    baliogabea = true;
                }
            }
            
            // Jaiotze data balidatu (aukerakoa baina formatua zuzena izan behar du eta gaurko data baino lehenagokoa)
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

        // Egiaztatu eremu guztiak beteta dauden (bazkidea berria bada)
        if (editatuId < 0) {
            // Variable finalak sortu lambda erabiltzeko
            final String nanFinal = nan;
            final String izenaFinal = izena;
            final String abizenaFinal = abizena;
            final String telefonoaFinal = telefonoa;
            final String postaFinal = posta;
            final String jaiotzeDataFinal = jaiotzeData;
            final String argazkiaFinal = argazkia;
            
            boolean eremuGuztiakBeteta = !nanFinal.isEmpty() && !izenaFinal.isEmpty() && !abizenaFinal.isEmpty() && 
                                        !telefonoaFinal.isEmpty() && !postaFinal.isEmpty() && 
                                        !jaiotzeDataFinal.isEmpty() && !argazkiaFinal.isEmpty();
            
            if (!eremuGuztiakBeteta) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Errorea: Eremu guztiak bete behar dira bazkidea berria sortzeko.", Toast.LENGTH_LONG).show();
                    // Erroreak erakutsi eremu hutsietan
                    if (nanFinal.isEmpty() && tilNan != null) tilNan.setError("NAN beharrezkoa da");
                    if (izenaFinal.isEmpty() && tilIzena != null) tilIzena.setError("Izena beharrezkoa da");
                    if (abizenaFinal.isEmpty() && tilAbizena != null) tilAbizena.setError("Abizena beharrezkoa da");
                    if (telefonoaFinal.isEmpty() && tilTelefonoa != null) tilTelefonoa.setError("Telefonoa beharrezkoa da");
                    if (postaFinal.isEmpty() && tilPosta != null) tilPosta.setError("Posta beharrezkoa da");
                    if (jaiotzeDataFinal.isEmpty() && tilJaiotzeData != null) tilJaiotzeData.setError("Jaiotze data beharrezkoa da");
                    if (argazkiaFinal.isEmpty() && tilArgazkia != null) tilArgazkia.setError("Argazkia beharrezkoa da");
                });
                return;
            }
            
            // Datuak beteta daude - formatuak balidatu
            // NAN formatua balidatu
            String nanErroreMezua = NanBalidatzailea.balidatuNanMezua(nanFinal);
            if (nanErroreMezua != null) {
                final String nanErroreMezuaFinal = nanErroreMezua;
                runOnUiThread(() -> {
                    if (tilNan != null) tilNan.setError(nanErroreMezuaFinal);
                });
                return;
            }
            
            // Posta formatua balidatu
            String postaErroreMezua = PostaBalidatzailea.balidatuPostaMezua(postaFinal);
            if (postaErroreMezua != null) {
                final String postaErroreMezuaFinal = postaErroreMezua;
                runOnUiThread(() -> {
                    if (tilPosta != null) tilPosta.setError(postaErroreMezuaFinal);
                });
                return;
            }
            
            // Telefono formatua balidatu
            String telefonoErroreMezua = TelefonoBalidatzailea.balidatuTelefonoaMezua(telefonoaFinal);
            if (telefonoErroreMezua != null) {
                final String telefonoErroreMezuaFinal = telefonoErroreMezua;
                runOnUiThread(() -> {
                    if (tilTelefonoa != null) tilTelefonoa.setError(telefonoErroreMezuaFinal);
                });
                return;
            }
            
            // Jaiotze data formatua balidatu (formatu eta gaurko data baino lehenagokoa)
            String dataErroreMezua = DataBalidatzailea.balidatuJaiotzeDataMezua(jaiotzeDataFinal);
            if (dataErroreMezua != null) {
                final String dataErroreMezuaFinal = dataErroreMezua;
                runOnUiThread(() -> {
                    if (tilJaiotzeData != null) tilJaiotzeData.setError(dataErroreMezuaFinal);
                });
                return;
            }
            
            // Datuak normalizatu
            // NAN normalizatu (gidoiak kendu, letra maiuskulaz)
            String nanNormalizatua = NanBalidatzailea.normalizatuNan(nanFinal);
            if (nanNormalizatua != null) {
                nan = nanNormalizatua;
            }
            
            // Telefono normalizatu (separadoreak kendu, prefijoak kendu)
            String telefonoNormalizatua = TelefonoBalidatzailea.normalizatuTelefonoa(telefonoaFinal);
            if (telefonoNormalizatua != null) {
                telefonoa = telefonoNormalizatua;
            }
        } else {
            // Editatzen: NAN normalizatu (gidoiak kendu, letra maiuskulaz)
            // Variable final sortu lambda erabiltzeko
            final String nanOriginala = nan;
            String nanNormalizatua = NanBalidatzailea.normalizatuNan(nan);
            if (nanNormalizatua == null && !nan.isEmpty()) {
                // NAN formatua okerra baina hutsik ez dago - errorea erakutsi
                runOnUiThread(() -> {
                    Toast.makeText(this, "Errorea: NAN formatua okerra da", Toast.LENGTH_LONG).show();
                    if (tilNan != null) tilNan.setError(NanBalidatzailea.balidatuNanMezua(nanOriginala));
                });
                return;
            }
            if (nanNormalizatua != null) {
                nan = nanNormalizatua;
            }
            
            // Telefono normalizatu (separadoreak kendu, prefijoak kendu) - aukerakoa baina formatua zuzena izan behar du
            if (!telefonoa.isEmpty()) {
                String telefonoNormalizatua = TelefonoBalidatzailea.normalizatuTelefonoa(telefonoa);
                if (telefonoNormalizatua != null) {
                    telefonoa = telefonoNormalizatua;
                }
            }
        }

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
