package uk.ac.ox.it.calendarimporter.jobs.csv;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.ImportedEventRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.service.CanvasCalendarService;
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
class CSVImportJobTest {

    @Autowired
    private CSVReader reader;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    public void testValidCallOneEvent() throws JobExecutionException, IOException, JOSEException, URISyntaxException {

        CSVImportJob csvImportJob = new CSVImportJob();
        CalendarImport calendarImport = new CalendarImport();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();
        JobExecutionContext context = mock(JobExecutionContext.class);
        Resource oneEventResource = resourceLoader.getResource("classpath:one-event.csv");

        JobDataMap map = new JobDataMap();
        map.put("calendar_import_id", 118L);
        map.put("time_zone",  TimeZone.getTimeZone("UTC").toString());
        map.put(CanvasCalendarJob.SOURCE_URL, getClass().getResource("/one-event.csv").toURI().toURL().toString());

        List<CalendarEvent> calendarEvents = new ArrayList<>();
        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setId(105);
        calendarEvent.setContextCode("user_105");
        calendarEvent.setStartAt(Instant.now());
        calendarEvent.setEndAt(Instant.now());
        calendarEvents.add(calendarEvent);

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        CanvasTokenCreator canvasTokenCreator = mock(CanvasTokenCreator.class);
        OauthToken oauthToken = mock(OauthToken.class);
        ImportedEventRepository importedEventRepository = mock(ImportedEventRepository.class);
        ProgressService progressService = mock(ProgressService.class);
        CalendarWriter calendarWriter = mock(CalendarWriter.class);
        CanvasCalendarService canvasCalendarService = mock(CanvasCalendarService.class);
        DepositService depositService = mock(DepositService.class);

        when(context.getMergedJobDataMap()).thenReturn(map);
        when(context.getTrigger()).thenReturn(trigger);
        when(context.getJobDetail()).thenReturn(job);
        when(tenantRepository.findByName(any())).thenReturn(Optional.of(new Tenant()));
        when(userRepository
                .findBySubjectAndTenantName(any(), any())).thenReturn(Optional.of(new User()));
        when(calendarImportRepository
                .findById(any())).thenReturn(Optional.of(calendarImport));
        when(canvasTokenCreator.getToken(any(), any())).thenReturn(oauthToken);
        when(progressService.updateJob(any(), any(), any())).thenReturn(null);
        doNothing().when(canvasCalendarService).resetRetryCounter(any());
        when(depositService.deposit(any(), any())).thenReturn(oneEventResource.getFile().toString());
        when(depositService.getInputStream(any(), any())).thenReturn(oneEventResource.getInputStream());

        csvImportJob.setTenantRepository(tenantRepository);
        csvImportJob.setUserRepository(userRepository);
        csvImportJob.setCalendarImportRepository(calendarImportRepository);
        csvImportJob.setCanvasTokenCreator(canvasTokenCreator);
        csvImportJob.setImportedEventRepository(importedEventRepository);
        csvImportJob.setCSVReader(reader);
        csvImportJob.setProgressService(progressService);
        csvImportJob.setCanvasCalendarService(canvasCalendarService);
        csvImportJob.setDepositService(depositService);
        csvImportJob.setCalendarWriter(calendarWriter);

        csvImportJob.execute(context);
        verify(calendarWriter, times(1)).createCalendarEvent(any());
    }

    @Test
    public void testValidCallNoHeaders() throws JobExecutionException, IOException, JOSEException, URISyntaxException {

        CSVImportJob csvImportJob = new CSVImportJob();
        CalendarImport calendarImport = new CalendarImport();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();
        JobExecutionContext context = mock(JobExecutionContext.class);
        Resource noHeadersResource = resourceLoader.getResource("classpath:no-headers.csv");

        JobDataMap map = new JobDataMap();
        map.put("calendar_import_id", 118L);
        map.put("time_zone",  TimeZone.getTimeZone("UTC").toString());
        map.put(CanvasCalendarJob.SOURCE_URL, noHeadersResource.getFile().getPath());

        List<CalendarEvent> calendarEvents = new ArrayList<>();
        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setId(105);
        calendarEvent.setContextCode("user_105");
        calendarEvent.setStartAt(Instant.now());
        calendarEvent.setEndAt(Instant.now());
        calendarEvents.add(calendarEvent);

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        CanvasTokenCreator canvasTokenCreator = mock(CanvasTokenCreator.class);
        OauthToken oauthToken = mock(OauthToken.class);
        ImportedEventRepository importedEventRepository = mock(ImportedEventRepository.class);
        ProgressService progressService = mock(ProgressService.class);
        CalendarWriter calendarWriter = mock(CalendarWriter.class);
        CanvasCalendarService canvasCalendarService = mock(CanvasCalendarService.class);
        DepositService depositService = mock(DepositService.class);

        when(context.getMergedJobDataMap()).thenReturn(map);
        when(context.getTrigger()).thenReturn(trigger);
        when(context.getJobDetail()).thenReturn(job);
        when(tenantRepository.findByName(any())).thenReturn(Optional.of(new Tenant()));
        when(userRepository
                .findBySubjectAndTenantName(any(), any())).thenReturn(Optional.of(new User()));
        when(calendarImportRepository
                .findById(any())).thenReturn(Optional.of(calendarImport));
        when(canvasTokenCreator.getToken(any(), any())).thenReturn(oauthToken);
        when(progressService.updateJob(any(), any(), any())).thenReturn(null);
        doNothing().when(canvasCalendarService).resetRetryCounter(any());
        when(depositService.deposit(any(), any())).thenReturn(noHeadersResource.getFile().toString());
        when(depositService.getInputStream(any(), any())).thenReturn(noHeadersResource.getInputStream());

        csvImportJob.setTenantRepository(tenantRepository);
        csvImportJob.setUserRepository(userRepository);
        csvImportJob.setCalendarImportRepository(calendarImportRepository);
        csvImportJob.setCanvasTokenCreator(canvasTokenCreator);
        csvImportJob.setImportedEventRepository(importedEventRepository);
        csvImportJob.setCSVReader(reader);
        csvImportJob.setProgressService(progressService);
        csvImportJob.setCanvasCalendarService(canvasCalendarService);
        csvImportJob.setDepositService(depositService);
        csvImportJob.setCalendarWriter(calendarWriter);

        csvImportJob.execute(context);
        verify(calendarWriter, times(0)).createCalendarEvent(any());
    }

    @Test
    public void testValidCallWrongHeaders() throws JobExecutionException, IOException, JOSEException, URISyntaxException {

        CSVImportJob csvImportJob = new CSVImportJob();
        CalendarImport calendarImport = new CalendarImport();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();
        JobExecutionContext context = mock(JobExecutionContext.class);
        Resource wrongHeadersResource = resourceLoader.getResource("classpath:wrong-headers.csv");

        JobDataMap map = new JobDataMap();
        map.put("calendar_import_id", 118L);
        map.put("time_zone",  TimeZone.getTimeZone("UTC").toString());
        map.put(CanvasCalendarJob.SOURCE_URL, wrongHeadersResource.getFile().getPath());

        List<CalendarEvent> calendarEvents = new ArrayList<>();
        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setId(105);
        calendarEvent.setContextCode("user_105");
        calendarEvent.setStartAt(Instant.now());
        calendarEvent.setEndAt(Instant.now());
        calendarEvents.add(calendarEvent);

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        CanvasTokenCreator canvasTokenCreator = mock(CanvasTokenCreator.class);
        OauthToken oauthToken = mock(OauthToken.class);
        ImportedEventRepository importedEventRepository = mock(ImportedEventRepository.class);
        ProgressService progressService = mock(ProgressService.class);
        CalendarWriter calendarWriter = mock(CalendarWriter.class);
        CanvasCalendarService canvasCalendarService = mock(CanvasCalendarService.class);
        DepositService depositService = mock(DepositService.class);

        when(context.getMergedJobDataMap()).thenReturn(map);
        when(context.getTrigger()).thenReturn(trigger);
        when(context.getJobDetail()).thenReturn(job);
        when(tenantRepository.findByName(any())).thenReturn(Optional.of(new Tenant()));
        when(userRepository
                .findBySubjectAndTenantName(any(), any())).thenReturn(Optional.of(new User()));
        when(calendarImportRepository
                .findById(any())).thenReturn(Optional.of(calendarImport));
        when(canvasTokenCreator.getToken(any(), any())).thenReturn(oauthToken);
        when(progressService.updateJob(any(), any(), any())).thenReturn(null);
        doNothing().when(canvasCalendarService).resetRetryCounter(any());
        when(depositService.deposit(any(), any())).thenReturn(wrongHeadersResource.getFile().toString());
        when(depositService.getInputStream(any(), any())).thenReturn(wrongHeadersResource.getInputStream());

        csvImportJob.setTenantRepository(tenantRepository);
        csvImportJob.setUserRepository(userRepository);
        csvImportJob.setCalendarImportRepository(calendarImportRepository);
        csvImportJob.setCanvasTokenCreator(canvasTokenCreator);
        csvImportJob.setImportedEventRepository(importedEventRepository);
        csvImportJob.setCSVReader(reader);
        csvImportJob.setProgressService(progressService);
        csvImportJob.setCanvasCalendarService(canvasCalendarService);
        csvImportJob.setDepositService(depositService);
        csvImportJob.setCalendarWriter(calendarWriter);

        csvImportJob.execute(context);
        verify(calendarWriter, times(0)).createCalendarEvent(any());
    }

    @Test
    public void testValidCallAPIThrowsIOException() throws IOException, JOSEException, URISyntaxException {

        CSVImportJob csvImportJob = new CSVImportJob();
        CalendarImport calendarImport = new CalendarImport();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();
        JobExecutionContext context = mock(JobExecutionContext.class);
        Resource oneEventResource = resourceLoader.getResource("classpath:one-event.csv");

        JobDataMap map = new JobDataMap();
        map.put("calendar_import_id", 118L);
        map.put("time_zone",  TimeZone.getTimeZone("UTC").toString());
        map.put(CanvasCalendarJob.SOURCE_URL, getClass().getResource("/one-event.csv").toURI().toURL().toString());

        List<CalendarEvent> calendarEvents = new ArrayList<>();
        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setId(105);
        calendarEvent.setContextCode("user_105");
        calendarEvent.setStartAt(Instant.now());
        calendarEvent.setEndAt(Instant.now());
        calendarEvents.add(calendarEvent);

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        CanvasTokenCreator canvasTokenCreator = mock(CanvasTokenCreator.class);
        OauthToken oauthToken = mock(OauthToken.class);
        ImportedEventRepository importedEventRepository = mock(ImportedEventRepository.class);
        ProgressService progressService = mock(ProgressService.class);
        CalendarWriter calendarWriter = mock(CalendarWriter.class);
        CanvasCalendarService canvasCalendarService = mock(CanvasCalendarService.class);
        DepositService depositService = mock(DepositService.class);

        when(context.getMergedJobDataMap()).thenReturn(map);
        when(context.getTrigger()).thenReturn(trigger);
        when(context.getJobDetail()).thenReturn(job);
        when(tenantRepository.findByName(any())).thenReturn(Optional.of(new Tenant()));
        when(userRepository
                .findBySubjectAndTenantName(any(), any())).thenReturn(Optional.of(new User()));
        when(calendarImportRepository
                .findById(any())).thenReturn(Optional.of(calendarImport));
        when(canvasTokenCreator.getToken(any(), any())).thenReturn(oauthToken);
        when(progressService.updateJob(any(), any(), any())).thenReturn(null);
        doNothing().when(canvasCalendarService).resetRetryCounter(any());
        when(depositService.deposit(any(), any())).thenReturn(oneEventResource.getURL().toString());
        when(depositService.getInputStream(any(), any())).thenReturn(oneEventResource.getInputStream());
        when(calendarWriter.createCalendarEvent(any())).thenThrow(new IOException());

        csvImportJob.setTenantRepository(tenantRepository);
        csvImportJob.setUserRepository(userRepository);
        csvImportJob.setCalendarImportRepository(calendarImportRepository);
        csvImportJob.setCanvasTokenCreator(canvasTokenCreator);
        csvImportJob.setImportedEventRepository(importedEventRepository);
        csvImportJob.setCSVReader(reader);
        csvImportJob.setProgressService(progressService);
        csvImportJob.setCanvasCalendarService(canvasCalendarService);
        csvImportJob.setDepositService(depositService);
        csvImportJob.setCalendarWriter(calendarWriter);

        assertThrows(JobExecutionException.class, () -> csvImportJob.execute(context));
        verify(calendarWriter, times(1)).createCalendarEvent(any());
    }

    @Test
    public void testCallNoTrigger() {
        CSVImportJob csvImportJob = new CSVImportJob();
        JobExecutionContext context = mock(JobExecutionContext.class);

        assertThrows(NullPointerException.class, () -> csvImportJob.execute(context));
    }

    @Test
    public void testCallNoConfig() {
        CSVImportJob csvImportJob = new CSVImportJob();
        JobExecutionContext context = mock(JobExecutionContext.class);

        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();

        when(context.getJobDetail()).thenReturn(job);
        when(context.getTrigger()).thenReturn(trigger);

        assertThrows(NullPointerException.class, () -> csvImportJob.execute(context));
    }

    @Test
    public void testCallNoCalendarImport() throws JOSEException, IOException, URISyntaxException {
        CSVImportJob csvImportJob = new CSVImportJob();
        CalendarImport calendarImport = new CalendarImport();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();
        JobExecutionContext context = mock(JobExecutionContext.class);
        Resource oneEventResource = resourceLoader.getResource("classpath:one-event.csv");

        JobDataMap map = new JobDataMap();
        map.put("time_zone",  TimeZone.getTimeZone("UTC").toString());
        map.put(CanvasCalendarJob.SOURCE_URL, getClass().getResource("/one-event.csv").toURI().toURL().toString());

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        CanvasTokenCreator canvasTokenCreator = mock(CanvasTokenCreator.class);
        OauthToken oauthToken = mock(OauthToken.class);
        ImportedEventRepository importedEventRepository = mock(ImportedEventRepository.class);
        ProgressService progressService = mock(ProgressService.class);
        CanvasCalendarService canvasCalendarService = mock(CanvasCalendarService.class);
        DepositService depositService = mock(DepositService.class);

        when(context.getMergedJobDataMap()).thenReturn(map);
        when(context.getTrigger()).thenReturn(trigger);
        when(context.getJobDetail()).thenReturn(job);
        when(tenantRepository.findByName(any())).thenReturn(Optional.of(new Tenant()));
        when(userRepository
                .findBySubjectAndTenantName(any(), any())).thenReturn(Optional.of(new User()));
        when(calendarImportRepository
                .findById(any())).thenReturn(Optional.of(calendarImport));
        when(canvasTokenCreator.getToken(any(), any())).thenReturn(oauthToken);
        when(progressService.updateJob(any(), any(), any())).thenReturn(null);
        doNothing().when(canvasCalendarService).resetRetryCounter(any());
        when(depositService.deposit(any(), any())).thenReturn(oneEventResource.getURL().toString());
        when(depositService.getInputStream(any(), any())).thenReturn(oneEventResource.getInputStream());

        csvImportJob.setTenantRepository(tenantRepository);
        csvImportJob.setUserRepository(userRepository);
        csvImportJob.setCalendarImportRepository(calendarImportRepository);
        csvImportJob.setCanvasTokenCreator(canvasTokenCreator);
        csvImportJob.setImportedEventRepository(importedEventRepository);
        csvImportJob.setProgressService(progressService);
        csvImportJob.setCanvasCalendarService(canvasCalendarService);
        csvImportJob.setDepositService(depositService);

        assertThrows(ClassCastException.class, () -> csvImportJob.execute(context));
    }

    @Test
    public void testCallNoTimeZone() throws JOSEException, IOException, URISyntaxException {
        CSVImportJob csvImportJob = new CSVImportJob();
        CalendarImport calendarImport = new CalendarImport();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();
        JobExecutionContext context = mock(JobExecutionContext.class);

        JobDataMap map = new JobDataMap();
        map.put("calendar_import_id", 118L);
        map.put(CanvasCalendarJob.SOURCE_URL, getClass().getResource("/one-event.csv").toURI().toURL().toString());

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        CanvasTokenCreator canvasTokenCreator = mock(CanvasTokenCreator.class);
        OauthToken oauthToken = mock(OauthToken.class);
        ImportedEventRepository importedEventRepository = mock(ImportedEventRepository.class);
        ProgressService progressService = mock(ProgressService.class);
        CanvasCalendarService canvasCalendarService = mock(CanvasCalendarService.class);
        DepositService depositService = mock(DepositService.class);

        when(context.getMergedJobDataMap()).thenReturn(map);
        when(context.getTrigger()).thenReturn(trigger);
        when(context.getJobDetail()).thenReturn(job);
        when(tenantRepository.findByName(any())).thenReturn(Optional.of(new Tenant()));
        when(userRepository
                .findBySubjectAndTenantName(any(), any())).thenReturn(Optional.of(new User()));
        when(calendarImportRepository
                .findById(any())).thenReturn(Optional.of(calendarImport));
        when(canvasTokenCreator.getToken(any(), any())).thenReturn(oauthToken);
        when(progressService.updateJob(any(), any(), any())).thenReturn(null);
        doNothing().when(canvasCalendarService).resetRetryCounter(any());

        csvImportJob.setTenantRepository(tenantRepository);
        csvImportJob.setUserRepository(userRepository);
        csvImportJob.setCalendarImportRepository(calendarImportRepository);
        csvImportJob.setCanvasTokenCreator(canvasTokenCreator);
        csvImportJob.setImportedEventRepository(importedEventRepository);
        csvImportJob.setProgressService(progressService);
        csvImportJob.setCanvasCalendarService(canvasCalendarService);
        csvImportJob.setDepositService(depositService);

        assertThrows(NullPointerException.class, () -> csvImportJob.execute(context));
    }

    @Test
    public void testCallNoUser() throws URISyntaxException, MalformedURLException {
        CSVImportJob csvImportJob = new CSVImportJob();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);

        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();

        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDataMap map = new JobDataMap();
        map.put("calendar_import_id", 118L);
        map.put("time_zone",  TimeZone.getTimeZone("UTC").toString());
        map.put(CanvasCalendarJob.SOURCE_URL, getClass().getResource("/one-event.csv").toURI().toURL().toString());
        when(context.getMergedJobDataMap()).thenReturn(map);
        when(context.getTrigger()).thenReturn(trigger);
        when(context.getJobDetail()).thenReturn(job);
        when(context.getMergedJobDataMap()).thenReturn(map);
        when(tenantRepository.findByName(any())).thenReturn(Optional.of(new Tenant()));

        csvImportJob.setUserRepository(userRepository);
        csvImportJob.setCalendarImportRepository(calendarImportRepository);
        csvImportJob.setTenantRepository(tenantRepository);

        assertThrows(NullPointerException.class, () -> csvImportJob.execute(context));
    }
}