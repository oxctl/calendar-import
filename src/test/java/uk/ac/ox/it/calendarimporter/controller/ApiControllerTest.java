package uk.ac.ox.it.calendarimporter.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.service.CanvasApiCreator;
import uk.ac.ox.it.calendarimporter.service.DepositService;
import uk.ac.ox.it.calendarimporter.service.ImportService;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiController.class)
class ApiControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ImportService importService;

    @MockBean
    private TenantRepository tenantRepository;
    
    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private DepositService depositService;
    
    @MockBean
    private CanvasApiCreator canvasApiCreator;
    
    @MockBean
    private CalendarImportRepository calendarImportRepository;

    @Test
    public void testApiHasAuth() throws Exception {
        mockMvc.perform(get("/api/")).andExpect(status().is(401));
    }

    @Test
    @WithMockUser()
    public void testNoEndpoint() throws Exception {
        mockMvc.perform(get("/api/")).andExpect(status().is(404));
    }
    

}