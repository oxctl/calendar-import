package uk.ac.ox.it.calendarimporter.termdata;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDate;

/**
 * The Academic Year object that comes back from the reference data endpoint.
 */
@Data
@JsonNaming(PropertyNamingStrategy.UpperCamelCaseStrategy.class)
public class AcademicYear {
	private String academicYear;
	private Integer sortOrder;
	
	// These are all localtime so we don't have a timezone.
	private LocalDate startDate;
	private LocalDate endDate;
	private LocalDate validFrom;
	private LocalDate validTo;
}
