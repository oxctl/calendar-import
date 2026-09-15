package uk.ac.ox.it.calendarimporter.termdata;

import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.LocalDate;

/**
 * The Academic Year Term object that comes back from the reference data endpoint.
 */
@Data
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
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
