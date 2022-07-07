package uk.ac.ox.it.calendarimporter.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uk.ac.ox.it.calendarimporter.controller.ImportType;

import java.util.Map;
import java.util.TimeZone;

@AllArgsConstructor
@Builder
@Getter
public class ImportConfig {

    private final ImportType type;
    private final String url;
    private final String filename;
    private final Long userId;
    /**
     * The context in which the import is to be done. Typically this is the course although in the
     * future we may wish to support importing into a user's calendar. Example: course_123.
     */
    private final String context;

    private final CourseSection into;
    private final TimeZone timeZone;

    /**
     * Custom parameters that are made available to the job.
     */
    private final Map<String, String> parameters;
}
