package com.kiro.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/** "Continue Watching" list: titles the user pressed Play on, newest first. */
public final class WatchHistory {
    private static final String KEY = "continue_watching";
    private static final int MAX = 15;

    private final SharedPreferences prefs;

    public WatchHistory(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences("kiro_prefs", Context.MODE_PRIVATE);
    }

    public synchronized List<Anime> getAll() {
        List<Anime> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                try {
                    out.add(Anime.fromJson(arr.getJSONObject(i)));
                } catch (JSONException ignored) {
                    // skip one bad entry
                }
            }
        } catch (JSONException ignored) {
            // corrupted value: treat as empty
        }
        return out;
    }

    public synchronized void add(Anime anime) {
        List<Anime> all = getAll();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).id == anime.id) all.remove(i);
        }
        all.add(0, anime);
        while (all.size() > MAX) all.remove(all.size() - 1);
        save(all);
    }

    public synchronized void remove(int id) {
        List<Anime> all = getAll();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).id == id) all.remove(i);
        }
        save(all);
    }

    private void save(List<Anime> list) {
        JSONArray arr = new JSONArray();
        for (Anime a : list) {
            try {
                arr.put(a.toJson());
            } catch (JSONException ignored) {
                // skip entry that cannot be serialised
            }
        }
        prefs.edit().putString(KEY, arr.toString()).apply();
    }
}
