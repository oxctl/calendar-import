package uk.ac.ox.it.calendarimporter.jobs.csv;

import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import edu.ksu.canvas.requestOptions.DeleteCalendarEventOptions;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEvent;
import uk.ac.ox.it.calendarimporter.persistence.repo.ImportedEventRepository;
import uk.ac.ox.it.calendarimporter.service.ImportEventService;
import uk.ac.ox.it.calendarimporter.utils.HiddenData;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

/**
 * This job reads a CSV from a URL (probably local), parses it and then uploads the events into
 * Canvas.
 */
public class CSVImportJob extends CanvasCalendarJob {

    private static final String HIDDEN_DATA_PREFIX = "csv-import:";
    private final Logger log = LoggerFactory.getLogger(CSVImportJob.class);

    @Autowired
    private ImportEventService importEventService;

    @Autowired
    private ImportedEventRepository importedEventRepository;

    @Autowired
    private CSVReader reader;

    @Override
    public void run() throws IOException, JobExecutionException {

        CalendarWriter calendarWriter = canvasApiFactory.getWriter(CalendarWriter.class, oauthToken);
        runJobRecovery(calendarWriter);

        TimeZone timeZone = TimeZone.getTimeZone(this.timeZone);

        // Just a short code that should be unique to group together imports.
        // We don't want to use the triggerID as it's semi secret, only need a few characters so they
        // don't clash
        String hiddenData = HiddenData.toHidden(HIDDEN_DATA_PREFIX + id);
        log("Import started, timezone of: " + timeZone.getID());
        URL url = new URL(this.url);
        log.debug("Attempting to load CSV file: {}", url);
        log("Reading in file.");
        // Programmers are 0 based, users are 1 based
        TrackingErrorHandler errorHandler = new TrackingErrorHandler();
        List<CalendarEvent> calendarEvents;
        try {
            URLConnection connection = url.openConnection();
            connection.setReadTimeout(10000);
            calendarEvents = reader.parseCSV(connection.getInputStream(), timeZone, errorHandler);
        } catch (HeaderException he) {
            failure("Failed to read file: " + he.getLocalizedMessage());
            return;
        }
        log.trace("Parsed {} rows.", calendarEvents.size());
        log("Source file read.");
        if (calendarEvents.isEmpty()) {
            problem("No events found in file.");
            return;
        }
        Progress progress = new Progress(calendarEvents.size());
        int created = 0;
        log("Creating events in Canvas.");
        for (CalendarEvent event : calendarEvents) {
            if (isInterrupted()) {
                log("Interrupted after %d of %d events", progress.getSeen(), progress.getTotal());
                throw new JobExecutionException("Job interrupted");
            }
            event.setContextCode(context);
            // Flag that this was created by an import
            String description = HiddenData.insertHidden(event.getDescription(), hiddenData);
            event.setDescription(description);
            if (section != null) {
                event = toSection(event, section);
            }
            Optional<CalendarEvent> calendarEventOpt = calendarWriter.createCalendarEvent(event);
            progress.increment();
            if (calendarEventOpt.isPresent()) {
                created++;
                log(progress, "Created event %d of %d", progress.getSeen(), progress.getTotal());
                importEventService.eventCreated(tenant.getId(), calendarImport, calendarEventOpt.get());
            }
        }
        if (errorHandler.hasProblems()) {
            log(
                    "Completed import, found %d events, imported %d events, problems with %d events",
                    progress.getTotal(), created, errorHandler.problems);

        } else {
            log(
                    "Completed import, found %d events, imported %d events into calendar.",
                    progress.getTotal(), created);
        }
        log.info(
                "Imported {} of {} events from calendar {} of {}",
                progress.getTotal(),
                calendarEvents.size(),
                context,
                tenant);
    }

    private void runJobRecovery(CalendarWriter calendarWriter) throws IOException {
        // If the job was running when the scheduler died we first remove all the events created by the
        // first run and
        // ony then re-attempt to import the file.
        List<ImportedEvent> existing = importedEventRepository.findByCalendarImport(calendarImport);
        if (!existing.isEmpty()) {
            log("Recovering, deleting existing events.");
            int events = existing.size(), deleted = 0;
            for (ImportedEvent event : existing) {
                calendarWriter.deleteCalendarEvent(
                        new DeleteCalendarEventOptions(event.getIdentity().getId()));
                deleted++;
                // Unlike the delete job, here we cleanup events.
                importedEventRepository.delete(event);
                log("Recovering, cleaning event %d of %d", deleted, events);
            }
            log("Recovery complete");
        }
    }

    /**
     * This moves the data into a calendar event section.
     *
     * @param event The original event.
     * @return A new event which will import the event into the specified section.
     */
    private CalendarEvent toSection(CalendarEvent event, String section) {
        CalendarEvent.ChildEvent childEvent = new CalendarEvent.ChildEvent();
        childEvent.setStartAt(event.getStartAt());
        childEvent.setEndAt(event.getEndAt());
        childEvent.setContextCode(section);
        event.setStartAt(null);
        event.setEndAt(null);
        event.setChildEventsData(Collections.singletonList(childEvent));
        return event;
    }

    private class TrackingErrorHandler implements CSVReader.ErrorHandler {
        private int problems = 0;

        @Override
        public void handleError(RowException e) {
            CSVImportJob.this.problem(
                    "Error on row %d, message: %s", e.getRow() + 1, e.getLocalizedMessage());
            problems++;
        }

        public boolean hasProblems() {
            return problems > 0;
        }
    }
}
