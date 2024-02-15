package uk.ac.ox.it.calendarimporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withJwkSetUri;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(RoleMappingConfiguration.class)
public class ApiWebSecurityConfig {

    private final Logger log = LoggerFactory.getLogger(ApiWebSecurityConfig.class);

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.jwks.uri}")
    private String jwksUri;

    @Value("${jwt.audience:}")
    private String audience;

    @Value("${frontend.origins}")
    private String[] origins;

    @Autowired
    private RoleMappingConfiguration roleMappingConfiguration;

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        Converter<Jwt, Collection<GrantedAuthority>> grantedAuthoritiesConverter = new CustomAuthorityMappingConverter(roleMappingConfiguration.getMapping());
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        http.securityMatcher("/api/**")
            .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> authorizationManagerRequestMatcherRegistry.anyRequest().authenticated())
            .cors(cors -> cors.configure(http))
            // It's only safe to do this when also making the application stateless (no cookies)    
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            .oauth2ResourceServer(oauth2ResourceServer -> {
                oauth2ResourceServer.jwt(jwt -> {
                    jwt.jwtAuthenticationConverter(jwtConverter);
                });
            });

        return http.build();
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        // This is so that we can allow downloads to work.
        resolver.setAllowUriQueryParameter(true);
        return resolver;
    }

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        // See org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer
        // We should do some validation here that the configuration is good.
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());
        if (audience != null && !audience.isEmpty()) {
            validators.add(new JwtAudienceValidator(audience));
        } else {
            log.warn("No audience configured, accepting all JWTs");
        }
        validators.add(new JwtIssuerValidator(issuer));
        DelegatingOAuth2TokenValidator<Jwt> jwtValidators =
                new DelegatingOAuth2TokenValidator<>(validators);
        NimbusJwtDecoder jwtDecoder = withJwkSetUri(jwksUri).build();
        jwtDecoder.setJwtValidator(jwtValidators);
        return jwtDecoder;
    }

    @Bean("corsConfigurationSource")
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        for (String origin : origins) {
            corsConfiguration.addAllowedOriginPattern(origin);
        }
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedMethods(
                Arrays.asList(
                        HttpMethod.GET.name(),
                        HttpMethod.HEAD.name(),
                        HttpMethod.DELETE.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.PATCH.name(),
                        HttpMethod.POST.name()));
        corsConfiguration.addAllowedHeader(CorsConfiguration.ALL);
        // On simple requests we want to expose the Link header
        corsConfiguration.addExposedHeader("Link");
        corsConfiguration.addExposedHeader("Content-Disposition");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", corsConfiguration);
        return source;
    }
}
