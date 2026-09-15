package uk.ac.ox.it.calendarimporter.termdata;

import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.LocalDate;

/**
 * The Academic Year object that comes back from the reference data endpoint.
 */
@Data
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class AcademicYear {
	private String academicYear;
	private Integer sortOrder;
	
	// These are all localtime, so we don't have a timezone.
	private LocalDate startDate;
	private LocalDate endDate;
	private LocalDate validFrom;
	private LocalDate validTo;

	/**
	 * Check if a date is inside this academic year.
	 * @param date The date to check against
	 * @return true if the supplied date is inside the start/end dates.
	 */
	public boolean isWithin(LocalDate date) {
		boolean goodData = startDate != null && endDate != null;
		// We want the dates to be inclusive of the start and end dates.
		return goodData && !(date.isBefore(startDate) || date.isAfter(endDate));
	}
}
