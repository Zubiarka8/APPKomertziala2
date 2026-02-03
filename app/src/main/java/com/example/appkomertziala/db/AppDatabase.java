package com.example.appkomertziala.db;

import android.content.Context;
import android.database.Cursor;

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
    version = 12,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instantzia;

    /** Taula existitzen den egiaztatzen du. (taulaIzena bakarrik taula izen seguruak direnean erabili.) */
    private static boolean taulaExistitzenDa(SupportSQLiteDatabase db, String taulaIzena) {
        try {
            String escaped = taulaIzena.replace("'", "''");
            try (Cursor c = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='" + escaped + "'")) {
                return c != null && c.moveToFirst();
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Taula baten zutabe bat existitzen den egiaztatzen du (ADD COLUMN bikoiztuak saihesteko). */
    private static boolean zutabeaExistitzenDa(SupportSQLiteDatabase db, String taulaIzena, String zutabeIzena) {
        try {
            String escaped = taulaIzena.replace("'", "''");
            try (Cursor c = db.query("PRAGMA table_info('" + escaped + "')")) {
                if (c == null) return false;
                int nameIdx = c.getColumnIndex("name");
                if (nameIdx == -1) return false;
                while (c.moveToNext()) {
                    if (zutabeIzena.equals(c.getString(nameIdx))) return true;
                }
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 1 -> 2: partnerrak taulan sortutakoData eremua gehitu (eguneko alta iragazteko).
     * Lehendik dauden erregistroei gaurko data esleitzen zaie.
     */
    private static final Migration MIGRAZIO_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            if (!taulaExistitzenDa(db, "partnerrak")) return;
            if (!zutabeaExistitzenDa(db, "partnerrak", "sortutakoData")) {
                db.execSQL("ALTER TABLE partnerrak ADD COLUMN sortutakoData TEXT DEFAULT ''");
                db.execSQL("UPDATE partnerrak SET sortutakoData = date('now') WHERE sortutakoData = '' OR sortutakoData IS NULL");
            }
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
            if (!taulaExistitzenDa(db, "partnerrak")) return;
            if (!zutabeaExistitzenDa(db, "partnerrak", "telefonoa")) db.execSQL("ALTER TABLE partnerrak ADD COLUMN telefonoa TEXT");
            if (!zutabeaExistitzenDa(db, "partnerrak", "posta")) db.execSQL("ALTER TABLE partnerrak ADD COLUMN posta TEXT");
            if (!zutabeaExistitzenDa(db, "partnerrak", "jaiotzeData")) db.execSQL("ALTER TABLE partnerrak ADD COLUMN jaiotzeData TEXT");
            if (!zutabeaExistitzenDa(db, "partnerrak", "argazkia")) db.execSQL("ALTER TABLE partnerrak ADD COLUMN argazkia TEXT");
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
            if (!taulaExistitzenDa(db, "katalogoa")) return;
            if (!zutabeaExistitzenDa(db, "katalogoa", "irudia_izena"))
                db.execSQL("ALTER TABLE katalogoa ADD COLUMN irudia_izena TEXT DEFAULT NULL");
        }
    };

    /**
     * 6 -> 7: eskaera_goiburuak eta eskaera_xehetasunak taulak berriz sortu (indizeekin; kanpo-gakoak instalazio berrian sortzen dira).
     * Migrazioan FK ez erabiltzea, datu zaharrek balioak ez betetzeagatik huts egitea saihesteko.
     */
    private static final Migration MIGRAZIO_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            if (taulaExistitzenDa(db, "eskaera_goiburuak")) {
                db.execSQL("CREATE TABLE IF NOT EXISTS eskaera_goiburuak_new (zenbakia TEXT PRIMARY KEY NOT NULL, data TEXT, komertzialKodea TEXT, ordezkaritza TEXT, partnerKodea TEXT)");
                db.execSQL("INSERT OR IGNORE INTO eskaera_goiburuak_new (zenbakia, data, komertzialKodea, ordezkaritza, partnerKodea) SELECT zenbakia, data, komertzialKodea, ordezkaritza, partnerKodea FROM eskaera_goiburuak");
                db.execSQL("DROP TABLE eskaera_goiburuak");
                db.execSQL("ALTER TABLE eskaera_goiburuak_new RENAME TO eskaera_goiburuak");
            } else {
                db.execSQL("CREATE TABLE IF NOT EXISTS eskaera_goiburuak (zenbakia TEXT PRIMARY KEY NOT NULL, data TEXT, komertzialKodea TEXT, ordezkaritza TEXT, partnerKodea TEXT)");
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS index_eskaera_goiburuak_komertzialKodea ON eskaera_goiburuak(komertzialKodea)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_eskaera_goiburuak_partnerKodea ON eskaera_goiburuak(partnerKodea)");

            if (taulaExistitzenDa(db, "eskaera_xehetasunak")) {
                db.execSQL("CREATE TABLE IF NOT EXISTS eskaera_xehetasunak_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, eskaeraZenbakia TEXT, artikuluKodea TEXT, kantitatea INTEGER NOT NULL, prezioa REAL NOT NULL)");
                db.execSQL("INSERT OR IGNORE INTO eskaera_xehetasunak_new (id, eskaeraZenbakia, artikuluKodea, kantitatea, prezioa) SELECT id, eskaeraZenbakia, artikuluKodea, kantitatea, prezioa FROM eskaera_xehetasunak");
                db.execSQL("DROP TABLE eskaera_xehetasunak");
                db.execSQL("ALTER TABLE eskaera_xehetasunak_new RENAME TO eskaera_xehetasunak");
            } else {
                db.execSQL("CREATE TABLE IF NOT EXISTS eskaera_xehetasunak (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, eskaeraZenbakia TEXT, artikuluKodea TEXT, kantitatea INTEGER NOT NULL, prezioa REAL NOT NULL)");
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS index_eskaera_xehetasunak_eskaeraZenbakia ON eskaera_xehetasunak(eskaeraZenbakia)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_eskaera_xehetasunak_artikuluKodea ON eskaera_xehetasunak(artikuluKodea)");
        }
    };

    /**
     * 7 -> 8: eskaera_goiburuak taulan partnerId eta komertzialId eremuak (ID bidezko loturak: nor erosi, nor kudeatu).
     */
    private static final Migration MIGRAZIO_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            if (!taulaExistitzenDa(db, "eskaera_goiburuak")) return;
            if (!zutabeaExistitzenDa(db, "eskaera_goiburuak", "komertzialId"))
                db.execSQL("ALTER TABLE eskaera_goiburuak ADD COLUMN komertzialId INTEGER");
            if (!zutabeaExistitzenDa(db, "eskaera_goiburuak", "partnerId"))
                db.execSQL("ALTER TABLE eskaera_goiburuak ADD COLUMN partnerId INTEGER");
            db.execSQL("UPDATE eskaera_goiburuak SET komertzialId = (SELECT id FROM komertzialak WHERE komertzialak.kodea = eskaera_goiburuak.komertzialKodea LIMIT 1) WHERE komertzialKodea IS NOT NULL AND komertzialKodea != ''");
            db.execSQL("UPDATE eskaera_goiburuak SET partnerId = (SELECT id FROM partnerrak WHERE partnerrak.kodea = eskaera_goiburuak.partnerKodea LIMIT 1) WHERE partnerKodea IS NOT NULL AND partnerKodea != ''");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_eskaera_goiburuak_komertzialId ON eskaera_goiburuak(komertzialId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_eskaera_goiburuak_partnerId ON eskaera_goiburuak(partnerId)");
        }
    };

    /**
     * 8 -> 9: agenda_bisitak taulan partnerId eta komertzialaId eremuak (kanpo-gakoak).
     * partnerId → Partnerra.id (bisitatzen den partnerra/bazkidea); komertzialaId → Komertziala.id (bisita sortu duen komertziala).
     */
    private static final Migration MIGRAZIO_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            if (!taulaExistitzenDa(db, "agenda_bisitak")) return;
            if (!zutabeaExistitzenDa(db, "agenda_bisitak", "partnerId"))
                db.execSQL("ALTER TABLE agenda_bisitak ADD COLUMN partnerId INTEGER");
            if (!zutabeaExistitzenDa(db, "agenda_bisitak", "komertzialaId"))
                db.execSQL("ALTER TABLE agenda_bisitak ADD COLUMN komertzialaId INTEGER");
            db.execSQL("UPDATE agenda_bisitak SET partnerId = (SELECT id FROM partnerrak WHERE partnerrak.kodea = agenda_bisitak.partnerKodea LIMIT 1) WHERE partnerKodea IS NOT NULL AND partnerKodea != ''");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_agenda_bisitak_partnerId ON agenda_bisitak(partnerId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_agenda_bisitak_komertzialaId ON agenda_bisitak(komertzialaId)");
        }
    };

    /**
     * 9 -> 10: komertzialak taulan abizena, posta, jaiotzeData, argazkia (XML komertzialak.xml egitura bateratu).
     */
    private static final Migration MIGRAZIO_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            komertzialakZutabeakGehitu(db);
        }
    };

    /**
     * 10 -> 11: komertzialak taulan falta diren zutabeak gehitu (abizena, posta, jaiotzeData, argazkia).
     * BD 10 bertsioz sortu bada entitate zaharrarekin (id, izena, kodea bakarrik), zutabeak hemen gehitzen dira.
     */
    private static final Migration MIGRAZIO_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            komertzialakZutabeakGehitu(db);
        }
    };

    /**
     * 11 -> 12: komertzialak taulan entitate berriko zutabeak (abizena, posta, jaiotzeData, argazkia).
     * BD 11 bertsioz entitate zaharrarekin sortu bada (3 zutabe bakarrik), zutabeak hemen gehitzen dira.
     */
    private static final Migration MIGRAZIO_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            komertzialakZutabeakGehitu(db);
        }
    };

    /** komertzialak taulan komertzialak.xml-eko eremu guztiak (abizena, posta, jaiotzeData, argazkia) badauden egiaztatu eta falta badira gehitu. */
    private static void komertzialakZutabeakGehitu(SupportSQLiteDatabase db) {
        if (!taulaExistitzenDa(db, "komertzialak")) return;
        if (!zutabeaExistitzenDa(db, "komertzialak", "abizena"))
            db.execSQL("ALTER TABLE komertzialak ADD COLUMN abizena TEXT");
        if (!zutabeaExistitzenDa(db, "komertzialak", "posta"))
            db.execSQL("ALTER TABLE komertzialak ADD COLUMN posta TEXT");
        if (!zutabeaExistitzenDa(db, "komertzialak", "jaiotzeData"))
            db.execSQL("ALTER TABLE komertzialak ADD COLUMN jaiotzeData TEXT");
        if (!zutabeaExistitzenDa(db, "komertzialak", "argazkia"))
            db.execSQL("ALTER TABLE komertzialak ADD COLUMN argazkia TEXT");
    }

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
                    ).addMigrations(MIGRAZIO_1_2, MIGRAZIO_2_3, MIGRAZIO_3_4, MIGRAZIO_4_5, MIGRAZIO_5_6, MIGRAZIO_6_7, MIGRAZIO_7_8, MIGRAZIO_8_9, MIGRAZIO_9_10, MIGRAZIO_10_11, MIGRAZIO_11_12)
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()  // Kontsulta bat hari nagusian egiten bada itxiera saihesteko
                            .build();
                }
            }
        }
        return instantzia;
    }
}
