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
class AcademicYearTermTest {

	private JacksonTester<AcademicYearTerm> json;
	
	@Autowired
	private ObjectMapper objectMapper;
	

	@BeforeEach
	public void setup() {
		JacksonTester.initFields(this, objectMapper);
	}

	@Test
	public void testWriteJson() throws IOException {
		AcademicYearTerm yearTerm = new AcademicYearTerm();
		yearTerm.setAcademicYearTermCode("LV2035/36");
		yearTerm.setAcademicYear("2035/36");
		yearTerm.setAcademicTermCode("LV");
		yearTerm.setAcademicTermName("Long Vacation");
		yearTerm.setStartDate(LocalDate.parse("2036-06-23"));
		yearTerm.setEndDate(LocalDate.parse("2036-10-05"));
		yearTerm.setValidFrom(LocalDate.parse("2020-01-01"));
		yearTerm.setSortOrder(186);
		assertThat(json.read("academic-year-term.json")).isEqualTo(yearTerm);
	}

}