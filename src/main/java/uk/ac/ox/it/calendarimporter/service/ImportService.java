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
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.jobs.DeleteJob;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.model.UserJob;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.ContextJobRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserJobRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static uk.ac.ox.it.calendarimporter.persistence.model.JobProgress.Status.QUEUED;
import static uk.ac.ox.it.calendarimporter.persistence.model.JobProgress.Status.RUNNING;

@Component
@Slf4j
public class ImportService {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ProgressService progressService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserJobRepository userJobRepository;

    @Autowired
    private CalendarImportRepository calendarImportRepository;

    @Autowired
    private ContextJobRepository contextJobRepository;

    @Autowired
    private TenantRepository tenantRepository;

    // TODO Handling of SchedulerException
    // Should we just pass in a User object?
    // Should url be an actual URL?
    public ImportJob importNow(ImportType type, String url, String filename, String token, Long userId, String context) throws SchedulerException {
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

        // TODO What exception?
        User user = userRepository.findById(userId).orElseThrow();

        Tenant tenant = user.getTenant();


        // Allow lookups from user to jobs.
        UserJob userJob = new UserJob(uuid.toString());
        userJob.setCreated(Instant.now());
        userJob.setUserId(userId);
        userJobRepository.save(userJob);


        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setContext(context);
        calendarImport.setCreated(Instant.now());
        calendarImport.setUser(user);
        calendarImport.setUrl(url);
        calendarImport.setFilename(filename);
        calendarImport.setType(type);
        calendarImport = calendarImportRepository.save(calendarImport);

        // For repeating jobs we want to lookup much more of this that way if the token/url gets updated
        // later on jobs that run will use the new URL/token
        Trigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withIdentity(TriggerUtils.toTriggerKey(uuid.toString(), tenant.getName(), user.getUsername()))
                .usingJobData(CanvasCalendarJob.URL, url)
                .usingJobData(CanvasCalendarJob.CONTEXT, context)
                .usingJobData(CanvasCalendarJob.TOKEN, token)
                .usingJobData(CanvasCalendarJob.CALENDAR_IMPORT_ID, calendarImport.getId())
                // This is in the trigger key but it's better to be explicit about this.
                .usingJobData(CanvasCalendarJob.TENANT_NAME, tenant.getName())
                .forJob(detail)
                .build();

        Date date = scheduler.scheduleJob(trigger);
        JobProgress jobProgress = progressService.updateJobCreated(uuid.toString());
        calendarImport.setLoad(jobProgress);
        calendarImportRepository.save(calendarImport);

        ContextJob contextJob = new ContextJob();
        contextJob.setCalendarImport(calendarImport);
        contextJob.setContext(context);
        contextJob.setCreated(Instant.now());
        contextJob.setTenant(tenant);
        contextJobRepository.save(contextJob);

        ImportJob job = new ImportJob();
        job.setStarted(date.toInstant());
        job.setProgressUrl("/api/v1/import/progress/" + uuid.toString());
        return job;
    }

    public void deleteImport(Long calendarImportId, String token, User user) throws SchedulerException {

        CalendarImport calendarImport = calendarImportRepository.findById(calendarImportId).orElseThrow(RuntimeException::new);
        JobProgress load = calendarImport.getLoad();
        if (QUEUED.equals(load.getStatus()) || RUNNING.equals(load.getStatus())) {
            throw new IllegalStateException("Cannot delete an import that is running or queued.");
        }

        JobDetail detail = scheduler.getJobDetail(JobKey.jobKey(DeleteJob.class.getName(), "delete"));
        if (detail == null) {
            detail = JobBuilder.newJob(DeleteJob.class)
                    .withIdentity(DeleteJob.class.getName(), "delete")
                    .storeDurably()
                    .build();
            scheduler.addJob(detail, true);
        }

        UUID uuid = UUID.randomUUID();

        Trigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withIdentity(TriggerUtils.toTriggerKey(uuid.toString(), user.getTenant().getName(), user.getUsername()))
                .usingJobData(CanvasCalendarJob.TENANT_NAME, user.getTenant().getName())
                .usingJobData(CanvasCalendarJob.TOKEN, token)
                .usingJobData(CanvasCalendarJob.CALENDAR_IMPORT_ID, calendarImportId)
                .forJob(detail)
                .build();

        Date date = scheduler.scheduleJob(trigger);
        JobProgress jobProgress = progressService.updateJobCreated(uuid.toString());
        calendarImport.setDelete(jobProgress);
        calendarImportRepository.save(calendarImport);
    }

    public Page<JobProgress> getJobs(User user, Pageable pageable) {
        Page<UserJob> userJobs = userJobRepository.findByUserIdOrderByCreatedDesc(user.getId(), pageable);
        //TODO This should change to a join or re-structure the objects
        return userJobs.map(job -> {
            Optional<JobProgress> byId = progressService.findById(job.getTriggerId());
            return byId.orElse(null);
        });
    }

    public Page<ContextJob> getJobs(Tenant tenant, String context, Pageable pageable) {
        Page<ContextJob> contextJobs = contextJobRepository.findByTenantAndContextOrderByCreatedDesc(tenant, context, pageable);
        return contextJobs;
    }
}
