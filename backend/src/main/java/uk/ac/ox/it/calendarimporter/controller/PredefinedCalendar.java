package uk.ac.ox.it.calendarimporter.controller;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * A calendar that the user doesn't have to upload.
 */
public class PredefinedCalendar {

	// The title that is displayed to the user.
	private final String title;
	// The filename of the calendar, this must be unique and must have an extension we can detect (csv/ical)
	private final String filename;
	// Additional properties for the calendar, this can be used for additional metadata.
	private final Map<String, String> properties;
	
	public PredefinedCalendar(String title, String filename) {
		this(title, filename, Collections.emptyMap());
	}

	public PredefinedCalendar(String title, String filename, Map<String, String> properties) {
		Objects.requireNonNull(title);
		Objects.requireNonNull(filename);
		Objects.requireNonNull(properties);
		this.title = title;
		this.filename = filename;
		this.properties = Map.copyOf(properties);
	}

	public String getTitle() {
		return title;
	}

	public String getFilename() {
		return filename;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PredefinedCalendar that = (PredefinedCalendar) o;
		return title.equals(that.title) && filename.equals(that.filename) && properties.equals(that.properties);
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, filename, properties);
	}
}
