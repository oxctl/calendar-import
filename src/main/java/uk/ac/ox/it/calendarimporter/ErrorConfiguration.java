package uk.ac.ox.it.calendarimporter;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ox.it.calendarimporter.controller.CustomErrorController;

// This is outside the main WebMvcConfigurer so that it doesn't get setup for tests.
@Configuration
public class ErrorConfiguration {

  @Autowired private ServerProperties serverProperties;

  @Autowired private List<ErrorViewResolver> errorViewResolvers;

  @Bean
  public BasicErrorController basicErrorController(ErrorAttributes errorAttributes) {
    return new CustomErrorController(
        errorAttributes, this.serverProperties.getError(), this.errorViewResolvers);
  }

  @Bean
  public CustomErrorAttributes errorAttributes() {
    return new CustomErrorAttributes(this.serverProperties.getError().isIncludeException());
  }
}
