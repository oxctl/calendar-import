package uk.ac.ox.it.calendarimporter.service;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uk.ac.ox.it.calendarimporter.TriggerUtils;
import uk.ac.ox.it.calendarimporter.beans.ImportJob;
import uk.ac.ox.it.calendarimporter.controller.ImportType;
import uk.ac.ox.it.calendarimporter.jobs.ical.IcalImportJob;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.model.UserJob;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserJobRepository;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class ImportService {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ProgressService progressService;

    @Autowired
    private UserJobRepository userJobRepository;

    // TODO Handling of SchedulerException
    // Should we just pass in a User object?
    // Should url be an actual URL?
    public ImportJob importNow(ImportType type, String url, String context, String token, String tenant, String username, Long userId) throws SchedulerException {
        // Job ID should come from config.
        JobDetail detail = scheduler.getJobDetail(JobKey.jobKey(type.name(), "import"));
        if (detail == null) {
            detail = JobBuilder.newJob(type.getJobClass())
                    .withIdentity(type.name(), "import")
                    .storeDurably()
                    .build();
            scheduler.addJob(detail, true);
        }
        // Non-guessable identity
        UUID uuid = UUID.randomUUID();

        // Allow lookups from user to jobs.
        UserJob userJob = new UserJob();
        userJob.setTriggerId(uuid.toString());
        userJob.setCreated(Instant.now());
        userJob.setUserId(userId);
        userJobRepository.save(userJob);


        // For repeating jobs we want to lookup much more of this that way if the token/url gets updated
        // later on jobs that run will use the new URL/token
        Trigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withIdentity(TriggerUtils.toTriggerKey(uuid.toString(), tenant, username))
                .usingJobData("url", url)
                .usingJobData("context", context)
                .usingJobData("token", token)
                // TODO This should come from user -> tenant
                .usingJobData("canvas_url", "https://oxeval.instructure.com")
                .forJob(detail)
                .build();
        Date date = scheduler.scheduleJob(trigger);
        progressService.updateJobCreated(uuid.toString());


        ImportJob job = new ImportJob();
        job.setStarted(date.toInstant());
        job.setProgressUrl("/api/v1/import/progress/" + uuid.toString());
        return job;
    }

    public Page<JobProgress> getJobs(User user, Pageable pageable) {
        Page<UserJob> userJobs = userJobRepository.findByUserIdOrderByCreatedDesc(user.getId(), pageable);
        return userJobs.map(job -> {
            Optional<JobProgress> byId = progressService.findById(job.getTriggerId());
            return byId.orElse(null);
        });
    }

}
