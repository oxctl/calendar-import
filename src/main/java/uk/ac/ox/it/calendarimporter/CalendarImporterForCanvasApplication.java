package uk.ac.ox.it.calendarimporter;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.utils.TenantProperties;

@EnableJpaRepositories("uk.ac.ox.it.calendarimporter.persistence.repo")
@EntityScan({"uk.ac.ox.it.calendarimporter.persistence.model"})
@SpringBootApplication(scanBasePackages = "uk.ac.ox.it.calendarimporter")
public class CalendarImporterForCanvasApplication {

  @Autowired private ApplicationEventPublisher applicationEventPublisher;

  @Autowired private TenantRepository tenantRepository;

  @Autowired(required = false)
  private TenantProperties tenantProperties;

  public static void main(String[] args) {
    SpringApplication.run(CalendarImporterForCanvasApplication.class, args);
  }

  @Bean
  public AuthenticationEventPublisher authenticationEventPublisher() {
    return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
  }

  @Bean
  InitializingBean sendDatabase() {
    return () -> {
      if (tenantProperties != null) {
        tenantProperties
            .getTenants()
            .forEach(
                tenant -> {
                  if (tenantRepository.findByName(tenant.getName()).isEmpty()) {
                    tenantRepository.save(tenant);
                  }
                });
      }
    };
  }
}
