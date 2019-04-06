package uk.ac.ox.it.calendarimporter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OAuth2AccessDeniedException extends Exception {

    public String getReloginUrl() {
        return reloginUrl;
    }

    private final String reloginUrl;

    public OAuth2AccessDeniedException(String s, String reloginUrl) {
        super(s);
        this.reloginUrl = reloginUrl;
    }
}
