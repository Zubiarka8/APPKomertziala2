package com.example.appkomertziala;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Agenda;
import com.example.appkomertziala.db.eredua.Partnerra;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Bisita berria sartzeko edo lehendik dagoen bisita editatzeko formularioa.
 * Eremuak: bisita_data, partnerra (kanpo-gakoa), deskribapena, egoera (Egina, Zain, Deuseztatua).
 */
public class BisitaFormularioActivity extends AppCompatActivity {

    /** Editatzeko: bisitaren gako nagusia. &lt; 0 bada bisita berria da. */
    public static final String EXTRA_BISITA_ID = "bisita_id";

    private static final String DATA_FORMAT = "yyyy-MM-dd";

    private TextInputEditText etBisitaData;
    private Spinner spinnerPartnerra;
    private TextInputEditText etBisitaDeskribapena;
    private Spinner spinnerEgoera;
    private AppDatabase datuBasea;
    private List<Partnerra> partnerrak;
    private long editatuId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bisita_formularioa);

        setTitle(getString(R.string.agenda_modulua_bisita_berria));
        datuBasea = AppDatabase.getInstance(this);
        editatuId = getIntent().getLongExtra(EXTRA_BISITA_ID, -1);
        if (editatuId >= 0) {
            setTitle(getString(R.string.btn_editatu));
        }

        etBisitaData = findViewById(R.id.etBisitaData);
        spinnerPartnerra = findViewById(R.id.spinnerBisitaPartnerra);
        etBisitaDeskribapena = findViewById(R.id.etBisitaDeskribapena);
        spinnerEgoera = findViewById(R.id.spinnerBisitaEgoera);
        MaterialButton btnUtzi = findViewById(R.id.btnBisitaUtzi);
        MaterialButton btnGorde = findViewById(R.id.btnBisitaGorde);

        beteEgoeraSpinner();
        etBisitaData.setOnClickListener(v -> erakutsiDataHautatzailea());
        btnUtzi.setOnClickListener(v -> finish());
        btnGorde.setOnClickListener(v -> gordeBisita());

        kargatuPartnerrak();
        if (editatuId >= 0) {
            kargatuBisitaEditatzeko();
        } else {
            String gaur = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault()).format(new Date());
            etBisitaData.setText(gaur);
        }
    }

    /** Egoera spinnerra bete: Egina, Zain, Deuseztatua. */
    private void beteEgoeraSpinner() {
        String[] egoerak = {
                getString(R.string.egoera_egina),
                getString(R.string.egoera_zain),
                getString(R.string.egoera_deuseztatua)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, egoerak);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEgoera.setAdapter(adapter);
    }

    /** Data hautatzeko MaterialDatePicker erakusten du. */
    private void erakutsiDataHautatzailea() {
        long hautatua = MaterialDatePicker.todayInUtcMilliseconds();
        String dataStr = etBisitaData.getText() != null ? etBisitaData.getText().toString().trim() : "";
        if (!dataStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(dataStr);
                if (d != null) hautatua = d.getTime();
            } catch (Exception ignored) {}
        }
        MaterialDatePicker.Builder<Long> eraikitzailea = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.data_hautatu))
                .setSelection(hautatua);
        MaterialDatePicker<Long> hautatzailea = eraikitzailea.build();
        hautatzailea.addOnPositiveButtonClickListener(milis -> {
            SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            etBisitaData.setText(sdf.format(new Date(milis)));
        });
        hautatzailea.show(getSupportFragmentManager(), "DATA_PICKER");
    }

    /** Partnerrak datu-baseatik kargatu eta spinnerra bete. */
    private void kargatuPartnerrak() {
        new Thread(() -> {
            partnerrak = datuBasea.partnerraDao().guztiak();
            if (partnerrak == null) partnerrak = new ArrayList<>();
            runOnUiThread(this::betePartnerraSpinner);
        }).start();
    }

    /** Partnerra spinnerra bete (lehenengo errenkada: hautatu). */
    private void betePartnerraSpinner() {
        List<String> izenak = new ArrayList<>();
        izenak.add(getString(R.string.agenda_bisita_partnerra) + " —");
        for (Partnerra p : partnerrak) {
            String s = (p.getIzena() != null ? p.getIzena() : "") + " (" + (p.getKodea() != null ? p.getKodea() : "") + ")";
            izenak.add(s);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, izenak);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPartnerra.setAdapter(adapter);
        if (editatuId >= 0) {
            // KargatuBisitaEditatzeko runOnUiThread-ek spinnerra eguneratuko du
        }
    }

    /** Editatzeko: bisita kargatu eta eremuak bete. */
    private void kargatuBisitaEditatzeko() {
        new Thread(() -> {
            Agenda a = datuBasea.agendaDao().idzBilatu(editatuId);
            if (a == null) return;
            final String data = a.getBisitaData() != null ? a.getBisitaData() : "";
            final String deskribapena = a.getDeskribapena() != null ? a.getDeskribapena() : "";
            final String partnerKodea = a.getPartnerKodea() != null ? a.getPartnerKodea() : "";
            final String egoera = a.getEgoera() != null ? a.getEgoera() : "";
            runOnUiThread(() -> {
                etBisitaData.setText(data);
                etBisitaDeskribapena.setText(deskribapena);
                hautatuPartnerraSpinner(partnerKodea);
                hautatuEgoeraSpinner(egoera);
            });
        }).start();
    }

    /** Partnerra spinnerrean hautatu kodearen arabera. */
    private void hautatuPartnerraSpinner(String partnerKodea) {
        if (partnerrak == null || partnerKodea == null || partnerKodea.isEmpty()) return;
        for (int i = 0; i < partnerrak.size(); i++) {
            Partnerra p = partnerrak.get(i);
            if (p.getKodea() != null && p.getKodea().equals(partnerKodea)) {
                spinnerPartnerra.setSelection(i + 1);
                return;
            }
        }
    }

    /** Egoera spinnerrean hautatu balioaren arabera. */
    private void hautatuEgoeraSpinner(String egoera) {
        String egina = getString(R.string.egoera_egina);
        String zain = getString(R.string.egoera_zain);
        String deuseztatua = getString(R.string.egoera_deuseztatua);
        if (egina.equals(egoera)) spinnerEgoera.setSelection(0);
        else if (zain.equals(egoera)) spinnerEgoera.setSelection(1);
        else if (deuseztatua.equals(egoera)) spinnerEgoera.setSelection(2);
    }

    /** Formularioa gorde: txertatu (berria) edo eguneratu (editatzeko). */
    private void gordeBisita() {
        String data = etBisitaData.getText() != null ? etBisitaData.getText().toString().trim() : "";
        if (data.isEmpty()) {
            Toast.makeText(this, R.string.bisita_errorea_data, Toast.LENGTH_SHORT).show();
            return;
        }
        String deskribapena = etBisitaDeskribapena.getText() != null ? etBisitaDeskribapena.getText().toString().trim() : "";
        String partnerKodea = "";
        int pos = spinnerPartnerra.getSelectedItemPosition();
        if (pos > 0 && partnerrak != null && pos <= partnerrak.size()) {
            Partnerra p = partnerrak.get(pos - 1);
            partnerKodea = p.getKodea() != null ? p.getKodea() : "";
        }
        String egoera = getString(R.string.egoera_zain);
        int egoeraPos = spinnerEgoera.getSelectedItemPosition();
        if (egoeraPos == 0) egoera = getString(R.string.egoera_egina);
        else if (egoeraPos == 1) egoera = getString(R.string.egoera_zain);
        else if (egoeraPos == 2) egoera = getString(R.string.egoera_deuseztatua);

        final String dataFinal = data;
        final String deskribapenaFinal = deskribapena;
        final String partnerKodeaFinal = partnerKodea;
        final String egoeraFinal = egoera;
        final long editatuIdFinal = editatuId;

        new Thread(() -> {
            try {
                if (editatuIdFinal >= 0) {
                    Agenda a = datuBasea.agendaDao().idzBilatu(editatuIdFinal);
                    if (a != null) {
                        a.setBisitaData(dataFinal);
                        a.setDeskribapena(deskribapenaFinal);
                        a.setPartnerKodea(partnerKodeaFinal);
                        a.setEgoera(egoeraFinal);
                        datuBasea.agendaDao().eguneratu(a);
                    }
                } else {
                    Agenda berria = new Agenda();
                    berria.setBisitaData(dataFinal);
                    berria.setDeskribapena(deskribapenaFinal);
                    berria.setPartnerKodea(partnerKodeaFinal);
                    berria.setEgoera(egoeraFinal);
                    datuBasea.agendaDao().txertatu(berria);
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, editatuIdFinal >= 0 ? R.string.ondo_gorde_dira_aldaketak : R.string.ondo_gorde_da, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                String mezu = e.getMessage() != null ? e.getMessage() : "";
                runOnUiThread(() ->
                        Toast.makeText(this, getString(R.string.errorea_gordetzean, mezu != null && !mezu.isEmpty() ? mezu : getString(R.string.errore_ezezaguna)), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
