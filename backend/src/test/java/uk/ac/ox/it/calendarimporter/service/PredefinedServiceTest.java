package uk.ac.ox.it.calendarimporter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.ac.ox.it.calendarimporter.termdata.AcademicYear;
import uk.ac.ox.it.calendarimporter.termdata.AcademicYearTerm;
import uk.ac.ox.it.calendarimporter.termdata.TermService;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class PredefinedServiceTest {

	private TermService termService;

	private PredefinedService predefinedService;

	@BeforeEach
	public void setUp() {
		termService = Mockito.mock(TermService.class);
		predefinedService = new PredefinedService(termService);
	}

	@Test
	public void testToFilename() {
		assertEquals("academic-year-21-22.csv", predefinedService.toFilename("21/22"));
		assertEquals("academic-year-thing.csv", predefinedService.toFilename("thing"));
	}

	@Test
	public void testFromFilename() {
		assertNull(predefinedService.fromFilename("unknown"));
		assertNull(predefinedService.fromFilename(""));
		assertNull(predefinedService.fromFilename("academic-year-21-22"));
		assertNull(predefinedService.fromFilename("prefix-academic-year-21-22.csv"));
		assertEquals("21/22", predefinedService.fromFilename("academic-year-21-22.csv"));
	}

	@Test
	public void testGetCalendars() {
		final AcademicYear year = new AcademicYear();
		year.setAcademicYear("21/22");
		year.setSortOrder(1);
		year.setStartDate(LocalDate.now());
		year.setEndDate(LocalDate.now());
		when(termService.getYears()).thenReturn(List.of(year));
		predefinedService.getCalendars();
	}

	@Test
	public void testLookupAcademicYear() {
		AcademicYearTerm term = new AcademicYearTerm();
		term.setAcademicYear("21/22");
		term.setAcademicTermCode("TT");
		when(termService.getTerms()).thenReturn(List.of(term));
		predefinedService.setValidTermCodes(List.of("TT"));
		List<AcademicYearTerm> academicYearTerms = predefinedService.lookupTerms("academic-year-21-22.csv");
		assertThat(academicYearTerms).hasSize(1);
	}

	@Test
	public void testLookupAcademicYearCodeNull() {
		List<AcademicYearTerm> academicYearTerms = predefinedService.lookupTerms("unknown");
		assertNull(academicYearTerms);
	}

	@Test
	public void testGenerateTerms() throws IOException {
		StringWriter writer = new StringWriter();
		AcademicYearTerm term = new AcademicYearTerm();
		term.setAcademicTermCode("TT");
		term.setStartDate(LocalDate.of(2000, 1, 8));
		term.setEndDate(LocalDate.of(2000, 1, 15));
		List<AcademicYearTerm> terms = List.of(term);

		predefinedService.generateTerms(writer, terms);

		String csv = writer.getBuffer().toString();
		assertThat(csv).isEqualToIgnoringWhitespace(
				"Title,Description,Date,Start,Duration" +
						// This is the week before term starts.
						"0th Week TT,,2000-01-01,00:00,167:59" +
						"1st Week TT,,2000-01-08,00:00,167:59" +
						// This is the week after term ends.
						"2nd Week TT,,2000-01-15,00:00,167:59"
		);

	}

	@Test
	public void testGetOrdinal(){
		assertThat(predefinedService.getOrdinal(0)).isEqualTo("th");
		assertThat(predefinedService.getOrdinal(1)).isEqualTo("st");
		assertThat(predefinedService.getOrdinal(2)).isEqualTo("nd");
		assertThat(predefinedService.getOrdinal(3)).isEqualTo("rd");
		assertThat(predefinedService.getOrdinal(4)).isEqualTo("th");
		assertThat(predefinedService.getOrdinal(10)).isEqualTo("th");
		assertThat(predefinedService.getOrdinal(11)).isEqualTo("th");
		assertThat(predefinedService.getOrdinal(12)).isEqualTo("th");
		assertThat(predefinedService.getOrdinal(13)).isEqualTo("th");
		assertThat(predefinedService.getOrdinal(14)).isEqualTo("th");
		assertThat(predefinedService.getOrdinal(21)).isEqualTo("st");
		assertThat(predefinedService.getOrdinal(22)).isEqualTo("nd");
		assertThat(predefinedService.getOrdinal(23)).isEqualTo("rd");
		assertThat(predefinedService.getOrdinal(24)).isEqualTo("th");
	}

}