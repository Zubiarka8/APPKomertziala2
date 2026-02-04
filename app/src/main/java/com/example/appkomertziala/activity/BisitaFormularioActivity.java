package com.example.appkomertziala.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appkomertziala.MainActivity;
import com.example.appkomertziala.R;
import com.example.appkomertziala.databinding.ActivityBisitaFormularioaBinding;
import com.example.appkomertziala.db.AppDatabase;
import com.example.appkomertziala.db.eredua.Agenda;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.example.appkomertziala.db.eredua.Komertziala;
import com.example.appkomertziala.segurtasuna.DataBalidatzailea;
import com.example.appkomertziala.xml.XMLKudeatzailea;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Bisita berria sartzeko edo lehendik dagoen bisita editatzeko formularioa.
 * 
 * Hau hemen bisita berria gehitu edo lehendik dagoen bisita editatu dezakegu.
 * Formularioa bete behar da: data, ordua, bazkidea, deskribapena, egoera.
 * Datuak balidatu behar dira: data eta deskribapena beharrezkoak, ordua formatua HH:mm.
 * 
 * Datuen balidazioa (UI): hutsen kontrolak, formatuen egiaztapena, erabiltzailearen feedback-a
 * TextInputLayout/Toast bidez, euskaraz.
 * Datu-basearen integritatea: kanpo-gakoen egiaztapena (bazkidea_kodea Bazkidea taulan),
 * Room transakzio seguruak.
 */
public class BisitaFormularioActivity extends AppCompatActivity {

    private static final String ETIKETA_LOG = "BisitaFormulario";

    /** Editatzeko: bisitaren gako nagusia (Intent extra). < 0 bada bisita berria da. */
    public static final String EXTRA_BISITA_ID = "bisita_id";

    /** Data formatua (yyyy/MM/dd). Integritasun-mugak: datu-baseak string gisa gordetzen du. */
    private static final String DATA_FORMAT = "yyyy/MM/dd";
    
    /** Deskribapenaren gehienezko luzera (karaktere). Balidazio-muga. */
    private static final int DESKRIBAPENA_GEHIENEZKO_LUZERA = 500;

    /** ViewBinding - UI elementuak kargatzeko. */
    private ActivityBisitaFormularioaBinding binding;
    
    /** Datu-basea (Room). */
    private AppDatabase datuBasea;
    
    /** Bazkide zerrenda (spinner betetzeko). */
    private List<Bazkidea> bazkideak;
    
    /** Editatzeko bisitaren ID. -1 bada berria da, >= 0 bada editatzen ari gara. */
    private long editatuId = -1;

    /**
     * Activity sortzean: ViewBinding kargatu, UI elementuak konfiguratu,
     * bazkideak kargatu, eta editatzen badago bisita kargatu.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ViewBinding kargatu - hau hemen badago, dena ondo doa
        binding = ActivityBisitaFormularioaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Izenburua jarri - editatzen badago "Editatu", bestela "Bisita berria"
        setTitle(getString(R.string.agenda_modulua_bisita_berria));
        
        // Datu-basea eta editatu ID lortu
        datuBasea = AppDatabase.getInstance(this);
        editatuId = getIntent() != null ? getIntent().getLongExtra(EXTRA_BISITA_ID, -1) : -1;
        if (editatuId >= 0) {
            setTitle(getString(R.string.btn_editatu));
        }

        // UI konfiguratu: erroreak garbitu fokusean, egoera spinner bete, listener-ak
        konfiguratuFokuseanErroreakGarbitu();
        beteEgoeraSpinner();
        binding.etBisitaData.setOnClickListener(v -> erakutsiDataHautatzailea());
        binding.etBisitaOrdua.setOnClickListener(v -> erakutsiOrduaHautatzailea());
        binding.btnBisitaUtzi.setOnClickListener(v -> finish());
        binding.btnBisitaGorde.setOnClickListener(v -> gordeBisita());

        // Bazkideak kargatu (hilo nagusitik kanpo)
        kargatuBazkideak();
        
        // Berria bada, gaurko data lehenetsi
        if (editatuId < 0) {
            String gaur = DataBalidatzailea.gaurkoData();
            binding.etBisitaData.setText(gaur);
        }
    }

    /**
     * Eremuetan fokusa sartzean balidazio-erroreak garbitu (TextInputLayout.setError null).
     * 
     * Erabiltzailearen feedback-a: erroreak soilik balidazio berrietan berriro erakusten dira.
     * Erabiltzaileak eremuan sartzean, errorea automatikoki garbitzen da - UX hobea.
     */
    private void konfiguratuFokuseanErroreakGarbitu() {
        // Data eremua: fokusa sartzean errorea garbitu
        binding.etBisitaData.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilBisitaData.setError(null);
        });
        
        // Ordua eremua: fokusa sartzean errorea garbitu (null bada ez egin)
        if (binding.etBisitaOrdua != null) {
            binding.etBisitaOrdua.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && binding.tilBisitaOrdua != null) binding.tilBisitaOrdua.setError(null);
            });
        }
        
        // Deskribapena eremua: fokusa sartzean errorea garbitu
        binding.etBisitaDeskribapena.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilBisitaDeskribapena.setError(null);
        });
    }

    /**
     * Egoera spinnerra bete: Egina, Zain, Deuseztatua.
     * 
     * Spinner-ak egoera aukerak erakusten ditu - bisita bakoitzak egoera bat du.
     */
    private void beteEgoeraSpinner() {
        // Egoera aukerak: Egina, Zain, Deuseztatua
        String[] egoerak = {
                getString(R.string.egoera_egina),
                getString(R.string.egoera_zain),
                getString(R.string.egoera_deuseztatua)
        };
        
        // ArrayAdapter sortu eta spinner-ari lotu
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, egoerak);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerBisitaEgoera.setAdapter(adapter);
    }

    /**
     * Data hautatzeko MaterialDatePicker erakusten du.
     * 
     * Data eremua sakatzean, MaterialDatePicker erakusten da data hautatzeko.
     * Data formatua YYYY-MM-DD mantentzen da - datu-basean gordetzeko formatua.
     */
    private void erakutsiDataHautatzailea() {
        // Gaurko data lehenetsi
        long hautatua = MaterialDatePicker.todayInUtcMilliseconds();
        
        // Eremuan dagoen data parseatu (existitzen bada)
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
        
        // MaterialDatePicker sortu eta erakutsi
        MaterialDatePicker.Builder<Long> eraikitzailea = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.data_hautatu))
                .setSelection(hautatua);
        MaterialDatePicker<Long> hautatzailea = eraikitzailea.build();
        
        // Data hautatzean, eremuan jarri (yyyy/MM/dd formatuan)
        hautatzailea.addOnPositiveButtonClickListener(milis -> {
            SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            binding.etBisitaData.setText(sdf.format(new Date(milis)));
        });
        hautatzailea.show(getSupportFragmentManager(), "DATA_PICKER");
    }

    /**
     * Ordua hautatzeko TimePicker erakusten du.
     * 
     * Ordua eremua sakatzean, TimePicker erakusten da ordua hautatzeko.
     * Ordua formatua HH:mm mantentzen da - datu-basean gordetzeko formatua.
     */
    private void erakutsiOrduaHautatzailea() {
        // Uneko ordua lehenetsi
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        int minute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE);
        
        // TimePicker sortu eta erakutsi
        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                this,
                (view, hourOfDay, minuteSelected) -> {
                    // Ordua hautatzean, eremuan jarri (HH:mm formatuan)
                    String ordua = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteSelected);
                    binding.etBisitaOrdua.setText(ordua);
                },
                hour,
                minute,
                true // 24 ordu formatua
        );
        timePickerDialog.show();
    }

    /**
     * Bazkideak datu-baseatik kargatu eta spinnerra bete.
     * 
     * Bazkide GUZTIAK kargatzen dira, iragazkirik gabe.
     * Hutsik badago, XML guztiak kargatzen ditu (datu-basea betetzeko) eta gero zerrenda osoa erakusten du.
     * Editatzeko: spinner bete ondoren bisita kargatzen da, hautatutako bazkidea lehentasunez mantentzeko.
     */
    private void kargatuBazkideak() {
        new Thread(() -> {
            try {
                // Bazkide GUZTIAK kargatu, iragazkirik gabe - hau hemen badago, dena ondo doa
                bazkideak = datuBasea.bazkideaDao().guztiak();
                if (bazkideak == null) {
                    bazkideak = new ArrayList<>();
                }
                
                // Hutsik badago, XML guztiak kargatzen ditu (datu-basea betetzeko)
                if (bazkideak.isEmpty()) {
                    try {
                        XMLKudeatzailea kud = new XMLKudeatzailea(this);
                        kud.guztiakInportatu();
                        bazkideak = datuBasea.bazkideaDao().guztiak();
                    } catch (Exception e) {
                        // XML guztiak kargatzean akatsa - bazkideak.xml bakarrik saiatu
                        try {
                            XMLKudeatzailea kud = new XMLKudeatzailea(this);
                            kud.inportatuFitxategia("bazkideak.xml");
                            bazkideak = datuBasea.bazkideaDao().guztiak();
                        } catch (Exception ignored) {
                            // Utzi zerrenda hutsik - erabiltzaileak XML kargatu behar du
                        }
                    }
                }
                
                // Segurtasuna: bazkideak null bada, zerrenda hutsa erabili
                if (bazkideak == null) {
                    bazkideak = new ArrayList<>();
                }
                
                Log.d(ETIKETA_LOG, "Bazkideak kargatuta: " + bazkideak.size() + " bazkide");
                
                // UI eguneratu - hilo nagusian
                runOnUiThread(() -> {
                    beteBazkideaSpinner();
                    // Editatzen badago, bisita kargatu (spinner bete ondoren)
                    if (editatuId >= 0) kargatuBisitaEditatzeko();
                });
            } catch (Exception e) {
                // Errorea log-ean erregistratu eta erabiltzaileari erakutsi
                Log.e(ETIKETA_LOG, "Errorea bazkideak kargatzean: " + (e.getMessage() != null ? e.getMessage() : "ezezaguna"), e);
                bazkideak = new ArrayList<>();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Errorea bazkideak kargatzean", Toast.LENGTH_SHORT).show();
                    beteBazkideaSpinner();
                });
            }
        }).start();
    }

    /**
     * Bazkidea spinnerra bete (lehenengo errenkada: «Hautatu»).
     * 
     * Bazkide GUZTIAK erakusten dira - izena eta abizena batera, edo kodea hutsik badago.
     * Lehenengo errenkada "Hautatu" da - hautaketa hutsa erakusteko.
     */
    private void beteBazkideaSpinner() {
        List<String> izenak = new ArrayList<>();
        // Lehenengo errenkada: hautaketa hutsa
        izenak.add(getString(R.string.agenda_bisita_bazkidea) + " —");
        
        // Bazkide GUZTIAK gehitu spinnerrera
        if (bazkideak != null && !bazkideak.isEmpty()) {
            for (Bazkidea b : bazkideak) {
                String izena = (b.getIzena() != null ? b.getIzena().trim() : "") + 
                               (b.getAbizena() != null && !b.getAbizena().trim().isEmpty() ? " " + b.getAbizena().trim() : "");
                String nan = b.getNan() != null ? b.getNan() : "";
                String s = izena.isEmpty() ? nan : izena + (nan.isEmpty() ? "" : " (" + nan + ")");
                izenak.add(s);
            }
            Log.d(ETIKETA_LOG, "Spinner beteta: " + izenak.size() + " elementu (1 hautaketa + " + bazkideak.size() + " bazkide)");
        } else {
            Log.w(ETIKETA_LOG, "Bazkide zerrenda hutsik dago edo null da");
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, izenak);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerBisitaBazkidea.setAdapter(adapter);
    }

    /**
     * Editatzeko: bisita kargatu eta eremuak bete.
     * 
     * SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu,
     * eta bakarrik bere bisitak editatu ditzake (idzBilatuSegurua).
     * Bisita aurkitu ondoren, formulario eremuak betetzen dira.
     */
    private void kargatuBisitaEditatzeko() {
        new Thread(() -> {
            try {
                // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                // Begiratu hemen ea kodea badaukagun - saioa hasita dagoen egiaztatu
                com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                    new com.example.appkomertziala.segurtasuna.SessionManager(this);
                String komertzialKodea = sessionManager.getKomertzialKodea();
                
                // Kodea hutsik badago, saioa ez dago hasita - errorea erakutsi eta itxi
                if (komertzialKodea == null || komertzialKodea.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Saioa ez dago hasita", Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }
                
                // SEGURTASUNA: idzBilatuSegurua erabili, ez idzBilatu
                // Hau hemen badago, dena ondo doa - bakarrik uneko komertzialaren bisitak editatu
                Agenda a = datuBasea.agendaDao().idzBilatuSegurua(editatuId, komertzialKodea);
                if (a == null) {
                    // Bisita ez da aurkitu edo ez duzu sarbiderik
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Bisita ez da aurkitu edo ez duzu sarbiderik", Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }
                
                // Datuak atera eta formularioa bete
                String dataRaw = a.getBisitaData() != null ? a.getBisitaData() : "";
                // Data formatua bihurtu yyyy/MM/dd formatura baldin beharrezkoa bada
                final String data = DataBalidatzailea.bihurtuFormatua(dataRaw) != null ? 
                    DataBalidatzailea.bihurtuFormatua(dataRaw) : dataRaw;
                final String ordua = a.getOrdua() != null ? a.getOrdua() : "";
                final String deskribapena = a.getDeskribapena() != null ? a.getDeskribapena() : "";
                final String bazkideaKodea = a.getBazkideaKodea() != null ? a.getBazkideaKodea() : "";
                final String egoera = a.getEgoera() != null ? a.getEgoera() : "";
                
                // UI eguneratu - hilo nagusian
                runOnUiThread(() -> {
                    binding.etBisitaData.setText(data);
                    if (binding.etBisitaOrdua != null) {
                        binding.etBisitaOrdua.setText(ordua);
                    }
                    binding.etBisitaDeskribapena.setText(deskribapena);
                    hautatuBazkideaSpinner(bazkideaKodea);
                    hautatuEgoeraSpinner(egoera);
                });
            } catch (Exception e) {
                Log.e(ETIKETA_LOG, "Errorea bisita kargatzean", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Errorea bisita kargatzean", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    /**
     * Bazkidea spinnerrean hautatu kodearen arabera.
     * 
     * Bazkidea kodea (NAN) erabiliz spinner-aren posizioa aurkitu eta hautatu.
     * 
     * @param bazkideaKodea Bazkidearen kodea (NAN)
     */
    private void hautatuBazkideaSpinner(String bazkideaKodea) {
        // Segurtasuna: kodea hutsik badago, ezer ez egin
        if (bazkideak == null || bazkideaKodea == null || bazkideaKodea.isEmpty()) return;
        
        // Bazkidea kodea bilatu zerrendan
        for (int i = 0; i < bazkideak.size(); i++) {
            Bazkidea b = bazkideak.get(i);
            if (b.getNan() != null && b.getNan().equals(bazkideaKodea)) {
                // Aurkitu da - spinner-aren posizioa ezarri (i + 1 lehenengo "Hautatu" errenkada dela)
                binding.spinnerBisitaBazkidea.setSelection(i + 1);
                return;
            }
        }
    }

    /**
     * Egoera spinnerrean hautatu balioaren arabera.
     * 
     * Egoera string-a erabiliz spinner-aren posizioa aurkitu eta hautatu.
     * 
     * @param egoera Egoera string-a (Egina, Zain, Deuseztatua)
     */
    private void hautatuEgoeraSpinner(String egoera) {
        // Egoera string-ak lortu
        String egina = getString(R.string.egoera_egina);
        String zain = getString(R.string.egoera_zain);
        String deuseztatua = getString(R.string.egoera_deuseztatua);
        
        // Egoera string-a konparatu eta posizioa ezarri
        if (egina.equals(egoera)) binding.spinnerBisitaEgoera.setSelection(0);
        else if (zain.equals(egoera)) binding.spinnerBisitaEgoera.setSelection(1);
        else if (deuseztatua.equals(egoera)) binding.spinnerBisitaEgoera.setSelection(2);
    }

    /**
     * Datuen balidazioa (UI mailan).
     * Hutsen kontrolak: data, bazkidea_kodea eta deskribapena ez hutsik.
     * Formatuen egiaztapena: data YYYY-MM-DD formatuan, ordua HH:mm formatuan.
     * Erabiltzailearen feedback-a: TextInputLayout.setError edo Toast, euskaraz.
     *
     * @return true balidazioak gainditu baditu, false bestela
     */
    private boolean baliozkotuFormularioa() {
        binding.tilBisitaData.setError(null);
        if (binding.tilBisitaOrdua != null) binding.tilBisitaOrdua.setError(null);
        binding.tilBisitaDeskribapena.setError(null);

        String data = binding.etBisitaData.getText() != null ? binding.etBisitaData.getText().toString().trim() : "";
        String ordua = binding.etBisitaOrdua != null && binding.etBisitaOrdua.getText() != null 
                ? binding.etBisitaOrdua.getText().toString().trim() : "";
        String deskribapena = binding.etBisitaDeskribapena.getText() != null ? binding.etBisitaDeskribapena.getText().toString().trim() : "";
        int pos = binding.spinnerBisitaBazkidea.getSelectedItemPosition();

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
                Toast.makeText(this, R.string.bisita_errorea_deskribapena_hutsa, Toast.LENGTH_SHORT).show();
                binding.spinnerBisitaBazkidea.requestFocus();
            }
            return false;
        }

        // Ordua formatua baliozkotu (HH:mm) - aukerakoa baina formatua zuzena izan behar du
        if (!ordua.isEmpty() && !ordua.matches("\\d{2}:\\d{2}")) {
            if (binding.tilBisitaOrdua != null) {
                binding.tilBisitaOrdua.setError("Ordua HH:mm formatuan izan behar da");
                binding.etBisitaOrdua.requestFocus();
            }
            return false;
        }

        // Data formatua balidatu (yyyy/MM/dd)
        String erroreMezua = DataBalidatzailea.balidatuDataMezua(data);
        if (erroreMezua != null) {
            binding.tilBisitaData.setError(erroreMezua);
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


    /**
     * Formularioa gorde: txertatu (berria) edo eguneratu (editatzeko).
     * Datu-basearen integritatea: kanpo-gakoen egiaztapena (bazkidea_kodea Bazkidea taulan), transakzio seguruak.
     * Salbuespenen kudeaketa: try-catch, erroreak euskaraz Toast eta log.
     */
    private void gordeBisita() {
        if (!baliozkotuFormularioa()) return;

        String data = binding.etBisitaData.getText() != null ? binding.etBisitaData.getText().toString().trim() : "";
        String ordua = binding.etBisitaOrdua != null && binding.etBisitaOrdua.getText() != null 
                ? binding.etBisitaOrdua.getText().toString().trim() : "";
        String deskribapena = binding.etBisitaDeskribapena.getText() != null ? binding.etBisitaDeskribapena.getText().toString().trim() : "";
        String bazkideaKodea = "";
        Long bazkideaIdFinal = null;
        int pos = binding.spinnerBisitaBazkidea.getSelectedItemPosition();
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
        final String orduaFinal = ordua;
        final String deskribapenaFinal = deskribapena;
        final String bazkideaKodeaFinal = bazkideaKodea;
        final Long bazkideaIdGordetzeko = bazkideaIdFinal;
        final Long komertzialaIdGordetzeko = komertzialaIdFinal;
        final String komertzialKodeaFinal = komertzialKodea;
        final String egoeraFinal = egoera;
        final long editatuIdFinal = editatuId;

        new Thread(() -> {
            try {
                // Kanpo-gakoen egiaztapena: bazkidea_kodea Bazkidea taulan existitzen dela ziurtatu (bisita sortu aurretik)
                if (datuBasea.bazkideaDao().nanBilatu(bazkideaKodeaFinal) == null) {
                    runOnUiThread(() -> Toast.makeText(this, R.string.bisita_errorea_deskribapena_hutsa, Toast.LENGTH_LONG).show());
                    return;
                }
                // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                    new com.example.appkomertziala.segurtasuna.SessionManager(this);
                String komertzialKodeaSegurua = sessionManager.getKomertzialKodea();
                
                if (komertzialKodeaSegurua == null || komertzialKodeaSegurua.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "Saioa ez dago hasita", Toast.LENGTH_LONG).show());
                    return;
                }
                
                // Egiaztatu komertzial kodea bat datorrela
                if (!komertzialKodeaSegurua.equals(komertzialKodeaFinal)) {
                    runOnUiThread(() -> Toast.makeText(this, "Errorea: komertzial kodea ez dator bat", Toast.LENGTH_LONG).show());
                    return;
                }
                
                // Transakzio seguruak: datuen osotasuna bermatzeko altak/aldaketak transakzio bakar batean
                datuBasea.runInTransaction(() -> {
                    if (editatuIdFinal >= 0) {
                        // SEGURTASUNA: idzBilatuSegurua erabili, ez idzBilatu
                        Agenda a = datuBasea.agendaDao().idzBilatuSegurua(editatuIdFinal, komertzialKodeaSegurua);
                        if (a != null) {
                            a.setBisitaData(dataFinal);
                            a.setOrdua(orduaFinal);
                            a.setDeskribapena(deskribapenaFinal);
                            a.setBazkideaKodea(bazkideaKodeaFinal);
                            a.setBazkideaId(bazkideaIdGordetzeko);
                            a.setKomertzialKodea(komertzialKodeaFinal);
                            a.setKomertzialaId(komertzialaIdGordetzeko);
                            a.setEgoera(egoeraFinal);
                            // SEGURTASUNA: eguneratuSegurua erabili (edo @Update baina balidazioarekin)
                            datuBasea.agendaDao().eguneratu(a);
                        }
                    } else {
                        Agenda berria = new Agenda();
                        berria.setBisitaData(dataFinal);
                        berria.setOrdua(orduaFinal);
                        berria.setDeskribapena(deskribapenaFinal);
                        berria.setBazkideaKodea(bazkideaKodeaFinal);
                        berria.setBazkideaId(bazkideaIdGordetzeko);
                        berria.setKomertzialKodea(komertzialKodeaFinal);
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
