package uk.ac.ox.it.calendarimporter.jobs.ical;

import java.time.Instant;

/**
 * Interface for reporting progress.
 * Store initially in the DB and don't save too often.
 */
public class ProgressWatcher {

    private String id;
    private Instant started;
    private Instant completed;
    private String lastMessage;
    private int percentage;

    public void update(int percentage) {
        this.percentage = percentage;
    }


}
