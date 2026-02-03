package com.example.appkomertziala;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appkomertziala.databinding.ActivityBisitaFormularioaBinding;
import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Agenda;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.example.appkomertziala.db.eredua.Komertziala;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Bisita berria sartzeko edo lehendik dagoen bisita editatzeko formularioa.
 * Datuen balidazioa (UI): hutsen kontrolak, formatuen egiaztapena, erabiltzailearen feedback-a TextInputLayout/Toast bidez, euskaraz.
 * Datu-basearen integritatea: kanpo-gakoen egiaztapena (bazkidea_kodea Bazkidea taulan), Room transakzio seguruak.
 */
public class BisitaFormularioActivity extends AppCompatActivity {

    private static final String ETIKETA_LOG = "BisitaFormulario";

    /** Editatzeko: bisitaren gako nagusia. &lt; 0 bada bisita berria da. */
    public static final String EXTRA_BISITA_ID = "bisita_id";

    /** Data formatua (YYYY-MM-DD). Integritasun-mugak: datu-baseak string gisa gordetzen du. */
    private static final String DATA_FORMAT = "yyyy-MM-dd";
    /** Deskribapenaren gehienezko luzera (karaktere). Balidazio-muga. */
    private static final int DESKRIBAPENA_GEHIENEZKO_LUZERA = 500;

    private ActivityBisitaFormularioaBinding binding;
    private AppDatabase datuBasea;
    private List<Bazkidea> bazkideak;
    private long editatuId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBisitaFormularioaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setTitle(getString(R.string.agenda_modulua_bisita_berria));
        datuBasea = AppDatabase.getInstance(this);
        editatuId = getIntent() != null ? getIntent().getLongExtra(EXTRA_BISITA_ID, -1) : -1;
        if (editatuId >= 0) {
            setTitle(getString(R.string.btn_editatu));
        }

        konfiguratuFokuseanErroreakGarbitu();
        beteEgoeraSpinner();
        binding.etBisitaData.setOnClickListener(v -> erakutsiDataHautatzailea());
        binding.btnBisitaUtzi.setOnClickListener(v -> finish());
        binding.btnBisitaGorde.setOnClickListener(v -> gordeBisita());

        kargatuBazkideak();
        if (editatuId < 0) {
            String gaur = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault()).format(new Date());
            binding.etBisitaData.setText(gaur);
        }
    }

    /**
     * Eremuetan fokusa sartzean balidazio-erroreak garbitu (TextInputLayout.setError null).
     * Erabiltzailearen feedback-a: erroreak soilik balidazio berrietan berriro erakusten dira.
     */
    private void konfiguratuFokuseanErroreakGarbitu() {
        binding.etBisitaData.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilBisitaData.setError(null);
        });
        binding.etBisitaDeskribapena.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilBisitaDeskribapena.setError(null);
        });
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
        binding.spinnerBisitaEgoera.setAdapter(adapter);
    }

    /** Data hautatzeko MaterialDatePicker erakusten du. Data formatua YYYY-MM-DD mantentzen da. */
    private void erakutsiDataHautatzailea() {
        long hautatua = MaterialDatePicker.todayInUtcMilliseconds();
        String dataStr = binding.etBisitaData.getText() != null ? binding.etBisitaData.getText().toString().trim() : "";
        if (!dataStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(dataStr);
                if (d != null) hautatua = d.getTime();
            } catch (Exception e) {
                Log.w(ETIKETA_LOG, "Data parseatzean salbuespena (hautatzailea): " + (e.getMessage() != null ? e.getMessage() : "ezezaguna"));
            }
        }
        MaterialDatePicker.Builder<Long> eraikitzailea = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.data_hautatu))
                .setSelection(hautatua);
        MaterialDatePicker<Long> hautatzailea = eraikitzailea.build();
        hautatzailea.addOnPositiveButtonClickListener(milis -> {
            SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            binding.etBisitaData.setText(sdf.format(new Date(milis)));
        });
        hautatzailea.show(getSupportFragmentManager(), "DATA_PICKER");
    }

    /**
     * Bazkideak datu-baseatik kargatu eta spinnerra bete.
     * Editatzeko: spinner bete ondoren bisita kargatzen da, hautatutako bazkidea lehentasunez mantentzeko.
     */
    private void kargatuBazkideak() {
        new Thread(() -> {
            bazkideak = datuBasea.bazkideaDao().guztiak();
            if (bazkideak == null) bazkideak = new ArrayList<>();
            runOnUiThread(() -> {
                beteBazkideaSpinner();
                if (editatuId >= 0) kargatuBisitaEditatzeko();
            });
        }).start();
    }

    /** Bazkidea spinnerra bete (lehenengo errenkada: «Hautatu»). */
    private void beteBazkideaSpinner() {
        List<String> izenak = new ArrayList<>();
        izenak.add(getString(R.string.agenda_bisita_partnerra) + " —");
        for (Bazkidea b : bazkideak) {
            String izena = (b.getIzena() != null ? b.getIzena().trim() : "") + 
                           (b.getAbizena() != null && !b.getAbizena().trim().isEmpty() ? " " + b.getAbizena().trim() : "");
            String nan = b.getNan() != null ? b.getNan() : "";
            String s = izena.isEmpty() ? nan : izena + (nan.isEmpty() ? "" : " (" + nan + ")");
            izenak.add(s);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, izenak);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerBisitaPartnerra.setAdapter(adapter);
    }

    /** Editatzeko: bisita kargatu eta eremuak bete. */
    private void kargatuBisitaEditatzeko() {
        new Thread(() -> {
            Agenda a = datuBasea.agendaDao().idzBilatu(editatuId);
            if (a == null) return;
            final String data = a.getBisitaData() != null ? a.getBisitaData() : "";
            final String deskribapena = a.getDeskribapena() != null ? a.getDeskribapena() : "";
            final String bazkideaKodea = a.getBazkideaKodea() != null ? a.getBazkideaKodea() : "";
            final String egoera = a.getEgoera() != null ? a.getEgoera() : "";
            runOnUiThread(() -> {
                binding.etBisitaData.setText(data);
                binding.etBisitaDeskribapena.setText(deskribapena);
                hautatuBazkideaSpinner(bazkideaKodea);
                hautatuEgoeraSpinner(egoera);
            });
        }).start();
    }

    /** Bazkidea spinnerrean hautatu kodearen arabera. */
    private void hautatuBazkideaSpinner(String bazkideaKodea) {
        if (bazkideak == null || bazkideaKodea == null || bazkideaKodea.isEmpty()) return;
        for (int i = 0; i < bazkideak.size(); i++) {
            Bazkidea b = bazkideak.get(i);
            if (b.getNan() != null && b.getNan().equals(bazkideaKodea)) {
                binding.spinnerBisitaPartnerra.setSelection(i + 1);
                return;
            }
        }
    }

    /** Egoera spinnerrean hautatu balioaren arabera. */
    private void hautatuEgoeraSpinner(String egoera) {
        String egina = getString(R.string.egoera_egina);
        String zain = getString(R.string.egoera_zain);
        String deuseztatua = getString(R.string.egoera_deuseztatua);
        if (egina.equals(egoera)) binding.spinnerBisitaEgoera.setSelection(0);
        else if (zain.equals(egoera)) binding.spinnerBisitaEgoera.setSelection(1);
        else if (deuseztatua.equals(egoera)) binding.spinnerBisitaEgoera.setSelection(2);
    }

    /**
     * Datuen balidazioa (UI mailan).
     * Hutsen kontrolak: data, partner_kodea eta deskribapena ez hutsik.
     * Formatuen egiaztapena: data YYYY-MM-DD formatuan.
     * Erabiltzailearen feedback-a: TextInputLayout.setError edo Toast, euskaraz.
     *
     * @return true balidazioak gainditu baditu, false bestela
     */
    private boolean baliozkotuFormularioa() {
        binding.tilBisitaData.setError(null);
        binding.tilBisitaDeskribapena.setError(null);

        String data = binding.etBisitaData.getText() != null ? binding.etBisitaData.getText().toString().trim() : "";
        String deskribapena = binding.etBisitaDeskribapena.getText() != null ? binding.etBisitaDeskribapena.getText().toString().trim() : "";
        int pos = binding.spinnerBisitaPartnerra.getSelectedItemPosition();

        boolean dataHutsa = data.isEmpty();
        boolean deskribapenaHutsa = deskribapena.isEmpty();
        boolean bazkideaEzHautatua = pos <= 0 || bazkideak == null || pos > bazkideak.size();

        if (dataHutsa || deskribapenaHutsa || bazkideaEzHautatua) {
            Toast.makeText(this, R.string.eremu_guztiak_bete_behar_dira, Toast.LENGTH_SHORT).show();
            if (dataHutsa) {
                binding.tilBisitaData.setError(getString(R.string.bisita_errorea_data));
                binding.etBisitaData.requestFocus();
            }
            if (deskribapenaHutsa) {
                binding.tilBisitaDeskribapena.setError(getString(R.string.bisita_errorea_deskribapena_hutsa));
                if (!dataHutsa) binding.etBisitaDeskribapena.requestFocus();
            }
            if (bazkideaEzHautatua && !dataHutsa && !deskribapenaHutsa) {
                Toast.makeText(this, R.string.bisita_errorea_partnerra, Toast.LENGTH_SHORT).show();
                binding.spinnerBisitaPartnerra.requestFocus();
            }
            return false;
        }

        if (!dataFormatuaZuzena(data)) {
            binding.tilBisitaData.setError(getString(R.string.bisita_errorea_data_formatua));
            binding.etBisitaData.requestFocus();
            return false;
        }

        if (deskribapena.length() > DESKRIBAPENA_GEHIENEZKO_LUZERA) {
            binding.tilBisitaDeskribapena.setError(getString(R.string.bisita_errorea_deskribapena_luzea, DESKRIBAPENA_GEHIENEZKO_LUZERA));
            binding.etBisitaDeskribapena.requestFocus();
            return false;
        }

        return true;
    }

    /** Data formatua baliozkotu (YYYY-MM-DD). Formatuen egiaztapena. */
    private boolean dataFormatuaZuzena(String data) {
        if (data == null || data.isEmpty()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
            sdf.setLenient(false);
            sdf.parse(data);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Formularioa gorde: txertatu (berria) edo eguneratu (editatzeko).
     * Datu-basearen integritatea: kanpo-gakoen egiaztapena (bazkidea_kodea Bazkidea taulan), transakzio seguruak.
     * Salbuespenen kudeaketa: try-catch, erroreak euskaraz Toast eta log.
     */
    private void gordeBisita() {
        if (!baliozkotuFormularioa()) return;

        String data = binding.etBisitaData.getText() != null ? binding.etBisitaData.getText().toString().trim() : "";
        String deskribapena = binding.etBisitaDeskribapena.getText() != null ? binding.etBisitaDeskribapena.getText().toString().trim() : "";
        String bazkideaKodea = "";
        Long bazkideaIdFinal = null;
        int pos = binding.spinnerBisitaPartnerra.getSelectedItemPosition();
        if (pos > 0 && bazkideak != null && pos <= bazkideak.size()) {
            Bazkidea b = bazkideak.get(pos - 1);
            bazkideaKodea = b.getNan() != null ? b.getNan() : "";
            bazkideaIdFinal = b.getId();
        }
        String komertzialKodea = getIntent() != null ? getIntent().getStringExtra(MainActivity.EXTRA_KOMMERTZIALA_KODEA) : null;
        Long komertzialaIdFinal = null;
        if (komertzialKodea != null && !komertzialKodea.trim().isEmpty()) {
            Komertziala kom = datuBasea.komertzialaDao().kodeaBilatu(komertzialKodea.trim());
            if (kom != null) komertzialaIdFinal = kom.getId();
        }
        String egoera = getString(R.string.egoera_zain);
        int egoeraPos = binding.spinnerBisitaEgoera.getSelectedItemPosition();
        if (egoeraPos == 0) egoera = getString(R.string.egoera_egina);
        else if (egoeraPos == 1) egoera = getString(R.string.egoera_zain);
        else if (egoeraPos == 2) egoera = getString(R.string.egoera_deuseztatua);

        final String dataFinal = data;
        final String deskribapenaFinal = deskribapena;
        final String bazkideaKodeaFinal = bazkideaKodea;
        final Long bazkideaIdGordetzeko = bazkideaIdFinal;
        final Long komertzialaIdGordetzeko = komertzialaIdFinal;
        final String egoeraFinal = egoera;
        final long editatuIdFinal = editatuId;

        new Thread(() -> {
            try {
                // Kanpo-gakoen egiaztapena: bazkidea_kodea Bazkidea taulan existitzen dela ziurtatu (bisita sortu aurretik)
                if (datuBasea.bazkideaDao().nanBilatu(bazkideaKodeaFinal) == null) {
                    runOnUiThread(() -> Toast.makeText(this, R.string.bisita_partnerra_ez_datu_basean, Toast.LENGTH_LONG).show());
                    return;
                }
                // Transakzio seguruak: datuen osotasuna bermatzeko altak/aldaketak transakzio bakar batean
                datuBasea.runInTransaction(() -> {
                    if (editatuIdFinal >= 0) {
                        Agenda a = datuBasea.agendaDao().idzBilatu(editatuIdFinal);
                        if (a != null) {
                            a.setBisitaData(dataFinal);
                            a.setDeskribapena(deskribapenaFinal);
                            a.setBazkideaKodea(bazkideaKodeaFinal);
                            a.setBazkideaId(bazkideaIdGordetzeko);
                            a.setKomertzialaId(komertzialaIdGordetzeko);
                            a.setEgoera(egoeraFinal);
                            datuBasea.agendaDao().eguneratu(a);
                        }
                    } else {
                        Agenda berria = new Agenda();
                        berria.setBisitaData(dataFinal);
                        berria.setDeskribapena(deskribapenaFinal);
                        berria.setBazkideaKodea(bazkideaKodeaFinal);
                        berria.setBazkideaId(bazkideaIdGordetzeko);
                        berria.setKomertzialaId(komertzialaIdGordetzeko);
                        berria.setEgoera(egoeraFinal);
                        datuBasea.agendaDao().txertatu(berria);
                    }
                });
                runOnUiThread(() -> {
                    Toast.makeText(this, editatuIdFinal >= 0 ? R.string.ondo_gorde_dira_aldaketak : R.string.ondo_gorde_da, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                String mezu = e.getMessage() != null ? e.getMessage() : "";
                Log.e(ETIKETA_LOG, "Bisita gordetzean salbuespena: " + (mezu.isEmpty() ? "ezezaguna" : mezu), e);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.errorea_gordetzean, !mezu.isEmpty() ? mezu : getString(R.string.errore_ezezaguna)), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
