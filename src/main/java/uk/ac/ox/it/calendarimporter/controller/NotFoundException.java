package uk.ac.ox.it.calendarimporter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Failed to find resource.")
public class NotFoundException extends RuntimeException {
    private final String id;

    public NotFoundException() {
        this(null);
    }

    public NotFoundException(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
