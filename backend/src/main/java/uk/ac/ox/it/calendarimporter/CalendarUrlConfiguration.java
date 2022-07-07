package uk.ac.ox.it.calendarimporter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "calendar.url")
public class CalendarUrlConfiguration {

    /**
     * These are the predefined calendars that we expand.
     */
    private final Map<String, URLMapper.Config> predefined = new HashMap<>();

    public Map<String, URLMapper.Config> getPredefined() {
        return predefined;
    }
}
