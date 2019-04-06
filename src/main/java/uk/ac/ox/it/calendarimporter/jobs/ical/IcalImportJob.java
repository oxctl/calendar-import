package uk.ac.ox.it.calendarimporter.jobs.ical;

import static net.fortuna.ical4j.model.Component.VEVENT;

import edu.ksu.canvas.interfaces.CalendarReader;
import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import edu.ksu.canvas.requestOptions.ListCalendarEventsOptions;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.validate.ValidationException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.utils.HiddenData;

// There's no place to store a UUID in Canvas which is required on a iCal event.
// Just have a simple map that takes a canvas ID and iCal UUID
// How far in advance to calculate re-occurance rules?
// Limits on event numbers?
// Different sync algorithms?

// Errors when validating aren't tied to line numbers in the source document.

// The calendar API returns date/times for all day events

// TODO Switch to interruptable job.

public class IcalImportJob extends CanvasCalendarJob {

  public static final int PAGINATION_PAGE_SIZE = 100;
  private static Logger logger = LoggerFactory.getLogger(IcalImportJob.class);
  private String file = "data.properties";

  // Number of days either side of today to sync.
  private int dayRange = 10;
  private int iCalEventLimit = 1000;
  private long inputLimit = 1048576 * 10;

  public IcalImportJob() {}

  public IcalImportJob(String url) {
    this.url = url;
  }

  public void setDayRange(int dayRange) {
    this.dayRange = dayRange;
  }

  public void setiCalEventLimit(int iCalEventLimit) {
    this.iCalEventLimit = iCalEventLimit;
  }

  public void setInputLimit(long inputLimit) {
    this.inputLimit = inputLimit;
  }

  public void run() throws IOException {

    URL url = new URL(this.url);

    CalendarReader reader =
        canvasApiFactory.getReader(CalendarReader.class, oauthToken, PAGINATION_PAGE_SIZE);
    CalendarWriter writer = canvasApiFactory.getWriter(CalendarWriter.class, oauthToken);

    ListCalendarEventsOptions listCalendarEventsOptions = new ListCalendarEventsOptions();
    listCalendarEventsOptions.contextCodes(Collections.singletonList(context));
    listCalendarEventsOptions.includeAllEvents(true);

    List<CalendarEvent> calendarEvents =
        reader.listCurrentUserCalendarEvents(listCalendarEventsOptions);
    Map<Integer, CalendarEvent> canvasEvents = new HashMap<>();
    for (CalendarEvent event : calendarEvents) {
      canvasEvents.put(event.getId(), event);
    }

    // If we expand events then there may be multiple events
    Map<String, Set<CalendarEvent>> canvasEventsByUuid = new HashMap<>();
    // Look for iCal UUIDs
    for (CalendarEvent event : calendarEvents) {
      String uuid = HiddenData.fromHidden(HiddenData.extractHidden(event.getDescription()));
      if (uuid != null) {
        if (!canvasEventsByUuid.containsKey(uuid)) {
          canvasEventsByUuid.put(uuid, new HashSet<>());
        }
        canvasEventsByUuid.get(uuid).add(event);
      }
    }

    CalendarIDLookupFile idLookup = new CalendarIDLookupFile(file);
    idLookup.load();

    int seen = 0, newEvents = 0, updatedEvents = 0;

    // TODO timeout on the URL request
    CalendarBuilder builder = new CalendarBuilder();
    try (InputStream in = new TerminatingInputStream(url.openStream(), inputLimit)) {
      Calendar calendar = builder.build(in);
      ComponentList<VEvent> events = calendar.getComponents(VEVENT);
      for (VEvent event : events) {
        seen++;
        if (seen > iCalEventLimit) {
          logger.error("Stopping processing after " + seen + " iCal events");
          break;
        }
        Uid uid = event.getUid();
        if (uid != null) {
          // Do we have it in our mapping table?
          // Set<Integer> canvasIDs = idLookup.getCanvasIDs(uid.getValue());
          Set<CalendarEvent> canvasEvenets = canvasEventsByUuid.get(uid.getValue());
          Set<Integer> canvasIDs =
              (canvasEvenets != null)
                  ? canvasEvenets.stream().map(CalendarEvent::getId).collect(Collectors.toSet())
                  : null;
          if (canvasIDs != null) {
            for (Integer id : canvasIDs) {
              CalendarEvent calendarEvent = canvasEvents.get(id);
              if (calendarEvent == null) {
                calendarEvent = new CalendarEvent();
                calendarEvent.setContextCode(context);
                newEvents++;
              }
              int oldHash = calendarEvent.hashCode();
              // Because Canvas doesn't fully persist our data some events always look to change.
              // We should improve the update so that it takes into account the Canvas crap
              update(event, calendarEvent);
              if (oldHash != calendarEvent.hashCode()) {
                Optional<CalendarEvent> returned;
                if (calendarEvent.getId() != null) {
                  returned = writer.editCalendarEvent(calendarEvent);
                } else {
                  returned = writer.createCalendarEvent(calendarEvent);
                }
                if (returned.isPresent()) {
                  updatedEvents++;
                }
                returned.ifPresent(
                    calendarEvent1 ->
                        idLookup.set(
                            uid.getValue(), Collections.singleton(calendarEvent1.getId())));
              }
            }
          } else {
            CalendarEvent calendarEvent = new CalendarEvent();
            calendarEvent.setContextCode(context);
            update(event, calendarEvent);
            Optional<CalendarEvent> returned = writer.createCalendarEvent(calendarEvent);
            if (returned.isPresent()) {
              newEvents++;
            }
            returned.ifPresent(
                calendarEvent1 ->
                    idLookup.set(uid.getValue(), Collections.singleton(calendarEvent1.getId())));
          }
        } else {
          logger.warn("Skipped event as it doesn't have a UUID.");
        }
      }
    } catch (ValidationException ve) {
      // TODO Handle invalid iCal.
      throw ve;
    } catch (TerminatedIOException tioe) {
      // TODO Formatting size
      logger.warn("Can only read files of up to " + inputLimit + " bytes.");
    } catch (ParserException e) {
      // TODO
    }
    // Summary of run.
    logger.info(
        "Events in source: "
            + calendarEvents.size()
            + " seen: "
            + seen
            + " new: "
            + newEvents
            + " updated: "
            + updatedEvents);

    idLookup.save();
  }

  /**
   * Updated a Canvas event from an iCal event.
   *
   * @param vevent The iCal event.
   * @param event The Canvas event.
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
      int offset = dateTime.getTimeZone().getOffset(dateTime.getTime());
      Instant utcInstant = dateTime.toInstant().plus(offset, ChronoUnit.MILLIS);
      instant = utcInstant;
    } else {
      // iCal4j zeros everything after the days when it's a date.
      instant = date.toInstant();
    }
    return instant;
  }
}
