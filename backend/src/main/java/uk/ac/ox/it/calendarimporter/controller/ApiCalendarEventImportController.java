package uk.ac.ox.it.calendarimporter.controller;

import net.minidev.json.JSONObject;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ox.it.calendarimporter.service.JobSchedulingService;

/**
 * This API subscribes or unsubscribes a user from the calendar course event sync job.
 */
@RestController
@RequestMapping("/api")
public class ApiCalendarEventImportController {

    private final Logger log = LoggerFactory.getLogger(ApiCalendarEventImportController.class);

    @Autowired
    private JobSchedulingService jobSchedulingService;

    @PostMapping("/subscribe")
    public JSONObject subscribe(
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_user_id']")
                    Number userId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['url']")
                    String url)
            throws SchedulerException {
        log.debug("Subscribing user to calendar course events sync for user {} using calendar url {}", userId, url);
        return jobSchedulingService.subscribeUserToCalendarImport( userId, url);
    }

    @PostMapping("/unsubscribe")
    public JSONObject unsubscribe(
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_user_id']")
                    Number userId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['url']")
                    String url)
            throws SchedulerException {
        log.debug("Unsubscribing user to calendar course events sync  for user {} using calendar url {}", userId, url);
        return jobSchedulingService.unsubscribeUserFromCalendarImport((Integer) userId, url);
    }

    @GetMapping("/isUserSubscribed")
    public boolean isUserSubscribed(
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_user_id']")
                    Number userId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['url']")
                    String url)
            throws SchedulerException {
        log.debug("Checking if user is subscribed to calendar course events sync for user {} using calendar url {}", userId, url);
        return jobSchedulingService.isUserSubscribed(userId, url);
    }
}
