package uk.ac.ox.it.calendarimporter.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.ac.ox.it.calendarimporter.jobs.TestJob;
import uk.ac.ox.it.calendarimporter.utils.TriggerUtils;

import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
    "calendar.reimport.max.retries=3"
})
@ActiveProfiles("test")
@Transactional
@DirtiesContext
public class CanvasCalendarServiceTest {

    @Autowired
    Scheduler scheduler;

    @Mock
    JobExecutionContext context;

    @Autowired
    CanvasCalendarService canvasCalendarService;

    @Test
    public void testRetryOrDeleteJob() throws SchedulerException {
        Trigger trigger = setUpJob("1");
        scheduler.triggerJob(trigger.getJobKey());
        when(context.getTrigger()).thenReturn(trigger);

        canvasCalendarService.retryOrDeleteJob(context);
        canvasCalendarService.retryOrDeleteJob(context);
        canvasCalendarService.retryOrDeleteJob(context);
        canvasCalendarService.retryOrDeleteJob(context);
        assertEquals(3, trigger.getJobDataMap().getInt("currentRetries"));
        assertFalse(scheduler.checkExists(trigger.getKey()));
    }

    @Test
    public void testRetryOrDeleteJobWithRestartCounter() throws SchedulerException {
        Trigger trigger = setUpJob("2");
        scheduler.triggerJob(trigger.getJobKey());
        when(context.getTrigger()).thenReturn(trigger);

        canvasCalendarService.retryOrDeleteJob(context);
        canvasCalendarService.retryOrDeleteJob(context);
        canvasCalendarService.resetRetryCounter(context);
        assertNull(trigger.getJobDataMap().get("currentRetries"));

        canvasCalendarService.retryOrDeleteJob(context);
        canvasCalendarService.retryOrDeleteJob(context);
        canvasCalendarService.retryOrDeleteJob(context);
        canvasCalendarService.retryOrDeleteJob(context);
        assertEquals(3, trigger.getJobDataMap().getInt("currentRetries"));
        assertFalse(scheduler.checkExists(trigger.getKey()));
    }

    private Trigger setUpJob(String triggerKey) throws SchedulerException {
        JobDetail detail = JobBuilder.newJob(TestJob.class)
                .storeDurably()
                .requestRecovery()
                .build();

        scheduler.addJob(detail, true);

        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                .startNow()
                .withIdentity(TriggerUtils.toTriggerKey(triggerKey, "tenant", "username"))
                .forJob(detail);

        triggerBuilder.withSchedule(SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMilliseconds(10000)
                .repeatForever()
        );

        Trigger trigger = triggerBuilder.build();
        scheduler.scheduleJob(trigger);
        return trigger;
    }
}
