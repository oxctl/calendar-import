package uk.ac.ox.it.calendarimporter.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.ac.ox.it.calendarimporter.persistence.model.*;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.ContextJobRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.security.WithMockClaims;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {

        tenant = new Tenant();
        tenant.setName("test.instructure.com");
        tenant.setLtiClientId("5678");
        when(tenantRepository.findByLtiClientId("5678")).thenReturn(Optional.of(tenant));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
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
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
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
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '2'} }")
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
    @WithMockClaims(claims = "{'aud': 'wrong', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
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
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
    public void testDownloadLogfileMissingContext() throws Exception {
        when(contextJobRepository.findById((long) 1234)).thenReturn(Optional.empty());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/log/1234/load")).andExpect(status().is(404));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
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

    @Test
    // This is checking that we still work correctly with numeric values in the JSON.
    // Instructure did a change for this that changed ann numbers in the LTI JSON to strings.
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': 1} }")
    public void testDownloadLogfileStillRunningWithNumericId() throws Exception {

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


    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_user_id': '1'} }")
    public void testDownloadLogfileByCalendarImportId() throws Exception {
        User user = new User();
        user.setId(1L);
        userRepository.save(user);


        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setId(2);
        calendarImport.setContext("user_1");
        calendarImport.setLoad(progress);
        calendarImportRepository.save(calendarImport);
        when(calendarImportRepository.findById(2L)).thenReturn(Optional.of(calendarImport));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/log/2/loadByCalendarImportId"))
                .andExpect(status().isOk())
                .andExpect(content().string("Example Log"))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_user_id': '1'} }")
    public void testDownloadLogfileByCalendarImportIdNotFound() throws Exception {
        User user = new User();
        user.setId(1L);
        userRepository.save(user);


        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setId(2);
        calendarImport.setContext("user_1");
        calendarImport.setLoad(progress);
        calendarImportRepository.save(calendarImport);
        when(calendarImportRepository.findById(2L)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/log/2/loadByCalendarImportId"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_user_id': '1'} }")
    public void testDownloadLogfileByCalendarImportIdContextsNotMatch() throws Exception {
        User user = new User();
        user.setId(1L);
        userRepository.save(user);


        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setId(2);
        calendarImport.setContext("wrong");
        calendarImport.setLoad(progress);
        calendarImportRepository.save(calendarImport);
        when(calendarImportRepository.findById(2L)).thenReturn(Optional.of(calendarImport));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/log/2/loadByCalendarImportId"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
    public void testDownloadDeleteLogfile() throws Exception {
        User user = new User();
        user.setId(1L);
        userRepository.save(user);


        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setId(2);
        calendarImport.setContext("user_1");
        calendarImport.setDelete(progress);
        calendarImportRepository.save(calendarImport);
        when(calendarImportRepository.findById(2L)).thenReturn(Optional.of(calendarImport));

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        when(contextJobRepository.findById((long) 1234)).thenReturn(Optional.of(job));

        mockMvc
                .perform(MockMvcRequestBuilders.get("/api/log/1234/delete"))
                .andExpect(status().isOk())
                .andExpect(content().string("Example Log"))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
    public void testDownloadDeleteLogfileContextJobNotFound() throws Exception {
        User user = new User();
        user.setId(1L);
        userRepository.save(user);


        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setId(2);
        calendarImport.setContext("user_1");
        calendarImport.setDelete(progress);
        calendarImportRepository.save(calendarImport);
        when(calendarImportRepository.findById(2L)).thenReturn(Optional.of(calendarImport));

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        mockMvc
                .perform(MockMvcRequestBuilders.get("/api/log/1234/delete"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
    public void testDownloadMediaTypeDefault() throws Exception {
        User user = new User();
        user.setId(1L);
        userRepository.save(user);


        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setId(2);
        calendarImport.setContext("user_1");
        calendarImport.setLoad(progress);
        calendarImport.setUrl(getClass().getResource("example.html").toExternalForm());
        calendarImport.setType(ImportType.TEST);
        calendarImport.setFilename("filename");

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        calendarImportRepository.save(calendarImport);
        when(calendarImportRepository.findById(2L)).thenReturn(Optional.of(calendarImport));
        when(contextJobRepository.findById(1234L)).thenReturn(Optional.of(job));

        String expected = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Example</title>
                </head>
                <body>

                </body>
                </html>""";

        mockMvc.perform(MockMvcRequestBuilders.get("/api/download/1234"))
                .andExpect(status().isOk())
                .andExpect(content().string(expected))
                .andExpect(content().contentType(new MediaType("application", "binary")))
                .andExpect(header().stringValues("Content-Disposition", "attachment; filename=\"filename\""));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
    public void testDownloadMediaTypeICal() throws Exception {
        User user = new User();
        user.setId(1L);
        userRepository.save(user);


        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setId(2);
        calendarImport.setContext("user_1");
        calendarImport.setLoad(progress);
        calendarImport.setUrl(getClass().getResource("example.html").toExternalForm());
        calendarImport.setType(ImportType.ICAL);
        calendarImport.setFilename("filename");

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        calendarImportRepository.save(calendarImport);
        when(calendarImportRepository.findById(2L)).thenReturn(Optional.of(calendarImport));
        when(contextJobRepository.findById(1234L)).thenReturn(Optional.of(job));

        String expected = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Example</title>
                </head>
                <body>

                </body>
                </html>""";

        mockMvc.perform(MockMvcRequestBuilders.get("/api/download/1234"))
                .andExpect(status().isOk())
                .andExpect(content().string(expected))
                .andExpect(content().contentType(new MediaType("text", "calendar")));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
    public void testDownloadMediaTypeCSV() throws Exception {
        User user = new User();
        user.setId(1L);
        userRepository.save(user);


        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setId(2);
        calendarImport.setContext("user_1");
        calendarImport.setLoad(progress);
        calendarImport.setUrl(getClass().getResource("example.html").toExternalForm());
        calendarImport.setType(ImportType.CSV);
        calendarImport.setFilename("filename");

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        calendarImportRepository.save(calendarImport);
        when(calendarImportRepository.findById(2L)).thenReturn(Optional.of(calendarImport));
        when(contextJobRepository.findById(1234L)).thenReturn(Optional.of(job));

        String expected = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Example</title>
                </head>
                <body>

                </body>
                </html>""";

        mockMvc.perform(MockMvcRequestBuilders.get("/api/download/1234"))
                .andExpect(status().isOk())
                .andExpect(content().string(expected))
                .andExpect(content().contentType(new MediaType("text", "csv")));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
    public void testDownloadContextJobNotFound() throws Exception {
        User user = new User();
        user.setId(1L);
        userRepository.save(user);


        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setId(2);
        calendarImport.setContext("user_1");
        calendarImport.setLoad(progress);
        calendarImport.setUrl(getClass().getResource("example.html").toExternalForm());
        calendarImport.setType(ImportType.TEST);
        calendarImport.setFilename("filename");

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        calendarImportRepository.save(calendarImport);
        when(calendarImportRepository.findById(2L)).thenReturn(Optional.of(calendarImport));
        when(contextJobRepository.findById(1234L)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/download/1234"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
    public void testDownloadContextsNotMatch() throws Exception {
        User user = new User();
        user.setId(1L);
        userRepository.save(user);


        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setId(2);
        calendarImport.setContext("user_1");
        calendarImport.setLoad(progress);
        calendarImport.setUrl(getClass().getResource("example.html").toExternalForm());
        calendarImport.setType(ImportType.TEST);
        calendarImport.setFilename("filename");

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("wrong");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        calendarImportRepository.save(calendarImport);
        when(calendarImportRepository.findById(2L)).thenReturn(Optional.of(calendarImport));
        when(contextJobRepository.findById(1234L)).thenReturn(Optional.of(job));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/download/1234"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
    public void testDownloadLogFileNull() throws Exception {
        User user = new User();
        user.setId(1L);
        userRepository.save(user);

        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setId(2);
        calendarImport.setContext("user_1");
        calendarImport.setLoad(progress);
        calendarImport.setUrl(null);
        calendarImport.setType(ImportType.TEST);
        calendarImport.setFilename("filename");

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        calendarImportRepository.save(calendarImport);
        when(calendarImportRepository.findById(2L)).thenReturn(Optional.of(calendarImport));
        when(contextJobRepository.findById(1234L)).thenReturn(Optional.of(job));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/download/1234"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
    public void testDownloadLogFileEmpty() throws Exception {
        User user = new User();
        user.setId(1L);
        userRepository.save(user);

        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setId(2);
        calendarImport.setContext("user_1");
        calendarImport.setLoad(progress);
        calendarImport.setUrl("");
        calendarImport.setType(ImportType.TEST);
        calendarImport.setFilename("filename");

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        calendarImportRepository.save(calendarImport);
        when(calendarImportRepository.findById(2L)).thenReturn(Optional.of(calendarImport));
        when(contextJobRepository.findById(1234L)).thenReturn(Optional.of(job));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/download/1234"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1'} }")
    public void testDownloadNoCalImportFilename() throws Exception {
        User user = new User();
        user.setId(1L);
        userRepository.save(user);


        JobProgress progress = new JobProgress();
        String logfile = getClass().getResource("log.txt").toExternalForm();
        progress.setLogfile(logfile);

        CalendarImport calendarImport = new CalendarImport();
        calendarImport.setId(2);
        calendarImport.setContext("user_1");
        calendarImport.setLoad(progress);
        calendarImport.setUrl(getClass().getResource("example.html").toExternalForm());
        calendarImport.setType(ImportType.TEST);
        calendarImport.setFilename(null);

        ContextJob job = new ContextJob();
        job.setId(1234);
        job.setContext("course_1");
        job.setTenant(tenant);
        job.setCalendarImport(calendarImport);

        calendarImportRepository.save(calendarImport);
        when(calendarImportRepository.findById(2L)).thenReturn(Optional.of(calendarImport));
        when(contextJobRepository.findById(1234L)).thenReturn(Optional.of(job));

        String expected = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Example</title>
                </head>
                <body>

                </body>
                </html>""";

        mockMvc.perform(MockMvcRequestBuilders.get("/api/download/1234"))
                .andExpect(status().isOk())
                .andExpect(content().string(expected))
                .andExpect(content().contentType(new MediaType("application", "binary")))
                .andExpect(header().doesNotExist("Content-Disposition"));
    }
}