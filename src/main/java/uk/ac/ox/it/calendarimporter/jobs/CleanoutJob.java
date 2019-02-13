package uk.ac.ox.it.calendarimporter.jobs;

import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.interfaces.CalendarReader;
import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import edu.ksu.canvas.requestOptions.DeleteCalendarEventOptions;
import edu.ksu.canvas.requestOptions.ListCalendarEventsOptions;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob.*;

/**
 * This will remove all events from a calendar. This is an admin job that isn't designed to be exposed to user.
 * TODO This should support only removing events created by our importer.
 */
public class CleanoutJob implements Job {

    private Logger log = LoggerFactory.getLogger(CleanoutJob.class);

    @Autowired
    private TenantRepository tenantRepository;

    public void execute(JobExecutionContext jobContext) throws JobExecutionException {
        JobDataMap config = jobContext.getMergedJobDataMap();
        Tenant tenant = tenantRepository.findByName(config.getString(TENANT_NAME))
               .orElseThrow(JobExecutionException::new);
        String token = config.getString(TOKEN);
        String context = config.getString(CONTEXT);
        log.debug("Cleaning out all events in {} of {}", context, tenant);
        CanvasApiFactory canvasApiFactory = new CanvasApiFactory(tenant.getUrl());
        NonRefreshableOauthToken nonRefreshableOauthToken = new NonRefreshableOauthToken(token);

        CalendarReader calendarReader = canvasApiFactory.getReader(CalendarReader.class, nonRefreshableOauthToken);
        CalendarWriter calendarWriter = canvasApiFactory.getWriter(CalendarWriter.class, nonRefreshableOauthToken);

        ListCalendarEventsOptions options = new ListCalendarEventsOptions();
        options.contextCodes(Collections.singletonList(context));
        options.includeAllEvents(true);
        try {
            List<CalendarEvent> calendarEvent = calendarReader.listCurrentUserCalendarEvents(options);
            for (CalendarEvent event : calendarEvent) {
                log.debug("Attempting to remove event ID {} from calendar {} of {}", event.getId(), context, tenant);
                calendarWriter.deleteCalendarEvent(new DeleteCalendarEventOptions(event.getId()));
            }
            log.info("Removed {} events from calendar {} of {}", calendarEvent.size(), context, tenant);
        } catch (IOException e) {
            throw new JobExecutionException(e);
        }
    }
}
