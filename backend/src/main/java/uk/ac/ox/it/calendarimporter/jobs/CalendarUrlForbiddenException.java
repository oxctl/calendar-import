package uk.ac.ox.it.calendarimporter.jobs;

import org.quartz.JobExecutionException;

public class CalendarUrlForbiddenException extends JobExecutionException {
    public CalendarUrlForbiddenException(Throwable cause) {
        super(cause);
    }
}
