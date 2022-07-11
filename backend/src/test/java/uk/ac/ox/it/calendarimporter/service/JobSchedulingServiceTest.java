package uk.ac.ox.it.calendarimporter.service;

import net.minidev.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// If we end up with more Quartz tests we could look at just bringing up the components that are needed
// for quartz
@SpringBootTest
@AutoConfigureTestDatabase
// While this isn't ideal it's the cleanest way to handle this.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(locations = "classpath:test.properties")
class JobSchedulingServiceTest {
    
    @Autowired
    private JobSchedulingService jobSchedulingService;
    
    @Autowired
    private Scheduler scheduler;
    
    private Number userId1 = 456;
    private Number userId2 = 678;

    private String calendarUrl1 = "https://blavatnik-calendar-import/user/456.csv";
    private String calendarUrl2 = "https://blavatnik-calendar-import/user/678.csv";

    @BeforeEach
    public void setUp() throws SchedulerException {
        // This is so that the scheduler doesn't run the jobs before we can check that we queued them correctly.
        scheduler.pauseAll();
    }

    @Test
    public void testScheduleCalendarImportJob() throws SchedulerException {
        JSONObject j1 = jobSchedulingService.subscribeUserToCalendarImport( userId1, calendarUrl1);
        assertEquals(new SimpleDateFormat("yyyyMMdd").format(new Date()), new SimpleDateFormat("yyyyMMdd").format(j1.get("date_last_ran")));
        assertEquals(true, j1.get("isUserSubscribed"));
        List<? extends Trigger> triggersOfJob = scheduler.getTriggersOfJob(JobKey.jobKey("calendar-import-" + userId1 + "-" + DigestUtils.sha256Hex(calendarUrl1)));
        assertEquals(1, triggersOfJob.size());
    }
    
    @Test
    public void testSchedule2Jobs() throws SchedulerException {
        // Schedule a couple of jobs to check that we have set things up correctly
        JSONObject j1 = jobSchedulingService.subscribeUserToCalendarImport( userId1, calendarUrl1);
        JSONObject j2 = jobSchedulingService.subscribeUserToCalendarImport( userId2 , calendarUrl2);

        assertEquals(new SimpleDateFormat("yyyyMMdd").format(new Date()), new SimpleDateFormat("yyyyMMdd").format(j1.get("date_last_ran")));
        assertEquals(true, j1.get("isUserSubscribed"));
        assertEquals(new SimpleDateFormat("yyyyMMdd").format(new Date()), new SimpleDateFormat("yyyyMMdd").format(j2.get("date_last_ran")));
        assertEquals(true, j2.get("isUserSubscribed"));

        List<? extends Trigger> triggersOfJob = scheduler.getTriggersOfJob(JobKey.jobKey("calendar-import-" + userId1 + "-" + DigestUtils.sha256Hex(calendarUrl1)));
        assertEquals(1, triggersOfJob.size());
        triggersOfJob = scheduler.getTriggersOfJob(JobKey.jobKey("calendar-import-" + userId2 + "-" + DigestUtils.sha256Hex(calendarUrl2)));
        assertEquals(1, triggersOfJob.size());
    }

    @Test
    public void testScheduleThenUnschedule2Jobs() throws SchedulerException {
        //Set up 2 jobs
        jobSchedulingService.subscribeUserToCalendarImport( userId1, calendarUrl1);
        jobSchedulingService.subscribeUserToCalendarImport( userId2, calendarUrl2);

        // check they started okay
        List<? extends Trigger> triggersOfJob = scheduler.getTriggersOfJob(JobKey.jobKey("calendar-import-" + userId1 + "-" + DigestUtils.sha256Hex(calendarUrl1)));
        assertEquals(1, triggersOfJob.size());
        triggersOfJob = scheduler.getTriggersOfJob(JobKey.jobKey("calendar-import-" + userId2 + "-" + DigestUtils.sha256Hex(calendarUrl2)));
        assertEquals(1, triggersOfJob.size());

        // Unschedule them
        jobSchedulingService.unsubscribeUserFromCalendarImport( userId1, calendarUrl1);
        jobSchedulingService.unsubscribeUserFromCalendarImport( userId2, calendarUrl2);

        // Check they both have been deleted okay
        triggersOfJob = scheduler.getTriggersOfJob(JobKey.jobKey("calendar-import-" + userId1 + "-" + DigestUtils.sha256Hex(calendarUrl1)));
        assertEquals(0, triggersOfJob.size());
        triggersOfJob = scheduler.getTriggersOfJob(JobKey.jobKey("calendar-import-" + userId2 + "-" + DigestUtils.sha256Hex(calendarUrl2)));
        assertEquals(0, triggersOfJob.size());
    }

    @Test
    public void testIsUserSubscribedNoJob() throws SchedulerException {
        assertFalse(jobSchedulingService.isUserSubscribed(userId1, calendarUrl1));
    }

    @Test
    public void testIsUserSubscribedWrongUser() throws SchedulerException {
        jobSchedulingService.subscribeUserToCalendarImport(userId2, calendarUrl2 );
        assertFalse(jobSchedulingService.isUserSubscribed(userId1, calendarUrl2));
    }

    @Test
    public void testIsUserSubscribedAfterSubscription() throws SchedulerException {
        jobSchedulingService.subscribeUserToCalendarImport(userId1, calendarUrl1 );
        assertTrue(jobSchedulingService.isUserSubscribed(userId1, calendarUrl1));
    }

    @Test
    public void testIsUserUnSubscribedAtStart() throws SchedulerException {
        assertFalse(jobSchedulingService.isUserSubscribed(userId1, calendarUrl1));
    }

    @Test
    public void testIsUserUnSubscribedAfterCancellingSubscription() throws SchedulerException {
        jobSchedulingService.subscribeUserToCalendarImport(userId1, calendarUrl1 );
        jobSchedulingService.unsubscribeUserFromCalendarImport( userId1, calendarUrl1);
        assertFalse(jobSchedulingService.isUserSubscribed(userId1, calendarUrl1));
    }
}