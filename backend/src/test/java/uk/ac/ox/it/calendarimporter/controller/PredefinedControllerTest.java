package uk.ac.ox.it.calendarimporter.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ox.it.calendarimporter.WebSecurityConfig;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.security.WithMockClaims;
import uk.ac.ox.it.calendarimporter.service.PredefinedService;
import uk.ac.ox.it.calendarimporter.termdata.AcademicYearTerm;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = PredefinedController.class)
@Import(WebSecurityConfig.class)
public class PredefinedControllerTest {
	
	@Autowired
	private MockMvc mockMvc;
	
	@MockBean
	private PredefinedService predefinedService;

	@MockBean
	private TenantRepository tenantRepository;
	
	@Test
	public void testCalendarsNoAuth() throws Exception {
		mockMvc.perform(get("/api/predefined"))
				.andExpect(status().isForbidden());
	}
	
	@Test
	@WithMockClaims
	public void testCalendarsEmpty() throws Exception {
		mockMvc.perform(get("/api/predefined"))
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().json("[]"));
	}

	@Test
	@WithMockClaims
	public void testCalendarSingle() throws Exception {
		Mockito.when(predefinedService.getCalendars()).thenReturn(List.of(
				new PredefinedCalendar("Title", "filename.csv", Map.of("key", "value"))
		));
		mockMvc.perform(get("/api/predefined"))
				.andExpect(status().is2xxSuccessful())
				.andExpect(jsonPath("$[0].title").value("Title"))
				.andExpect(jsonPath("$[0].filename").value("filename.csv"))
				.andExpect(jsonPath("$[0].properties").exists())
				;
	}
	
	@Test
	public void testFileNoAuth() throws Exception {
		mockMvc.perform(get("/api/predefined/filename.csv"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockClaims
	public void testFileMissing() throws Exception {
		mockMvc.perform(get("/api/predefined/filename.csv"))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockClaims
	public void testFileFound() throws Exception {
		List<AcademicYearTerm> terms =  List.of(new AcademicYearTerm());
		Mockito.when(predefinedService.lookupTerms("filename.csv"))
						.thenReturn(terms);
		mockMvc.perform(get("/api/predefined/filename.csv"))
				.andExpect(status().isOk());
	}
	
	@Test
	public void testPublicEndpoint() throws Exception {
		// This is an unauthenticated endpoint, check we don't require authentication
		List<AcademicYearTerm> terms =  List.of(new AcademicYearTerm());
		Mockito.when(predefinedService.lookupTerms("filename.csv"))
				.thenReturn(terms);
		mockMvc.perform(get("/public/predefined/filename.csv"))
				.andExpect(status().isOk());
	}
	
	@Test
	public void testLast2Years() throws Exception {
		// This is an unauthenticated endpoint, check we don't require authentication
		List<AcademicYearTerm> terms =  List.of(new AcademicYearTerm());
		Mockito.when(predefinedService.lookupTerms("last2years.csv"))
				.thenReturn(terms);
		mockMvc.perform(get("/public/predefined/last2years.csv"))
				.andExpect(status().isOk());
	}
}
