package uk.ac.ox.it.calendarimporter.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ox.it.calendarimporter.service.PredefinedService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * This controller hands back the list of predefined calendars that can be imported.
 */
@RestController
@RequestMapping("/api/predefined")
public class PredefinedController {

	private final PredefinedService predefinedService;

	public PredefinedController(PredefinedService predefinedService) {
		this.predefinedService = predefinedService;
	}

	@GetMapping()
	public List<PredefinedCalendar> getCalendars() {
		return predefinedService.getCalendars();
	}
	
	@GetMapping("/{filename}")
	public void getCalendar(@PathVariable() String filename, HttpServletResponse response) throws IOException {
		PredefinedService.AcademicYear academicYear = predefinedService.lookupAcademicYear(filename);
		if (academicYear == null) {
			throw new NotFoundException("No calendar for "+ filename);
		}
		response.setContentType("text/csv");
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + filename + "\"");
		predefinedService.generateYear(response.getWriter(), academicYear);
	}

}
