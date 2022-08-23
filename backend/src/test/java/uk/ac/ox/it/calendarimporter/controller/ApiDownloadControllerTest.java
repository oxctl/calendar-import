package uk.ac.ox.it.calendarimporter.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.ContextJobRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.security.WithMockClaims;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiDownloadController.class)
@TestPropertySource(locations = "classpath:test.properties")
public class ApiDownloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContextJobRepository contextJobRepository;
    
    @MockBean
    private TenantRepository tenantRepository;
    private Tenant tenant;

    @MockBean
    private CalendarImportRepository calendarImportRepository;

    @BeforeEach
    public void setUp() {

        tenant = new Tenant();
        tenant.setName("test.instructure.com");
        tenant.setLtiClientId("5678");
        when(tenantRepository.findByLtiClientId("5678")).thenReturn(Optional.of(tenant));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': 1} }")
    public void testDownloadLogfile() throws Exception {

        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setLoad(progress);

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        when(contextJobRepository.findById((long) 1234)).thenReturn(Optional.of(job));
        mockMvc
                .perform(MockMvcRequestBuilders.get("/api/log/1234/load"))
                .andExpect(status().isOk())
                .andExpect(content().string("Example Log"))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': 1} }")
    public void testDownloadLogfileMissing() throws Exception {
        JobProgress progress = new JobProgress();
        String logfile = "file:///doesnotexist.txt";
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setLoad(progress);

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        when(contextJobRepository.findById((long) 1234)).thenReturn(Optional.of(job));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/log/1234/load")).andExpect(status().is(404));
    }

    @Test
    public void testDownloadLogfileNotAuthenticated() throws Exception {
        JobProgress progress = new JobProgress();
        String logfile = "file:///doesnotexist.txt";
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setLoad(progress);

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        when(contextJobRepository.findById((long) 1234)).thenReturn(Optional.of(job));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/log/1234/load")).andExpect(status().is(401));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': 2} }")
    public void testDownloadLogfileWrongContext() throws Exception {
        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setLoad(progress);

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        when(contextJobRepository.findById((long) 1234)).thenReturn(Optional.of(job));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/log/1234/load")).andExpect(status().is(403));
    }

    @Test
    @WithMockClaims(claims = "{'aud': 'wrong', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': 1} }")
    public void testDownloadLogfileWrongTenant() throws Exception {
        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setLoad(progress);

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        when(contextJobRepository.findById((long) 1234)).thenReturn(Optional.of(job));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/log/1234/load")).andExpect(status().is(404));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': 1} }")
    public void testDownloadLogfileMissingContext() throws Exception {
        when(contextJobRepository.findById((long) 1234)).thenReturn(Optional.empty());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/log/1234/load")).andExpect(status().is(404));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': 1} }")
    public void testDownloadLogfileStillRunning() throws Exception {

        JobProgress progress = new JobProgress();
        // No log file yet.

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setLoad(progress);

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        when(contextJobRepository.findById((long) 1234)).thenReturn(Optional.of(job));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/log/1234/load")).andExpect(status().is(404));
    }
}
