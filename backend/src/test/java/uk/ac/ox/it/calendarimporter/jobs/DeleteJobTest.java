package uk.ac.ox.it.calendarimporter.jobs;

import com.nimbusds.jose.JOSEException;
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
import org.springframework.boot.test.context.SpringBootTest;
import uk.ac.ox.it.calendarimporter.jobs.csv.CSVImportJob;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEvent;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.ImportedEventRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.service.CanvasTokenCreator;
import uk.ac.ox.it.calendarimporter.service.DepositService;
import uk.ac.ox.it.calendarimporter.service.ProgressService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest
class DeleteJobTest {

    @Test
    public void testValidCall() throws JobExecutionException, IOException, JOSEException, URISyntaxException {

        DeleteJob deleteJob = new DeleteJob();
        User user = new User();
        CalendarImport calendarImport = new CalendarImport();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(DeleteJob.class).build();

        List<CalendarEvent> calendarEvents = new ArrayList<>();
        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setId(105);
        calendarEvent.setContextCode("user_105");
        calendarEvent.setStartAt(Instant.now());
        calendarEvent.setEndAt(Instant.now());
        calendarEvents.add(calendarEvent);

        ImportedEvent.ImportedEventIdentity identity =
                new ImportedEvent.ImportedEventIdentity(5678L, calendarEvent.getId());
        ImportedEvent event = new ImportedEvent(identity, calendarImport, ImportedEvent.Status.CREATED);
        List< ImportedEvent > importedEvents = new ArrayList<>();
        importedEvents.add(event);

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        CanvasTokenCreator canvasTokenCreator = mock(CanvasTokenCreator.class);
        ImportedEventRepository importedEventRepository = mock(ImportedEventRepository.class);
        ProgressService progressService = mock(ProgressService.class);
        CalendarWriter calendarWriter = mock(CalendarWriter.class);
        DepositService depositService = mock(DepositService.class);
        OauthToken oauthToken = mock(OauthToken.class);

        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDataMap map = new JobDataMap();
        map.put("calendar_import_id", 118L);
        map.put("time_zone",  TimeZone.getTimeZone("UTC").toString());
        map.put("url", getClass().getResource("/one-event.csv").toURI().toURL().toString());
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
        when(importedEventRepository.findByCalendarImport(any())).thenReturn(importedEvents);
        when(calendarWriter.deleteCalendarEvent(any())).thenReturn(Optional.of(calendarEvent));
        when(depositService.deposit(any(), any())).thenReturn(getClass().getResource("/one-event.csv").toURI().toURL());

        deleteJob.setTenantRepository(tenantRepository);
        deleteJob.setUserRepository(userRepository);
        deleteJob.setCalendarImportRepository(calendarImportRepository);
        deleteJob.setCanvasTokenCreator(canvasTokenCreator);
        deleteJob.setImportedEventRepository(importedEventRepository);
        deleteJob.setProgressService(progressService);
        deleteJob.setDepositService(depositService);
        deleteJob.setCalendarWriter(calendarWriter);

        deleteJob.execute(context);
        verify(importedEventRepository, times(1)).save(any());
        verify(calendarWriter, times(1)).deleteCalendarEvent(any());
    }

    @Test
    public void testCallNoTrigger() {
        DeleteJob deleteJob = new DeleteJob();

        JobExecutionContext context = mock(JobExecutionContext.class);
        assertThrows(NullPointerException.class, () -> deleteJob.execute(context));
    }

    @Test
    public void testCallNoConfig() {
        DeleteJob deleteJob = new DeleteJob();
        JobExecutionContext context = mock(JobExecutionContext.class);

        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();

        when(context.getJobDetail()).thenReturn(job);
        when(context.getTrigger()).thenReturn(trigger);
        assertThrows(NullPointerException.class, () -> deleteJob.execute(context));
    }

    @Test
    public void testCallNoUser() throws URISyntaxException, MalformedURLException {
        DeleteJob deleteJob = new DeleteJob();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        JobDetail job = JobBuilder.newJob(DeleteJob.class).build();

        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDataMap map = new JobDataMap();
        map.put("calendar_import_id", 118L);
        map.put("time_zone",  TimeZone.getTimeZone("UTC").toString());
        map.put("url", getClass().getResource("/one-event.csv").toURI().toURL().toString());
        when(context.getMergedJobDataMap()).thenReturn(map);
        when(context.getTrigger()).thenReturn(trigger);
        when(context.getJobDetail()).thenReturn(job);
        when(context.getMergedJobDataMap()).thenReturn(map);
        when(tenantRepository.findByName(any())).thenReturn(Optional.of(new Tenant()));

        deleteJob.setUserRepository(userRepository);
        deleteJob.setCalendarImportRepository(calendarImportRepository);
        deleteJob.setTenantRepository(tenantRepository);

        assertThrows(NullPointerException.class, () -> deleteJob.execute(context));
    }
}