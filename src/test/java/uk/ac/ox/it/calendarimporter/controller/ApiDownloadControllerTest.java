package uk.ac.ox.it.calendarimporter.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.repo.ContextJobRepository;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiDownloadController.class)
@TestPropertySource(locations = "classpath:test.properties")
public class ApiDownloadControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ContextJobRepository contextJobRepository;

  @Test
  @WithMockUser(username = "canvas", roles = "LTI_USER")
  public void testDownloadLogfile() throws Exception {
    Tenant tenant = new Tenant();
    tenant.setName("test.instructure.com");

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
        .perform(MockMvcRequestBuilders.get("/app/log/1234/load"))
        .andExpect(status().isOk())
        .andExpect(content().string("Example Log"))
        .andExpect(content().contentType(MediaType.TEXT_PLAIN));
  }

  @Test
  @WithMockUser(username = "canvas", roles = "LTI_USER")
  public void testDownloadLogfileMissing() throws Exception {
    Tenant tenant = new Tenant();
    tenant.setName("test.instructure.com");

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
    mockMvc.perform(MockMvcRequestBuilders.get("/app/log/1234/load")).andExpect(status().is(404));
  }

  @Test
  public void testDownloadLogfileNotAuthenticated() throws Exception {
    Tenant tenant = new Tenant();
    tenant.setName("test.instructure.com");

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
    mockMvc.perform(MockMvcRequestBuilders.get("/app/log/1234/load")).andExpect(status().is(403));
  }

  @WithMockUser(username = "canvas", roles = "LTI_USER")
  @Test
  public void testDownloadLogfileWrongContext() throws Exception {
    Tenant tenant = new Tenant();
    tenant.setName("test.instructure.com");

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
    mockMvc.perform(MockMvcRequestBuilders.get("/app/log/1234/load")).andExpect(status().is(403));
  }

  @WithMockUser(username = "canvas", roles = "LTI_USER")
  @Test
  public void testDownloadLogfileWrongTenant() throws Exception {
    Tenant tenant = new Tenant();
    tenant.setName("test.instructure.com");

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
    mockMvc.perform(MockMvcRequestBuilders.get("/app/log/1234/load")).andExpect(status().is(403));
  }

  @WithMockUser(username = "canvas", roles = "LTI_USER")
  @Test
  public void testDownloadLogfileMissingContext() throws Exception {
    when(contextJobRepository.findById((long) 1234)).thenReturn(Optional.empty());
    mockMvc.perform(MockMvcRequestBuilders.get("/app/log/1234/load")).andExpect(status().is(404));
  }

  @WithMockUser(username = "canvas", roles = "LTI_USER")
  @Test
  public void testDownloadLogfileStillRunning() throws Exception {
    Tenant tenant = new Tenant();
    tenant.setName("test.instructure.com");

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
    mockMvc.perform(MockMvcRequestBuilders.get("/app/log/1234/load")).andExpect(status().is(404));
  }
}
