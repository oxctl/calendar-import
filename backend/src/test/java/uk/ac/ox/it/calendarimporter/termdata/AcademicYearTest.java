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

}