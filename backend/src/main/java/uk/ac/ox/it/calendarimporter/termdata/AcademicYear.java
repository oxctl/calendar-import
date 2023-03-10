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
		// We want the dates to be inclusive of the start and end dates.
		return !(date.isBefore(startDate) || date.isAfter(endDate));
	}
}
