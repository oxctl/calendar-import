package uk.ac.ox.it.calendarimporter.jobs.ical;

import java.io.IOException;

/**
 * Allows calling code to catch this when it happens.
 */
public class TerminatedIOException extends IOException {
    public TerminatedIOException(String error) {
        super(error);
    }
}
