package uk.ac.ox.it.calendarimporter.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.controller.PredefinedCalendar;
import uk.ac.ox.it.calendarimporter.termdata.AcademicYearTerm;
import uk.ac.ox.it.calendarimporter.termdata.TermService;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This is just a placeholder service so that we can continue with development until we have
 * an official feed of dates.
 */
@Service
public class PredefinedService {

	// Just a safety so we don't ever have a runaway loop.
	public static final int MAX_WEEKS_PER_TERM = 1000;

	private static final String ACADEMIC_YEAR = "academic-year-";
	private static final String CSV = ".csv";

	private final TermService termService;

	public void setValidTermCodes(Collection<String> validTermCodes) {
		this.validTermCodes = validTermCodes;
	}

	// We only want to include some terms in the output
	@Value("${term.codes:MT,TT,HT}")
	private Collection<String> validTermCodes;

	public PredefinedService(TermService termService) {
		this.termService = termService;
	}
	
	String toFilename(String yearCode) {
		String filenameSafe = yearCode.replace("/", "-");
		return ACADEMIC_YEAR + filenameSafe+ CSV;
	}
	
	String fromFilename(String filename) {
		if (filename.startsWith(ACADEMIC_YEAR)) {
			if (filename.endsWith(CSV)) {
				String year = filename.substring(ACADEMIC_YEAR.length(), filename.length()- CSV.length());
				return year.replace('-', '/');
			}
		}
		return null;
	}

	/**
	 * Get all the predefined calendars available.
	 */
	public List<PredefinedCalendar> getCalendars() {
		return termService.getYears().stream().map((year) -> new PredefinedCalendar(
				year.getAcademicYear()+ " Academic Year",
				toFilename(year.getAcademicYear()),
				Map.of(
						"start", year.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
						"end", year.getEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
				)
		)).collect(Collectors.toList());
	}

	/**
	 * Find all the terms for a filename.
	 * @return A list of terms for the filename
	 */
	public List<AcademicYearTerm> lookupAcademicYear(String filename) {
		String yearCode = fromFilename(filename);
		if (yearCode == null) {
			return null;
		}
		List<AcademicYearTerm> terms = termService.getTerms();
		return terms.stream()
				.filter(term -> yearCode.equals(term.getAcademicYear()))
				.filter(term -> validTermCodes.contains(term.getAcademicTermCode()))
				.collect(Collectors.toList());
	}
	
	public void generateTerms(Writer writer, List<AcademicYearTerm> terms) throws IOException {
		CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT);
		csv.printRecord("Title", "Description", "Date", "Start", "Duration");
		for (AcademicYearTerm term : terms) {
			generateTerm(csv, term);
		}
	}
	
	void generateTerm(CSVPrinter csv, AcademicYearTerm term) throws IOException {
		DateTimeFormatter dateFormat = DateTimeFormatter.ISO_DATE;
		// We want to generate 0th week.
		LocalDate week = term.getStartDate().minus(1, ChronoUnit.WEEKS);
		// We want to generate 9th week.
		LocalDate endDate = term.getEndDate().plus(1, ChronoUnit.WEEKS);
		for(
				int weekNumber = 0;
				(weekNumber < MAX_WEEKS_PER_TERM) && week.isBefore(endDate);
				weekNumber++, week = week.plus(1, ChronoUnit.WEEKS)
		) {
			csv.printRecord(
					weekNumber+ getOrdinal(weekNumber)+ " Week "+ term.getAcademicTermCode(),
					"",
					dateFormat.format(week),
					"00:00",
					"167:59" // One week in hours(minus 1 minute)
			);
		}
	}

	/**
	 * Gets the suffix for a number.
	 * 
	 * @param number The number
	 * @return The suffix
	 */
	String getOrdinal(int number) {
		// The teens are different
		if ( (number % 100) / 10 == 1) {
			return "th";
		}
		switch (number % 10) {
			case 1: return "st";
			case 2: return "nd";
			case 3: return "rd";
			default: return "th";
		}
	}
}
