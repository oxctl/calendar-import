package uk.ac.ox.it.calendarimporter.service;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.KeyMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.controller.ImportType;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.jobs.CleanoutJob;
import uk.ac.ox.it.calendarimporter.jobs.DeleteJob;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.model.UserJob;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.ContextJobRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserJobRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.utils.TriggerUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static uk.ac.ox.it.calendarimporter.persistence.model.JobProgress.Status.PROCESSING;
import static uk.ac.ox.it.calendarimporter.persistence.model.JobProgress.Status.QUEUED;

@Service
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
    
    @Value("${calendar.reimport.interval}")
    private Duration reimportInterval;

    // TODO Handling of SchedulerException
    // Should we just pass in a User object?
    // Should url be an actual URL?

    public ContextJob importNow(ImportConfig importConfig) throws SchedulerException {
        // This method shouldn't be transactional as we want each repository call to be in it's own
        // transaction.
        // This is so that all the data is in the DB before we trigger the job, otherwise the job can
        // end up getting
        // run before the data is setup.

        // Job ID should come from config.
        JobDetail detail =
                scheduler.getJobDetail(JobKey.jobKey(importConfig.getType().name(), "import"));
        if (detail == null) {
            detail =
                    JobBuilder.newJob(importConfig.getType().getJobClass())
                            .withIdentity(importConfig.getType().name(), "import")
                            .storeDurably()
                            .requestRecovery()
                            .build();
            scheduler.addJob(detail, true);
        }
        // Non-guessable identity
        UUID uuid = UUID.randomUUID();

        // TODO What exception?
        User user = importConfig.getUser();

        Tenant tenant = user.getTenant();

        // Allow lookups from user to jobs.
        UserJob userJob = new UserJob(uuid.toString());
        userJob.setCreated(Instant.now());
        userJob.setUserId(importConfig.getUser().getId());
        userJobRepository.save(userJob);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setContext(importConfig.getContext());
        calendarImport.setCreated(Instant.now());
        calendarImport.setUser(user);
        calendarImport.setUrl(importConfig.getUrl());
        calendarImport.setFilename(importConfig.getFilename());
        calendarImport.setType(importConfig.getType());
        if (importConfig.getInto() != null) {
            calendarImport.setDestinationId(importConfig.getInto().getSectionId());
            calendarImport.setDestinationName(importConfig.getInto().getName());
        }
        calendarImport = calendarImportRepository.save(calendarImport);

        // For repeating jobs we want to lookup much more of this that way if the token/url gets updated
        // later on jobs that run will use the new URL/token
        String section = (importConfig.getInto() != null) ? importConfig.getInto().getSectionId() : null;
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                        .startNow()
                        .withIdentity(
                                TriggerUtils.toTriggerKey(uuid.toString(), tenant.getName(), user.getSubject()))
                        .usingJobData(CanvasCalendarJob.SOURCE_URL, importConfig.getUrl())
                        .usingJobData(CanvasCalendarJob.CONTEXT, importConfig.getContext())
                        .usingJobData(CanvasCalendarJob.SECTION, section)
                        .usingJobData(CanvasCalendarJob.CALENDAR_IMPORT_ID, calendarImport.getId())
                        // This is in the trigger key but it's better to be explicit about this.
                        .usingJobData(CanvasCalendarJob.TENANT_NAME, tenant.getName())
                        .usingJobData(CanvasCalendarJob.SUBJECT, user.getSubject())
                        .usingJobData(CanvasCalendarJob.TIME_ZONE, importConfig.getTimeZone().getID())
                        .forJob(detail);
        
        if (importConfig.getType().isRepeats()) {
            triggerBuilder.withSchedule(SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInMilliseconds(reimportInterval.toMillis())
                    .repeatForever()
            );
        }
        if (importConfig.getParameters() != null) {
            // This is done because it's advised to only use simple types in the 
            // quartz job data map. This is to avoid serialisation issues with upgrades.
            for (Map.Entry<String,String> entry: importConfig.getParameters().entrySet()) {
                triggerBuilder.usingJobData(CanvasCalendarJob.PARAM_PREFIX + entry.getKey(), entry.getValue());
            }
        }

        ContextJob contextJob = new ContextJob();
        contextJob.setCalendarImport(calendarImport);
        contextJob.setContext(importConfig.getContext());
        contextJob.setCreated(Instant.now());
        contextJob.setTenant(tenant);
        contextJobRepository.save(contextJob);

        JobProgress jobProgress = progressService.updateJobCreated(uuid.toString());
        calendarImport.setLoad(jobProgress);
        calendarImportRepository.save(calendarImport);

        Date date = scheduler.scheduleJob(triggerBuilder.build());
        return contextJob;
    }
    
    

    /**
     * @throws SchedulerException    If we failed to schedule the job.
     * @throws IllegalStateException If the import is in a state that it can't be deleted.
     */
    public void deleteImport(Long calendarImportId, User user)
            throws SchedulerException {

        CalendarImport calendarImport =
                calendarImportRepository.findById(calendarImportId).orElseThrow(RuntimeException::new);

        JobProgress load = calendarImport.getLoad();
        if (QUEUED.equals(load.getStatus()) || PROCESSING.equals(load.getStatus())) {
            throw new IllegalStateException("Cannot delete an import that is running or queued.");
        }

        JobDetail detail = scheduler.getJobDetail(JobKey.jobKey(DeleteJob.class.getName(), "delete"));
        if (detail == null) {
            detail =
                    JobBuilder.newJob(DeleteJob.class)
                            .withIdentity(DeleteJob.class.getName(), "delete")
                            .storeDurably()
                            .build();
            scheduler.addJob(detail, true);
        }

        UUID uuid = UUID.randomUUID();

        Trigger trigger =
                TriggerBuilder.newTrigger()
                        .startNow()
                        .withIdentity(
                                TriggerUtils.toTriggerKey(
                                        uuid.toString(), user.getTenant().getName(), user.getSubject()))
                        .usingJobData(CanvasCalendarJob.TENANT_NAME, user.getTenant().getName())
                        .usingJobData(CanvasCalendarJob.SUBJECT, user.getSubject())
                        .usingJobData(CanvasCalendarJob.CALENDAR_IMPORT_ID, calendarImportId)
                        .forJob(detail)
                        .build();

        Date date = scheduler.scheduleJob(trigger);
        JobProgress jobProgress = progressService.updateJobCreated(uuid.toString());
        calendarImport.setDelete(jobProgress);
        calendarImportRepository.save(calendarImport);
    }

    public void purgeImports(String context, String tenantName, String subject, boolean all)
            throws SchedulerException {

        JobDetail job = JobBuilder.newJob(CleanoutJob.class).build();
        Trigger trigger =
                TriggerBuilder.newTrigger()
                        .startNow()
                        .usingJobData(CanvasCalendarJob.CONTEXT, context)
                        .usingJobData(CanvasCalendarJob.TENANT_NAME, tenantName)
                        .usingJobData(CanvasCalendarJob.SUBJECT, subject)
                        .usingJobData(CleanoutJob.ALL, all)
                        .forJob(job)
                        .build();

        scheduler.scheduleJob(job, trigger);
    }

    public Page<ContextJob> getJobs(String tenantName, String context, Pageable pageable) {
        Page<ContextJob> contextJobs =
                contextJobRepository.findByTenantNameAndContextAndHiddenOrderByCreatedDesc(
                        tenantName, context, false, pageable);
        return contextJobs;
    }

    /**
     * This loads a context job and checks that it's in the expected tenant and context.
     * @param tenantName The name of the current tenant.
     * @param context The context it's being access from.
     * @param id The ID of the context job.
     * @return The context job.
     */
    public Optional<ContextJob> getJob(String tenantName, String context, Long id) {
        return contextJobRepository.findById(id) .filter(contextJob ->
                contextJob.getTenant().getName().equals(tenantName)  &&
                contextJob.getContext().equals(context)
        );
    }

    /**
     * @param contextJob
     */
    public void hideImport(ContextJob contextJob) {
        contextJob.setHidden(true);
        contextJobRepository.save(contextJob);
    }
}
