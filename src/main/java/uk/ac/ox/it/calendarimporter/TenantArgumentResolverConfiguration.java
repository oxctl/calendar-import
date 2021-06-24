package uk.ac.ox.it.calendarimporter;

import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.ac.ox.it.calendarimporter.controller.TenantArgumentResolver;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;

import java.util.List;

@Component
public class TenantArgumentResolverConfiguration implements WebMvcConfigurer {

    private final TenantRepository tenantRepository;

    public TenantArgumentResolverConfiguration(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new TenantArgumentResolver(tenantRepository));
    }
}
