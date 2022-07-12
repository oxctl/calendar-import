package uk.ac.ox.it.calendarimporter.service;

import net.minidev.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.jobs.CalendarEventImportJob;

import java.util.Date;

import static org.quartz.CronScheduleBuilder.cronSchedule;

@Service
public class JobSchedulingService {

    /**
     * Cron schedule for importing the course events.
     */
    @Value("${calendar.import.cron.schedule:0 0 3 * * ?}")
    private String cronSchedule;
    
    private final Logger log = LoggerFactory.getLogger(JobSchedulingService.class);
    
    private final Scheduler scheduler;
    
    public JobSchedulingService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Schedule a job to create a calendar event.
     * @param userId The user whose calendar is to be imported.
     * @param url The url of the calendar to import.
     */
    public JSONObject subscribeUserToCalendarImport(Number userId, String url) throws SchedulerException {

        JSONObject jsonObject = new JSONObject();
        if (!isUserSubscribed(userId, url)) {

            log.debug("User {} is not subscribed to url {} so schedule calendar course events import", userId, url);

            JobDetail jobDetail = JobBuilder
                    .newJob(CalendarEventImportJob.class)
                    .withIdentity(userId + "", DigestUtils.sha256Hex(url))
                    // As we don't have a trigger initially we need to store them durably
                    .storeDurably()
                    .withIdentity(JobKey.jobKey("calendar-import-" + userId + "-" + DigestUtils.sha256Hex(url)))
                    .build();

            Trigger trigger = TriggerBuilder
                    .newTrigger()
                    .withSchedule(cronSchedule(cronSchedule))
                    .usingJobData("url", DigestUtils.sha256Hex(url))
                    .usingJobData("userId", String.valueOf(userId))
                    .forJob(JobKey.jobKey("calendar-import-" + userId + "-" + DigestUtils.sha256Hex(url)))
                    .startNow()
                    .build();
            final Date date = scheduler.scheduleJob(jobDetail, trigger);
            log.debug("Created scheduled calendar course events sync for user {} for calendar url {}, due to run right now and then next at", userId, url, date);

            jsonObject.put("date_last_ran", new Date());
            jsonObject.put("isUserSubscribed", true);
        }
        else {
            log.debug("User {} is subscribed to url {} so ignore attempt to schedule calendar course events import", userId, url);
            jsonObject.put("isUserSubscribed", false);
        }
        return jsonObject;
    }

    /**
     * unschedule a job that syncs a user's calendar events.
     * @param userId The user whose calendar is to be imported.
     * @param url The url of the calendar to import.
     */
    public JSONObject unsubscribeUserFromCalendarImport(Number userId, String url) throws SchedulerException {
        log.debug("Unscheduling calendar course events sync for user {} and with calendar url {}", userId, url);
        scheduler.deleteJob(JobKey.jobKey("calendar-import-" + userId + "-" + DigestUtils.sha256Hex(url)));
        log.debug("Unscheduled calendar course events sync for user {} and with calendar url {}, ", userId, url);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("isUserSubscribed", false);
        return jsonObject;
    }

    /**
     * Returns true /false if user is subscribed to url a job that syncs a user's calendar events.
     * @param userId The user whose calendar is to be imported.
     * @param url The url of the calendar to import.
     */
    public boolean isUserSubscribed(Number userId, String url) throws SchedulerException {
        log.debug("Checking if user is subscribed to calendar course events sync for user {} using calendar url {}", userId, url);
        return !scheduler.getTriggersOfJob(JobKey.jobKey("calendar-import-" + userId + "-" + DigestUtils.sha256Hex(url))).isEmpty();
    }
}
