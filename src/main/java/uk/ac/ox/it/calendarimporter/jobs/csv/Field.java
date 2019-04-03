package uk.ac.ox.it.calendarimporter.jobs.csv;

/** Holds the fields the importer supports. */
enum Field {
  TITLE("Title", true),
  DATE("Date", true),
  TIME("Start", true),
  DURATION("Duration", false),
  END_TIME("End", false),
  DESCRIPTION("Description", false),
  LOCATION("Location", false),
  ADDRESS("Address", false);

  private final String header;

  private final boolean required;

  Field(String header, boolean required) {
    this.header = header;
    this.required = required;
  }

  /**
   * The header value in the CSV.
   *
   * @return A String that can be used in the header.
   */
  public String getHeader() {
    return header;
  }

  /** @return Is this field required. */
  public boolean isRequired() {
    return required;
  }

  /** @return The header value for the CSV Parser. */
  public String toString() {
    return getHeader();
  }
}
