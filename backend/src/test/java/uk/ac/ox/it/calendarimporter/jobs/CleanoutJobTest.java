package uk.ac.ox.it.calendarimporter.jobs;

import com.nimbusds.jose.JOSEException;
import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.interfaces.CalendarReader;
import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import edu.ksu.canvas.oauth.OauthToken;
import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import uk.ac.ox.it.calendarimporter.jobs.csv.CSVImportJob;
import uk.ac.ox.it.calendarimporter.jobs.csv.CSVReader;
import uk.ac.ox.it.calendarimporter.jobs.csv.HeaderException;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.service.CanvasCalendarService;
import uk.ac.ox.it.calendarimporter.service.CanvasTokenCreator;
import uk.ac.ox.it.calendarimporter.service.DepositService;
import uk.ac.ox.it.calendarimporter.service.ImportEventService;
import uk.ac.ox.it.calendarimporter.service.ProgressService;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CleanoutJobTest {

    @Test
    public void testValidCall() throws JobExecutionException, IOException, JOSEException, HeaderException {

        CleanoutJob cleanoutJob = new CleanoutJob();
        User user = new User();
        CalendarImport calendarImport = new CalendarImport();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CleanoutJob.class).build();

        List<CalendarEvent> calendarEvents = new ArrayList<>();
        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setId(105);
        calendarEvent.setDescription("105");
        calendarEvent.setContextCode("user_105");
        calendarEvent.setStartAt(Instant.now());
        calendarEvent.setEndAt(Instant.now());
        calendarEvents.add(calendarEvent);

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        CanvasTokenCreator canvasTokenCreator = mock(CanvasTokenCreator.class);
        ProgressService progressService = mock(ProgressService.class);
        CSVReader csvReader = mock(CSVReader.class);
        ImportEventService importEventService = mock(ImportEventService.class);
        CanvasCalendarService canvasCalendarService = mock(CanvasCalendarService.class);
        DepositService depositService = mock(DepositService.class);
        CanvasApiFactory canvasApiFactory = mock(CanvasApiFactory.class);
        CalendarWriter calendarWriter = mock(CalendarWriter.class);
        CalendarReader calendarReader = mock(CalendarReader.class);
        OauthToken oauthToken = mock(OauthToken.class);

        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDataMap map = new JobDataMap();
        map.put("calendar_import_id", 118L);
        map.put("time_zone", TimeZone.getDefault().toString());
        map.put("url", "http://bbc.co.uk");
        map.put("all", true);
        when(context.getMergedJobDataMap()).thenReturn(map);
        when(context.getTrigger()).thenReturn(trigger);
        when(context.getJobDetail()).thenReturn(job);
        when(tenantRepository.findByName(any())).thenReturn(Optional.of(new Tenant()));
        when(userRepository
                .findBySubjectAndTenantName(any(), any())).thenReturn(Optional.of(user));
        when(calendarImportRepository
                .findById(any())).thenReturn(Optional.of(calendarImport));
        when(canvasTokenCreator.getToken(any(), any())).thenReturn(oauthToken);
        when(progressService.updateJob(any(), any(), any())).thenReturn(null);
        when(csvReader.parseCSV(any(), any(), any())).thenReturn(calendarEvents);
        when(calendarReader.listCurrentUserCalendarEvents(any())).thenReturn(calendarEvents);
        when(depositService.deposit(any(), any())).thenReturn(new URL("https://bbc.co.uk"));
        when(canvasApiFactory.getReader(any(), any())).thenReturn(calendarReader);
        when(canvasApiFactory.getWriter(any(), any())).thenReturn(calendarWriter);
        doNothing().when(importEventService).eventCreated(any(), any(), any());
        doNothing().when(canvasCalendarService).resetRetryCounter(any());

        cleanoutJob.setTenantRepository(tenantRepository);
        cleanoutJob.setUserRepository(userRepository);
        cleanoutJob.setCanvasTokenCreator(canvasTokenCreator);
        cleanoutJob.setCanvasApiFactory(canvasApiFactory);

        cleanoutJob.execute(context);
        verify(calendarReader, times(1)).listCurrentUserCalendarEvents(any());
        verify(calendarWriter, times(1)).deleteCalendarEvent(any());
    }

    @Test
    public void testCallNoTrigger() {
        CleanoutJob cleanoutJob = new CleanoutJob();

        JobExecutionContext context = mock(JobExecutionContext.class);
        assertThrows(Exception.class, () -> cleanoutJob.execute(context));
    }

    @Test
    public void testCallNoConfig() {
        CleanoutJob cleanoutJob = new CleanoutJob();
        JobExecutionContext context = mock(JobExecutionContext.class);

        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();

        when(context.getJobDetail()).thenReturn(job);
        when(context.getTrigger()).thenReturn(trigger);
        assertThrows(NullPointerException.class, () -> cleanoutJob.execute(context));
    }

    @Test
    public void testCallNoUser() {
        CleanoutJob cleanoutJob = new CleanoutJob();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        JobDetail job = JobBuilder.newJob(CleanoutJob.class).build();

        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDataMap map = new JobDataMap();
        map.put("calendar_import_id", 118L);
        map.put("time_zone", TimeZone.getDefault().toString());
        map.put("url", "http://bbc.co.uk");
        when(context.getMergedJobDataMap()).thenReturn(map);
        when(context.getTrigger()).thenReturn(trigger);
        when(context.getJobDetail()).thenReturn(job);
        when(context.getMergedJobDataMap()).thenReturn(map);
        when(tenantRepository.findByName(any())).thenReturn(Optional.of(new Tenant()));

        cleanoutJob.setUserRepository(userRepository);
        cleanoutJob.setTenantRepository(tenantRepository);

        assertThrows(JobExecutionException.class, () -> cleanoutJob.execute(context));
    }
}