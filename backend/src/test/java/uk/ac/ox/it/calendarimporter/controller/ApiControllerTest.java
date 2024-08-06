package uk.ac.ox.it.calendarimporter.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.ac.ox.it.calendarimporter.ApiWebSecurityConfig;
import uk.ac.ox.it.calendarimporter.WebSecurityConfig;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.ContextJobRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.security.WithMockClaims;
import uk.ac.ox.it.calendarimporter.service.CourseSection;
import uk.ac.ox.it.calendarimporter.service.DepositService;
import uk.ac.ox.it.calendarimporter.service.ImportConfig;
import uk.ac.ox.it.calendarimporter.service.ImportService;
import uk.ac.ox.it.calendarimporter.service.UserService;

import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiController.class)
@TestPropertySource(locations = "classpath:test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({WebSecurityConfig.class, ApiWebSecurityConfig.class})
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImportService importService;

    @MockBean
    private TenantRepository tenantRepository;

    private Tenant tenant;

    private User user;

    private ContextJob contextJob;

    private CalendarImport calendarImport;

    @MockBean
    private DepositService depositService;

    @MockBean
    private UserService userService;

    @MockBean
    private ContextJobRepository contextJobRepository;

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

        user = new User();
        user.setId(789L);
        user.setSubject("subject");
        userRepository.save(user);

        calendarImport = new CalendarImport();
        calendarImport.setId(456);
        calendarImport.setUser(user);
        calendarImportRepository.save(calendarImport);

        contextJob = new ContextJob();
        contextJob.setId(123);
        contextJob.setCalendarImport(calendarImport);
        contextJobRepository.save(contextJob);
    }

    @Test
    public void testApiHasAuth() throws Exception {
        mockMvc.perform(get("/api/")).andExpect(status().is(401));
    }

    @Test
    @WithMockUser()
    public void testNoEndpoint() throws Exception {
        mockMvc.perform(get("/api/")).andExpect(status().is(404));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'course_home_sub_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1', 'canvas_user_id': '1'} }")
    public void testGetImportsContextTypeCourse() throws Exception {
        Page<ContextJob> contextJobs = new PageImpl<>(List.of(contextJob));
        when(importService.getJobs(any(), any(), any())).thenReturn(contextJobs);

        mockMvc.perform(get("/api/imports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$.content[0].id").value(123))
                .andExpect(jsonPath("$.content[0].calendarImport.id").value(456))
                .andExpect(jsonPath("$.content[0].calendarImport.user.id").value(789));

        verify(importService).getJobs("test.instructure.com", "course_1", Pageable.ofSize(20));
    }

    @Test
    // This is checking that we still work correctly with numeric values in the JSON.
    // Instructure did a change for this that changed ann numbers in the LTI JSON to strings.
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'course_home_sub_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': 1, 'canvas_user_id': 1} }")
    public void testGetImportsContextTypeCourseNumericIds() throws Exception {
        Page<ContextJob> contextJobs = new PageImpl<>(List.of(contextJob));
        when(importService.getJobs(any(), any(), any())).thenReturn(contextJobs);

        mockMvc.perform(get("/api/imports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$.content[0].id").value(123))
                .andExpect(jsonPath("$.content[0].calendarImport.id").value(456))
                .andExpect(jsonPath("$.content[0].calendarImport.user.id").value(789));

        verify(importService).getJobs("test.instructure.com", "course_1", Pageable.ofSize(20));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'course_home_sub_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1', 'canvas_user_id': '1'} }")
    public void testGetImportContextTypeCourse() throws Exception {
        when(importService.getJob(any(), any(), any())).thenReturn(Optional.of(contextJob));

        mockMvc.perform(get("/api/imports/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.calendarImport.id").value(456))
                .andExpect(jsonPath("$.calendarImport.user.id").value(789));

        verify(importService).getJob("test.instructure.com", "course_1", 123L);
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'course_home_sub_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1', 'canvas_user_id': '1'} }")
    public void testDeleteImportFound() throws Exception {
        when(userService.getUser(any(), any())).thenReturn(user);
        when(importService.getJob(any(), any(), any())).thenReturn(Optional.of(contextJob));

        mockMvc.perform(delete("/api/imports/123"))
                .andExpect(status().isAccepted());

        verify(importService).getJob("test.instructure.com", "course_1", 123L);
        verify(importService).deleteImport(456L, user);
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'course_home_sub_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1', 'canvas_user_id': '1'} }")
    public void testDeleteImportNotFound() throws Exception {
        when(userService.getUser(any(), any())).thenReturn(user);
        when(importService.getJob(any(), any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/imports/123"))
                .andExpect(status().isNotFound());

        verify(importService).getJob("test.instructure.com", "course_1", 123L);
        verify(importService, never()).deleteImport(any(), any());
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'course_home_sub_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1', 'canvas_user_id': '1'} }")
    public void testHideImportFound() throws Exception {
        when(userService.getUser(any(), any())).thenReturn(user);
        when(importService.getJob(any(), any(), any())).thenReturn(Optional.of(contextJob));

        mockMvc.perform(post("/api/imports/123/hide"))
                .andExpect(status().isNoContent());

        verify(importService).getJob("test.instructure.com", "course_1", 123L);
        verify(importService).hideImport(contextJob);
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'course_home_sub_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1', 'canvas_user_id': '1'} }")
    public void testHideImportNotFound() throws Exception {
        when(userService.getUser(any(), any())).thenReturn(user);
        when(importService.getJob(any(), any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/imports/123/hide"))
                .andExpect(status().isNotFound());

        verify(importService).getJob("test.instructure.com", "course_1", 123L);
        verify(importService, never()).hideImport(any());
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'course_home_sub_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1', 'canvas_user_id': '1', 'canvas_user_sis_id': 'sis_id_1'} }")
    public void testRunJob() throws Exception {
        when(userService.getUser(any(), any())).thenReturn(user);
        when(importService.getJob(any(), any(), any())).thenReturn(Optional.of(contextJob));
        when(depositService.deposit(any(), any())).thenReturn("path/to/file");
        when(importService.importNow(any())).thenReturn(contextJob);


        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/run")
                        .file(new MockMultipartFile("file", "originalFilename", "text/csv", "content".getBytes())))
                .andExpect(status().isOk());

        verify(importService).importNow(ArgumentMatchers.refEq(new ImportConfig(
                ImportType.CSV,
                "path/to/file",
                "originalFilename",
                user,
                "course_1",
                null,
                TimeZone.getDefault(),
                Utils.paramBuilder().courseId("1").sisUserId("sis_id_1").build()
        )));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'account_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_account_id': '1', 'canvas_user_id': '2', 'canvas_user_sis_id': 'sis_id_1'} }")
    public void testRunJobAccountPlacement() throws Exception {
        when(userService.getUser(any(), any())).thenReturn(user);
        when(importService.getJob(any(), any(), any())).thenReturn(Optional.of(contextJob));
        when(depositService.deposit(any(), any())).thenReturn("path/to/file");
        when(importService.importNow(any())).thenReturn(contextJob);


        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/run")
                        .file(new MockMultipartFile("file", "originalFilename", "text/csv", "content".getBytes())))
                .andExpect(status().isOk());

        verify(importService).importNow(ArgumentMatchers.refEq(new ImportConfig(
                ImportType.CSV,
                "path/to/file",
                "originalFilename",
                user,
                "account_1",
                null,
                TimeZone.getDefault(),
                Utils.paramBuilder().accountId("1").sisUserId("sis_id_1").build()
        )));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'course_home_sub_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1', 'canvas_user_id': '1', 'person_address_timezone': 'CST', 'canvas_user_sis_id': 'sis_id_1'} }")
    public void testRunJobWithTimezone() throws Exception {
        when(userService.getUser(any(), any())).thenReturn(user);
        when(importService.getJob(any(), any(), any())).thenReturn(Optional.of(contextJob));
        when(depositService.deposit(any(), any())).thenReturn("path/to/file");
        when(importService.importNow(any())).thenReturn(contextJob);


        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/run")
                        .file(new MockMultipartFile("file", "originalFilename", "text/csv", "content".getBytes())))
                .andExpect(status().isOk());

        verify(importService).importNow(ArgumentMatchers.refEq(new ImportConfig(
                ImportType.CSV,
                "path/to/file",
                "originalFilename",
                user,
                "course_1",
                null,
                TimeZone.getTimeZone("CST"),
                Utils.paramBuilder().courseId("1").sisUserId("sis_id_1").build())));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'course_home_sub_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1', 'canvas_user_id': '1', 'canvas_user_sis_id': 'sis_id_1'} }")
    public void testRunJobWithSection() throws Exception {
        when(userService.getUser(any(), any())).thenReturn(user);
        when(importService.getJob(any(), any(), any())).thenReturn(Optional.of(contextJob));
        when(depositService.deposit(any(), any())).thenReturn("path/to/file");
        when(importService.importNow(any())).thenReturn(contextJob);


        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/run")
                        .file(new MockMultipartFile("file", "originalFilename", "text/csv", "content".getBytes()))
                        .param("sectionId", "sectionId")
                        .param("sectionName", "sectionName"))
                .andExpect(status().isOk());

        verify(importService).importNow(ArgumentMatchers.refEq(new ImportConfig(
                ImportType.CSV,
                "path/to/file",
                "originalFilename",
                user,
                "course_1",
                new CourseSection("sectionId", "sectionName"),
                TimeZone.getDefault(),
                Utils.paramBuilder().courseId("1").sisUserId("sis_id_1").build())));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'course_home_sub_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1', 'canvas_user_id': '1', 'canvas_user_sis_id': 'sis_id_1'} }")
    public void testRunJobWithNullFilename() throws Exception {
        when(userService.getUser(any(), any())).thenReturn(user);
        when(importService.getJob(any(), any(), any())).thenReturn(Optional.of(contextJob));
        when(depositService.deposit(any(), any())).thenReturn("path/to/file");
        when(importService.importNow(any())).thenReturn(contextJob);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/run")
                        .file(new MockMultipartFile("file", null, "text/csv", "content".getBytes())))
                .andExpect(status().isOk());

        verify(importService).importNow(ArgumentMatchers.refEq(new ImportConfig(
                ImportType.CSV,
                "path/to/file",
                "file.csv",
                user,
                "course_1",
                null,
                TimeZone.getDefault(),
                Utils.paramBuilder().courseId("1").sisUserId("sis_id_1").build())));
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'course_home_sub_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1', 'canvas_user_id': '1'} }")
    public void testPurge() throws Exception {
        when(userService.getUser(any(), any())).thenReturn(user);

        mockMvc.perform(post("/api/purge"))
                .andExpect(status().isAccepted());

        verify(importService).purgeImports("course_1", "test.instructure.com", "subject", false);
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'course_home_sub_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1', 'canvas_user_id': '1'} }")
    public void testPurgeAll() throws Exception {
        when(userService.getUser(any(), any())).thenReturn(user);

        mockMvc.perform(post("/api/purge").param("all", "true"))
                .andExpect(status().isAccepted());

        verify(importService).purgeImports("course_1", "test.instructure.com", "subject", true);
    }

    @Test
    @WithMockClaims(claims = "{'aud': '5678', 'https://www.instructure.com/placement': 'course_home_sub_navigation', 'https://purl.imsglobal.org/spec/lti/claim/custom': {'canvas_course_id': '1', 'canvas_user_id': '1'} }")
    public void testPurgeNotAll() throws Exception {
        when(userService.getUser(any(), any())).thenReturn(user);

        mockMvc.perform(post("/api/purge").param("all", "false"))
                .andExpect(status().isAccepted());

        verify(importService).purgeImports("course_1", "test.instructure.com", "subject", false);
    }
}
