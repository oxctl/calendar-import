package uk.ac.ox.it.calendarimporter.jobs.ical;

import com.nimbusds.jose.JOSEException;
import edu.ksu.canvas.interfaces.CalendarReader;
import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import edu.ksu.canvas.oauth.OauthToken;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Uid;
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
import uk.ac.ox.it.calendarimporter.jobs.csv.CSVReader;
import uk.ac.ox.it.calendarimporter.jobs.csv.HeaderException;
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
import uk.ac.ox.it.calendarimporter.service.ImportEventService;
import uk.ac.ox.it.calendarimporter.service.ProgressService;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class IcalSyncJobTest {

    @Test
    public void testValidCall() throws JobExecutionException, IOException, JOSEException, ParserException, URISyntaxException {

        IcalSyncJob icalSyncJob = new IcalSyncJob();
        CalendarImport calendarImport = new CalendarImport();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(IcalImportJob.class).build();

        List<CalendarEvent> calendarEvents = new ArrayList<>();
        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setId(105);
        calendarEvent.setContextCode("user_105");
        calendarEvent.setStartAt(Instant.now());
        calendarEvent.setEndAt(Instant.now());
        calendarEvent.setDescription("<div class=\"calendar-data-1\" style=\"display: none;\" data-calendar=\"" +
                "1234" + "\"></div>");
        calendarEvents.add(calendarEvent);

        VEvent vEvent = mock(VEvent.class);

        List<CalendarComponent> events = new ComponentList<>();
        events.add(vEvent);

        Calendar calendar = new Calendar();
        VEvent e = new VEvent();
        e.getProperties().add(new DtStart(new Date()));
        e.getProperties().add(new DtEnd(new Date()));
        e.getProperties().add(new Uid("5FC53010-1267-4F8E-BC28-1D7AE55A7C99"));
        calendar.getComponents().add(e);

        TenantRepository tenantRepository = mock(TenantRepository.class);
        OauthToken oauthToken = mock(OauthToken.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        CanvasTokenCreator canvasTokenCreator = mock(CanvasTokenCreator.class);
        ProgressService progressService = mock(ProgressService.class);
        ImportEventService importEventService = mock(ImportEventService.class);
        CalendarWriter calendarWriter = mock(CalendarWriter.class);
        CalendarReader calendarReader = mock(CalendarReader.class);
        CanvasCalendarService canvasCalendarService = mock(CanvasCalendarService.class);
        DepositService depositService = mock(DepositService.class);
        CalendarBuilder calendarBuilder = mock(CalendarBuilder.class);

        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDataMap map = new JobDataMap();
        map.put("calendar_import_id", 118L);
        map.put("time_zone",  TimeZone.getTimeZone("UTC").toString());
        map.put("url", getClass().getResource("/calendar-one-event.ics").toURI().toURL().toString());

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
        when(calendarWriter.createCalendarEvent(any())).thenReturn(Optional.of(calendarEvent));
        when(calendarReader.listCurrentUserCalendarEvents(any())).thenReturn(calendarEvents);
        when(depositService.deposit(any(), any())).thenReturn(getClass().getResource("/one-event.csv").toURI().toURL());
        when(calendarBuilder.build((InputStream) any())).thenReturn(calendar);
        doNothing().when(importEventService).eventCreated(any(), any(), any());
        doNothing().when(canvasCalendarService).resetRetryCounter(any());
        doNothing().when(importEventService).eventCreated(any(), any(), any());

        icalSyncJob.setTenantRepository(tenantRepository);
        icalSyncJob.setUserRepository(userRepository);
        icalSyncJob.setCalendarImportRepository(calendarImportRepository);
        icalSyncJob.setCanvasTokenCreator(canvasTokenCreator);
        icalSyncJob.setCanvasCalendarService(canvasCalendarService);
        icalSyncJob.setProgressService(progressService);
        icalSyncJob.setDepositService(depositService);
        icalSyncJob.setCalendarReader(calendarReader);
        icalSyncJob.setCalendarWriter(calendarWriter);

        icalSyncJob.execute(context);
        verify(calendarWriter, times(1)).createCalendarEvent(any());
    }

    @Test
    public void testCallNoTrigger() {
        IcalSyncJob IcalSyncJob = new IcalSyncJob();

        JobExecutionContext context = mock(JobExecutionContext.class);
        assertThrows(Exception.class, () -> IcalSyncJob.execute(context));
    }

    @Test
    public void testCallNoConfig() {
        IcalSyncJob IcalSyncJob = new IcalSyncJob();
        JobExecutionContext context = mock(JobExecutionContext.class);

        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();

        when(context.getJobDetail()).thenReturn(job);
        when(context.getTrigger()).thenReturn(trigger);
        assertThrows(NullPointerException.class, () -> IcalSyncJob.execute(context));
    }

    @Test
    public void testCallNoUser() throws URISyntaxException, MalformedURLException {
        IcalSyncJob IcalSyncJob = new IcalSyncJob();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);

        JobDetail job = JobBuilder.newJob(IcalSyncJob.class).build();

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

        IcalSyncJob.setUserRepository(userRepository);
        IcalSyncJob.setCalendarImportRepository(calendarImportRepository);
        IcalSyncJob.setTenantRepository(tenantRepository);

        assertThrows(NullPointerException.class, () -> IcalSyncJob.execute(context));
    }

    @Test
    public void testCallNoCalendarImport() throws JOSEException, IOException, HeaderException, URISyntaxException {
        IcalSyncJob icalSyncJob = new IcalSyncJob();
        CalendarImport calendarImport = new CalendarImport();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();
        JobExecutionContext context = mock(JobExecutionContext.class);

        JobDataMap map = new JobDataMap();
        map.put("time_zone",  TimeZone.getTimeZone("UTC").toString());
        map.put("url", getClass().getResource("/one-event.csv").toURI().toURL().toString());

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        CanvasTokenCreator canvasTokenCreator = mock(CanvasTokenCreator.class);
        OauthToken oauthToken = mock(OauthToken.class);
        ImportedEventRepository importedEventRepository = mock(ImportedEventRepository.class);
        ProgressService progressService = mock(ProgressService.class);
        CSVReader csvReader = mock(CSVReader.class);
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
        when(depositService.deposit(any(), any())).thenReturn(getClass().getResource("/one-event.csv").toURI().toURL());

        icalSyncJob.setTenantRepository(tenantRepository);
        icalSyncJob.setUserRepository(userRepository);
        icalSyncJob.setCalendarImportRepository(calendarImportRepository);
        icalSyncJob.setCanvasTokenCreator(canvasTokenCreator);
        icalSyncJob.setProgressService(progressService);
        icalSyncJob.setCanvasCalendarService(canvasCalendarService);
        icalSyncJob.setDepositService(depositService);

        assertThrows(ClassCastException.class, () -> icalSyncJob.execute(context));
    }

    @Test
    public void testCallNoTimeZone() throws JOSEException, IOException, URISyntaxException {
        IcalSyncJob icalSyncJob = new IcalSyncJob();
        CalendarImport calendarImport = new CalendarImport();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();
        JobExecutionContext context = mock(JobExecutionContext.class);

        JobDataMap map = new JobDataMap();
        map.put("calendar_import_id", 118L);
        map.put("url", getClass().getResource("/one-event.csv").toURI().toURL().toString());

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        CanvasTokenCreator canvasTokenCreator = mock(CanvasTokenCreator.class);
        OauthToken oauthToken = mock(OauthToken.class);
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
        when(depositService.deposit(any(), any())).thenReturn(getClass().getResource("/one-event.csv").toURI().toURL());

        icalSyncJob.setTenantRepository(tenantRepository);
        icalSyncJob.setUserRepository(userRepository);
        icalSyncJob.setCalendarImportRepository(calendarImportRepository);
        icalSyncJob.setCanvasTokenCreator(canvasTokenCreator);
        icalSyncJob.setProgressService(progressService);
        icalSyncJob.setCanvasCalendarService(canvasCalendarService);
        icalSyncJob.setDepositService(depositService);

        assertThrows(NullPointerException.class, () -> icalSyncJob.execute(context));
    }
}