package com.example.appkomertziala.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appkomertziala.R;
import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.EskaeraGoiburua;
import com.example.appkomertziala.db.eredua.Komertziala;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.example.appkomertziala.segurtasuna.DataBalidatzailea;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Zita berria (EskaeraGoiburua) gehitzeko pantaila.
 * Komertzial kodearekin eta bazkide zerrendarekin.
 */
public class ZitaGehituActivity extends AppCompatActivity {

    private static final String ETIKETA = "CitaGehitu";

    public static final String EXTRA_KOMMERTZIALA_KODEA = "komertziala_kodea";
    /** Editatzeko: eskaera zenbakia. Present bada, zita kargatu eta eguneratu. */
    public static final String EXTRA_ESKAERA_ZENBAKIA = "eskaera_zenbakia";

    private static final String DATA_FORMAT = "yyyy/MM/dd";
    private static final String DATA_FORMAT_INTERNA = "yyyy-MM-dd"; // DatePicker-ek erabiltzen du
    private static final String ORDUA_FORMAT = "HH:mm";

    private TextInputEditText etZenbakia;
    private TextInputEditText etData;
    private TextInputEditText etCitaOrdua;
    private TextInputEditText etOrdezkaritza;
    private AppDatabase datuBasea;
    private String komertzialKodea;
    /** Editatzeko: eskaera zenbakia. Null bada zita berria da. */
    private String editatuZenbakia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zita_gehitu);

        komertzialKodea = getIntent() != null ? getIntent().getStringExtra(EXTRA_KOMMERTZIALA_KODEA) : null;
        if (komertzialKodea == null) {
            komertzialKodea = "";
        }
        editatuZenbakia = getIntent() != null ? getIntent().getStringExtra(EXTRA_ESKAERA_ZENBAKIA) : null;

        setTitle(editatuZenbakia != null && !editatuZenbakia.isEmpty()
                ? getString(R.string.zita_editatu_titulua)
                : getString(R.string.zita_gehitu_titulua));
        datuBasea = AppDatabase.getInstance(this);

        etZenbakia = findViewById(R.id.etCitaZenbakia);
        etData = findViewById(R.id.etCitaData);
        etCitaOrdua = findViewById(R.id.etCitaOrdua);
        etOrdezkaritza = findViewById(R.id.etCitaOrdezkaritza);
        MaterialButton btnUtzi = findViewById(R.id.btnCitaUtzi);
        MaterialButton btnGorde = findViewById(R.id.btnCitaGorde);

        // Data gaurkoa lehenetsi (yyyy/MM/dd)
        String gaur = DataBalidatzailea.gaurkoData();
        etData.setText(gaur);

        etData.setOnClickListener(v -> erakutsiDataHautatzailea());
        etCitaOrdua.setOnClickListener(v -> erakutsiOrduaHautatzailea());

        if (editatuZenbakia != null && !editatuZenbakia.isEmpty()) {
            kargatuZitaEditatzeko();
        }
        btnUtzi.setOnClickListener(v -> finish());
        btnGorde.setOnClickListener(v -> gordeCita());
    }

    /** Editatzeko: eskaera goiburua kargatu eta eremuak bete. */
    private void kargatuZitaEditatzeko() {
        new Thread(() -> {
            EskaeraGoiburua goi = datuBasea.eskaeraGoiburuaDao().zenbakiaBilatu(editatuZenbakia);
            if (goi == null) return;
            String data = goi.getData() != null ? goi.getData() : "";
            String ordua = "";
            if (data.contains(" ")) {
                int i = data.indexOf(" ");
                ordua = data.substring(i + 1).trim();
                data = data.substring(0, i).trim();
            }
            // Data formatua bihurtu yyyy/MM/dd formatura baldin beharrezkoa bada
            data = DataBalidatzailea.bihurtuFormatua(data);
            if (data == null) data = "";
            final String dataStr = data;
            final String orduaStr = ordua;
            final String zenb = goi.getZenbakia() != null ? goi.getZenbakia() : "";
            final String ordezk = goi.getOrdezkaritza() != null ? goi.getOrdezkaritza() : "";
            final String bazkideaKode = goi.getBazkideaKodea() != null ? goi.getBazkideaKodea() : "";
            runOnUiThread(() -> {
                etZenbakia.setText(zenb);
                etZenbakia.setEnabled(false);
                etData.setText(dataStr);
                etCitaOrdua.setText(orduaStr);
                etOrdezkaritza.setText(ordezk);
            });
        }).start();
    }

    /** MaterialDatePicker erakusten du — data hautatzeko. */
    private void erakutsiDataHautatzailea() {
        long selection = MaterialDatePicker.todayInUtcMilliseconds();
        String dataStr = etData.getText() != null ? etData.getText().toString().trim() : "";
        if (!dataStr.isEmpty()) {
            try {
                // yyyy/MM/dd formatutik parseatu
                SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(dataStr);
                if (d != null) selection = d.getTime();
            } catch (Exception ignored) {}
        }
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.data_hautatu))
                .setSelection(selection);
        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selectionMillis -> {
            // yyyy/MM/dd formatuan jarri
            SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            etData.setText(sdf.format(new Date(selectionMillis)));
        });
        picker.show(getSupportFragmentManager(), "DATA_PICKER");
    }

    /** MaterialTimePicker erakusten du — ordua hautatzeko. */
    private void erakutsiOrduaHautatzailea() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int minute = Calendar.getInstance().get(Calendar.MINUTE);
        String orduaStr = etCitaOrdua.getText() != null ? etCitaOrdua.getText().toString().trim() : "";
        if (!orduaStr.isEmpty()) {
            try {
                String[] part = orduaStr.split(":");
                if (part.length >= 2) {
                    hour = Integer.parseInt(part[0].trim());
                    minute = Integer.parseInt(part[1].trim());
                }
            } catch (Exception ignored) {}
        }
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(getString(R.string.ordua_hautatu))
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            int h = picker.getHour();
            int m = picker.getMinute();
            etCitaOrdua.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
        });
        picker.show(getSupportFragmentManager(), "ORDUA_PICKER");
    }

    private void gordeCita() {
        String zenbakia = etZenbakia.getText() != null ? etZenbakia.getText().toString().trim() : "";
        String data = etData.getText() != null ? etData.getText().toString().trim() : "";
        String ordua = etCitaOrdua.getText() != null ? etCitaOrdua.getText().toString().trim() : "";
        
        if (zenbakia.isEmpty()) {
            Toast.makeText(this, R.string.zita_errorea_zenbakia, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Data balidatu (ordua baino lehen)
        if (data.isEmpty()) {
            Toast.makeText(this, R.string.zita_errorea_data, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Data formatua balidatu (yyyy/MM/dd)
        String erroreMezua = DataBalidatzailea.balidatuDataMezua(data);
        if (erroreMezua != null) {
            Toast.makeText(this, erroreMezua, Toast.LENGTH_LONG).show();
            etData.requestFocus();
            return;
        }
        
        // Ordua gehitu data-ri baldin badago
        if (!ordua.isEmpty()) {
            data = data + " " + ordua;
        }
        
        String ordezkaritza = etOrdezkaritza.getText() != null ? etOrdezkaritza.getText().toString().trim() : "";
        String bazkideaKodea = "";

        final boolean editMode = editatuZenbakia != null && !editatuZenbakia.isEmpty();
        final String dataFinal = data;
        final String zenbakiaFinal = zenbakia;
        final String komertzialKodeaFinal = komertzialKodea;
        final String ordezkaritzaFinal = ordezkaritza.isEmpty() ? "" : ordezkaritza;
        final String bazkideaKodeaFinal = bazkideaKodea;
        runOnUiThread(() -> Toast.makeText(this, R.string.debug_zita_datu_basean, Toast.LENGTH_LONG).show());
        new Thread(() -> {
            try {
                Komertziala kom = datuBasea.komertzialaDao().kodeaBilatu(komertzialKodeaFinal);
                Bazkidea bazkidea = datuBasea.bazkideaDao().nanBilatu(bazkideaKodeaFinal);
                Long komertzialId = kom != null ? kom.getId() : null;
                Long bazkideaId = bazkidea != null ? bazkidea.getId() : null;
                Log.d(ETIKETA, "Gorde: komertzialId=" + komertzialId + ", bazkideaId=" + bazkideaId + ", editMode=" + editMode);

                if (editMode) {
                    EskaeraGoiburua goi = datuBasea.eskaeraGoiburuaDao().zenbakiaBilatu(editatuZenbakia);
                    if (goi != null) {
                        goi.setData(dataFinal);
                        goi.setKomertzialKodea(komertzialKodeaFinal);
                        goi.setKomertzialId(komertzialId);
                        goi.setOrdezkaritza(ordezkaritzaFinal);
                        goi.setBazkideaKodea(bazkideaKodeaFinal);
                        goi.setBazkideaId(bazkideaId);
                        int errenkadak = datuBasea.eskaeraGoiburuaDao().eguneratu(goi);
                        Log.d(ETIKETA, "Gorde: eguneratu errenkadak=" + errenkadak);
                    } else {
                        Log.e(ETIKETA, "Gorde: Ez da zita aurkitu zenbakiarekin: " + editatuZenbakia);
                        final String mezuZita = getString(R.string.errorea_gordetzean, "Ez da zita aurkitu.");
                        runOnUiThread(() -> Toast.makeText(ZitaGehituActivity.this, mezuZita, Toast.LENGTH_LONG).show());
                        return;
                    }
                } else {
                    EskaeraGoiburua goiBerria = new EskaeraGoiburua();
                    goiBerria.setZenbakia(zenbakiaFinal);
                    goiBerria.setData(dataFinal);
                    goiBerria.setKomertzialKodea(komertzialKodeaFinal);
                    goiBerria.setKomertzialId(komertzialId);
                    goiBerria.setOrdezkaritza(ordezkaritzaFinal);
                    goiBerria.setBazkideaKodea(bazkideaKodeaFinal);
                    goiBerria.setBazkideaId(bazkideaId);
                    long id = datuBasea.eskaeraGoiburuaDao().txertatu(goiBerria);
                    Log.d(ETIKETA, "Gorde: txertatu id=" + id);
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.debug_datu_basea_ondo, Toast.LENGTH_LONG).show();
                    Toast.makeText(this, editMode ? R.string.ondo_gorde_dira_aldaketak : R.string.ondo_gorde_da, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                Log.e(ETIKETA, "Gorde: salbuespena", e);
                String mezu = e.getMessage() != null ? e.getMessage() : "";
                if (mezu.isEmpty()) mezu = getString(R.string.errore_ezezaguna);
                final String mezuFinal = mezu;
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.debug_zita_akatsa, mezuFinal), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
