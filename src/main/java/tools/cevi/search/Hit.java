package tools.cevi.search;

/**
 * One match on the Search Results Page or among the Live Results (UC-010 step 3, A1).
 *
 * @param title    the event's title or the offer's Organisation
 * @param location the location of the event or offer
 * @param dateText the event's Date Text; empty for an offer
 * @param excerpt  plain text from the Description, rendered escaped
 * @param href     where picking the hit leads (step 5)
 * @param key      the value the accent colour of the card is derived from, as on the lists
 */
public record Hit(String title, String location, String dateText, String excerpt, String href, String key) {
}
