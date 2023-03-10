package uk.ac.ox.it.calendarimporter.termdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JsonTest
class AcademicYearTest {

	private JacksonTester<AcademicYear> json;
	
	@Autowired
	private ObjectMapper objectMapper;
	

	@BeforeEach
	public void setup() {
		JacksonTester.initFields(this, objectMapper);
	}

	@Test
	public void testWriteJson() throws IOException {
		AcademicYear year = new AcademicYear();
		year.setAcademicYear("1906/07");
		year.setEndDate(LocalDate.parse("1907-07-31"));
		year.setStartDate(LocalDate.parse("1906-08-01"));
		year.setValidFrom(LocalDate.parse("2020-01-01"));
		year.setSortOrder(1906);
		assertThat(json.read("academic-year.json")).isEqualTo(year);
	}
	
	@Test
	public void testIsWithin() {
		AcademicYear year = new AcademicYear();
		year.setStartDate(LocalDate.parse("1980-02-01")); // Start of Feb
		year.setEndDate(LocalDate.parse("1980-11-30")); // End of Nov
		
		// A long way outside
		assertFalse(year.isWithin(LocalDate.parse("1970-06-30")));
		assertFalse(year.isWithin(LocalDate.parse("1990-06-12")));
		
		// The day outside
		assertFalse(year.isWithin(LocalDate.parse("1980-01-30")));
		assertFalse(year.isWithin(LocalDate.parse("1980-12-01")));
		
		// The actual start/end
		assertTrue(year.isWithin(LocalDate.parse("1980-02-01")));
		assertTrue(year.isWithin(LocalDate.parse("1980-11-30")));
		
		// Somewhere in the middle
		assertTrue(year.isWithin(LocalDate.parse("1980-06-15")));
	}

}