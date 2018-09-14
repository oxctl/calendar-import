package uk.ac.ox.it.calendarimporter.controller;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ox.it.calendarimporter.beans.ImportJob;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.jobs.SleepyJob;
import uk.ac.ox.it.calendarimporter.service.ProgressService;

import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/import")
public class ImportController {

    public static final String NAME = "job-1";
    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ProgressService progressService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    // TODO Handling of SchedulerException
    public ImportJob create(@RequestParam("url") String url, @RequestParam("context") String context) throws SchedulerException {
        // Create job and return progress object.
        //JobDetail detail = JobBuilder.newJob(ImportCalendarJob.class)

        // Job ID should come from config.
        JobDetail detail = scheduler.getJobDetail(JobKey.jobKey(NAME, "import"));
        if (detail == null) {
            detail = JobBuilder.newJob(SleepyJob.class)
                    .withIdentity(NAME, "import")
                    .storeDurably()
                    .build();
            scheduler.addJob(detail, true);
        }
        // Non-guessable identity
        UUID uuid = UUID.randomUUID();

        Trigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withIdentity(uuid.toString(), "ui")
                .usingJobData("url", url)
                .usingJobData("context", context)
                .forJob(detail)
                .build();
        Date date = scheduler.scheduleJob(trigger);
        progressService.updateJobCreated(uuid.toString());

        ImportJob job = new ImportJob();
        job.setStarted(date.toInstant());
        job.setProgressUrl("/api/v1/import/progress/" + uuid.toString());
        return job;
    }



    @GetMapping("/progress/{id}")
    public JobProgress status(@PathVariable("id") String id) throws SchedulerException {
        return progressService.findById(id).orElseThrow(NotFoundException::new);
    }

}
