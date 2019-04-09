package uk.ac.ox.it.calendarimporter;

import org.springframework.security.access.AccessDeniedException;

/**
 * Exception that's thrown when a user looks to be blocking cookies on the redirect after they should have been logged in.
 */
public class NoCookiesException extends AccessDeniedException {
    public NoCookiesException(String msg, Throwable t) {
        super(msg, t);
    }

    public NoCookiesException(String msg) {
        super(msg);
    }
}
