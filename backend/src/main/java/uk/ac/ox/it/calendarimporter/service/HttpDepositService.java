package uk.ac.ox.it.calendarimporter.service;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * This is used to allow retrieval of files from remove services.
 */
@Service
@Qualifier("implementation")
@Order(200)
public class HttpDepositService implements DepositService {
    @Override
    public String deposit(File file, Type type) throws IOException {
        throw new UnsupportedOperationException("No uploading for http supported.");
    }

    @Override
    public InputStream getInputStream(String deposit, Map<String, String> parameters) throws IOException {
        if (!canHandle(deposit)) {
            // Put the double check in here so that this doesn't ever become the vector to allowing file://
            // URLs to be accessed.
            throw new IllegalArgumentException("Only HTTP/HTTPS supported.");
        }
        StringSubstitutor substitutor = new StringSubstitutor(parameters);
        String updatedUrl = substitutor.replace(deposit);
        URLConnection urlConnection = new URL(updatedUrl).openConnection();
        urlConnection.setConnectTimeout(10000);
        return urlConnection.getInputStream();
    }

    @Override
    public void remove(String deposit) {
        throw new UnsupportedOperationException("No removal for http supported.");
    }
    
    @Override
    public boolean canHandle(String deposit) {
        // Some old entries in the DB have leading and trailing whitespace that we need to trim off.
        String clean = deposit.trim();
        return clean.startsWith("http://") || clean.startsWith("https://");
    }
}
