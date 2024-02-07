package uk.ac.ox.it.calendarimporter.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ox.it.calendarimporter.service.PredefinedService;
import uk.ac.ox.it.calendarimporter.termdata.AcademicYearTerm;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * This controller hands back the list of predefined calendars that can be imported.
 */
@RestController
// We want to allow access to this controller through both an authenticated endpoint (/api, older method used by the tool)
// and a public endpoint (/public, embedded in documentation and directly accessed by users) 
@RequestMapping({"/api/predefined", "/public/predefined"})
public class PredefinedController {

	private final PredefinedService predefinedService;

	public PredefinedController(PredefinedService predefinedService) {
		this.predefinedService = predefinedService;
	}

	/**
	 * Gets a list of all the predefined calendars that are available.
	 */
	@GetMapping()
	public List<PredefinedCalendar> getCalendars() {
		return predefinedService.getCalendars();
	}

	/**
	 * Gets an actual pre-defined calendar.
	 */
	@GetMapping("/{filename}")
	public void getFile(@PathVariable() String filename, HttpServletResponse response) throws IOException {
		List<AcademicYearTerm> terms = predefinedService.lookupTerms(filename);
		if (terms == null || terms.isEmpty()) {
			throw new NotFoundException("No calendar for "+ filename);
		}
		response.setContentType("text/csv");
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + filename + "\"");
		predefinedService.generateTerms(response.getWriter(), terms);
	}

}
