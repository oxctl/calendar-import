package uk.ac.ox.it.calendarimporter.service;

import static org.quartz.impl.matchers.GroupMatcher.groupEquals;

import javax.annotation.PostConstruct;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.OrMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This watches for job start/stops and updates the basic job progress. This should actually be a
 * TriggerListener as that's more what it's interested in.
 */
@Component
public class JobProgressListener implements JobListener {

  @Autowired private Scheduler scheduler;

  @Autowired private ProgressService progressService;

  @PostConstruct
  public void init() throws SchedulerException {
    scheduler
        .getListenerManager()
        .addJobListener(this, OrMatcher.<JobKey>or(groupEquals("import"), groupEquals("delete")));
  }

  @Override
  public String getName() {
    return "UI Job Progress Listener";
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext context) {
    String triggerId = context.getTrigger().getKey().getName();
    progressService.updateJobStarted(triggerId);
  }

  @Override
  public void jobExecutionVetoed(JobExecutionContext context) {}

  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    String jobId = context.getTrigger().getKey().getName();
    String error = null;
    if (jobException != null) {
      error = jobException.getMessage();
    }
    progressService.updateJobStopped(jobId, error);
  }
}
