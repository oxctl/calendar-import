package uk.ac.ox.it.calendarimporter.beans;

import java.time.Instant;

/**
 * Holds status of an import.
 */
public class ImportJob {

    private String progressUrl;
    private Instant started;

    public String getProgressUrl() {
        return progressUrl;
    }

    public void setProgressUrl(String progressUrl) {
        this.progressUrl = progressUrl;
    }

    public Instant getStarted() {
        return started;
    }

    public void setStarted(Instant started) {
        this.started = started;
    }
}
