package uk.ac.ox.it.calendarimporter.jobs;

import com.nimbusds.jose.JOSEException;
import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.exception.UnauthorizedException;
import edu.ksu.canvas.interfaces.CalendarReader;
import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import edu.ksu.canvas.oauth.OauthToken;
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
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.service.CanvasTokenCreator;
import uk.ac.ox.it.calendarimporter.utils.HiddenData;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static edu.ksu.canvas.requestOptions.ListCalendarEventsOptions.Exclude.ASSIGNMENT;
import static edu.ksu.canvas.requestOptions.ListCalendarEventsOptions.Exclude.CHILD_EVENTS;
import static uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob.*;

/**
 * This will remove all events from a calendar that were created by any import. It does this by
 * looking for additional metadata hidden in event descriptions. This is used so that if we have a
 * problem with a delete job not removing some events we can just flush out all events created by
 * the importer and then re-import the actual files we want.
 */
public class CleanoutJob implements Job {

    /**
     * If true in the job map then we remove everything in the calendar and not just those that were
     * imported.
     */
    public static final String ALL = "all";

    private final Logger log = LoggerFactory.getLogger(CleanoutJob.class);

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private CanvasTokenCreator canvasTokenCreator;
    @Autowired
    private UserRepository userRepository;

    public void execute(JobExecutionContext jobContext) throws JobExecutionException {
        JobDataMap config = jobContext.getMergedJobDataMap();
        boolean all = config.getBoolean(ALL);
        Tenant tenant =
                tenantRepository
                        .findByName(config.getString(TENANT_NAME))
                        .orElseThrow(JobExecutionException::new);
        String context = config.getString(CONTEXT);
        log.info("Cleaning out all events in {} of {}, all: {}", context, tenant, all);

        User user =
                userRepository
                        .findBySubjectAndTenantName(config.getString(SUBJECT), tenant.getName())
                        .orElseThrow(
                                () ->
                                        new JobExecutionException("Failed to find user: " + config.getString(SUBJECT)));
        OauthToken oauthToken;
        try {
            oauthToken = canvasTokenCreator.getToken(tenant, user.getSubject());
        } catch (JOSEException e) {
            throw new JobExecutionException("Failed to create JWT.", e);
        }
        CanvasApiFactory canvasApiFactory = new CanvasApiFactory(tenant.getProxyHost());

        CalendarReader calendarReader = canvasApiFactory.getReader(CalendarReader.class, oauthToken);
        CalendarWriter calendarWriter = canvasApiFactory.getWriter(CalendarWriter.class, oauthToken);

        ListCalendarEventsOptions options = new ListCalendarEventsOptions();
        options.contextCodes(Collections.singletonList(context));
        options.includeAllEvents(true);
        // This just excludes attributes we don't need.
        options.excludes(Arrays.asList(CHILD_EVENTS, ASSIGNMENT));
        try {
            List<CalendarEvent> calendarEvents = calendarReader.listCurrentUserCalendarEvents(options);
            int removed = 0;
            for (CalendarEvent event : calendarEvents) {
                if (isImportedEvent(event) || all) {
                    log.debug(
                            "Attempting to remove event ID {} from calendar {} of {}",
                            event.getId(),
                            context,
                            tenant);
                    try {
                        if (!isChildEvent(event)) {
                            calendarWriter.deleteCalendarEvent(new DeleteCalendarEventOptions(event.getId()));
                            removed++;
                        } else {
                            log.debug(
                                    "Skipped removal of event ID {} from calendar {} of {}",
                                    event.getId(),
                                    context,
                                    tenant);
                        }
                    } catch (UnauthorizedException ue) {
                        log.warn(
                                "Failed to remove event ID {} from calendar {} of {}",
                                event.getId(),
                                context,
                                tenant);
                    }
                } else {
                    log.debug("Ignoring event ID {} from calendar {} of {}", event.getId(), context, tenant);
                }
            }
            log.info(
                    "Removed {} of {} events from calendar {} of {}",
                    removed,
                    calendarEvents.size(),
                    context,
                    tenant);
        } catch (IOException e) {
            throw new JobExecutionException(e);
        }
    }

    private boolean isImportedEvent(CalendarEvent event) {
        return HiddenData.extractHidden(event.getDescription()) != null;
    }

    private boolean isChildEvent(CalendarEvent event) {
        return event.getEffectiveContextCode() != null
                && !event.getEffectiveContextCode().equals(event.getContextCode());
    }
}
