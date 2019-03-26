package uk.ac.ox.it.calendarimporter;

import com.samskivert.mustache.Mustache;
import java.util.Optional;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mustache.MustacheEnvironmentCollector;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;

@EnableJpaRepositories("uk.ac.ox.it.calendarimporter.persistence.repo")
@EntityScan({"uk.ac.ox.it.calendarimporter.persistence.model"})
@SpringBootApplication(scanBasePackages = "uk.ac.ox.it.calendarimporter")
public class CalendarImporterForCanvasApplication {

  @Autowired private ApplicationEventPublisher applicationEventPublisher;

  @Autowired private TenantRepository tenantRepository;

  public static void main(String[] args) {
    SpringApplication.run(CalendarImporterForCanvasApplication.class, args);
  }

  @Bean
  public AuthenticationEventPublisher authenticationEventPublisher() {
    return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
  }

  @Bean
  InitializingBean sendDatabase() {
    // TODO Pull from config and also load LTI details here.
    return () -> {
      tenantRepository
          .findByName("canvas")
          .or(
              () -> {
                Tenant tenant = new Tenant();
                tenant.setName("canvas");
                tenant.setUrl("https://oxeval.instructure.com/");
                tenant.setDisplayName("Oxford Evaluation");
                return Optional.of(tenantRepository.save(tenant));
              });
    };
  }

  @Bean()
  @Lazy
  public Mustache.Compiler mustacheCompiler(
      Mustache.TemplateLoader templateLoader, Environment environment) {

    MustacheEnvironmentCollector collector = new MustacheEnvironmentCollector();
    collector.setEnvironment(environment);

    return Mustache.compiler()
        .defaultValue("Some Default Value")
        .withLoader(templateLoader)
        .withCollector(collector);
  }
}
