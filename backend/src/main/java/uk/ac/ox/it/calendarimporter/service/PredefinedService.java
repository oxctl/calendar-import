package uk.ac.ox.it.calendarimporter.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.controller.PredefinedCalendar;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * This is just a placeholder service so that we can continue with development until we have
 * an official feed of dates.
 */
@Service
public class PredefinedService {

	// Just a safety so we don't ever have a runaway loop.
	public static final int MAX_WEEKS_PER_TERM = 1000;

	public List<PredefinedCalendar> getCalendars() {
		return List.of(
				new PredefinedCalendar("2021/22 Academic Year", "academic-year-2021.csv", Map.of("year", "2021", "start", "2021-09-01", "end", "2022-08-30")),
				new PredefinedCalendar("2022/23 Academic Year", "academic-year-2022.csv", Map.of("year", "2022", "start", "2022-09-01", "end", "2023-08-30"))
		);
	}

	/**
	 * This gets the actual predefined file.
	 * @param filename The filename to lookup.
	 * @return Either a reference to a file to import or null if it doesn't exist or can't be generated.
	 */
	public File getFile(String filename) {
		return null;
	}
	
	private static Map<String, AcademicYear> years = Map.of(
			"academic-year-2021.csv", new AcademicYear(Year.of(2021), List.of(
					new Term("Michaelmas", LocalDate.of(2021, 10, 10), LocalDate.of(2021, 12, 4)),
					new Term("Hilary", LocalDate.of(2022, 1, 16), LocalDate.of(2022, 3, 12)),
					new Term("Trinity", LocalDate.of(2022, 4, 24), LocalDate.of(2022, 6, 18))
			)),
			"academic-year-2022.csv", new AcademicYear(Year.of(2022), List.of(
					new Term("Michaelmas", LocalDate.of(2021, 10, 9), LocalDate.of(2021, 12, 3)),
					new Term("Hilary", LocalDate.of(2022, 1, 15), LocalDate.of(2022, 3, 11)),
					new Term("Trinity", LocalDate.of(2022, 4, 23), LocalDate.of(2022, 6, 17))
			))
	);

	public AcademicYear lookupAcademicYear(String filename) {
		return years.get(filename);
	}

	public static class AcademicYear {
		public final Year year;
		public final List<Term> terms;
		
		private static DateTimeFormatter START_YEAR = DateTimeFormatter.ofPattern("yyyy");
		private static DateTimeFormatter END_YEAR = DateTimeFormatter.ofPattern("yy");

		public AcademicYear(Year year, List<Term> terms) {
			this.year = year;
			this.terms = terms;
		}
		
		public String toYears() {
			return
				 START_YEAR.format(year) +
				 "-" +
				 END_YEAR.format(year.plus(1, ChronoUnit.YEARS));
		}
	}
	
	public static class Term {
		public final String name;
		public final LocalDate start;
		public final LocalDate end;

		public Term(String name, LocalDate start, LocalDate end) {
			this.name = name;
			this.start = start;
			this.end = end;
		}
	}
	
	enum EventType {WEEKLY_SUNDAY_DAY, WEEKLY_SUNDAY_WEEK}
	
	public void generateYear(Writer writer, AcademicYear academicYear) throws IOException {
		CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT);
		csv.printRecord("Title", "Description", "Date", "Start");
		generateYear(csv, academicYear);
	}
	
	public void generateYear(CSVPrinter csv, AcademicYear year) throws IOException {
		for (Term term : year.terms) {
			generateTerm(csv, year, term);
		}
	}
	
	public void generateTerm(CSVPrinter csv, AcademicYear year, Term term) throws IOException {
		DateTimeFormatter dateFormat = DateTimeFormatter.ISO_DATE;
		LocalDate week = term.start;
		int weekNumber = 1;
		do {
			csv.printRecord("Week "+ weekNumber, year.toYears(),dateFormat.format(week), "00:00");
			weekNumber++;
			week = week.plus(1, ChronoUnit.WEEKS);
		} while (weekNumber < MAX_WEEKS_PER_TERM && week.isBefore(term.end));
	}
}
