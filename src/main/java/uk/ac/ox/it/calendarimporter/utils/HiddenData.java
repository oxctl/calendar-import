package uk.ac.ox.it.calendarimporter.utils;

import java.nio.charset.Charset;
import java.util.Base64;

/** Utility methods for hiding data in the calendar description */
public class HiddenData {

  private static final String prefix =
      "<div class=\"calendar-data-1\" style=\"display: none;\" data-calendar=\"";
  private static final String suffix = "\"></div>";

  public static String extractHidden(String description) {
    if (description != null) {
      int start = description.lastIndexOf("<div class=\"calendar-data-1\"");
      if (start != -1) {
        int end = description.indexOf("</div>", start);
        return description.substring(start, end + 6);
      }
    }
    return null;
  }

  public static String insertHidden(String description, String hidden) {
    return description + hidden;
  }

  public static String toHidden(String uuid) {
    // We don't want the UUID
    String encoded = Base64.getEncoder().encodeToString(uuid.getBytes(Charset.forName("UTF-8")));
    StringBuilder comment = new StringBuilder();
    comment.append(prefix);
    comment.append(encoded);
    comment.append(suffix);
    return comment.toString();
  }

  public static String fromHidden(String comment) {
    if (comment != null && comment.startsWith(prefix) && comment.endsWith(suffix)) {
      byte[] decoded =
          Base64.getDecoder()
              .decode(comment.substring(prefix.length(), comment.length() - suffix.length()));
      String s = new String(decoded, Charset.forName("UTF-8"));
      return s;
    }
    return null;
  }
}
