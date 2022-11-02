package uk.ac.ox.it.calendarimporter.jobs.ical;

import com.nimbusds.jose.JOSEException;
import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.interfaces.CalendarReader;
import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import edu.ksu.canvas.oauth.OauthToken;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
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
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class IcalImportJobTest {

    @Test
    public void testValidCall() throws JobExecutionException, IOException, JOSEException, HeaderException, ParserException, ParseException {

        IcalImportJob icalImportJob = new IcalImportJob();
        CalendarImport calendarImport = new CalendarImport();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(IcalImportJob.class).build();

        List<CalendarEvent> calendarEvents = new ArrayList<>();
        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setId(105);
        calendarEvent.setContextCode("user_105");
        calendarEvent.setStartAt(Instant.now());
        calendarEvent.setEndAt(Instant.now());
        calendarEvents.add(calendarEvent);

        Calendar calendar = new Calendar();
        VEvent e = new VEvent();
        e.getProperties().add(new DtStart("20131010T101010Z"));
        e.getProperties().add(new DtEnd("20131010T091010Z"));
        calendar.getComponents().add(e);

        TenantRepository tenantRepository = mock(TenantRepository.class);
        OauthToken oauthToken = mock(OauthToken.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        CanvasTokenCreator canvasTokenCreator = mock(CanvasTokenCreator.class);
        ProgressService progressService = mock(ProgressService.class);
        CSVReader csvReader = mock(CSVReader.class);
        ImportEventService importEventService = mock(ImportEventService.class);
        CalendarWriter calendarWriter = mock(CalendarWriter.class);
        CanvasCalendarService canvasCalendarService = mock(CanvasCalendarService.class);
        DepositService depositService = mock(DepositService.class);
        CalendarBuilder calendarBuilder = mock(CalendarBuilder.class);
        CanvasApiFactory canvasApiFactory = mock(CanvasApiFactory.class);

        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDataMap map = new JobDataMap();
        map.put("calendar_import_id", 118L);
        map.put("time_zone", TimeZone.getDefault().toString());
        map.put("url", "http://bbc.co.uk");
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
        when(csvReader.parseCSV(any(), any(), any())).thenReturn(calendarEvents);
        when(calendarWriter.createCalendarEvent(any())).thenReturn(Optional.of(calendarEvent));
        when(depositService.deposit(any(), any())).thenReturn(new URL("https://bbc.co.uk"));
        when(calendarBuilder.build((InputStream) any())).thenReturn(calendar);
        doNothing().when(importEventService).eventCreated(any(), any(), any());
        doNothing().when(canvasCalendarService).resetRetryCounter(any());
        doNothing().when(importEventService).eventCreated(any(), any(), any());
        when(canvasApiFactory.getWriter(any(), any())).thenReturn(calendarWriter);

        icalImportJob.setTenantRepository(tenantRepository);
        icalImportJob.setUserRepository(userRepository);
        icalImportJob.setCalendarImportRepository(calendarImportRepository);
        icalImportJob.setCanvasTokenCreator(canvasTokenCreator);
        icalImportJob.setCanvasCalendarService(canvasCalendarService);
        icalImportJob.setProgressService(progressService);
        icalImportJob.setDepositService(depositService);
        icalImportJob.setCalendarBuilder(calendarBuilder);
        icalImportJob.setImportEventService(importEventService);
        icalImportJob.setCanvasApiFactory(canvasApiFactory);

        icalImportJob.execute(context);
        verify(calendarWriter, times(1)).createCalendarEvent(any());
    }

    @Test
    public void testCallNoTrigger() {
        IcalImportJob IcalImportJob = new IcalImportJob();

        JobExecutionContext context = mock(JobExecutionContext.class);
        assertThrows(Exception.class, () -> IcalImportJob.execute(context));
    }

    @Test
    public void testCallNoConfig() {
        IcalImportJob IcalImportJob = new IcalImportJob();
        JobExecutionContext context = mock(JobExecutionContext.class);

        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();

        when(context.getJobDetail()).thenReturn(job);
        when(context.getTrigger()).thenReturn(trigger);
        assertThrows(NullPointerException.class, () -> IcalImportJob.execute(context));
    }

    @Test
    public void testCallNoUser() {
        IcalImportJob IcalImportJob = new IcalImportJob();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);

        JobDetail job = JobBuilder.newJob(IcalImportJob.class).build();

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

        IcalImportJob.setUserRepository(userRepository);
        IcalImportJob.setCalendarImportRepository(calendarImportRepository);
        IcalImportJob.setTenantRepository(tenantRepository);

        assertThrows(NullPointerException.class, () -> IcalImportJob.execute(context));
    }

    @Test
    public void testCallNoCalendarImport() throws JOSEException, IOException, HeaderException {
        IcalImportJob icalImportJob = new IcalImportJob();
        CalendarImport calendarImport = new CalendarImport();
        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity("key").build();
        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();
        JobExecutionContext context = mock(JobExecutionContext.class);

        JobDataMap map = new JobDataMap();
        map.put("time_zone", TimeZone.getDefault().toString());
        map.put("url", "https://bbc.co.uk");

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        CanvasTokenCreator canvasTokenCreator = mock(CanvasTokenCreator.class);
        OauthToken oauthToken = mock(OauthToken.class);
        ProgressService progressService = mock(ProgressService.class);
        CalendarWriter calendarWriter = mock(CalendarWriter.class);
        CalendarReader calendarReader = mock(CalendarReader.class);
        CanvasCalendarService canvasCalendarService = mock(CanvasCalendarService.class);
        DepositService depositService = mock(DepositService.class);
        CanvasApiFactory canvasApiFactory = mock(CanvasApiFactory.class);

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
        when(depositService.deposit(any(), any())).thenReturn(new URL("https://bbc.co.uk"));
        when(canvasApiFactory.getReader(any(), any())).thenReturn(calendarReader);
        when(canvasApiFactory.getWriter(any(), any())).thenReturn(calendarWriter);

        icalImportJob.setTenantRepository(tenantRepository);
        icalImportJob.setUserRepository(userRepository);
        icalImportJob.setCalendarImportRepository(calendarImportRepository);
        icalImportJob.setCanvasTokenCreator(canvasTokenCreator);
        icalImportJob.setProgressService(progressService);
        icalImportJob.setCanvasCalendarService(canvasCalendarService);
        icalImportJob.setDepositService(depositService);
        icalImportJob.setCanvasApiFactory(canvasApiFactory);

        assertThrows(ClassCastException.class, () -> icalImportJob.execute(context));
    }
}