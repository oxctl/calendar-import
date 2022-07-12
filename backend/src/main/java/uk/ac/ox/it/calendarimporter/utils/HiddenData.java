package uk.ac.ox.it.calendarimporter.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility methods for hiding data in the calendar description.
 */
public class HiddenData {

    private static final String prefix =
            "<div class=\"calendar-data-1\" style=\"display: none;\" data-calendar=\"";
    private static final String suffix = "\"></div>";

    /**
     * This returns the description without any hidden data.
     * @param description The description possibly containing hidden data.
     * @return The description that was originally used (without the hidden data).
     */
    public static String removeHidden(String description) {
        if (description != null) {
            int start = description.lastIndexOf(prefix);
            if (start != -1) {
                int end = description.indexOf(suffix, start);
                if (end != -1) {
                    return description.substring(0, start)+ description.substring(end+suffix.length());
                }
            }
        }
        return description;
    }
    public static String extractHidden(String description) {
        if (description != null) {
            int start = description.lastIndexOf(prefix);
            if (start != -1) {
                int end = description.indexOf(suffix, start);
                if (end != -1) {
                    return description.substring(start, end + suffix.length());
                }
            }
        }
        return null;
    }

    public static String insertHidden(String description, String hidden) {
        return description + hidden;
    }

    public static String toHidden(String data) {
        // We don't want the UUID
        String encoded = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
        return prefix + encoded + suffix;
    }

    public static String fromHidden(String comment) {
        if (comment != null && comment.startsWith(prefix) && comment.endsWith(suffix)) {
            byte[] decoded =
                    Base64.getDecoder()
                            .decode(comment.substring(prefix.length(), comment.length() - suffix.length()));
            return new String(decoded, StandardCharsets.UTF_8);
        }
        return null;
    }
}
