package uk.ac.ox.it.calendarimporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = "uk.ac.ox.it.calendarimporter")
@EnableCaching
public class CalendarImporterForCanvasApplication {

  public static void main(String[] args) {
    SpringApplication.run(CalendarImporterForCanvasApplication.class, args);
  }

}
