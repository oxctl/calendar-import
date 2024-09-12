package uk.ac.ox.it.calendarimporter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "calendar.url")
public class CalendarUrlConfiguration {

    /**
     * These are the predefined calendars that we expand.
     */
    private final Map<String, Config> predefined = new HashMap<>();

    public Map<String, Config> getPredefined() {
        return predefined;
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
}
