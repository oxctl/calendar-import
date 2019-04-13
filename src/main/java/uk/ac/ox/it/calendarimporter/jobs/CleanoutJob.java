package uk.ac.ox.it.calendarimporter.jobs;

import static edu.ksu.canvas.requestOptions.ListCalendarEventsOptions.Exclude.*;
import static uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob.*;

import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.exception.UnauthorizedException;
import edu.ksu.canvas.interfaces.CalendarReader;
import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.DeleteCalendarEventOptions;
import edu.ksu.canvas.requestOptions.ListCalendarEventsOptions;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.UserTokens;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserTokensRepository;
import uk.ac.ox.it.calendarimporter.service.OauthTokenFactory;
import uk.ac.ox.it.calendarimporter.utils.HiddenData;

/**
 * This will remove all events from a calendar that were created by any import. This is used so that
 * if we have a problem with a delete job not removing some events we can just flush out all events
 * created by the importer and then re-import the actual files we want.
 */
public class CleanoutJob implements Job {

  /**
   * If true in the job map then we remove everything in the calendar and not just those that were
   * imported.
   */
  public static final String ALL = "all";

  private Logger log = LoggerFactory.getLogger(CleanoutJob.class);

  @Autowired private TenantRepository tenantRepository;
  @Autowired private OauthTokenFactory oauthTokenFactory;
  @Autowired private UserTokensRepository userTokensRepository;

  public void execute(JobExecutionContext jobContext) throws JobExecutionException {
    JobDataMap config = jobContext.getMergedJobDataMap();
    Tenant tenant =
        tenantRepository
            .findByName(config.getString(TENANT_NAME))
            .orElseThrow(JobExecutionException::new);
    String context = config.getString(CONTEXT);
    log.info("Cleaning out all events in {} of {}", context, tenant);

    String tenantUser = config.getString(TENANT_NAME) + ":" + config.getString(USERNAME);
    UserTokens userTokens =
        userTokensRepository
            .findById(tenantUser)
            .orElseThrow(
                () -> new JobExecutionException("Couldn't find tokens for: " + tenantUser));
    OauthToken oauthToken =
        oauthTokenFactory.getToken(
            tenant,
            tenantUser,
            userTokens.getAccessToken().getTokenValue(),
            userTokens.getRefreshToken().getTokenValue());
    CanvasApiFactory canvasApiFactory = new CanvasApiFactory(tenant.getUrl());

    CalendarReader calendarReader = canvasApiFactory.getReader(CalendarReader.class, oauthToken);
    CalendarWriter calendarWriter = canvasApiFactory.getWriter(CalendarWriter.class, oauthToken);

    ListCalendarEventsOptions options = new ListCalendarEventsOptions();
    options.contextCodes(Collections.singletonList(context));
    options.includeAllEvents(true);
    // This just excludes attributes we don't need.
    options.excludes(Arrays.asList(CHILD_EVENTS, ASSIGNMENT));
    boolean all = config.getBoolean(ALL);
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
