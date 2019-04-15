package uk.ac.ox.it.calendarimporter.jobs.csv;

/** When there's a problem finding the headers in the file. */
public class HeaderException extends Exception {

  public HeaderException(String message) {
    super(message);
  }
}
