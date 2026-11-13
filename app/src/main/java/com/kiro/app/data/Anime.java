package com.kiro.app.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Immutable anime model, parsed from the AniList GraphQL API and persisted locally as JSON. */
public final class Anime {
    /** AniList media id. */
    public final int id;
    public final String title;
    public final String titleEnglish;
    public final String synopsis;
    public final String imageUrl;
    public final String largeImageUrl;
    public final String type;
    public final String status;
    public final String trailerUrl;
    public final String url;
    /** 0-10 (AniList's 0-100 average score divided by 10); 0 when unrated. */
    public final double score;
    /** -1 when unknown. */
    public final int episodes;
    public final int year;
    public final List<String> genres;

    public Anime(int id, String title, String titleEnglish, String synopsis,
                 String imageUrl, String largeImageUrl, String type, String status,
                 String trailerUrl, String url, double score, int episodes, int year,
                 List<String> genres) {
        this.id = id;
        this.title = nz(title);
        this.titleEnglish = nz(titleEnglish);
        this.synopsis = nz(synopsis);
        this.imageUrl = nz(imageUrl);
        this.largeImageUrl = nz(largeImageUrl);
        this.type = nz(type);
        this.status = nz(status);
        this.trailerUrl = nz(trailerUrl);
        this.url = nz(url);
        this.score = score;
        this.episodes = episodes;
        this.year = year;
        this.genres = genres == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(genres));
    }

    /** English title when AniList has one, otherwise the romaji title. */
    public String displayTitle() {
        return !titleEnglish.isEmpty() ? titleEnglish : title;
    }

    /** Text used by the search ranker (both title variants). */
    public String searchText() {
        return titleEnglish.isEmpty() || titleEnglish.equals(title) ? title : title + " " + titleEnglish;
    }

    /** Best available poster. */
    public String bestImage() {
        return !largeImageUrl.isEmpty() ? largeImageUrl : imageUrl;
    }

    // ------------------------------------------------------------------ parsing

    /** Parses one element of AniList's Page.media array. */
    public static Anime fromAniList(JSONObject o) throws JSONException {
        int id = o.getInt("id");

        JSONObject titles = o.optJSONObject("title");
        String romaji = titles == null ? "" : str(titles, "romaji");
        String english = titles == null ? "" : str(titles, "english");

        JSONObject cover = o.optJSONObject("coverImage");
        String large = cover == null ? "" : str(cover, "large");
        String extraLarge = cover == null ? "" : str(cover, "extraLarge");

        String trailerUrl = "";
        JSONObject trailer = o.optJSONObject("trailer");
        if (trailer != null) {
            String tid = str(trailer, "id");
            String site = str(trailer, "site").toLowerCase(Locale.ROOT);
            if (!tid.isEmpty()) {
                if (site.equals("youtube")) {
                    trailerUrl = "https://www.youtube.com/watch?v=" + tid;
                } else if (site.equals("dailymotion")) {
                    trailerUrl = "https://www.dailymotion.com/video/" + tid;
                }
            }
        }

        int year = o.isNull("seasonYear") ? 0 : o.optInt("seasonYear", 0);
        if (year == 0) {
            JSONObject start = o.optJSONObject("startDate");
            if (start != null && !start.isNull("year")) year = start.optInt("year", 0);
        }

        List<String> genres = new ArrayList<>();
        JSONArray g = o.optJSONArray("genres");
        if (g != null) {
            for (int i = 0; i < g.length(); i++) {
                String name = g.optString(i, "");
                if (!name.isEmpty() && !name.equals("null")) genres.add(name);
            }
        }

        double score = o.isNull("averageScore") ? 0.0 : o.optInt("averageScore", 0) / 10.0;

        return new Anime(
                id,
                romaji.isEmpty() ? english : romaji,
                english,
                cleanDescription(str(o, "description")),
                large,
                extraLarge,
                formatLabel(str(o, "format")),
                statusLabel(str(o, "status")),
                trailerUrl,
                str(o, "siteUrl"),
                score,
                o.isNull("episodes") ? -1 : o.optInt("episodes", -1),
                year,
                genres);
    }

    // ------------------------------------------------------------- persistence

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("titleEn", titleEnglish);
        o.put("synopsis", synopsis);
        o.put("img", imageUrl);
        o.put("large", largeImageUrl);
        o.put("type", type);
        o.put("status", status);
        o.put("trailer", trailerUrl);
        o.put("url", url);
        o.put("score", score);
        o.put("eps", episodes);
        o.put("year", year);
        o.put("genres", new JSONArray(genres));
        return o;
    }

    public static Anime fromJson(JSONObject o) throws JSONException {
        List<String> genres = new ArrayList<>();
        JSONArray g = o.optJSONArray("genres");
        if (g != null) {
            for (int i = 0; i < g.length(); i++) genres.add(g.optString(i, ""));
        }
        return new Anime(
                o.getInt("id"),
                o.optString("title", ""),
                o.optString("titleEn", ""),
                o.optString("synopsis", ""),
                o.optString("img", ""),
                o.optString("large", ""),
                o.optString("type", ""),
                o.optString("status", ""),
                o.optString("trailer", ""),
                o.optString("url", ""),
                o.optDouble("score", 0.0),
                o.optInt("eps", -1),
                o.optInt("year", 0),
                genres);
    }

    // ----------------------------------------------------------------- helpers

    /** org.json's optString() returns the text "null" for JSON null - this returns "". */
    private static String str(JSONObject o, String key) {
        return o.isNull(key) ? "" : o.optString(key, "");
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** AniList descriptions contain a little HTML (br, i, b, a). Turn it into plain text. */
    static String cleanDescription(String s) {
        if (s == null || s.isEmpty()) return "";
        String t = s.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
        t = t.replaceAll("\\s*\\(Source:[^)]*\\)", "")
                .replaceAll("\\s*\\[Written by[^\\]]*\\]", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n");
        return t.trim();
    }

    private static String formatLabel(String f) {
        switch (f) {
            case "":
                return "";
            case "TV":
                return "TV";
            case "TV_SHORT":
                return "TV Short";
            case "MOVIE":
                return "Movie";
            case "SPECIAL":
                return "Special";
            case "OVA":
                return "OVA";
            case "ONA":
                return "ONA";
            case "MUSIC":
                return "Music";
            default:
                return f.replace('_', ' ');
        }
    }

    private static String statusLabel(String s) {
        switch (s) {
            case "":
                return "";
            case "RELEASING":
                return "Currently Airing";
            case "FINISHED":
                return "Finished Airing";
            case "NOT_YET_RELEASED":
                return "Not Yet Aired";
            case "CANCELLED":
                return "Cancelled";
            case "HIATUS":
                return "On Hiatus";
            default:
                return s.replace('_', ' ');
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Anime && ((Anime) other).id == id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
