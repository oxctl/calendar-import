package uk.ac.ox.it.calendarimporter.jobs.ical;

import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.validate.ValidationException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.persistence.repo.ImportedEventRepository;
import uk.ac.ox.it.calendarimporter.service.ImportEventService;
import uk.ac.ox.it.calendarimporter.utils.HiddenData;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static net.fortuna.ical4j.model.Component.VEVENT;

public class IcalImportJob extends CanvasCalendarJob {

    private static final String HIDDEN_DATA_PREFIX = "ical-import:";
    private static final Logger logger = LoggerFactory.getLogger(IcalImportJob.class);
    // Number of days either side of today to sync.
    private int iCalEventLimit = 1000;
    private long inputLimit = 1048576 * 10;

    @Autowired
    private ImportEventService importEventService;

    public IcalImportJob() {
    }

    public IcalImportJob(String url) {
        this.path = url;
    }

    public void setiCalEventLimit(int iCalEventLimit) {
        this.iCalEventLimit = iCalEventLimit;
    }

    public void setInputLimit(long inputLimit) {
        this.inputLimit = inputLimit;
    }

    private CalendarWriter writer;

    public void run() throws IOException, JobExecutionException {

        if (writer==null){
            writer = canvasApiFactory.getWriter(CalendarWriter.class, oauthToken);
        }

        AtomicInteger created = new AtomicInteger(0);

        log("Import started.");

        CalendarBuilder builder = new CalendarBuilder();

        Progress progress;
        try (InputStream in = new TerminatingInputStream(depositService.getInputStream(this.path), inputLimit)) {
            log("Reading in file.");
            Calendar calendar = builder.build(in);
            ComponentList<VEvent> events = calendar.getComponents(VEVENT);
            if (events.isEmpty()) {
                problem("No events found in file");
                return;
            }
            progress = new Progress(events.size());
            log("Creating events in Canvas.");
            for (VEvent event : events) {
                if (isInterrupted()) {
                    log("Interrupted after %d of %d events", progress.getSeen(), progress.getTotal());
                    throw new JobExecutionException("Job interrupted");
                }
                progress.increment();
                if (progress.getSeen() > iCalEventLimit) {
                    logger.error("Stopping processing after " + progress.getSeen() + " iCal events");
                    break;
                }
                CalendarEvent calendarEvent = new CalendarEvent();
                calendarEvent.setContextCode(context);
                update(event, calendarEvent);
                if (section != null) {
                    toSection(calendarEvent, section);
                }
                // TODO Look for re-occurance rules.
                writer
                        .createCalendarEvent(calendarEvent)
                        .ifPresent(
                                (createdEvent) -> {
                                    created.incrementAndGet();
                                    log(progress, "Created event %d of %d", progress.getSeen(), progress.getTotal());
                                    importEventService.eventCreated(tenant.getId(), calendarImport, createdEvent);
                                });
            }
            log(
                    "Completed import, found %d events, imported %d events into calendar.",
                    progress.getTotal(), created.get());
        } catch (ValidationException ve) {
            failure("File is not valid iCal. " + ve.getLocalizedMessage());
        } catch (TerminatedIOException tioe) {
            failure("File it too large. It can only be up to %d bytes", inputLimit);
        } catch (ParserException e) {
            failure("Failed to read file. " + e.getLocalizedMessage());
        }
    }

    /**
     * Updated a Canvas event from an iCal event.
     *
     * @param vevent The iCal event.
     * @param event  The Canvas event.
     */
    public void update(VEvent vevent, CalendarEvent event) {
        Summary summary = vevent.getSummary();
        if (summary != null) {
            event.setTitle(summary.getValue());
        }
        Description description = vevent.getDescription();
        if (description != null) {
            // Canvas description is HTML
            String escapedDescription = StringEscapeUtils.escapeHtml4(description.getValue());
            escapedDescription =
                    HiddenData.insertHidden(
                            escapedDescription, HiddenData.toHidden(vevent.getUid().getValue()));
            event.setDescription(escapedDescription);
        }
        DtStart startDate = vevent.getStartDate();
        // This is a required field so should we blow up when it doesn't?
        if (startDate != null) {
            // Can be date or date-time
            Date date = startDate.getDate();
            Instant instant = toInstant(date);
            event.setStartAt(instant);
            // Just use the
            boolean allDay = !(date instanceof DateTime);
            event.setAllDay(allDay);
        }
        // Not required.
        DtEnd endDate = vevent.getEndDate();
        if (endDate != null) {
            Date date = endDate.getDate();
            Instant instant = toInstant(date);
            event.setEndAt(instant);
        }

        Location location = vevent.getLocation();
        if (location != null) {
            event.setLocationName(location.getValue());
        }
    }

    /**
     * This converts a date to an instant. The instant will be in UTC.
     *
     * @param date The date to convert.
     * @return The instant in UTC.
     */
    private Instant toInstant(Date date) {
        Instant instant;
        if (date instanceof DateTime) {
            // Need to convert to UTC, don't want to adjust the existing value.
            DateTime dateTime = (DateTime) date;
            TimeZone timeZone = dateTime.getTimeZone();
            if (timeZone != null) {
                int offset = timeZone.getOffset(dateTime.getTime());
                instant = dateTime.toInstant().plus(offset, ChronoUnit.MILLIS);
            } else {
                // Some don't have a timezone
                instant = date.toInstant();
            }
        } else {
            // iCal4j zeros everything after the days when it's a date.
            instant = date.toInstant();
        }
        return instant;
    }

    /**
     * This moves the data into a calendar event section.
     *
     * @param event   The original event.
     * @param section The section to import into.
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

    public void setImportEventService(ImportEventService importEventService) {
        this.importEventService = importEventService;
    }

    public void setCalendarWriter(CalendarWriter calendarWriter) {
        this.writer = calendarWriter;
    }
}
