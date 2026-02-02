package com.example.appkomertziala.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.appkomertziala.db.eredua.Agenda;
import com.example.appkomertziala.db.eredua.Bazkidea;
import com.example.appkomertziala.db.eredua.EskaeraGoiburua;
import com.example.appkomertziala.db.eredua.EskaeraXehetasuna;
import com.example.appkomertziala.db.eredua.Katalogoa;
import com.example.appkomertziala.db.eredua.Komertziala;
import com.example.appkomertziala.db.eredua.Logina;
import com.example.appkomertziala.db.eredua.Partnerra;
import com.example.appkomertziala.db.kontsultak.AgendaDao;
import com.example.appkomertziala.db.kontsultak.BazkideaDao;
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
        Bazkidea.class,
        Katalogoa.class,
        EskaeraGoiburua.class,
        EskaeraXehetasuna.class,
        Logina.class,
        Agenda.class
    },
    version = 6,
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

    /**
     * 2 -> 3: agenda_bisitak taula sortu (Agenda modulua).
     * Eremuak: id (gako nagusia), bisita_data, partner_kodea, deskribapena, egoera.
     */
    private static final Migration MIGRAZIO_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS agenda_bisitak (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, bisitaData TEXT, partnerKodea TEXT, deskribapena TEXT, egoera TEXT)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_agenda_bisitak_partnerKodea ON agenda_bisitak(partnerKodea)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_agenda_bisitak_bisitaData ON agenda_bisitak(bisitaData)");
        }
    };

    /**
     * 3 -> 4: partnerrak taulan bazkideak.xml eremuak (telefonoa, posta, jaiotzeData, argazkia).
     */
    private static final Migration MIGRAZIO_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE partnerrak ADD COLUMN telefonoa TEXT");
            db.execSQL("ALTER TABLE partnerrak ADD COLUMN posta TEXT");
            db.execSQL("ALTER TABLE partnerrak ADD COLUMN jaiotzeData TEXT");
            db.execSQL("ALTER TABLE partnerrak ADD COLUMN argazkia TEXT");
        }
    };

    /**
     * 4 -> 5: bazkideak taula sortu (bazkideak.xml egitura: NAN, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia).
     */
    private static final Migration MIGRAZIO_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS bazkideak (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nan TEXT, izena TEXT, abizena TEXT, telefonoZenbakia TEXT, posta TEXT, jaiotzeData TEXT, argazkia TEXT)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_bazkideak_nan ON bazkideak(nan)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_bazkideak_izena ON bazkideak(izena)");
        }
    };

    /**
     * 5 -> 6: katalogoa taulan irudia_izena eremua gehitu (drawable baliabidearen izena).
     * Asteko inportazioan produktu bakoitzaren irudia gordetzeko.
     */
    private static final Migration MIGRAZIO_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE katalogoa ADD COLUMN irudia_izena TEXT DEFAULT NULL");
        }
    };

    /** Komertzialak taularen kontsultak. */
    public abstract KomertzialaDao komertzialaDao();
    /** Partnerrak taularen kontsultak. */
    public abstract PartnerraDao partnerraDao();
    /** Bazkideak taularen kontsultak. */
    public abstract BazkideaDao bazkideaDao();
    /** Katalogoa taularen kontsultak. */
    public abstract KatalogoaDao katalogoaDao();
    /** Eskaera goiburuak taularen kontsultak. */
    public abstract EskaeraGoiburuaDao eskaeraGoiburuaDao();
    /** Eskaera xehetasunak taularen kontsultak. */
    public abstract EskaeraXehetasunaDao eskaeraXehetasunaDao();
    /** Loginak taularen kontsultak. */
    public abstract LoginaDao loginaDao();
    /** Agenda (bisitak) taularen kontsultak. */
    public abstract AgendaDao agendaDao();

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
                    ).addMigrations(MIGRAZIO_1_2, MIGRAZIO_2_3, MIGRAZIO_3_4, MIGRAZIO_4_5, MIGRAZIO_5_6)
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()  // Kontsulta bat hari nagusian egiten bada itxiera saihesteko
                            .build();
                }
            }
        }
        return instantzia;
    }
}
