package uk.ac.ox.it.calendarimporter.controller;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;

import java.util.List;
import java.util.Optional;

/**
 * Attempts to resolve the tenant from the JWT Authentication associated with the request.
 */
public class TenantArgumentResolver implements HandlerMethodArgumentResolver {

    private final TenantRepository tenantRepository;

    public TenantArgumentResolver(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameter().getType().equals(Tenant.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) authentication;
            List<String> audience = jwtAuthenticationToken.getToken().getAudience();
            return audience.stream()
                    .map(tenantRepository::findByLtiClientId)
                    .flatMap(Optional::stream)
                    .findFirst()
                    .orElseThrow(
                            () ->
                                    new NotFoundException(
                                            "Failed to find tenant for: " + String.join(", ", audience)));
        }
        throw new IllegalStateException("No JwtAuthenticationToken found");
    }
}
