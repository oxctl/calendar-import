package uk.ac.ox.it.calendarimporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// This is outside the main WebMvcConfigurer so that it doesn't get setup for tests.
@Configuration
public class ErrorConfiguration {

  @Autowired private ServerProperties serverProperties;

  @Bean
  public CustomErrorAttributes errorAttributes() {
    return new CustomErrorAttributes(this.serverProperties.getError().isIncludeException());
  }
}
