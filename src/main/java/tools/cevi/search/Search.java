package tools.cevi.search;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import tools.cevi.event.Event;
import tools.cevi.infra.Excerpt;
import tools.cevi.voluntary.VoluntaryService;

/**
 * Matches a Search Term against the entries the public lists show (UC-010). The entries are
 * read by the caller; this class only decides what matches, how it ranks and what is shown of it.
 *
 * <p>The term never reaches the database: the lists are small enough (NFR-046 sizes them at 500
 * events and 200 offers) to match in memory, which is also the only way to compare the text a
 * visitor reads rather than the stored markup (BR-002).
 */
public final class Search {

    /** UC-010 BR-004: the field ranks, best first. */
    private static final int RANK_TITLE = 0;
    private static final int RANK_PLACE = 1;
    private static final int RANK_DESCRIPTION = 2;
    private static final int NO_MATCH = Integer.MAX_VALUE;

    /** How much text precedes the match in the excerpt, so the passage reads as a passage. */
    private static final int LEAD = 40;

    private Search() {
    }

    /**
     * @param events upcoming events, nearest first (UC-001 BR-002)
     * @param limit  the most hits per group, or {@link Integer#MAX_VALUE}
     */
    public static List<Hit> events(SearchTerm term, List<Event> events, int limit) {
        List<Ranked<Event>> ranked = new ArrayList<>();
        for (Event event : events) {
            String text = plain(event.description);
            int rank = rank(term.words(), new String[]{event.title}, new String[]{event.location, event.date}, text);
            if (rank != NO_MATCH) {
                ranked.add(new Ranked<>(event, rank, text));
            }
        }
        // UC-010 BR-004: by rank; ties keep the Event List order, nearest Display Date first.
        ranked.sort(Comparator.comparingInt(Ranked::rank));
        return hits(ranked, limit, r -> new Hit(r.entry().title, r.entry().location, r.entry().date,
                excerpt(r.entry().description, r.text(), term), "/anlaesse/detail?slug=" + encode(r.entry().slug), r.entry().slug));
    }

    public static List<Hit> offers(SearchTerm term, List<VoluntaryService> offers, int limit) {
        List<Ranked<VoluntaryService>> ranked = new ArrayList<>();
        for (VoluntaryService offer : offers) {
            String text = plain(offer.description);
            int rank = rank(term.words(), new String[]{offer.organization}, new String[]{offer.location}, text);
            if (rank != NO_MATCH) {
                ranked.add(new Ranked<>(offer, rank, text));
            }
        }
        // UC-010 BR-004: by rank; ties by Organisation in alphabetical order, then fixed by id.
        Collator collator = Collator.getInstance(Locale.forLanguageTag("de-CH"));
        ranked.sort(Comparator.<Ranked<VoluntaryService>>comparingInt(Ranked::rank)
                .thenComparing(r -> r.entry().organization, collator)
                .thenComparingInt(r -> r.entry().id));
        return hits(ranked, limit, r -> new Hit(r.entry().organization, r.entry().location, "",
                excerpt(r.entry().description, r.text(), term), chosenOffer(r.entry().id), String.valueOf(r.entry().id)));
    }

    /** UC-010 step 5: the offer's entry in the Offer List, marked, opened and scrolled to. */
    public static String chosenOffer(int id) {
        return "/volontariat?angebot=" + id + "#angebot-" + id;
    }

    private static <T> List<Hit> hits(List<Ranked<T>> ranked, int limit, Function<Ranked<T>, Hit> toHit) {
        return ranked.stream().limit(limit).map(toHit).toList();
    }

    /**
     * UC-010 BR-003: every word of the term begins a word somewhere in the entry. The result is the
     * best field rank in which any word occurs (BR-004), or {@link #NO_MATCH}.
     */
    private static int rank(List<String> words, String[] titles, String[] places, String description) {
        if (words.isEmpty()) {
            return NO_MATCH;
        }
        List<String> titleWords = wordsOf(titles);
        List<String> placeWords = wordsOf(places);
        List<String> descriptionWords = Words.of(description);
        int best = NO_MATCH;
        for (String word : words) {
            boolean inTitle = beginsAny(titleWords, word);
            boolean inPlace = beginsAny(placeWords, word);
            boolean inDescription = beginsAny(descriptionWords, word);
            if (!inTitle && !inPlace && !inDescription) {
                return NO_MATCH;
            }
            best = Math.min(best, bestRank(inTitle, inPlace));
        }
        return best;
    }

    private static int bestRank(boolean inTitle, boolean inPlace) {
        if (inTitle) {
            return RANK_TITLE;
        }
        return inPlace ? RANK_PLACE : RANK_DESCRIPTION;
    }

    private static List<String> wordsOf(String[] fields) {
        List<String> words = new ArrayList<>();
        for (String field : fields) {
            words.addAll(Words.of(field));
        }
        return words;
    }

    private static boolean beginsAny(List<String> entryWords, String prefix) {
        return entryWords.stream().anyMatch(entryWord -> entryWord.startsWith(prefix));
    }

    // UC-010 BR-002: the Description as a visitor reads it — no markup, no link addresses.
    private static String plain(String description) {
        return Excerpt.from(description, Integer.MAX_VALUE);
    }

    /**
     * UC-010 step 3 (OQ-003): 160 characters around the earliest occurrence of any word of the
     * term, or the beginning of the Description as on the lists when it occurs only elsewhere.
     */
    static String excerpt(String description, String text, SearchTerm term) {
        int position = Words.firstOccurrence(text, term.words());
        if (position < 0) {
            return Excerpt.from(description);
        }
        if (text.length() <= Excerpt.DEFAULT_LENGTH) {
            return text;
        }
        int start = Math.max(0, position - LEAD);
        if (start > 0) {
            int space = text.indexOf(' ', start);
            start = space >= 0 && space < position ? space + 1 : position;
        }
        int end = Math.min(text.length(), start + Excerpt.DEFAULT_LENGTH);
        if (end < text.length()) {
            int space = text.lastIndexOf(' ', end);
            if (space > position) {
                end = space;
            }
        }
        return (start > 0 ? "…" : "") + text.substring(start, end).strip() + (end < text.length() ? "…" : "");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record Ranked<T>(T entry, int rank, String text) {
    }
}
