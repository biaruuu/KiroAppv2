package com.kiro.app.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Blocking client for the public AniList GraphQL API (https://graphql.anilist.co) - no API key needed.
 * Call from a background thread only. Requests are spaced out to respect AniList's rate limit and
 * retried on HTTP 429 (honouring Retry-After) and 5xx.
 */
public final class AnimeApi {
    private static final String ENDPOINT = "https://graphql.anilist.co";
    private static final int MAX_ATTEMPTS = 3;
    private static final long MIN_GAP_MS = 600L;
    private static final int MAX_PER_PAGE = 50;

    private static final String MEDIA_FIELDS =
            "id title{romaji english} description(asHtml:false) coverImage{large extraLarge} "
                    + "averageScore episodes seasonYear startDate{year} format status genres "
                    + "trailer{id site} siteUrl";

    /** Optional variables ($search, $genre, $status) are simply omitted from the request when unused. */
    private static final String QUERY =
            "query($perPage:Int,$sort:[MediaSort],$search:String,$genre:String,$status:MediaStatus){"
                    + "Page(page:1,perPage:$perPage){"
                    + "media(type:ANIME,isAdult:false,sort:$sort,search:$search,genre:$genre,status:$status){"
                    + MEDIA_FIELDS
                    + "}}}";

    private static long lastRequestAt = 0L;

    /** Currently airing shows people are talking about right now (used for the hero banner). */
    public List<Anime> topAiring(int limit) throws IOException {
        return fetch(limit, "TRENDING_DESC", null, null, "RELEASING");
    }

    /** Currently airing shows, most popular first. */
    public List<Anime> seasonNow(int limit) throws IOException {
        return fetch(limit, "POPULARITY_DESC", null, null, "RELEASING");
    }

    public List<Anime> topByPopularity(int limit) throws IOException {
        return fetch(limit, "POPULARITY_DESC", null, null, null);
    }

    /** @param genre an AniList genre name such as "Action" or "Slice of Life". */
    public List<Anime> byGenre(String genre, int limit) throws IOException {
        return fetch(limit, "POPULARITY_DESC", null, genre, null);
    }

    public List<Anime> search(String query, int limit) throws IOException {
        return fetch(limit, "SEARCH_MATCH", query, null, null);
    }

    // ------------------------------------------------------------------ internals

    private List<Anime> fetch(int limit, String sort, String search, String genre, String status)
            throws IOException {
        String body;
        try {
            JSONObject vars = new JSONObject();
            vars.put("perPage", Math.max(1, Math.min(limit, MAX_PER_PAGE)));
            vars.put("sort", new JSONArray().put(sort));
            if (search != null) vars.put("search", search);
            if (genre != null) vars.put("genre", genre);
            if (status != null) vars.put("status", status);

            JSONObject req = new JSONObject();
            req.put("query", QUERY);
            req.put("variables", vars);
            body = req.toString();
        } catch (JSONException e) {
            throw new IOException("Could not build request", e);
        }

        JSONObject root = post(body);

        List<Anime> out = new ArrayList<>();
        JSONObject data = root.optJSONObject("data");
        JSONObject page = data == null ? null : data.optJSONObject("Page");
        JSONArray media = page == null ? null : page.optJSONArray("media");
        if (media == null) return out;

        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < media.length(); i++) {
            JSONObject item = media.optJSONObject(i);
            if (item == null) continue;
            try {
                Anime anime = Anime.fromAniList(item);
                if (anime.bestImage().isEmpty() || anime.displayTitle().isEmpty()) continue;
                if (seen.add(anime.id)) out.add(anime);
            } catch (JSONException ignored) {
                // skip malformed entries instead of failing the whole list
            }
        }
        return out;
    }

    private JSONObject post(String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        IOException last = null;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            throttle();
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(20_000);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", "Kiro/1.0 (Android)");
                conn.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bytes);
                }

                int code = conn.getResponseCode();
                if (code == 429) {
                    last = new IOException("AniList is limiting requests right now. Try again in a moment.");
                    sleepQuietly(retryAfterMs(conn));
                    continue;
                }
                if (code >= 500) {
                    last = new IOException("AniList is having trouble right now (HTTP " + code + ")");
                    sleepQuietly(1500L * (attempt + 1));
                    continue;
                }

                InputStream in = code < 400 ? conn.getInputStream() : conn.getErrorStream();
                String text = in == null ? "" : readAll(in);

                JSONObject root;
                try {
                    root = new JSONObject(text);
                } catch (JSONException e) {
                    throw new IOException("Unexpected response from AniList (HTTP " + code + ")", e);
                }

                JSONArray errors = root.optJSONArray("errors");
                if (errors != null && errors.length() > 0) {
                    JSONObject first = errors.optJSONObject(0);
                    String message = first == null ? "" : first.optString("message", "");
                    throw new IOException("AniList error: " + (message.isEmpty() ? "HTTP " + code : message));
                }
                if (code >= 400) throw new IOException("Request failed (HTTP " + code + ")");
                return root;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        throw last != null ? last : new IOException("Request failed");
    }

    private static long retryAfterMs(HttpURLConnection conn) {
        long seconds = 2;
        String header = conn.getHeaderField("Retry-After");
        if (header != null) {
            try {
                seconds = Long.parseLong(header.trim());
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }
        return Math.max(1000L, Math.min(seconds, 10L) * 1000L);
    }

    private static synchronized void throttle() throws IOException {
        long wait = MIN_GAP_MS - (System.currentTimeMillis() - lastRequestAt);
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted");
            }
        }
        lastRequestAt = System.currentTimeMillis();
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String readAll(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = reader.read(buf)) != -1) sb.append(buf, 0, n);
            return sb.toString();
        }
    }
}
