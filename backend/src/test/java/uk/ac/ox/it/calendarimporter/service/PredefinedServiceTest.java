package uk.ac.ox.it.calendarimporter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.ac.ox.it.calendarimporter.termdata.AcademicYear;
import uk.ac.ox.it.calendarimporter.termdata.AcademicYearTerm;
import uk.ac.ox.it.calendarimporter.termdata.TermService;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
		// This term has bad data and should just be ignored.
		AcademicYearTerm nullTerm = new AcademicYearTerm();
		when(termService.getTerms()).thenReturn(List.of(term, nullTerm));
		predefinedService.setValidTermCodes(List.of("TT"));
		List<AcademicYearTerm> academicYearTerms = predefinedService.lookupTerms("academic-year-21-22.csv");
		assertThat(academicYearTerms).hasSize(1);
	}

	@Test
	public void testLookupTermsDynamic() {
		AcademicYear y2020 = new AcademicYear();
		y2020.setAcademicYear("y2020");
		y2020.setStartDate(LocalDate.parse("2020-01-01"));
		y2020.setEndDate(LocalDate.parse("2020-12-31"));
		AcademicYear y2021 = new AcademicYear();
		y2021.setAcademicYear("y2021");
		y2021.setStartDate(LocalDate.parse("2021-01-01"));
		y2021.setEndDate(LocalDate.parse("2021-12-31"));
		AcademicYear y2022 = new AcademicYear();
		y2022.setAcademicYear("y2022");
		y2022.setStartDate(LocalDate.parse("2022-01-01"));
		y2022.setEndDate(LocalDate.parse("2022-12-31"));
		AcademicYear y2023 = new AcademicYear();
		y2023.setAcademicYear("y2023");
		y2023.setStartDate(LocalDate.parse("2023-01-01"));
		y2023.setEndDate(LocalDate.parse("2023-12-31"));
		// This year has no data and should be ignored.
		AcademicYear nullYear = new AcademicYear();
		
		when(termService.getYears()).thenReturn(List.of(y2020, y2021, y2022, y2023, nullYear));
		predefinedService.setClock(Clock.fixed(Instant.parse("2021-02-15T00:00:00.00Z"), ZoneId.of("UTC")));
		predefinedService.setValidTermCodes(List.of("TT"));
		
		// No terms so nothing should be found
		{
			List<AcademicYearTerm> academicYearTerms = predefinedService.lookupTerms("last2years.csv");
			assertThat(academicYearTerms).isEmpty();
		}
		
		AcademicYearTerm y2020Jan = new AcademicYearTerm();
		y2020Jan.setAcademicTermName("y2020Jan");
		y2020Jan.setAcademicYear("y2020");
		y2020Jan.setAcademicTermCode("TT");
		AcademicYearTerm y2021Jan = new AcademicYearTerm();
		y2021Jan.setAcademicTermName("y2021Jan");
		y2021Jan.setAcademicYear("y2021");
		y2021Jan.setAcademicTermCode("TT");
		AcademicYearTerm y2022Jan = new AcademicYearTerm();
		y2022Jan.setAcademicTermName("y2022Jan");
		y2022Jan.setAcademicYear("y2022");
		y2022Jan.setAcademicTermCode("TT");
		AcademicYearTerm y2023Jan = new AcademicYearTerm();
		y2023Jan.setAcademicTermName("y2023Jan");
		y2023Jan.setAcademicYear("y2023");
		y2023Jan.setAcademicTermCode("TT");
		
		// Only one term
		when(termService.getTerms()).thenReturn(List.of(y2020Jan));

		// No wrong year so nothing should be found
		{
			List<AcademicYearTerm> academicYearTerms = predefinedService.lookupTerms("last2years.csv");
			assertThat(academicYearTerms).isEmpty();
		}
		
		when(termService.getTerms()).thenReturn(List.of(y2020Jan, y2021Jan, y2022Jan, y2023Jan));

		// Now we should get the term for now and next year
		{
			List<AcademicYearTerm> academicYearTerms = predefinedService.lookupTerms("last2years.csv");
			assertThat(academicYearTerms).contains(y2021Jan, y2022Jan);
		}
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