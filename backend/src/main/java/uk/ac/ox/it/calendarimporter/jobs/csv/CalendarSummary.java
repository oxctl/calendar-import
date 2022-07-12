package uk.ac.ox.it.calendarimporter.jobs.csv;

import java.time.Instant;
import java.util.Objects;


/**
 * This is a holder for all the data we care about on a CalendarEvent that if changes means we need to update
 * the event.
 */
public class CalendarSummary {

    private String title;
    private String description;
    private String locationName;
    private String locationAddress;
    private Instant starts;
    private Instant ends;

    public CalendarSummary(String title, String description, String locationName, String locationAddress, Instant starts, Instant ends) {
        this.title = title;
        this.description = description;
        this.locationName = locationName;
        this.locationAddress = locationAddress;
        this.starts = starts;
        this.ends = ends;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalendarSummary that = (CalendarSummary) o;
        return Objects.equals(title, that.title) && Objects.equals(description, that.description) && Objects.equals(locationName, that.locationName) && Objects.equals(locationAddress, that.locationAddress) && Objects.equals(starts, that.starts) && Objects.equals(ends, that.ends);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, locationName, locationAddress, starts, ends);
    }
}
