package uk.ac.ox.it.calendarimporter.jobs.csv;

import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.service.EventService;
import uk.ac.ox.it.calendarimporter.utils.HiddenData;

/**
 * This job reads a CSV from a URL (probably local), parses it and then uploads the events into
 * Canvas.
 */
public class CSVImportJob extends CanvasCalendarJob {

  private static final String HIDDEN_DATA_PREFIX = "csv-import:";
  private Logger log = LoggerFactory.getLogger(CSVImportJob.class);

  private boolean hasErrors;

  @Autowired private EventService eventService;

  @Autowired private CSVReader reader;

  @Override
  public void run() throws IOException, JobExecutionException {

    TimeZone timeZone = TimeZone.getTimeZone(this.timeZone);

    int progress = 0;
    // Just a short code that should be unique to group together imports.
    // We don't want to use the triggerID as it's semi secret, only need a few characters so they don't clash
    String hiddenData = HiddenData.toHidden(HIDDEN_DATA_PREFIX + UUID.randomUUID().toString().substring(0, 6));
    log(progress, "Import started, timezone of: "+ timeZone.getDisplayName());
    URL url = new URL(this.url);
    log.debug("Attempting to load CSV file: {}", url);
    CSVReader.ErrorHandler errorHandler =
        e -> {
          // Programmers are 0 based, users are 1 based
          log("Error on row %d, message: %s", e.getRow() + 1, e.getLocalizedMessage());
          hasErrors = true;
        };
    List<CalendarEvent> calendarEvents = reader.parseCSV(url, timeZone, errorHandler);
    log.trace("Parsed {} rows and has errors: {}", calendarEvents.size(), hasErrors);
    log("Source file read.");
    if (calendarEvents.isEmpty()) {
      log("No events found in file");
      return;
    }
    CalendarWriter calendarWriter = canvasApiFactory.getWriter(CalendarWriter.class, oauthToken);
    int eventProgress = 0;
    int eventTotal = calendarEvents.size();
    int progressPerEvent = (100 - progress) / eventTotal;
    int created = 0;
    for (CalendarEvent event : calendarEvents) {
      if (isInterrupted()) {
        log("Interrupted after %d of %d events", ++eventProgress, eventTotal);
        throw new JobExecutionException("Job interrupted");
      }
      event.setContextCode(context);
      // Flag that this was created by an import
      String description = HiddenData.insertHidden(event.getDescription(), hiddenData);
      event.setDescription(description);
      Optional<CalendarEvent> calendarEventOpt = calendarWriter.createCalendarEvent(event);
      if (calendarEventOpt.isPresent()) {
        eventService.eventCreated(tenant.getId(), calendarImport, calendarEventOpt.get());
        created++;
      }

      progress += progressPerEvent;
      log(progress, "Processed event %d of %d", ++eventProgress, eventTotal);
    }
    log(
        "Completed import, found %d events, imported %d events into calendar.",
        eventTotal, created);
  }

  public boolean hasErrors() {
    return hasErrors;
  }
}
