package uk.ac.ox.it.calendarimporter.jobs.csv;

import edu.ksu.canvas.exception.ObjectNotFoundException;
import edu.ksu.canvas.exception.UnauthorizedException;
import edu.ksu.canvas.interfaces.CalendarReader;
import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import edu.ksu.canvas.requestOptions.DeleteCalendarEventOptions;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.ac.ox.it.calendarimporter.CalendarUrlConfiguration;
import uk.ac.ox.it.calendarimporter.URLMapper;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEvent;
import uk.ac.ox.it.calendarimporter.persistence.repo.ImportedEventRepository;
import uk.ac.ox.it.calendarimporter.service.ImportEventService;
import uk.ac.ox.it.calendarimporter.utils.HiddenData;

import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

/**
 * This job supports re-importing events into a calendar on a regular basis.
 */
@PersistJobDataAfterExecution
public class CSVReimportJob extends CanvasCalendarJob {

    private static final String HIDDEN_DATA_PREFIX = "csv-reimport:";

    private final Logger log = LoggerFactory.getLogger(CSVReimportJob.class);

    @Autowired
    private ImportEventService importEventService;

    @Autowired
    private ImportedEventRepository importedEventRepository;

    @Autowired
    private CSVReader reader;
    
    @Autowired
    private CalendarUrlConfiguration calendarUrlConfiguration;
    
    @Value("${calendar.reimport.max.events}")
    private int maxEventsInCsv;
    
    @Override
    public void run() throws IOException, JobExecutionException {
        // Reset the progress.
        reset(parameters.get("triggerId"));

        CalendarWriter calendarWriter = canvasApiFactory.getWriter(CalendarWriter.class, oauthToken);
        CalendarReader calendarReader = canvasApiFactory.getReader(CalendarReader.class, oauthToken);

        TimeZone timeZone = TimeZone.getTimeZone(this.timeZone);

        // Just a short code that should be unique to group together imports.
        // We don't want to use the triggerID as it's semi secret, only need a few characters so they don't clash
        String hiddenData = HiddenData.toHidden(HIDDEN_DATA_PREFIX + id);
        log("Import started, timezone of: " + timeZone.getID());
        log.debug("Attempting to load CSV file: {}", url);
        log("Reading in file.");
        CSVReimportJob.TrackingErrorHandler errorHandler = new CSVReimportJob.TrackingErrorHandler();
        List<CalendarEvent> importingEvents;
        URLMapper urlMapper = new URLMapper(calendarUrlConfiguration.getPredefined(), ()->parameters);
        String openedUrl = null;
        try {
            // We do the mapping in the job so that if anything needs to be updated all the existing jobs can 
            // continue to work.
            URLConnection connection = urlMapper.open(url);
            openedUrl = connection.getURL().toExternalForm();
            log("Open connection to: %s", openedUrl);
            importingEvents = reader.parseCSV(connection.getInputStream(), timeZone, errorHandler);
        } catch (HeaderException he) {
            failure("Failed to read file: " + he.getLocalizedMessage());
            return;
        }
        log.trace("Parsed {} rows.", importingEvents.size());
        log("Source file read.");
        // Unlike other jobs we still process the file when it's empty.
        // We limit the number of events we process so the jobs can't run for a long period of time.
        if (importingEvents.size() > maxEventsInCsv) {
            failure("More than "+ maxEventsInCsv+ " events in file, not processing.");
            return;
        }

        List<ImportedEvent> existingEvents = importedEventRepository.findByCalendarImportAndStatusIn(calendarImport, ImportedEvent.Status.CREATED);
        log.trace("Found {} existing rows for import", existingEvents.size());
        
        Progress progress = new Progress(importingEvents.size()+existingEvents.size());
        log("Looking up events in Canvas.");
        List<CalendarEvent> canvasEvents = new ArrayList<>();
        long lookedUp = 0;
        for(ImportedEvent event : existingEvents) {
            if (isInterrupted()) {
                throw new JobExecutionException("Job interrupted");
            }
            try {
                progress.increment();
                lookedUp++;
                Optional<CalendarEvent> calendarEventOpt = calendarReader.getCalendarEvent(event.getId());
                if (calendarEventOpt.isPresent()) {
                    canvasEvents.add(calendarEventOpt.get());
                    log("Looked up event %d of %d", lookedUp, existingEvents.size());
                }

            } catch (UnauthorizedException | ObjectNotFoundException e) {
                log("Failed to find event {}: {}", event.getId(), e.getMessage());
                event.setStatus(ImportedEvent.Status.MISSING);
                importedEventRepository.save(event);
            }
        }
        
        Set<CalendarSummary> canvasSummaries = new HashSet<>();
        for(CalendarEvent event: canvasEvents) {
            CalendarSummary calendarSummary = toCalendarSummary(event);
            canvasSummaries.add(calendarSummary);
        }
        
        long created = 0;
        log("Creating events in Canvas.");
        for (CalendarEvent event : importingEvents) {
            if (isInterrupted()) {
                throw new JobExecutionException("Job interrupted");
            }
            event.setContextCode(context);
            if (canvasSummaries.remove(toCalendarSummary(event))) {
                log.debug("Not inserting event that already exists: "+ event.getTitle());
                continue;
            }
            progress.increment();
            // Flag that this was created by an import
            String description = HiddenData.insertHidden(event.getDescription(), hiddenData);
            event.setDescription(description);
            Optional<CalendarEvent> calendarEventOpt = calendarWriter.createCalendarEvent(event);
            if (calendarEventOpt.isPresent()) {
                created++;
                log(progress, "Created event %d of %d", created, importingEvents.size());
                importEventService.eventCreated(tenant.getId(), calendarImport, calendarEventOpt.get());
            }
        }
        long deleted = 0;
        for (CalendarEvent event : canvasEvents) {
            if (canvasSummaries.contains(toCalendarSummary(event))) {
                if (isInterrupted()) {
                    throw new JobExecutionException("Job interrupted");
                }
                progress.increment();
                // We didn't find this in the import so we now remove it.
                try {
                    calendarWriter.deleteCalendarEvent(new DeleteCalendarEventOptions(event.getId()));
                    deleted++;
                    importEventService.eventDeleted(tenant.getId(), event);
                    log("Deleted event id: %d as it's no longer in source.", event.getId());
                } catch (UnauthorizedException | ObjectNotFoundException e) {
                    importEventService.eventMissing(tenant.getId(), event);
                }
            }
        }
        
        
        
        if (errorHandler.hasProblems()) {
            log(
                    "Completed import, %d events in source, created %d events, deleted %d events.",
                    importingEvents.size(), created, deleted, errorHandler.problems);

        } else {
            log(
                    "Completed import, %d events in source, created %d events, deleted %d events.",
                    importingEvents.size(), created, deleted);
        }
        log.info(
                "Completed import from {}, {} events in source, created {} events, deleted {} events, for {} in tenant {}.",
                openedUrl,
                importingEvents.size(), created, deleted,
                context,
                tenant);
    }

    private CalendarSummary toCalendarSummary(CalendarEvent event) {
        return new CalendarSummary(
                event.getTitle(),
                // We don't want to include the hidden data.
                HiddenData.removeHidden(event.getDescription()),
                event.getLocationName(),
                event.getLocationAddress(),
                event.getStartAt(),
                event.getEndAt()
        );
    }

    private class TrackingErrorHandler implements CSVReader.ErrorHandler {
        private int problems = 0;

        @Override
        public void handleError(RowException e) {
            CSVReimportJob.this.problem(
                    "Error on row %d, message: %s", e.getRow() + 1, e.getLocalizedMessage());
            problems++;
        }

        public boolean hasProblems() {
            return problems > 0;
        }
    }
    
}
