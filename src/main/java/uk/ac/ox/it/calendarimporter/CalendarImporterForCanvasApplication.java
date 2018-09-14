package uk.ac.ox.it.calendarimporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories("uk.ac.ox.it.calendarimporter.persistence.repo")
@EntityScan("uk.ac.ox.it.calendarimporter.persistence.model")
@SpringBootApplication
public class CalendarImporterForCanvasApplication {

	public static void main(String[] args) {
		SpringApplication.run(CalendarImporterForCanvasApplication.class, args);
	}
}
