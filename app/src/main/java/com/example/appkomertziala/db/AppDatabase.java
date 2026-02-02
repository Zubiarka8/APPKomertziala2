package com.example.appkomertziala.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.appkomertziala.db.eredua.EskaeraGoiburua;
import com.example.appkomertziala.db.eredua.EskaeraXehetasuna;
import com.example.appkomertziala.db.eredua.Katalogoa;
import com.example.appkomertziala.db.eredua.Komertziala;
import com.example.appkomertziala.db.eredua.Logina;
import com.example.appkomertziala.db.eredua.Partnerra;
import com.example.appkomertziala.db.kontsultak.EskaeraGoiburuaDao;
import com.example.appkomertziala.db.kontsultak.EskaeraXehetasunaDao;
import com.example.appkomertziala.db.kontsultak.KatalogoaDao;
import com.example.appkomertziala.db.kontsultak.KomertzialaDao;
import com.example.appkomertziala.db.kontsultak.LoginaDao;
import com.example.appkomertziala.db.kontsultak.PartnerraDao;

/**
 * Aplikazioko Room datu-basea: eredu-entitateak eta kontsulta-DAOak.
 * Erlazio-diagrama: Komertziala, Partnerra, Katalogoa, EskaeraGoiburua, EskaeraXehetasuna, Logina.
 */
@Database(
    entities = {
        Komertziala.class,
        Partnerra.class,
        Katalogoa.class,
        EskaeraGoiburua.class,
        EskaeraXehetasuna.class,
        Logina.class
    },
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instantzia;

    /**
     * 1 -> 2: partnerrak taulan sortutakoData eremua gehitu (eguneko alta iragazteko).
     * Lehendik dauden erregistroei gaurko data esleitzen zaie.
     */
    private static final Migration MIGRAZIO_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE partnerrak ADD COLUMN sortutakoData TEXT DEFAULT ''");
            db.execSQL("UPDATE partnerrak SET sortutakoData = date('now') WHERE sortutakoData = '' OR sortutakoData IS NULL");
        }
    };

    /** Komertzialak taularen kontsultak. */
    public abstract KomertzialaDao komertzialaDao();
    /** Partnerrak taularen kontsultak. */
    public abstract PartnerraDao partnerraDao();
    /** Katalogoa taularen kontsultak. */
    public abstract KatalogoaDao katalogoaDao();
    /** Eskaera goiburuak taularen kontsultak. */
    public abstract EskaeraGoiburuaDao eskaeraGoiburuaDao();
    /** Eskaera xehetasunak taularen kontsultak. */
    public abstract EskaeraXehetasunaDao eskaeraXehetasunaDao();
    /** Loginak taularen kontsultak. */
    public abstract LoginaDao loginaDao();

    /**
     * Datu-basearen instantzia bakarra itzuli (singleton).
     * Kontekstua aplikazioko kontekstua izan behar da.
     */
    public static AppDatabase getInstance(Context context) {
        if (instantzia == null) {
            synchronized (AppDatabase.class) {
                if (instantzia == null) {
                    instantzia = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "techno_basque_db"
                    ).addMigrations(MIGRAZIO_1_2).fallbackToDestructiveMigration().build();
                }
            }
        }
        return instantzia;
    }
}
