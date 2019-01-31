package uk.ac.ox.it.calendarimporter;

import com.samskivert.mustache.Mustache;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mustache.MustacheEnvironmentCollector;
import org.springframework.cache.annotation.EnableCaching;
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
@EnableCaching
@EntityScan("uk.ac.ox.it.calendarimporter.persistence.model")
@SpringBootApplication
public class CalendarImporterForCanvasApplication {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private TenantRepository tenantRepository;

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
            Tenant tenant = new Tenant();
            tenant.setName("canvas");
            tenant.setUrl("https://oxeval.instructure.com/");
            tenant.setDisplayName("Oxford Evaluation");
            tenantRepository.save(tenant);
        };
    }

    @Bean()
    @Lazy
    public Mustache.Compiler mustacheCompiler( Mustache.TemplateLoader templateLoader, Environment environment) {

        MustacheEnvironmentCollector collector = new MustacheEnvironmentCollector();
        collector.setEnvironment(environment);

        return Mustache.compiler()
                .defaultValue("Some Default Value")
                .withLoader(templateLoader)
                .withCollector(collector);
    }
}
