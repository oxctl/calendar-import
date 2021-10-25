package uk.ac.ox.it.calendarimporter.termdata;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDate;

/**
 * The Academic Year Term object that comes back from the reference data endpoint.
 */
@Data
@JsonNaming(PropertyNamingStrategy.UpperCamelCaseStrategy.class)
public class AcademicYearTerm {
	private String academicYearTermCode;
	private String academicYear;
	private String academicTermCode;
	private String academicTermName;
	
	private Integer sortOrder;
	
	private LocalDate startDate;
	private LocalDate endDate;
	private LocalDate validFrom;
	private LocalDate validTo;
}
