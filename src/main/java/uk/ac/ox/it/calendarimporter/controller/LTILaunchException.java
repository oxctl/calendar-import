package uk.ac.ox.it.calendarimporter.controller;

/**
 * Exception used to indicate something isn't right with the LTI launch. We can't use
 * {@link @ResponseStatus} to map it to a 400 error as that's only available if you're inside Spring
 * MVC world, which we aren't yet.
 */
public class LTILaunchException extends RuntimeException {

  public LTILaunchException(String message) {
    super(message);
  }
}
