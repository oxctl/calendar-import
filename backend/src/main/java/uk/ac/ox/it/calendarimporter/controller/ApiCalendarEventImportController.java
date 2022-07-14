package uk.ac.ox.it.calendarimporter.controller;

import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.service.ImportConfig;
import uk.ac.ox.it.calendarimporter.service.ImportService;
import uk.ac.ox.it.calendarimporter.service.UserService;
import uk.ac.ox.it.calendarimporter.utils.TriggerUtils;

import java.util.Map;
import java.util.TimeZone;

import static uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob.CALENDAR_IMPORT_ID;
import static uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob.SOURCE_URL;

/**
 * This API subscribes or unsubscribes a user from the calendar course event sync job.
 */
@RestController
@RequestMapping("/api")
public class ApiCalendarEventImportController {

    private final Logger log = LoggerFactory.getLogger(ApiCalendarEventImportController.class);

    @Autowired
    private ImportService importService;

    @Autowired
    private UserService userService;

    @Autowired
    private Scheduler scheduler;

    @PostMapping("/subscribe")
    public ResponseEntity<ContextJob> subscribe(
            Tenant tenant,
            JwtAuthenticationToken authentication,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_user_id']")
                    Number userId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_course_id']")
                    Number courseId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_user_sis_id']")
                    Number userSisId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['url']")
                    String url)
            throws SchedulerException {
        if (!isUserSubscribed(tenant, authentication, userId, url)){
            log.debug("User not currently subscribed so creating calendar course events sync job for user {} using calendar url {}", userId, url);
            User user = userService.getUser(authentication, tenant);

            ContextJob contextJob =
                    importService.importNow(
                            new ImportConfig(
                                    ImportType.CSV_REIMPORT,
                                    url,
                                    null,
                                    user,
                                    "user_" + userId,
                                    null,
                                    TimeZone.getDefault(),
                                    Map.of("course.id", courseId.toString(), "user.sis_id", userSisId.toString())));
            return ResponseEntity.ok(contextJob);
        }
        else {
            log.debug("User {} is currently subscribed to calendar url {} so not creating another calendar course events sync job ", userId, url);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(
            @RequestParam(name = "all", required = false, defaultValue = "false") boolean all,
            Tenant tenant,
            JwtAuthenticationToken authentication,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_user_id']")
                    Number userId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['url']")
                    String url)
            throws SchedulerException {

        if (isUserSubscribed(tenant, authentication, userId, url)){
            log.debug("User {} is currently subscribed to url {} so now deleting calendar course events sync job for user {} and calendar url {}", userId, url);

            User user = userService.getUser(authentication, tenant);
            String groupName = TriggerUtils.getTriggerGroup(tenant.getName(), user.getSubject());
            for (TriggerKey key : scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals( groupName))) {
                Trigger trigger = scheduler.getTrigger(key);
                JobDataMap config = trigger.getJobDataMap();
                long calendarImportId = config.getLongValue(CALENDAR_IMPORT_ID);
                scheduler.unscheduleJob(key);
                importService.deleteImport(calendarImportId, user);
            }
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
        else {
            log.debug("User {} not is currently subscribed to calendar url {} so not trying to delete a calendar course events sync job ", userId, url);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/isUserSubscribed")
    public boolean isUserSubscribed(
            Tenant tenant,
            JwtAuthenticationToken authentication,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_user_id']")
                    Number userId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['url']")
                    String url)
            throws SchedulerException {

        log.debug("Checking if user is subscribed to calendar course events sync for user {} using calendar url {}", userId, url);

        User user = userService.getUser(authentication, tenant);
        String groupName = TriggerUtils.getTriggerGroup(tenant.getName(), user.getSubject());
        for (TriggerKey key : scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals( groupName))) {
            Trigger trigger = scheduler.getTrigger(key);
            JobDataMap config = trigger.getJobDataMap();
            String configURL = config.getString(SOURCE_URL);
            boolean isReimportJob = ImportType.CSV_REIMPORT.toString().equals(((SimpleTriggerImpl) trigger).getJobName());
            boolean isUserSubscribedToURL = url.equals(configURL);
            if (isReimportJob && isUserSubscribedToURL){
                return true;
            }
        }
        return false;
    }
}
