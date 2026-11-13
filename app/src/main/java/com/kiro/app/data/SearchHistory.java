package com.kiro.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/** Recent search queries, newest first, persisted in SharedPreferences. */
public final class SearchHistory {
    private static final String KEY = "recent_searches";
    private static final int MAX = 10;

    private final SharedPreferences prefs;

    public SearchHistory(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences("kiro_prefs", Context.MODE_PRIVATE);
    }

    public synchronized List<String> getAll() {
        List<String> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) out.add(arr.getString(i));
        } catch (JSONException ignored) {
            // corrupted value: treat as empty
        }
        return out;
    }

    public synchronized void add(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return;
        List<String> all = getAll();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).equalsIgnoreCase(q)) all.remove(i);
        }
        all.add(0, q);
        while (all.size() > MAX) all.remove(all.size() - 1);
        save(all);
    }

    public synchronized void remove(String query) {
        List<String> all = getAll();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).equalsIgnoreCase(query)) all.remove(i);
        }
        save(all);
    }

    public synchronized void clear() {
        prefs.edit().remove(KEY).apply();
    }

    private void save(List<String> list) {
        prefs.edit().putString(KEY, new JSONArray(list).toString()).apply();
    }
}
