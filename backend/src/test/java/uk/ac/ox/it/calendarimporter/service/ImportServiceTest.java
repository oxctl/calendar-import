package uk.ac.ox.it.calendarimporter.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ox.it.calendarimporter.controller.ImportType;
import uk.ac.ox.it.calendarimporter.jobs.CleanoutJob;
import uk.ac.ox.it.calendarimporter.persistence.model.*;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.ContextJobRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.utils.TriggerUtils;

import java.util.Map;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@ActiveProfiles("test")
public class ImportServiceTest {

    @Autowired
    ImportService importService;

    @Autowired
    Scheduler scheduler;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    ContextJobRepository contextJobRepository;

    @Autowired
    CalendarImportRepository calendarImportRepository;

    @Mock
    JobExecutionContext context;

    Tenant tenant;

    User user;

    @BeforeAll
    public void setUp(){
        tenant = new Tenant();
        tenant.setName("name");
        tenant.setUrl("url");
        tenantRepository.save(tenant);

        user = new User();
        user.setSubject("subject");
        user.setUsername("username");
        user.setTenant(tenant);
        userRepository.save(user);
    }

    @Test
    public void testImportNowNotRepeats() throws SchedulerException {
        ImportConfig importConfig = setUpImportConfig(ImportType.TEST);

        ContextJob contextJob = importService.importNow(importConfig);

        ContextJob foundContextJob = contextJobRepository.findAll().iterator().next();
        CalendarImport foundCalendarImport = calendarImportRepository.findAll().iterator().next();
        SimpleTriggerImpl trigger = (SimpleTriggerImpl) getSingleTrigger(tenant.getName(), user.getSubject());

        assertEquals(contextJob, foundContextJob);
        assertEquals(foundCalendarImport, contextJob.getCalendarImport());
        assertEquals(trigger.getName(), contextJob.getCalendarImport().getLoad().getId());
        assertEquals(0, trigger.getRepeatCount());
    }

    @Test
    public void testImportNowIsRepeats() throws SchedulerException {
        ImportConfig importConfig = setUpImportConfig(ImportType.TEST_REPEATS);

        ContextJob contextJob = importService.importNow(importConfig);

        ContextJob foundContextJob = contextJobRepository.findAll().iterator().next();
        CalendarImport foundCalendarImport = calendarImportRepository.findAll().iterator().next();
        SimpleTriggerImpl trigger = (SimpleTriggerImpl) getSingleTrigger(tenant.getName(), user.getSubject());

        assertEquals(contextJob, foundContextJob);
        assertEquals(foundCalendarImport, contextJob.getCalendarImport());
        assertEquals(trigger.getName(), contextJob.getCalendarImport().getLoad().getId());
        assertEquals(-1, trigger.getRepeatCount());
    }

    @Test
    public void testImportNowWithParameters() throws SchedulerException {
        ImportConfig importConfig = setUpImportConfig(ImportType.TEST, Map.of("key", "value"));

        ContextJob contextJob = importService.importNow(importConfig);

        ContextJob foundContextJob = contextJobRepository.findAll().iterator().next();
        CalendarImport foundCalendarImport = calendarImportRepository.findAll().iterator().next();
        SimpleTriggerImpl trigger = (SimpleTriggerImpl) getSingleTrigger(tenant.getName(), user.getSubject());

        assertEquals(contextJob, foundContextJob);
        assertEquals(foundCalendarImport, contextJob.getCalendarImport());
        assertEquals(trigger.getName(), contextJob.getCalendarImport().getLoad().getId());
        assertEquals("value", trigger.getJobDataMap().get("param-key"));
        assertEquals(0, trigger.getRepeatCount());
    }

    @Test
    public void testDeleteImport() throws SchedulerException {
        ImportConfig importConfig = setUpImportConfig(ImportType.TEST);

        ContextJob contextJob = importService.importNow(importConfig);
        contextJob.getCalendarImport().getLoad().setStatus(JobProgress.Status.COMPLETED);
        importService.deleteImport(contextJob.getCalendarImport().getId(), user);

        CalendarImport calendarImport = calendarImportRepository.findAll().iterator().next();

        assertNotNull(calendarImport.getDelete());
    }

    @Test
    public void testPurgeImports() throws SchedulerException {
        ImportConfig importConfig = setUpImportConfig(ImportType.TEST);
        ContextJob contextJob = importService.importNow(importConfig);

        importService.purgeImports(contextJob.getContext(), tenant.getName(), user.getSubject(), false);

        TriggerKey triggerKey = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals("DEFAULT")).iterator().next();
        SimpleTriggerImpl trigger = (SimpleTriggerImpl) scheduler.getTrigger(triggerKey);
        JobDetail jobDetail = scheduler.getJobDetail(trigger.getJobKey());

        assertTrue(scheduler.checkExists(trigger.getKey()));
        assertEquals(CleanoutJob.class, jobDetail.getJobClass());
        assertFalse((Boolean) trigger.getJobDataMap().get("all"));
    }

    @Test
    public void testPurgeImportsAll() throws SchedulerException {
        ImportConfig importConfig = setUpImportConfig(ImportType.TEST);
        ContextJob contextJob = importService.importNow(importConfig);

        importService.purgeImports(contextJob.getContext(), tenant.getName(), user.getSubject(), true);

        TriggerKey triggerKey = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals("DEFAULT")).iterator().next();
        SimpleTriggerImpl trigger = (SimpleTriggerImpl) scheduler.getTrigger(triggerKey);
        JobDetail jobDetail = scheduler.getJobDetail(trigger.getJobKey());

        assertTrue(scheduler.checkExists(trigger.getKey()));
        assertEquals(CleanoutJob.class, jobDetail.getJobClass());
        assertTrue((Boolean) trigger.getJobDataMap().get("all"));
    }

    @Test
    public void testGetJob() throws SchedulerException {
        ImportConfig importConfig = setUpImportConfig(ImportType.TEST);
        ContextJob contextJob = importService.importNow(importConfig);

        ContextJob foundContextJob = importService.getJob(tenant.getName(), contextJob.getContext(), contextJob.getId()).get();
        assertEquals(foundContextJob, contextJob);
    }

    @Test
    public void testHideImport() throws SchedulerException {
        ImportConfig importConfig = setUpImportConfig(ImportType.TEST);
        ContextJob contextJob = importService.importNow(importConfig);
        assertFalse(contextJob.isHidden());

        importService.hideImport(contextJob);
        assertTrue(contextJob.isHidden());
    }

    // util to get a trigger for when there is only one scheduled
    private Trigger getSingleTrigger(String tenantName, String subject) throws SchedulerException {
        String groupName = TriggerUtils.getTriggerGroup(tenantName, subject);
        TriggerKey triggerKey = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals( groupName)).iterator().next();
        return scheduler.getTrigger(triggerKey);
    }

    private ImportConfig setUpImportConfig(ImportType importType){
        return setUpImportConfig(importType, Map.of());
    }

    private ImportConfig setUpImportConfig(ImportType importType, Map<String, String> parameters){
        return new ImportConfig(
                importType,
                "url",
                "filename",
                user,
                "context",
                new CourseSection(),
                TimeZone.getTimeZone("GMT"),
                parameters);
    }
}
