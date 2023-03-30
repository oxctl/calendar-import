package uk.ac.ox.it.calendarimporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.utils.TenantProperties;

/**
 * This imports default data into the DB on startup.
 */
@Configuration
public class SetupDBConfig {
    
    private final Logger log = LoggerFactory.getLogger(SetupDBConfig.class);

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired(required = false)
    private TenantProperties tenantProperties;

    @Bean
    InitializingBean sendDatabase() {
        return () -> {
            if (tenantProperties != null) {
                tenantProperties
                        .getTenants()
                        .forEach(
                                tenant -> {
                                    if (tenantRepository.findByName(tenant.getName()).isEmpty()) {
                                        log.info("Adding {} (LTI ID: {}) to the database", tenant.getName(), tenant.getLtiClientId());
                                        tenantRepository.save(tenant);
                                    }
                                });
            }
        };
    }
}
