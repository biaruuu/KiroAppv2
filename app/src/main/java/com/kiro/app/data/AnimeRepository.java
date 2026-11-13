package com.kiro.app.data;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * In-memory cache in front of {@link AnimeApi}. Fresh entries (10 min) are served without a request;
 * if the network fails, a stale entry is returned instead of an error so the app keeps working offline.
 * All methods block - call them off the main thread.
 */
public final class AnimeRepository {
    private static final long TTL_MS = 10 * 60 * 1000L;

    private static volatile AnimeRepository instance;

    public static AnimeRepository getInstance() {
        if (instance == null) {
            synchronized (AnimeRepository.class) {
                if (instance == null) instance = new AnimeRepository();
            }
        }
        return instance;
    }

    private interface Loader {
        List<Anime> load() throws IOException;
    }

    private static final class Entry {
        final long at;
        final List<Anime> data;

        Entry(long at, List<Anime> data) {
            this.at = at;
            this.data = data;
        }
    }

    private final AnimeApi api = new AnimeApi();
    private final Map<String, Entry> cache = new HashMap<>();

    private AnimeRepository() {
    }

    public List<Anime> topAiring(final int limit) throws IOException {
        return cached("airing:" + limit, () -> api.topAiring(limit));
    }

    public List<Anime> seasonNow(final int limit) throws IOException {
        return cached("season:" + limit, () -> api.seasonNow(limit));
    }

    public List<Anime> topByPopularity(final int limit) throws IOException {
        return cached("popular:" + limit, () -> api.topByPopularity(limit));
    }

    public List<Anime> byGenre(final String genre, final int limit) throws IOException {
        return cached("genre:" + genre + ":" + limit, () -> api.byGenre(genre, limit));
    }

    public List<Anime> search(final String query, final int limit) throws IOException {
        final String q = query.trim();
        return cached("search:" + q.toLowerCase(Locale.ROOT) + ":" + limit, () -> api.search(q, limit));
    }

    public void clearCache() {
        synchronized (cache) {
            cache.clear();
        }
    }

    private List<Anime> cached(String key, Loader loader) throws IOException {
        Entry existing;
        synchronized (cache) {
            existing = cache.get(key);
        }
        long now = System.currentTimeMillis();
        if (existing != null && now - existing.at < TTL_MS) return existing.data;

        try {
            List<Anime> fresh = loader.load();
            synchronized (cache) {
                cache.put(key, new Entry(System.currentTimeMillis(), fresh));
            }
            return fresh;
        } catch (IOException e) {
            if (existing != null) return existing.data; // stale beats nothing
            throw e;
        }
    }
}
