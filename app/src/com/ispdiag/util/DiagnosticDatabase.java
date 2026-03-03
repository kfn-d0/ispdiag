package com.ispdiag.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Banco de dados SQLite para historico de diagnosticos.
 */
public class DiagnosticDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "isp_diagnostic.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_HISTORY = "history";
    private static final String COL_ID = "id";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_SERVICE_COUNT = "service_count";
    private static final String COL_OK_COUNT = "ok_count";
    private static final String COL_PARTIAL_COUNT = "partial_count";
    private static final String COL_FAIL_COUNT = "fail_count";
    private static final String COL_JSON_REPORT = "json_report";
    private static final String COL_SERVICES_SUMMARY = "services_summary";

    public DiagnosticDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_HISTORY + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TIMESTAMP + " TEXT NOT NULL, "
                + COL_SERVICE_COUNT + " INTEGER, "
                + COL_OK_COUNT + " INTEGER, "
                + COL_PARTIAL_COUNT + " INTEGER, "
                + COL_FAIL_COUNT + " INTEGER, "
                + COL_SERVICES_SUMMARY + " TEXT, "
                + COL_JSON_REPORT + " TEXT)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    /**
     * Salva um relatorio de diagnostico no historico.
     */
    public long saveReport(String timestamp, int serviceCount,
            int okCount, int partialCount, int failCount,
            String servicesSummary, String jsonReport) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TIMESTAMP, timestamp);
        values.put(COL_SERVICE_COUNT, serviceCount);
        values.put(COL_OK_COUNT, okCount);
        values.put(COL_PARTIAL_COUNT, partialCount);
        values.put(COL_FAIL_COUNT, failCount);
        values.put(COL_SERVICES_SUMMARY, servicesSummary);
        values.put(COL_JSON_REPORT, jsonReport);
        long id = db.insert(TABLE_HISTORY, null, values);
        db.close();
        return id;
    }

    /**
     * Obtem todas as entradas do historico (mais recentes primeiro).
     */
    public List<HistoryEntry> getAllHistory() {
        List<HistoryEntry> entries = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(TABLE_HISTORY, null, null, null,
                null, null, COL_ID + " DESC");

        while (cursor.moveToNext()) {
            HistoryEntry entry = new HistoryEntry();
            entry.id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
            entry.timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));
            entry.serviceCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SERVICE_COUNT));
            entry.okCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_OK_COUNT));
            entry.partialCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PARTIAL_COUNT));
            entry.failCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_FAIL_COUNT));
            entry.servicesSummary = cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICES_SUMMARY));
            entries.add(entry);
        }
        cursor.close();
        db.close();
        return entries;
    }

    /**
     * Obtem o relatorio JSON para uma entrada especifica do historico.
     */
    public String getReportById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY, new String[] { COL_JSON_REPORT },
                COL_ID + "=?", new String[] { String.valueOf(id) },
                null, null, null);

        String report = null;
        if (cursor.moveToFirst()) {
            report = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return report;
    }

    /**
     * Exclui uma entrada especifica do historico.
     */
    public void deleteEntry(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_HISTORY, COL_ID + "=?", new String[] { String.valueOf(id) });
        db.close();
    }

    /**
     * Limpa todo o historico.
     */
    public void clearHistory() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
        db.close();
    }

    /**
     * Classe de dados simples para entradas do historico.
     */
    public static class HistoryEntry {
        public long id;
        public String timestamp;
        public int serviceCount;
        public int okCount;
        public int partialCount;
        public int failCount;
        public String servicesSummary;
    }
}
