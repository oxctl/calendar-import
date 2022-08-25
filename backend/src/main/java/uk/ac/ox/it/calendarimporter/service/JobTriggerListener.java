package uk.ac.ox.it.calendarimporter.service;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.TriggerKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;

@Service
@Slf4j
public class JobTriggerListener implements TriggerListener{

    @Autowired
    private Scheduler scheduler;

    private static final String NAME = "JobTriggerListener";
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {

    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false;
    }

    @Override
    public void triggerMisfired(Trigger trigger) {

    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, Trigger.CompletedExecutionInstruction triggerInstructionCode) {
        if(!isRetrying(trigger)){
            resetRetryCounter(trigger);
        }
    }

    private boolean isRetrying(Trigger trigger){
        return trigger.getJobDataMap().containsKey(CanvasCalendarJob.RETRY);
    }

    private void resetRetryCounter(Trigger trigger){
        trigger.getJobDataMap().remove(CanvasCalendarJob.CURRENT_RETRIES);
        TriggerKey triggerKey = trigger.getKey();
        try{
            scheduler.rescheduleJob(triggerKey, trigger);
            log.info("Rescheduled job {}.", triggerKey);
        }catch(SchedulerException e){
            log.warn("Could not reschedule job {}.", triggerKey);
        }
    }
}
