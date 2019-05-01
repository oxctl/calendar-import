package uk.ac.ox.it.calendarimporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;

@SpringBootApplication(scanBasePackages = "uk.ac.ox.it.calendarimporter")
@EnableCaching
public class CalendarImporterForCanvasApplication {

  @Autowired private ApplicationEventPublisher applicationEventPublisher;

  public static void main(String[] args) {
    SpringApplication.run(CalendarImporterForCanvasApplication.class, args);
  }

  @Bean
  public AuthenticationEventPublisher authenticationEventPublisher() {
    return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
  }
}
