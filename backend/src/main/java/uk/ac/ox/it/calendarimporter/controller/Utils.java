package uk.ac.ox.it.calendarimporter.controller;

import org.springframework.web.multipart.MultipartFile;

public class Utils {

    /**
     * Attempts to work out the import type based on the upload.
     *
     * @param upload The upload being submitted.
     * @return The type or null if it cannot be determined.
     */
    public static ImportType toImportType(MultipartFile upload) {
        String contentType = upload.getContentType();
        if (contentType != null) {
            switch (contentType) {
                case "text/calendar":
                    return ImportType.ICAL;
                case "text/csv":
                    return ImportType.CSV;
                default:
                    // Do nothing.
            }
        }
        String filename = upload.getOriginalFilename();
        return toImportType(filename);
    }

    /**
     * Attempts to work out the import type based on the file extension.
     *
     * @param filename The filename.
     * @return The type or null if it cannot be determined.
     */
    public static ImportType toImportType(String filename) {
        if (filename != null) {
            filename = filename.toLowerCase();
            if (filename.endsWith(".csv")) {
                return ImportType.CSV;
            }
            if (filename.endsWith(".ical")) {
                return ImportType.ICAL;
            }
            if (filename.endsWith(".ics")) {
                return ImportType.ICAL;
            }
            if (filename.endsWith(".icalendar")) {
                return ImportType.ICAL;
            }
        }
        return null;
    }

    public static String userIdToContext(Number userId){
        return "user_" + userId;
    }
}
