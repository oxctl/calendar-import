package uk.ac.ox.it.calendarimporter.service;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Simple service that loads CSS from a URL, this is extracted out so it's simpler to make it
 * cacheable in the future. This currently isn't needed as we are just changing the JSON URL into a
 * CSS URL to load the brand CSS, but if that stops working we can switch back to this.
 */
@Service
public class CSSLoader {

  public Map loadJSON(String url) {
    // TODO User agent, accept headers
    RestTemplate restTemplate =
        new RestTemplateBuilder()
            .setReadTimeout(Duration.ofSeconds(5))
            .setConnectTimeout(Duration.ofSeconds(5))
            .build();
    ResponseEntity<Map> jsonResponse = restTemplate.getForEntity(url, Map.class);
    Map body = jsonResponse.getBody();
    return body;
  }
}
