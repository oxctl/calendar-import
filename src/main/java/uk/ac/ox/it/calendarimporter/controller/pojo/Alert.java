package uk.ac.ox.it.calendarimporter.controller.pojo;

import lombok.Data;

/**
 * Holder for alert messages. This ties into the Canvas UI classes so that the alerts are
 * appropriately styled.
 */
@Data
public class Alert {

  private final Type type;
  private final String message;

  public enum Type {
    INFO("info"),
    SUCCESS("check"),
    WARNING("warning"),
    ERROR("warning");

    private final String icon;

    Type(String icon) {
      this.icon = icon;
    }

    public String getIcon() {
      return icon;
    }

    public String getCss() {
      return name().toLowerCase();
    }
  }
}
