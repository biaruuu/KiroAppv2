//! Pure (JNI-free) title ranking used by Kiro's search.
//!
//! Scoring (higher is better), computed on normalised text (lowercase, alphanumerics only):
//!   exact title 1000 | title starts with query 800 | a word starts with query 600 | substring 400
//!   + per query token: exact word 100, word prefix 70, substring 40, typo (edit distance) 30
//!   + 50 if every token matched, minus a small penalty for very long titles.

/// Lowercase, replace every non-alphanumeric run with a single space, trim.
pub fn normalize(s: &str) -> String {
    let mut out = String::with_capacity(s.len());
    let mut last_space = true;
    for c in s.chars() {
        if c.is_alphanumeric() {
            for lc in c.to_lowercase() {
                out.push(lc);
            }
            last_space = false;
        } else if !last_space {
            out.push(' ');
            last_space = true;
        }
    }
    while out.ends_with(' ') {
        out.pop();
    }
    out
}

/// Levenshtein distance on chars.
pub fn edit_distance(a: &str, b: &str) -> usize {
    let a: Vec<char> = a.chars().collect();
    let b: Vec<char> = b.chars().collect();
    if a.is_empty() {
        return b.len();
    }
    if b.is_empty() {
        return a.len();
    }
    let mut prev: Vec<usize> = (0..=b.len()).collect();
    let mut cur: Vec<usize> = vec![0; b.len() + 1];
    for i in 1..=a.len() {
        cur[0] = i;
        for j in 1..=b.len() {
            let cost = if a[i - 1] == b[j - 1] { 0 } else { 1 };
            cur[j] = (prev[j] + 1).min(cur[j - 1] + 1).min(prev[j - 1] + cost);
        }
        std::mem::swap(&mut prev, &mut cur);
    }
    prev[b.len()]
}

/// Score `text` against `query`. Both must already be normalised. 0 means "no match".
pub fn score(query: &str, text: &str) -> i32 {
    if query.is_empty() || text.is_empty() {
        return 0;
    }
    if text == query {
        return 1000;
    }

    let mut total: i32 = 0;
    if text.starts_with(query) {
        total += 800;
    } else if text.contains(&format!(" {}", query)) {
        total += 600;
    } else if text.contains(query) {
        total += 400;
    }

    let words: Vec<&str> = text.split(' ').collect();
    let mut matched = 0;
    let mut tokens = 0;
    for t in query.split(' ') {
        tokens += 1;
        let tlen = t.chars().count();
        let mut best = 0;
        for w in &words {
            let s = if *w == t {
                100
            } else if w.starts_with(t) {
                70
            } else if tlen >= 3 && w.contains(t) {
                40
            } else if tlen >= 4 && edit_distance(w, t) <= if tlen <= 5 { 1 } else { 2 } {
                30
            } else {
                0
            };
            if s > best {
                best = s;
            }
        }
        if best > 0 {
            matched += 1;
            total += best;
        }
    }

    if total == 0 {
        return 0;
    }
    if matched == tokens {
        total += 50;
    }
    total - (text.chars().count().min(100) as i32) / 10
}

/// Indices of `texts` that match `query`, best first (ties keep original order).
pub fn rank_indices<S: AsRef<str>>(query: &str, texts: &[S]) -> Vec<i32> {
    let q = normalize(query);
    if q.is_empty() {
        return Vec::new();
    }
    let mut scored: Vec<(i32, i32)> = texts
        .iter()
        .enumerate()
        .filter_map(|(i, t)| {
            let s = score(&q, &normalize(t.as_ref()));
            if s > 0 {
                Some((s, i as i32))
            } else {
                None
            }
        })
        .collect();
    scored.sort_by(|a, b| b.0.cmp(&a.0).then(a.1.cmp(&b.1)));
    scored.into_iter().map(|(_, i)| i).collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn normalizes() {
        assert_eq!(normalize("  Re:ZERO -Starting Life!  "), "re zero starting life");
        assert_eq!(normalize("K-On!!"), "k on");
    }

    #[test]
    fn edit_distance_basic() {
        assert_eq!(edit_distance("naruto", "narto"), 1);
        assert_eq!(edit_distance("", "abc"), 3);
        assert_eq!(edit_distance("same", "same"), 0);
    }

    #[test]
    fn exact_beats_prefix_beats_word_beats_substring() {
        let texts = vec![
            "Bleach: Thousand-Year Blood War", // word match on "war"? no - used for other test
            "One Piece",
            "One Punch Man",
            "Piece of Mind",
            "Masterpiece Theater",
        ];
        let r = rank_indices("one piece", &texts);
        assert_eq!(r[0], 1); // exact
        let r = rank_indices("piece", &texts);
        // "piece of mind" starts with piece (800) > "one piece" word (600) > "masterpiece" substring (400)
        assert_eq!(r, vec![3, 1, 4]);
    }

    #[test]
    fn multi_token_out_of_order() {
        let texts = vec!["Attack on Titan", "Titan A.E.", "Attack of the Clones"];
        let r = rank_indices("attack titan", &texts);
        assert_eq!(r[0], 0);
    }

    #[test]
    fn tolerates_typos() {
        let texts = vec!["Naruto Shippuden", "Boruto"];
        let r = rank_indices("narto", &texts);
        assert_eq!(r, vec![0]);
    }

    #[test]
    fn no_match_is_empty() {
        let texts = vec!["Haikyu!!", "Death Note"];
        assert!(rank_indices("zzzzqq", &texts).is_empty());
        assert!(rank_indices("   ", &texts).is_empty());
    }

    #[test]
    fn ties_keep_original_order() {
        let texts = vec!["Slime A", "Slime B"];
        assert_eq!(rank_indices("slime", &texts), vec![0, 1]);
    }
}
