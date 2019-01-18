package uk.ac.ox.it.calendarimporter.jobs;

import edu.ksu.canvas.interfaces.CalendarReader;
import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import edu.ksu.canvas.requestOptions.DeleteCalendarEventOptions;
import edu.ksu.canvas.requestOptions.ListCalendarEventsOptions;
import org.quartz.JobExecutionException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class CleanoutJob extends CanvasCalendarJob {

    @Override
    public void run() throws IOException, JobExecutionException {
        CalendarReader calendarReader = canvasApiFactory.getReader(CalendarReader.class, nonRefreshableOauthToken);
        CalendarWriter calendarWriter = canvasApiFactory.getWriter(CalendarWriter.class, nonRefreshableOauthToken);

        ListCalendarEventsOptions options = new ListCalendarEventsOptions();
        options.contextCodes(Collections.singletonList(context));
        options.includeAllEvents(true);
        List<CalendarEvent> calendarEvent = calendarReader.listCurrentUserCalendarEvents(options);
        for(CalendarEvent event : calendarEvent) {
            calendarWriter.deleteCalendarEvent(new DeleteCalendarEventOptions(event.getId()));
        }

    }
}
