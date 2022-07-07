package uk.ac.ox.it.calendarimporter;


import org.apache.commons.text.StringSubstitutor;

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * This service expands any variables in URL and opens a connection to the service.
 * It does more than just the variable expansion as it needs add the basic authentication onto the connection 
 * when it's needed.
 */
public class URLMapper {

    /*
     * This is map of URL -> config. This is so that we can support config for specific URLs.
     */
    final private Map<String, Config> known;
    final private CalendarTemplateReplacements templateReplacements;

    public URLMapper(Map<String, Config> known, CalendarTemplateReplacements templateReplacements) {
        this.known = known;
        this.templateReplacements = templateReplacements;
    }

    public URLConnection open(String url) throws IOException {
        if (url.startsWith("calendar://")) {
            return openCalendarConnection(url.substring("calendar://".length()));
        }
        return openConnection(url);
    }

    protected URLConnection openCalendarConnection(String host) throws IOException {
        Config config = known.get(host);
        if (config == null) {
            throw new UnknownHostException("Unknown host: " + host);
        }
        final URLConnection urlConnection = openConnection(config.url);
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
        return urlConnection;
    }
    
    private URLConnection openConnection(String url) throws IOException {
        Map<String, String> replacements = templateReplacements.getReplacements();
        StringSubstitutor substitutor = new StringSubstitutor(replacements);
        String expandedTemplate = substitutor.replace(url);
        return new URL(expandedTemplate).openConnection();
    }

    public static final class Config {
        private String url;
        public String username;
        public String password;
        
        public Config() {
        }

        public Config(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

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