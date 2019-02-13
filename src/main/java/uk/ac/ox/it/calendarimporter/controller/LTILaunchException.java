package uk.ac.ox.it.calendarimporter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception used to indicate something isn't right with the LTI launch.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class LTILaunchException extends RuntimeException {

    public LTILaunchException(String message) {
        super(message);
    }

}
