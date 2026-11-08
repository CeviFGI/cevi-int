package tools.cevi.event;

import jakarta.ws.rs.FormParam;

import java.time.LocalDate;

/**
 * The values of the event form exactly as the administrator typed them (UC-002 step 3).
 *
 * <p>The resource reads a submission into this class and the form template renders it back, so a
 * rejected entry returns with every value as it was typed — an unreadable date included (UC-002 A2)
 * — and not as the stored event would hold it.
 */
public class EventForm {
    @FormParam("id")
    public long id;
    @FormParam("title")
    public String title;
    @FormParam("slug")
    public String slug;
    @FormParam("date")
    public String date;
    @FormParam("displayDate")
    public String displayDate;
    @FormParam("firstDay")
    public String firstDay;
    @FormParam("lastDay")
    public String lastDay;
    @FormParam("location")
    public String location;
    @FormParam("description")
    public String description;

    /** UC-002 step 2: an empty form with today's date pre-filled as the display date. */
    static EventForm empty() {
        EventForm form = new EventForm();
        form.title = "";
        form.slug = "";
        form.date = "";
        form.displayDate = LocalDate.now().toString();
        form.firstDay = "";
        form.lastDay = "";
        form.location = "";
        form.description = "";
        return form;
    }

    /** UC-002 A1: the form filled with the current values of the event. */
    static EventForm of(Event event) {
        EventForm form = new EventForm();
        form.id = event.id;
        form.title = event.title;
        form.slug = event.slug;
        form.date = event.date;
        form.displayDate = event.displayDate.toString();
        form.firstDay = event.firstDay == null ? "" : event.firstDay.toString();
        form.lastDay = event.lastDay == null ? "" : event.lastDay.toString();
        form.location = event.location;
        form.description = event.description;
        return form;
    }
}
