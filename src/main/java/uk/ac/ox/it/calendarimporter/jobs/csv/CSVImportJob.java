package uk.ac.ox.it.calendarimporter.jobs.csv;

import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.service.EventService;

/**
 * This job reads a CSV from a URL (probably local), parses it and then uploads the events into
 * Canvas.
 */
public class CSVImportJob extends CanvasCalendarJob {

  private Logger log = LoggerFactory.getLogger(CSVImportJob.class);

  private long inputLimit = 1048576 * 10;

  private boolean hasErrors;

  @Autowired private EventService eventService;

  @Autowired private CSVReader reader;

  @Override
  public void run() throws IOException, JobExecutionException {
    int progress = 0;
    log(progress, "Import started.");
    URL url = new URL(this.url);
    log.debug("Attempting to load CSV file: {}", url);
    CSVReader.ErrorHandler errorHandler =
        e -> {
          // Programmers are 0 based, users are 1 based
          log("Error on row %d, message: %s", e.getRow() + 1, e.getLocalizedMessage());
          hasErrors = true;
        };
    List<CalendarEvent> calendarEvents = reader.parseCSV(url, errorHandler);
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
