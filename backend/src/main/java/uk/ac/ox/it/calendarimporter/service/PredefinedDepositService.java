package uk.ac.ox.it.calendarimporter.service;

import jakarta.annotation.PostConstruct;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.CalendarUrlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * This deposit service is used for calendar:// URIs that are expanded to actual URLs.
 * For these configured URLs we can then add basic authentication.
 * We don't add basic auth to any other URLs so that there's no way we leak the credentials.
 */
@Service
@Qualifier("implementation")
@Order(100)
public class PredefinedDepositService implements DepositService {
    
    private final Logger log = LoggerFactory.getLogger(PredefinedDepositService.class);

    /*
     * This is map of URL -> config. This is so that we can support config for specific URLs.
     */
    @Autowired
    private CalendarUrlConfiguration calendarUrlConfiguration;
    
    @PostConstruct
    public void init() {
        log.info("PredefinedDepositService initialized with: {} configurations", calendarUrlConfiguration.getPredefined().size());
    }
    
    @Override
    public String deposit(File file, Type type) throws IOException {
        throw new UnsupportedOperationException("No uploading for calendar:// supported.");
    }

    @Override
    public InputStream getInputStream(String deposit, Map<String, String> parameters) throws IOException {
        URI uri = URI.create(deposit);
        if (!canHandle(deposit)) {
            // Put the double check in here so that this doesn't ever become the vector to allowing file://
            // URLs to be accessed.
            throw new IllegalArgumentException("Only calendar:// supported.");
        }
        CalendarUrlConfiguration.Config config = calendarUrlConfiguration.getPredefined().get(uri.getHost());
        if (config == null) {
            throw new UnknownHostException("Unknown host: " + (uri.getHost()));
        }
        String url = config.getUrl();
        StringSubstitutor substitutor = new StringSubstitutor(parameters);
        String updatedUrl = substitutor.replace(url);
        URLConnection urlConnection = new URL(updatedUrl).openConnection();
        if (urlConnection instanceof HttpURLConnection httpURLConnection) {
            if (
                    config.username != null && !config.username.isEmpty() &&
                            config.password != null && !config.password.isEmpty()
            ) {
                // We only want to add the authenticator here so that if you hardcode a URL we don't add the
                // authenticator to it.
                httpURLConnection.setAuthenticator(new BasicAuthenticator(config.username, config.password));
            }
        }
        urlConnection.setConnectTimeout(10000);
        return urlConnection.getInputStream();
    }

    @Override
    public void remove(String deposit) {
        throw new UnsupportedOperationException("No removal for calendar:// supported.");
    }

    @Override
    public boolean canHandle(String deposit) {
        URI uri = URI.create(deposit);
        return ("calendar".equals(uri.getScheme()));
    }

    /**
     * Simple basic authenticator for HTTP connections.
     */
    private static final class BasicAuthenticator extends Authenticator {
        private final String username;
        private final String password;

        private BasicAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }
}
