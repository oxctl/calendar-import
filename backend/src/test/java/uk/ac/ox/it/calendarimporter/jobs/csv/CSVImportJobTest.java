package uk.ac.ox.it.calendarimporter.jobs.csv;

import com.nimbusds.jose.JOSEException;
import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.oauth.OauthToken;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.triggers.CalendarIntervalTriggerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.core.OAuth2Token;
import uk.ac.ox.it.calendarimporter.controller.ApiController;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.jobs.CleanoutJob;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.ImportedEventRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.service.CanvasTokenCreator;
import uk.ac.ox.it.calendarimporter.utils.TriggerUtils;


import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import static org.mockito.Mockito.*;

class CSVImportJobTest {

    @Test
    public void testValidCall() throws JobExecutionException, IOException, JOSEException {

        CSVImportJob csvImportJob = new CSVImportJob();
        User user = new User();
        CalendarImport calendarImport = new CalendarImport();


        Trigger trigger =
                TriggerBuilder.newTrigger()
                        .startNow()
                        .withIdentity("key")
//                        .withIdentity(
//                                TriggerUtils.toTriggerKey(
//                                        uuid.toString(), user.getTenant().getName(), user.getSubject()))
//                        .usingJobData(CanvasCalendarJob.TENANT_NAME, user.getTenant().getName())
//                        .usingJobData(CanvasCalendarJob.SUBJECT, user.getSubject())
//                        .usingJobData(CanvasCalendarJob.CALENDAR_IMPORT_ID, calendarImportId)
//                        .forJob(detail)
                        .build();

        JobDetail job = JobBuilder.newJob(CSVImportJob.class).build();

        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CalendarImportRepository calendarImportRepository = mock(CalendarImportRepository.class);
        CanvasTokenCreator canvasTokenCreator = mock(CanvasTokenCreator.class);
        ImportedEventRepository importedEventRepository = mock(ImportedEventRepository.class);

        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDataMap map = new JobDataMap();
        map.put("participantId", 321L);
        map.put("userCanvasId", 108L);
        map.put("calendar_import_id", 118L);
        when(context.getMergedJobDataMap()).thenReturn(map);
        when(context.getTrigger()).thenReturn(trigger);
        when(context.getJobDetail()).thenReturn(job);

        Tenant tenant = new Tenant();
        tenant.setName("test.instructure.com");
        tenant.setLtiClientId("5678");

        edu.ksu.canvas.oauth.OauthToken oAuthToken = new OauthToken() {
            @Override
            public String getAccessToken() {
                return null;
            }

            @Override
            public void refresh() {

            }
        };

        when(tenantRepository.findByName(any())).thenReturn(Optional.of(tenant));
        when(userRepository
                .findBySubjectAndTenantName(any(), any())).thenReturn(Optional.of(user));
        when(calendarImportRepository
                .findById(any())).thenReturn(Optional.of(calendarImport));
        when(canvasTokenCreator.getToken(any(), any())).thenReturn(oAuthToken);

        csvImportJob.setTenantRepository(tenantRepository);
        csvImportJob.setUserRepository(userRepository);
        csvImportJob.setCalendarImportRepository(calendarImportRepository);
        csvImportJob.setCanvasTokenCreator(canvasTokenCreator);
        csvImportJob.setImportedEventRepository(importedEventRepository);

        try (MockedStatic<TimeZone> utilities = Mockito.mockStatic(TimeZone.class)) {
            utilities.when(() -> TimeZone.getTimeZone(""))
                    .thenReturn(timeZone);
        }


        csvImportJob.execute(context);
    }
}