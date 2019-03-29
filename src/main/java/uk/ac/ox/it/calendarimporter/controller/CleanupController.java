package uk.ac.ox.it.calendarimporter.controller;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.jobs.CleanoutJob;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;

@RestController
@RequestMapping("/api/v1/cleanup")
public class CleanupController {

  @Autowired private UserRepository userRepository;

  @Autowired private Scheduler scheduler;

  @PostMapping
  public void cleanup(
      @RequestParam String tenant,
      @RequestParam String context,
      OAuth2AuthenticationToken authentication)
      throws SchedulerException {
    User user =
        userRepository
            .findByOAuth2AuthenticationToken(authentication)
            .orElseThrow(RuntimeException::new);

    String token = null; // TODO
    JobDetail job = JobBuilder.newJob(CleanoutJob.class).build();
    Trigger trigger =
        TriggerBuilder.newTrigger()
            .startNow()
            .usingJobData(CanvasCalendarJob.CONTEXT, context)
            .usingJobData(CanvasCalendarJob.ACCESS_TOKEN, token)
            .usingJobData(CanvasCalendarJob.TENANT_NAME, tenant)
            .forJob(job)
            .build();

    scheduler.scheduleJob(job, trigger);
  }
}
