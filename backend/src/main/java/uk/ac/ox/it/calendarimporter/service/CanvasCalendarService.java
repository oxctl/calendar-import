package uk.ac.ox.it.calendarimporter.service;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;

@Service
@Slf4j
public class CanvasCalendarService {

    @Autowired
    private Scheduler scheduler;

    @Value("${calendar.reimport.max.retries}")
    private Integer maxRetries;

    public void retryOrDeleteJob(JobExecutionContext context){
        Trigger trigger = context.getTrigger();
        if (!isRepeatingJob(trigger)) return;

        TriggerKey triggerKey = trigger.getKey();
        JobDataMap jobDataMap = trigger.getJobDataMap();
        int currentRetries = (int) jobDataMap.getOrDefault(CanvasCalendarJob.CURRENT_RETRIES, 0);
        if(currentRetries >= maxRetries) {
            unscheduleJob(triggerKey);
        }else{
            ++currentRetries;
            log.info("Attempting to retry job {}: attempt {} of {}", triggerKey, currentRetries, maxRetries);
            jobDataMap.put(CanvasCalendarJob.CURRENT_RETRIES, currentRetries);
            if(!rescheduleJob(trigger)) unscheduleJob(triggerKey);
        }
    }

    private boolean isRepeatingJob(Trigger trigger){
        return trigger.getNextFireTime() != null;
    }

    public void resetRetryCounter(JobExecutionContext context){
        JobDataMap jobDataMap = context.getTrigger().getJobDataMap();
        if(!jobDataMap.containsKey(CanvasCalendarJob.CURRENT_RETRIES)) return;
        jobDataMap.remove(CanvasCalendarJob.CURRENT_RETRIES);
        rescheduleJob(context.getTrigger());
    }

    private boolean rescheduleJob(Trigger trigger){
        TriggerKey triggerKey = trigger.getKey();
        try{
            scheduler.rescheduleJob(triggerKey, trigger);
            log.info("Rescheduled job {}.", triggerKey);
            return true;
        }catch(SchedulerException e){
            log.warn("Could not reschedule job {}.", triggerKey);
            return false;
        }
    }

    private void unscheduleJob(TriggerKey triggerKey){
        try {
            scheduler.unscheduleJob(triggerKey);
            log.info("Deleting job {}.", triggerKey);
        } catch (SchedulerException e) {
            log.warn("Could not delete job {}.", triggerKey);
        }
    }
}
