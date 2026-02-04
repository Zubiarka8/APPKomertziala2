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
import com.example.appkomertziala.db.eredua.Eskaera;
import com.example.appkomertziala.db.eredua.EskaeraGoiburua;
import com.example.appkomertziala.db.eredua.EskaeraXehetasuna;
import com.example.appkomertziala.db.eredua.HistorialCompra;
import com.example.appkomertziala.db.eredua.Katalogoa;
import com.example.appkomertziala.db.eredua.Komertziala;
import com.example.appkomertziala.db.eredua.Logina;
import com.example.appkomertziala.db.kontsultak.AgendaDao;
import com.example.appkomertziala.db.kontsultak.BazkideaDao;
import com.example.appkomertziala.db.kontsultak.EskaeraDao;
import com.example.appkomertziala.db.kontsultak.EskaeraGoiburuaDao;
import com.example.appkomertziala.db.kontsultak.EskaeraXehetasunaDao;
import com.example.appkomertziala.db.kontsultak.HistorialCompraDao;
import com.example.appkomertziala.db.kontsultak.KatalogoaDao;
import com.example.appkomertziala.db.kontsultak.KomertzialaDao;
import com.example.appkomertziala.db.kontsultak.LoginaDao;

/**
 * Aplikazioko Room datu-basea: eredu-entitateak eta kontsulta-DAOak.
 * Erlazio-diagrama: Komertziala, Bazkidea, Katalogoa, EskaeraGoiburua, EskaeraXehetasuna, Logina, Agenda.
 */
    @Database(
    entities = {
        Komertziala.class,
        Bazkidea.class,
        Eskaera.class,
        Katalogoa.class,
        EskaeraGoiburua.class,
        EskaeraXehetasuna.class,
        Logina.class,
        Agenda.class,
        HistorialCompra.class
    },
    version = 19,  // 18 -> 19: historial_compras taulan Foreign Keys gehitu (eskaeraZenbakia, komertzialId, bazkideaId) eta eskaera_xehetasunak taulan Foreign Key
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
     * 1 -> 2: Migración histórica (ya no aplica).
     */
    private static final Migration MIGRAZIO_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            // Migrazioa historikoa - jada ez da aplikatzen
        }
    };

    /**
     * 2 -> 3: agenda_bisitak taula sortu (Agenda modulua).
     * Eremuak: id (gako nagusia), bisita_data, bazkidea_kodea, deskribapena, egoera.
     */
    private static final Migration MIGRAZIO_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS agenda_bisitak (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, bisitaData TEXT, bazkideaKodea TEXT, deskribapena TEXT, egoera TEXT)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_agenda_bisitak_bazkideaKodea ON agenda_bisitak(bazkideaKodea)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_agenda_bisitak_bisitaData ON agenda_bisitak(bisitaData)");
        }
    };

    /**
     * 3 -> 4: Migrazioa historikoa (jada ez da aplikatzen).
     */
    private static final Migration MIGRAZIO_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            // Migrazioa historikoa - jada ez da aplikatzen
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
                if (!zutabeaExistitzenDa(db, "eskaera_goiburuak", "bazkideaKodea")) {
                    db.execSQL("ALTER TABLE eskaera_goiburuak ADD COLUMN bazkideaKodea TEXT");
                }
            } else {
                db.execSQL("CREATE TABLE IF NOT EXISTS eskaera_goiburuak (zenbakia TEXT PRIMARY KEY NOT NULL, data TEXT, komertzialKodea TEXT, ordezkaritza TEXT, bazkideaKodea TEXT)");
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS index_eskaera_goiburuak_komertzialKodea ON eskaera_goiburuak(komertzialKodea)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_eskaera_goiburuak_bazkideaKodea ON eskaera_goiburuak(bazkideaKodea)");

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
     * 7 -> 8: eskaera_goiburuak taulan bazkideaId eta komertzialId eremuak (ID bidezko loturak: nor erosi, nor kudeatu).
     */
    private static final Migration MIGRAZIO_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            if (!taulaExistitzenDa(db, "eskaera_goiburuak")) return;
            if (!zutabeaExistitzenDa(db, "eskaera_goiburuak", "komertzialId"))
                db.execSQL("ALTER TABLE eskaera_goiburuak ADD COLUMN komertzialId INTEGER");
            if (!zutabeaExistitzenDa(db, "eskaera_goiburuak", "bazkideaId"))
                db.execSQL("ALTER TABLE eskaera_goiburuak ADD COLUMN bazkideaId INTEGER");
            db.execSQL("UPDATE eskaera_goiburuak SET komertzialId = (SELECT id FROM komertzialak WHERE komertzialak.kodea = eskaera_goiburuak.komertzialKodea LIMIT 1) WHERE komertzialKodea IS NOT NULL AND komertzialKodea != ''");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_eskaera_goiburuak_komertzialId ON eskaera_goiburuak(komertzialId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_eskaera_goiburuak_bazkideaId ON eskaera_goiburuak(bazkideaId)");
        }
    };

    /**
     * 8 -> 9: agenda_bisitak taulan bazkideaId eta komertzialaId eremuak (kanpo-gakoak).
     * komertzialaId → Komertziala.id (bisita sortu duen komertziala).
     */
    private static final Migration MIGRAZIO_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            if (!taulaExistitzenDa(db, "agenda_bisitak")) return;
            if (!zutabeaExistitzenDa(db, "agenda_bisitak", "bazkideaId"))
                db.execSQL("ALTER TABLE agenda_bisitak ADD COLUMN bazkideaId INTEGER");
            if (!zutabeaExistitzenDa(db, "agenda_bisitak", "komertzialaId"))
                db.execSQL("ALTER TABLE agenda_bisitak ADD COLUMN komertzialaId INTEGER");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_agenda_bisitak_bazkideaId ON agenda_bisitak(bazkideaId)");
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

    /**
     * 12 -> 13: Actualizar agenda_bisitak y eskaera_goiburuak: asegurar que existen las columnas bazkideaKodea/bazkideaId.
     * KRITIKOA: Bazkidea entitateak kodea bakarrik erabiltzen du - komertzialKodea EZ dago entitatean.
     * Hau hemen kodea zutabea bakarrik gehitzen dugu, komertzialKodea EZ (17->18 migrazioan ezabatu egingo da).
     */
    private static final Migration MIGRAZIO_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            // Eremu falta direnak gehitu bazkideak taulan existitzen ez badira
            // KRITIKOA: Bazkidea entitatearekin bateratuta - kodea bakarrik (komertzialKodea EZ)
            if (taulaExistitzenDa(db, "bazkideak")) {
                if (!zutabeaExistitzenDa(db, "bazkideak", "kodea"))
                    db.execSQL("ALTER TABLE bazkideak ADD COLUMN kodea TEXT");
                // komertzialKodea zutabea gehitzen dugu hemen (garapen fasean zegoen), baina 17->18 migrazioan ezabatu egingo da
                if (!zutabeaExistitzenDa(db, "bazkideak", "komertzialKodea"))
                    db.execSQL("ALTER TABLE bazkideak ADD COLUMN komertzialKodea TEXT");
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bazkideak_kodea ON bazkideak(kodea)");
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bazkideak_komertzialKodea ON bazkideak(komertzialKodea)");
            }

            // Ziurtatu bazkideaKodea eta bazkideaId zutabeak existitzen direla
            if (taulaExistitzenDa(db, "agenda_bisitak")) {
                if (!zutabeaExistitzenDa(db, "agenda_bisitak", "bazkideaKodea"))
                    db.execSQL("ALTER TABLE agenda_bisitak ADD COLUMN bazkideaKodea TEXT");
                if (!zutabeaExistitzenDa(db, "agenda_bisitak", "bazkideaId"))
                    db.execSQL("ALTER TABLE agenda_bisitak ADD COLUMN bazkideaId INTEGER");
                db.execSQL("CREATE INDEX IF NOT EXISTS index_agenda_bisitak_bazkideaKodea ON agenda_bisitak(bazkideaKodea)");
                db.execSQL("CREATE INDEX IF NOT EXISTS index_agenda_bisitak_bazkideaId ON agenda_bisitak(bazkideaId)");
            }

            if (taulaExistitzenDa(db, "eskaera_goiburuak")) {
                if (!zutabeaExistitzenDa(db, "eskaera_goiburuak", "bazkideaKodea"))
                    db.execSQL("ALTER TABLE eskaera_goiburuak ADD COLUMN bazkideaKodea TEXT");
                if (!zutabeaExistitzenDa(db, "eskaera_goiburuak", "bazkideaId"))
                    db.execSQL("ALTER TABLE eskaera_goiburuak ADD COLUMN bazkideaId INTEGER");
                db.execSQL("CREATE INDEX IF NOT EXISTS index_eskaera_goiburuak_bazkideaKodea ON eskaera_goiburuak(bazkideaKodea)");
                db.execSQL("CREATE INDEX IF NOT EXISTS index_eskaera_goiburuak_bazkideaId ON eskaera_goiburuak(bazkideaId)");
            }

        }
    };

    /**
     * 13 -> 14: Reconstruir bazkideak taula egitura ZUZEKIN (Foreign Key eta indize guztiak barne).
     * KRITIKOA: Hau hemen komertzialKodea mantentzen dugu (garapen fasean zegoen), baina 17->18 migrazioan ezabatu egingo da.
     * SQLite-k ezin du Foreign Key bat gehitu ALTER TABLE-rekin, beraz "table swap" estrategia erabiltzen da:
     * 1. Sortu bazkideak_new taula egitura ZUZEKIN (FK eta indize guztiak barne)
     * 2. Kopiatu datu guztiak taula zaharretik berrira (zutabeak existitzen badira bakarrik)
     * 3. Ezabatu taula zaharra eta aldatu berriaren izena bazkideak izatera
     */
    private static final Migration MIGRAZIO_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            if (!taulaExistitzenDa(db, "bazkideak")) {
                // Taula ez badago, sortu egitura ZUZEKIN - komertzialKodea hemen mantentzen dugu (17->18 migrazioan ezabatu egingo da)
                db.execSQL("CREATE TABLE bazkideak (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nan TEXT, izena TEXT, abizena TEXT, telefonoZenbakia TEXT, posta TEXT, jaiotzeData TEXT, argazkia TEXT, kodea TEXT, komertzialKodea TEXT, FOREIGN KEY(komertzialKodea) REFERENCES komertzialak(kodea) ON DELETE CASCADE)");
                db.execSQL("CREATE INDEX index_bazkideak_nan ON bazkideak(nan)");
                db.execSQL("CREATE INDEX index_bazkideak_izena ON bazkideak(izena)");
                db.execSQL("CREATE INDEX index_bazkideak_kodea ON bazkideak(kodea)");
                db.execSQL("CREATE INDEX index_bazkideak_komertzialKodea ON bazkideak(komertzialKodea)");
                return;
            }

            // Table swap estrategia: sortu taula berria egitura ZUZEKIN
            // Hau hemen komertzialKodea mantentzen dugu (garapen fasean zegoen), baina 17->18 migrazioan ezabatu egingo da
            db.execSQL("CREATE TABLE bazkideak_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nan TEXT, izena TEXT, abizena TEXT, telefonoZenbakia TEXT, posta TEXT, jaiotzeData TEXT, argazkia TEXT, kodea TEXT, komertzialKodea TEXT, FOREIGN KEY(komertzialKodea) REFERENCES komertzialak(kodea) ON DELETE CASCADE)");

            // Kopiatu datu guztiak: XML-eko zutabeak (nan, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia)
            // eta zutabe gehigarriak (kodea, komertzialKodea - hau hemen mantentzen dugu, baina 17->18 migrazioan ezabatu egingo da)
            // Begiratu hemen ea kodea eta komertzialKodea zutabeak badaude, horiek kopiatzen ditugu
            boolean kodeaExistitzenDa = zutabeaExistitzenDa(db, "bazkideak", "kodea");
            boolean komertzialKodeaExistitzenDa = zutabeaExistitzenDa(db, "bazkideak", "komertzialKodea");
            
            String kodeaSelect = kodeaExistitzenDa ? "kodea" : "NULL";
            String komertzialKodeaSelect = komertzialKodeaExistitzenDa ? "komertzialKodea" : "NULL";
            
            db.execSQL("INSERT INTO bazkideak_new (id, nan, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia, kodea, komertzialKodea) " +
                       "SELECT id, nan, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia, " + kodeaSelect + ", " + komertzialKodeaSelect + " FROM bazkideak");

            // Ezabatu taula zaharra eta indize zaharrak
            db.execSQL("DROP INDEX IF EXISTS index_bazkideak_nan");
            db.execSQL("DROP INDEX IF EXISTS index_bazkideak_izena");
            db.execSQL("DROP INDEX IF EXISTS index_bazkideak_kodea");
            db.execSQL("DROP INDEX IF EXISTS index_bazkideak_komertzialKodea");
            db.execSQL("DROP TABLE bazkideak");

            // Aldatu berriaren izena bazkideak izatera
            db.execSQL("ALTER TABLE bazkideak_new RENAME TO bazkideak");

            // Sortu indize berriak - komertzialKodea indizea hemen mantentzen dugu (17->18 migrazioan ezabatu egingo da)
            db.execSQL("CREATE INDEX index_bazkideak_nan ON bazkideak(nan)");
            db.execSQL("CREATE INDEX index_bazkideak_izena ON bazkideak(izena)");
            db.execSQL("CREATE INDEX index_bazkideak_kodea ON bazkideak(kodea)");
            db.execSQL("CREATE INDEX index_bazkideak_komertzialKodea ON bazkideak(komertzialKodea)");
        }
    };

    /**
     * 14 -> 15: Migración histórica (ya no aplica).
     */
    private static final Migration MIGRAZIO_14_15 = new Migration(14, 15) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            // Migrazioa historikoa - jada ez da aplikatzen
        }
    };

    /**
     * 15 -> 16: historial_compras taula sortu (HistorialCompra entitatea).
     * Eremuak: id (gako nagusia), bidalketaId, kodea, helmuga, data, amaituta,
     * productoId, productoIzena, eskatuta, bidalita, prezioUnit, argazkia.
     */
    private static final Migration MIGRAZIO_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS historial_compras (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, bidalketaId INTEGER NOT NULL, kodea TEXT, helmuga TEXT, data TEXT, amaituta INTEGER NOT NULL, productoId TEXT, productoIzena TEXT, eskatuta INTEGER NOT NULL, bidalita INTEGER NOT NULL, prezioUnit REAL NOT NULL, argazkia TEXT)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_historial_compras_kodea ON historial_compras(kodea)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_historial_compras_data ON historial_compras(data)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_historial_compras_productoId ON historial_compras(productoId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_historial_compras_bidalketaId ON historial_compras(bidalketaId)");
        }
    };

    /**
     * 16 -> 17: bazkideak taulan kodea eremua gehitu (NAN balioa kopiatu kodea eremura).
     * kodea eremua Bazkidea entitatean gehitu dugu, baina datu-base zaharretan falta da.
     * Migrazio honek kodea eremua gehitzen du eta NAN balioa kopiatzen du kodea eremura.
     */
    private static final Migration MIGRAZIO_16_17 = new Migration(16, 17) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            if (!taulaExistitzenDa(db, "bazkideak")) return;
            
            // kodea eremua gehitu ez badago
            if (!zutabeaExistitzenDa(db, "bazkideak", "kodea")) {
                db.execSQL("ALTER TABLE bazkideak ADD COLUMN kodea TEXT");
                // NAN balioa kodea eremura kopiatu erregistro existententzat
                db.execSQL("UPDATE bazkideak SET kodea = nan WHERE kodea IS NULL OR kodea = ''");
            }
            
            // kodea indizea sortu ez badago
            try {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bazkideak_kodea ON bazkideak(kodea)");
            } catch (Exception e) {
                // Indizea existitzen bada, ez da errorea
            }
        }
    };

    /**
     * 17 -> 18: KRITIKOA - komertzialKodea zutabea ezabatu bazkideak taulatik.
     * Bazkidea entitatean EZ dago komertzialKodea zutabea, baina DB zaharretan badago.
     * Hau hemen taula berri bat sortzen dugu komertzialKodea GABE, eta datuak kopiatzen ditugu.
     * Begiratu hemen ea kodea zutabea badago, hori mantentzen dugu.
     */
    private static final Migration MIGRAZIO_17_18 = new Migration(17, 18) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            if (!taulaExistitzenDa(db, "bazkideak")) {
                // Taula ez badago, sortu egitura ZUZEKIN - Bazkidea entitatearekin bateratuta (komertzialKodea GABE)
                db.execSQL("CREATE TABLE bazkideak (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nan TEXT, izena TEXT, abizena TEXT, telefonoZenbakia TEXT, posta TEXT, jaiotzeData TEXT, argazkia TEXT, kodea TEXT)");
                db.execSQL("CREATE INDEX index_bazkideak_nan ON bazkideak(nan)");
                db.execSQL("CREATE INDEX index_bazkideak_izena ON bazkideak(izena)");
                db.execSQL("CREATE INDEX index_bazkideak_kodea ON bazkideak(kodea)");
                return;
            }

            // Table swap estrategia: sortu taula berria komertzialKodea GABE
            // Hau hemen badago, dena ondo doa - komertzialKodea zutabea ezabatzen dugu
            db.execSQL("CREATE TABLE bazkideak_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nan TEXT, izena TEXT, abizena TEXT, telefonoZenbakia TEXT, posta TEXT, jaiotzeData TEXT, argazkia TEXT, kodea TEXT)");

            // Kopiatu datu guztiak: XML-eko zutabeak (nan, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia)
            // eta kodea zutabea bakarrik - komertzialKodea EZ kopiatzen dugu (entitatean ez dago)
            // Begiratu hemen ea kodea zutabea badago, hori kopiatzen dugu
            boolean kodeaExistitzenDa = zutabeaExistitzenDa(db, "bazkideak", "kodea");
            String kodeaSelect = kodeaExistitzenDa ? "kodea" : "NULL";
            
            db.execSQL("INSERT INTO bazkideak_new (id, nan, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia, kodea) " +
                       "SELECT id, nan, izena, abizena, telefonoZenbakia, posta, jaiotzeData, argazkia, " + kodeaSelect + " FROM bazkideak");

            // Ezabatu taula zaharra eta indize zaharrak
            db.execSQL("DROP INDEX IF EXISTS index_bazkideak_nan");
            db.execSQL("DROP INDEX IF EXISTS index_bazkideak_izena");
            db.execSQL("DROP INDEX IF EXISTS index_bazkideak_kodea");
            db.execSQL("DROP INDEX IF EXISTS index_bazkideak_komertzialKodea");  // komertzialKodea indizea ezabatu
            db.execSQL("DROP TABLE bazkideak");

            // Aldatu berriaren izena bazkideak izatera
            db.execSQL("ALTER TABLE bazkideak_new RENAME TO bazkideak");

            // Sortu indize berriak - komertzialKodea indizea EZ sortzen dugu (zutabea ez dago)
            db.execSQL("CREATE INDEX index_bazkideak_nan ON bazkideak(nan)");
            db.execSQL("CREATE INDEX index_bazkideak_izena ON bazkideak(izena)");
            db.execSQL("CREATE INDEX index_bazkideak_kodea ON bazkideak(kodea)");
        }
    };

    /**
     * 18 -> 19: historial_compras taulan Foreign Keys gehitu (eskaeraZenbakia, komertzialId, bazkideaId)
     * eta eskaera_xehetasunak taulan Foreign Key gehitu (eskaeraZenbakia → EskaeraGoiburua.zenbakia).
     * SQLite-k ezin du Foreign Key bat gehitu ALTER TABLE-rekin, beraz "table swap" estrategia erabiltzen da.
     */
    private static final Migration MIGRAZIO_18_19 = new Migration(18, 19) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            // 1. historial_compras taula berriztatu Foreign Keys-ekin
            if (taulaExistitzenDa(db, "historial_compras")) {
                // Table swap estrategia
                db.execSQL("CREATE TABLE historial_compras_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "bidalketaId INTEGER NOT NULL, " +
                        "kodea TEXT, " +
                        "helmuga TEXT, " +
                        "data TEXT, " +
                        "amaituta INTEGER NOT NULL, " +
                        "productoId TEXT, " +
                        "productoIzena TEXT, " +
                        "eskatuta INTEGER NOT NULL, " +
                        "bidalita INTEGER NOT NULL, " +
                        "prezioUnit REAL NOT NULL, " +
                        "argazkia TEXT, " +
                        "eskaeraZenbakia TEXT, " +
                        "komertzialId INTEGER, " +
                        "bazkideaId INTEGER, " +
                        "FOREIGN KEY(eskaeraZenbakia) REFERENCES eskaera_goiburuak(zenbakia) ON DELETE CASCADE, " +
                        "FOREIGN KEY(komertzialId) REFERENCES komertzialak(id) ON DELETE SET NULL, " +
                        "FOREIGN KEY(bazkideaId) REFERENCES bazkideak(id) ON DELETE SET NULL)");

                // Datuak kopiatu - zutabe berriak NULL izango dira erregistro zaharretan
                boolean eskaeraZenbakiaExistitzenDa = zutabeaExistitzenDa(db, "historial_compras", "eskaeraZenbakia");
                boolean komertzialIdExistitzenDa = zutabeaExistitzenDa(db, "historial_compras", "komertzialId");
                boolean bazkideaIdExistitzenDa = zutabeaExistitzenDa(db, "historial_compras", "bazkideaId");

                String eskaeraZenbakiaSelect = eskaeraZenbakiaExistitzenDa ? "eskaeraZenbakia" : "NULL";
                String komertzialIdSelect = komertzialIdExistitzenDa ? "komertzialId" : "NULL";
                String bazkideaIdSelect = bazkideaIdExistitzenDa ? "bazkideaId" : "NULL";

                db.execSQL("INSERT INTO historial_compras_new " +
                        "(id, bidalketaId, kodea, helmuga, data, amaituta, productoId, productoIzena, eskatuta, bidalita, prezioUnit, argazkia, eskaeraZenbakia, komertzialId, bazkideaId) " +
                        "SELECT id, bidalketaId, kodea, helmuga, data, amaituta, productoId, productoIzena, eskatuta, bidalita, prezioUnit, argazkia, " +
                        eskaeraZenbakiaSelect + ", " + komertzialIdSelect + ", " + bazkideaIdSelect + " " +
                        "FROM historial_compras");

                // Indize zaharrak ezabatu
                db.execSQL("DROP INDEX IF EXISTS index_historial_compras_kodea");
                db.execSQL("DROP INDEX IF EXISTS index_historial_compras_data");
                db.execSQL("DROP INDEX IF EXISTS index_historial_compras_productoId");
                db.execSQL("DROP INDEX IF EXISTS index_historial_compras_bidalketaId");
                db.execSQL("DROP TABLE historial_compras");

                // Taula berria izendatu
                db.execSQL("ALTER TABLE historial_compras_new RENAME TO historial_compras");

                // Indize berriak sortu
                db.execSQL("CREATE INDEX index_historial_compras_kodea ON historial_compras(kodea)");
                db.execSQL("CREATE INDEX index_historial_compras_data ON historial_compras(data)");
                db.execSQL("CREATE INDEX index_historial_compras_productoId ON historial_compras(productoId)");
                db.execSQL("CREATE INDEX index_historial_compras_bidalketaId ON historial_compras(bidalketaId)");
                db.execSQL("CREATE INDEX index_historial_compras_eskaeraZenbakia ON historial_compras(eskaeraZenbakia)");
                db.execSQL("CREATE INDEX index_historial_compras_komertzialId ON historial_compras(komertzialId)");
                db.execSQL("CREATE INDEX index_historial_compras_bazkideaId ON historial_compras(bazkideaId)");
            } else {
                // Taula ez badago, sortu egitura ZUZEKIN Foreign Keys-ekin
                db.execSQL("CREATE TABLE historial_compras (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "bidalketaId INTEGER NOT NULL, " +
                        "kodea TEXT, " +
                        "helmuga TEXT, " +
                        "data TEXT, " +
                        "amaituta INTEGER NOT NULL, " +
                        "productoId TEXT, " +
                        "productoIzena TEXT, " +
                        "eskatuta INTEGER NOT NULL, " +
                        "bidalita INTEGER NOT NULL, " +
                        "prezioUnit REAL NOT NULL, " +
                        "argazkia TEXT, " +
                        "eskaeraZenbakia TEXT, " +
                        "komertzialId INTEGER, " +
                        "bazkideaId INTEGER, " +
                        "FOREIGN KEY(eskaeraZenbakia) REFERENCES eskaera_goiburuak(zenbakia) ON DELETE CASCADE, " +
                        "FOREIGN KEY(komertzialId) REFERENCES komertzialak(id) ON DELETE SET NULL, " +
                        "FOREIGN KEY(bazkideaId) REFERENCES bazkideak(id) ON DELETE SET NULL)");
                db.execSQL("CREATE INDEX index_historial_compras_kodea ON historial_compras(kodea)");
                db.execSQL("CREATE INDEX index_historial_compras_data ON historial_compras(data)");
                db.execSQL("CREATE INDEX index_historial_compras_productoId ON historial_compras(productoId)");
                db.execSQL("CREATE INDEX index_historial_compras_bidalketaId ON historial_compras(bidalketaId)");
                db.execSQL("CREATE INDEX index_historial_compras_eskaeraZenbakia ON historial_compras(eskaeraZenbakia)");
                db.execSQL("CREATE INDEX index_historial_compras_komertzialId ON historial_compras(komertzialId)");
                db.execSQL("CREATE INDEX index_historial_compras_bazkideaId ON historial_compras(bazkideaId)");
            }

            // 2. eskaera_xehetasunak taula berriztatu Foreign Key-ekin
            if (taulaExistitzenDa(db, "eskaera_xehetasunak")) {
                // Table swap estrategia
                db.execSQL("CREATE TABLE eskaera_xehetasunak_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "eskaeraZenbakia TEXT, " +
                        "artikuluKodea TEXT, " +
                        "kantitatea INTEGER NOT NULL, " +
                        "prezioa REAL NOT NULL, " +
                        "FOREIGN KEY(eskaeraZenbakia) REFERENCES eskaera_goiburuak(zenbakia) ON DELETE CASCADE)");

                // Datuak kopiatu
                db.execSQL("INSERT INTO eskaera_xehetasunak_new " +
                        "(id, eskaeraZenbakia, artikuluKodea, kantitatea, prezioa) " +
                        "SELECT id, eskaeraZenbakia, artikuluKodea, kantitatea, prezioa " +
                        "FROM eskaera_xehetasunak");

                // Indize zaharrak ezabatu
                db.execSQL("DROP INDEX IF EXISTS index_eskaera_xehetasunak_eskaeraZenbakia");
                db.execSQL("DROP INDEX IF EXISTS index_eskaera_xehetasunak_artikuluKodea");
                db.execSQL("DROP TABLE eskaera_xehetasunak");

                // Taula berria izendatu
                db.execSQL("ALTER TABLE eskaera_xehetasunak_new RENAME TO eskaera_xehetasunak");

                // Indize berriak sortu
                db.execSQL("CREATE INDEX index_eskaera_xehetasunak_eskaeraZenbakia ON eskaera_xehetasunak(eskaeraZenbakia)");
                db.execSQL("CREATE INDEX index_eskaera_xehetasunak_artikuluKodea ON eskaera_xehetasunak(artikuluKodea)");
            }
            // Taula ez badago, Room-ek sortuko du entitatearen arabera
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
    /** Bazkideak taularen kontsultak. */
    public abstract BazkideaDao bazkideaDao();
    /** Eskaerak taularen kontsultak. */
    public abstract EskaeraDao eskaeraDao();
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
    /** Historial de compras taularen kontsultak. */
    public abstract HistorialCompraDao historialCompraDao();

    /**
     * Datu-basearen instantzia bakarra itzuli (singleton).
     * Kontekstua aplikazioko kontekstua izan behar da.
     * Hau hemen badago, dena ondo doa - datu-basea prest dago kontsultak egiteko.
     */
    public static AppDatabase getInstance(Context context) {
        // Begiratu hemen ea instantzia badago, hori itzultzen dugu (singleton patroia)
        if (instantzia == null) {
            // Synchronized blokea - hainbat hari aldi berean instantzia sortzea saihesteko
            synchronized (AppDatabase.class) {
                // Double-check locking - beste hari batek sortu badu bitartean, berriro egiaztatu
                if (instantzia == null) {
                    // Hau hemen datu-basea eraikitzen dugu - migrazio guztiak gehitzen ditugu
                    instantzia = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "techno_basque_db"
                    ).addMigrations(MIGRAZIO_1_2, MIGRAZIO_2_3, MIGRAZIO_3_4, MIGRAZIO_4_5, MIGRAZIO_5_6, MIGRAZIO_6_7, MIGRAZIO_7_8, MIGRAZIO_8_9, MIGRAZIO_9_10, MIGRAZIO_10_11, MIGRAZIO_11_12, MIGRAZIO_12_13, MIGRAZIO_13_14, MIGRAZIO_14_15, MIGRAZIO_15_16, MIGRAZIO_16_17, MIGRAZIO_17_18, MIGRAZIO_18_19)
                            .fallbackToDestructiveMigration()  // KRITIKOA: Garapen fasean bagaude, eskema aldaketa handia: datu-base zaharra ezabatu eta berria sortu
                            .allowMainThreadQueries()  // Kontsulta bat hari nagusian egiten bada itxiera saihesteko
                            .build();
                    // Hau hemen badago, datu-basea prest dago kontsultak egiteko
                }
            }
        }
        // Begiratu hemen ea instantzia badago, hori itzultzen dugu
        return instantzia;
    }
}
