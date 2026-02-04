package com.example.appkomertziala.db;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.appkomertziala.db.eredua.Agenda;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.example.appkomertziala.db.eredua.Komertziala;
import com.example.appkomertziala.db.kontsultak.AgendaDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Agenda Repository: Room eta UI-aren arteko geruza abstraktua.
 * Repository pattern erabiliz, datu-basearen eragiketak modu seguruan eta asinkronoan exekutatzen dira.
 * Erabiltzaile-interfazea azkar mantentzen du, karga-denborarik gabe.
 * SEGURTASUNA: Bisita guztietan komertzialKodea filtroa aplikatzen da (sarbide kontrola).
 */
public class AgendaRepository {

    /** Log-erako etiketa. */
    private static final String ETIKETA = "AgendaRepository";
    /** Exekutatzailerako hari kopurua (thread pool). */
    private static final int EXEKUTATZAILE_KOPURUA = 4;

    /** Agenda DAO: datu-base kontsultak. */
    private final AgendaDao agendaDao;
    /** Exekutatzailea: eragiketak atzerapen-harian exekutatzeko. */
    private final ExecutorService executorService;
    /** Aplikazioko testuingurua. */
    private final Context context;

    /**
     * Eraikitzailea: AgendaRepository instantzia sortu.
     *
     * @param context Aplikazioko kontekstua (getApplicationContext() erabiliko da)
     */
    public AgendaRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(this.context);
        this.agendaDao = db.agendaDao();
        this.executorService = Executors.newFixedThreadPool(EXEKUTATZAILE_KOPURUA);
    }

    /**
     * Bisita bat txertatu (upsert: existitzen bada eguneratu, bestela sortu).
     * @param agenda Txertatu behar den bisita
     * @param callback Emaitza jaso behar duen callback (null bada, ez da deituko)
     */
    public void txertatuBisita(@NonNull Agenda agenda, @Nullable TxertatuCallback callback) {
        executorService.execute(() -> {
            try {
                long id = agendaDao.txertatu(agenda);
                // Callback deitu emaitza jakinarazteko (null ez bada)
                if (callback != null) {
                    callback.onEmaitza(id > 0, id);
                }
                Log.d(ETIKETA, "Bisita txertatua: ID=" + id);
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea bisita txertatzean", e);
                // Errore kasuan callback-ean false jakinarazi
                if (callback != null) {
                    callback.onEmaitza(false, -1);
                }
            }
        });
    }

    /**
     * Hainbat bisita txertatu transakzio bakar batean (errendimendua bermatzeko).
     * @param bisitak Txertatu behar diren bisitak
     * @param callback Emaitza jaso behar duen callback
     */
    public void txertatuBisitak(@NonNull List<Agenda> bisitak, @Nullable TxertatuGuztiakCallback callback) {
        executorService.execute(() -> {
            try {
                List<Long> ids = agendaDao.txertatuGuztiak(bisitak);
                if (callback != null) {
                    callback.onEmaitza(true, ids.size());
                }
                Log.d(ETIKETA, bisitak.size() + " bisita txertatu dira transakzioan");
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea bisitak txertatzean", e);
                if (callback != null) {
                    callback.onEmaitza(false, 0);
                }
            }
        });
    }

    /**
     * Bisita bat eguneratu.
     * @param agenda Eguneratu behar den bisita
     * @param callback Emaitza jaso behar duen callback
     */
    public void eguneratuBisita(@NonNull Agenda agenda, @Nullable EguneratuCallback callback) {
        executorService.execute(() -> {
            try {
                int errenkadak = agendaDao.eguneratu(agenda);
                boolean ondo = errenkadak > 0;
                if (callback != null) {
                    callback.onEmaitza(ondo, errenkadak);
                }
                Log.d(ETIKETA, "Bisita eguneratua: errenkadak=" + errenkadak);
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea bisita eguneratzean", e);
                if (callback != null) {
                    callback.onEmaitza(false, 0);
                }
            }
        });
    }

    /**
     * Bisita bat ezabatu.
     * @param agenda Ezabatu behar den bisita
     * @param callback Emaitza jaso behar duen callback
     */
    public void ezabatuBisita(@NonNull Agenda agenda, @Nullable EzabatuCallback callback) {
        executorService.execute(() -> {
            try {
                int errenkadak = agendaDao.ezabatu(agenda);
                boolean ondo = errenkadak > 0;
                if (callback != null) {
                    callback.onEmaitza(ondo);
                }
                Log.d(ETIKETA, "Bisita ezabatua: errenkadak=" + errenkadak);
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea bisita ezabatzean", e);
                if (callback != null) {
                    callback.onEmaitza(false);
                }
            }
        });
    }

    /**
     * Bisita guztiak kargatu (SEGURTASUNA: uneko komertzialarenak bakarrik).
     * @param callback Emaitza jaso behar duen callback
     */
    public void kargatuBisitak(@NonNull KargatuCallback callback) {
        executorService.execute(() -> {
            try {
                // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                com.example.appkomertziala.segurtasuna.SessionManager sessionManager =
                    new com.example.appkomertziala.segurtasuna.SessionManager(context);
                String komertzialKodea = sessionManager.getKomertzialKodea();

                // Saioa hasi gabe bada, zerrenda hutsa itzuli
                if (komertzialKodea == null || komertzialKodea.isEmpty()) {
                    Log.w(ETIKETA, "kargatuBisitak: Saioa ez dago hasita");
                    callback.onEmaitza(new java.util.ArrayList<>());
                    return;
                }

                // SEGURTASUNA: getVisitsByKomertzial erabili (komertzial bakoitzak bere bisitak bakarrik)
                List<Agenda> bisitak = agendaDao.getVisitsByKomertzial(komertzialKodea);
                callback.onEmaitza(bisitak);
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea bisitak kargatzean", e);
                callback.onEmaitza(null);
            }
        });
    }

    /**
     * Bisita bat ID baten arabera bilatu.
     * SEGURTASUNA: Uneko komertzialaren bisita bakarrik bilatzen da.
     * @param id Bilatu behar den bisitaren ID
     * @param callback Emaitza jaso behar duen callback
     */
    public void bilatuBisitaIdz(long id, @NonNull BilatuCallback callback) {
        executorService.execute(() -> {
            try {
                // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                    new com.example.appkomertziala.segurtasuna.SessionManager(context);
                String komertzialKodea = sessionManager.getKomertzialKodea();
                
                if (komertzialKodea == null || komertzialKodea.isEmpty()) {
                    Log.w(ETIKETA, "bilatuBisitaIdz: Saioa ez dago hasita");
                    callback.onEmaitza(null);
                    return;
                }
                
                // SEGURTASUNA: idzBilatuSegurua erabili, ez idzBilatu
                Agenda agenda = agendaDao.idzBilatuSegurua(id, komertzialKodea);
                callback.onEmaitza(agenda);
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea bisita bilatzean ID: " + id, e);
                callback.onEmaitza(null);
            }
        });
    }

    /**
     * Data zehatzaren arabera bisitak bilatu.
     * SEGURTASUNA: Uneko komertzialaren bisitak bakarrik bilatzen dira.
     * @param data Bilaketa data (yyyy-MM-dd)
     * @param callback Emaitza jaso behar duen callback
     */
    public void bilatuDataz(@NonNull String data, @NonNull KargatuCallback callback) {
        executorService.execute(() -> {
            try {
                // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                    new com.example.appkomertziala.segurtasuna.SessionManager(context);
                String komertzialKodea = sessionManager.getKomertzialKodea();
                
                if (komertzialKodea == null || komertzialKodea.isEmpty()) {
                    Log.w(ETIKETA, "bilatuDataz: Saioa ez dago hasita");
                    callback.onEmaitza(new java.util.ArrayList<>());
                    return;
                }
                
                // SEGURTASUNA: bilatuDataz segurua erabili
                List<Agenda> bisitak = agendaDao.bilatuDataz(data, komertzialKodea);
                callback.onEmaitza(bisitak);
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea bisitak bilatzean data: " + data, e);
                callback.onEmaitza(null);
            }
        });
    }

    /**
     * Bezeroaren arabera bisitak bilatu (bazkidea kodea, izena edo abizena).
     * SEGURTASUNA: Uneko komertzialaren bisitak bakarrik bilatzen dira.
     * @param filter Bilaketa testua
     * @param callback Emaitza jaso behar duen callback
     */
    public void bilatuBezeroaz(@NonNull String filter, @NonNull KargatuCallback callback) {
        executorService.execute(() -> {
            try {
                // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                    new com.example.appkomertziala.segurtasuna.SessionManager(context);
                String komertzialKodea = sessionManager.getKomertzialKodea();
                
                if (komertzialKodea == null || komertzialKodea.isEmpty()) {
                    Log.w(ETIKETA, "bilatuBezeroaz: Saioa ez dago hasita");
                    callback.onEmaitza(new java.util.ArrayList<>());
                    return;
                }
                
                // SEGURTASUNA: bilatuBezeroaz segurua erabili
                List<Agenda> bisitak = agendaDao.bilatuBezeroaz(filter.trim(), komertzialKodea);
                callback.onEmaitza(bisitak);
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea bisitak bilatzean bezero: " + filter, e);
                callback.onEmaitza(null);
            }
        });
    }

    /**
     * Komertzialaren arabera bisitak bilatu.
     * SEGURTASUNA: Uneko komertzialaren bisitak bakarrik bilatzen dira.
     * ONDO: Metodo hau orain ez da beharrezkoa, komertzial bakoitzak bere bisitak bakarrik ikus ditzakeelako.
     * Hala ere, mantentzen da atzera-egokitasunerako.
     * @param filter Bilaketa testua (ez da erabiltzen, uneko komertzialaren bisitak bakarrik itzultzen dira)
     * @param callback Emaitza jaso behar duen callback
     */
    public void bilatuKomertzialaz(@NonNull String filter, @NonNull KargatuCallback callback) {
        executorService.execute(() -> {
            try {
                // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                    new com.example.appkomertziala.segurtasuna.SessionManager(context);
                String komertzialKodea = sessionManager.getKomertzialKodea();
                
                if (komertzialKodea == null || komertzialKodea.isEmpty()) {
                    Log.w(ETIKETA, "bilatuKomertzialaz: Saioa ez dago hasita");
                    callback.onEmaitza(new java.util.ArrayList<>());
                    return;
                }
                
                // SEGURTASUNA: Uneko komertzialaren bisitak bakarrik itzuli
                // ONDO: bilatuKomertzialaz ez dago, getVisitsByKomertzial erabili
                List<Agenda> bisitak = agendaDao.getVisitsByKomertzial(komertzialKodea);
                callback.onEmaitza(bisitak);
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea bisitak bilatzean komertzial: " + filter, e);
                callback.onEmaitza(null);
            }
        });
    }

    /**
     * Data tartearen arabera bisitak bilatu.
     * SEGURTASUNA: Uneko komertzialaren bisitak bakarrik bilatzen dira.
     * @param hasieraData Hasiera data (yyyy-MM-dd)
     * @param amaieraData Amaiera data (yyyy-MM-dd)
     * @param callback Emaitza jaso behar duen callback
     */
    public void bilatuDataTarteaz(@NonNull String hasieraData, @NonNull String amaieraData, @NonNull KargatuCallback callback) {
        executorService.execute(() -> {
            try {
                // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                    new com.example.appkomertziala.segurtasuna.SessionManager(context);
                String komertzialKodea = sessionManager.getKomertzialKodea();
                
                if (komertzialKodea == null || komertzialKodea.isEmpty()) {
                    Log.w(ETIKETA, "bilatuDataTarteaz: Saioa ez dago hasita");
                    callback.onEmaitza(new java.util.ArrayList<>());
                    return;
                }
                
                // SEGURTASUNA: bilatuDataTarteaz segurua erabili
                List<Agenda> bisitak = agendaDao.bilatuDataTarteaz(hasieraData, amaieraData, komertzialKodea);
                callback.onEmaitza(bisitak);
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea bisitak bilatzean data tarte: " + hasieraData + " - " + amaieraData, e);
                callback.onEmaitza(null);
            }
        });
    }

    /**
     * Bilaketa orokorra: bilatu data, bazkidea izena/kodea, deskribapena eta egoera eremuen artean.
     * SEGURTASUNA: Uneko komertzialaren bisitak bakarrik bilatzen dira.
     * @param filter Bilaketa testua
     * @param callback Emaitza jaso behar duen callback
     */
    public void bilatuOrokorra(@NonNull String filter, @NonNull KargatuCallback callback) {
        executorService.execute(() -> {
            try {
                // SEGURTASUNA: SessionManager erabiliz uneko komertzialaren kodea lortu
                com.example.appkomertziala.segurtasuna.SessionManager sessionManager = 
                    new com.example.appkomertziala.segurtasuna.SessionManager(context);
                String komertzialKodea = sessionManager.getKomertzialKodea();
                
                if (komertzialKodea == null || komertzialKodea.isEmpty()) {
                    Log.w(ETIKETA, "bilatuOrokorra: Saioa ez dago hasita");
                    callback.onEmaitza(new java.util.ArrayList<>());
                    return;
                }
                
                // SEGURTASUNA: bilatuOrokorra segurua erabili
                List<Agenda> bisitak = agendaDao.bilatuOrokorra(filter.trim(), komertzialKodea);
                callback.onEmaitza(bisitak);
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea bisitak bilatzean orokorra: " + filter, e);
                callback.onEmaitza(null);
            }
        });
    }

    /**
     * Hilabete zehatzaren arabera bisitak kargatu.
     * @param urtea Urtea (yyyy)
     * @param hilabetea Hilabetea (MM, 01-12)
     * @param callback Emaitza jaso behar duen callback
     */
    public void kargatuHilabetearenBisitak(@NonNull String urtea, @NonNull String hilabetea, @NonNull KargatuCallback callback) {
        executorService.execute(() -> {
            try {
                List<Agenda> bisitak = agendaDao.hilabetearenBisitak(urtea, hilabetea);
                callback.onEmaitza(bisitak);
            } catch (Exception e) {
                Log.e(ETIKETA, "Errorea hilabetearen bisitak kargatzean", e);
                callback.onEmaitza(null);
            }
        });
    }

    /**
     * Repository itxi (executorService itxi).
     */
    public void itxi() {
        executorService.shutdown();
    }

    // ========== Callback interfazeak ==========

    /** Bisita txertatzearen emaitza jaso behar duen callback. */
    public interface TxertatuCallback {
        void onEmaitza(boolean ondo, long id);
    }

    /** Hainbat bisita txertatzearen emaitza jaso behar duen callback. */
    public interface TxertatuGuztiakCallback {
        void onEmaitza(boolean ondo, int kopurua);
    }

    /** Bisita eguneratzearen emaitza jaso behar duen callback. */
    public interface EguneratuCallback {
        void onEmaitza(boolean ondo, int errenkadak);
    }

    /** Bisita ezabatzearen emaitza jaso behar duen callback. */
    public interface EzabatuCallback {
        void onEmaitza(boolean ondo);
    }

    /** Bisitak kargatzearen emaitza jaso behar duen callback (zerrenda). */
    public interface KargatuCallback {
        void onEmaitza(@Nullable List<Agenda> bisitak);
    }

    /** Bisita bakarra bilatzearen emaitza jaso behar duen callback. */
    public interface BilatuCallback {
        void onEmaitza(@Nullable Agenda agenda);
    }
}

